# 📊 Szybka Referencyjna Tabela Drukowania

> **⚠️ WAŻNE (v1.6):** Naprawiono duplikację drukowania przy zmianie statusu przez socket!  
> Gdy socket wysyła zmianę PROCESSING → ACCEPTED, drukuje się **tylko raz** (wcześniej 2x).  
> Więcej: `FIX_SOCKET_STATUS_DUPLICATION.md`
>
> **v1.4:** Naprawiono duplikację przy manualnej akceptacji - socket confirmation pomijany.  
> Więcej: `FIX_DOUBLE_PRINTING_MANUAL_ACCEPTANCE.md`

## 🎯 Kiedy Drukuje Się Zamówienie?

### DELIVERY / PICKUP / inne (nie DINE_IN)

| Auto-print | Po akcept. | Kuchnia | Scenariusz | Drukarki | Ile |
|------------|------------|---------|------------|----------|-----|
| ✅ ON | ❌ OFF | ❌ OFF | Nowe (PROCESSING) | Główna | **1** |
| ✅ ON | ❌ OFF | ❌ OFF | Nowe (ACCEPTED) | Główna | **1** |
| ❌ OFF | ✅ ON | ❌ OFF | Akceptacja manualna | Główna | **1** |
| ❌ OFF | ✅ ON | ❌ OFF | Nowe (ACCEPTED) | Główna | **1** |
| ❌ OFF | ✅ ON | ❌ OFF | Zmiana przez socket | Główna | **1** |
| ❌ OFF | ✅ ON | ✅ ON | Akceptacja manualna | Główna + Kuchnia | **2** |
| ❌ OFF | ✅ ON | ✅ ON | Nowe (ACCEPTED) | Główna + Kuchnia | **2** |
| ❌ OFF | ✅ ON | ✅ ON | Zmiana przez socket | Główna + Kuchnia | **2** |
| ✅ ON | ✅ ON | ❌ OFF | Nowe → Akcept. | Główna (2x) | **2** |
| ✅ ON | ✅ ON | ✅ ON | Nowe → Akcept. | Główna (2x) + Kuchnia (1x) | **3** |

---

### DINE_IN / ROOM_SERVICE

| Auto DINE_IN | Wybór | Po Akcept. | Kuchnia | Scenariusz | Drukarki | Ile |
|--------------|-------|------------|---------|------------|----------|-----|
| ✅ ON | Główna | ❌ OFF | ❌ OFF | Nowe | Główna | **1** |
| ✅ ON | Kuchnia | ❌ OFF | ❌ OFF | Nowe | Kuchnia | **1** |
| ✅ ON | Główna | ✅ ON | ❌ OFF | Nowe | Główna | **1** ⚠️ |
| ✅ ON | Kuchnia | ✅ ON | ❌ OFF | Nowe | Kuchnia | **1** ⚠️ |
| ✅ ON | Kuchnia | ✅ ON | ✅ ON | Nowe (nie akceptuj!) | Kuchnia | **1** |
| ✅ ON | Kuchnia | ✅ ON | ✅ ON | Nowe + Akcept. ręcznie | Kuchnia (1x) + Główna (1x) + Kuchnia (1x) | **3** ⚠️ |
| ❌ OFF | - | ✅ ON | ✅ ON | Akceptacja manualna | Główna + Kuchnia | **2** |
| ❌ OFF | - | ❌ OFF | ❌ OFF | - | - | **0** ❌
| ❌ OFF | - | ✅ ON | ✅ ON | Po zaakceptowaniu | Główna + Kuchnia | **2** |
| ❌ OFF | - | ❌ OFF | ❌ OFF | Nigdy | - | **0** ⚠️ |

---

## 🔑 Legenda

### Symbole
- ✅ = Włączone
- ❌ = Wyłączone
- ⚠️ = Ostrzeżenie (nieprawidłowa konfiguracja)

### Scenariusze DELIVERY/PICKUP
- **Nowe (PROCESSING)** - Zamówienie przychodzi ze statusem PROCESSING
- **Nowe (ACCEPTED)** - Zamówienie przychodzi już jako ACCEPTED (Uber, Glovo)
- **Akceptacja manualna** - Pracownik klika "Akceptuj" w aplikacji
- **Zmiana przez socket** - Backend zmienia status przez socket (zewnętrzny system)

### Scenariusze DINE_IN
- **Nowe** - Zamówienie DINE_IN przychodzi (auto-druk natychmiast)
- **Nowe (nie akceptuj!)** - DINE_IN nie wymaga akceptacji
- **Nowe + Akcept. ręcznie** - Jeśli mimo wszystko zaakceptujesz DINE_IN ręcznie (duplikacja!)

---

## 📋 Szczegóły Ustawień

| Nazwa Skrócona | Pełna Nazwa | Lokalizacja | Dotyczy |
|----------------|-------------|-------------|---------|
| **Auto-print** | Automatyczne drukowanie | Ustawienia → Drukowanie | DELIVERY, PICKUP (nie DINE_IN!) |
| **Po akcept.** | Drukuj po zaakceptowaniu | Ustawienia → Drukowanie | Wszystkie typy |
| **Auto DINE_IN** | Auto-drukuj zamówienia na miejscu | Ustawienia → Drukowanie | DINE_IN, ROOM_SERVICE |
| **Wybór** | Drukarka dla DINE_IN | Pod "Auto DINE_IN" | DINE_IN, ROOM_SERVICE |
| **Kuchnia** | Drukowanie na kuchni | Ustawienia → Drukowanie | Tylko z "Po akcept." |

---

## 📖 Szczegółowe Wyjaśnienie Scenariuszy

### 💡 Kluczowe Reguły

1. **DINE_IN/ROOM_SERVICE są WYKLUCZENI z "Auto-print"**
   - Nie drukują się przez "Automatyczne drukowanie"
   - Tylko przez "Auto DINE_IN" lub "Po akcept."

2. **"Po akcept." działa na 3 sposoby:**
   - Manualna akceptacja (kliknięcie w UI) → DRUKUJE
   - Zamówienie przychodzi już ACCEPTED → DRUKUJE
   - Socket zmienia status na ACCEPTED → DRUKUJE

3. **Ochrona przed duplikacją:**
   - Manualna akceptacja + socket confirmation → DRUKUJE TYLKO RAZ (fix v1.4)
   - Set `manuallyAcceptedOrders` zapobiega duplikacji

### 🎯 Scenariusze DELIVERY Krok Po Kroku

#### Scenariusz A: Auto-print ON, Po akcept. OFF
```
1. Nowe PROCESSING → ✅ DRUKUJE (główna)
2. Pracownik akceptuje → ❌ Nie drukuje (Po akcept. = OFF)
= 1 wydruk
```

#### Scenariusz B: Auto-print OFF, Po akcept. ON
```
1. Nowe PROCESSING → ❌ Nie drukuje (Auto-print = OFF)
2. Pracownik akceptuje → ✅ DRUKUJE (główna)
   - Socket confirmation POMIJANY (fix v1.4)
= 1 wydruk
```

#### Scenariusz C: Auto-print OFF, Po akcept. ON, Nowe ACCEPTED
```
1. Nowe ACCEPTED (z Uber) → ✅ DRUKUJE (główna)
   - observeSocketEvents() wykrywa ACCEPTED
= 1 wydruk
```

#### Scenariusz D: Auto-print OFF, Po akcept. ON, Socket zmienia
```
1. Nowe PROCESSING → ❌ Nie drukuje
2. Socket: status → ACCEPTED → ✅ DRUKUJE (główna)
   - socketEventsRepo.statuses wykrywa zmianę
= 1 wydruk
```

#### Scenariusz E: Auto-print ON, Po akcept. ON (DUPLIKACJA ZAMIERZONA)
```
1. Nowe PROCESSING → ✅ DRUKUJE #1 (główna)
2. Pracownik akceptuje → ✅ DRUKUJE #2 (główna)
   - Socket confirmation POMIJANY (fix v1.4)
= 2 wydruki (dwa różne momenty - zamierzone)
```

### 🍽️ Scenariusze DINE_IN Krok Po Kroku

#### Scenariusz F: Auto DINE_IN ON (Kuchnia)
```
1. Nowe DINE_IN → ✅ DRUKUJE (kuchnia)
   - OrdersViewModel.observeSocketEvents()
= 1 wydruk
```

#### Scenariusz G: Auto DINE_IN ON + Po akcept. ON (NIE AKCEPTUJ!)
```
1. Nowe DINE_IN → ✅ DRUKUJE (kuchnia)
2. NIE akceptuj ręcznie! (DINE_IN nie wymaga akceptacji)
= 1 wydruk ✅
```

#### Scenariusz H: Auto DINE_IN ON + Akceptacja ręczna (BŁĄD!)
```
1. Nowe DINE_IN → ✅ DRUKUJE #1 (kuchnia)
2. Pracownik akceptuje → ✅ DRUKUJE #2 (główna + kuchnia)
= 3 wydruki ⚠️ (nieprawidłowe - DINE_IN nie wymaga akceptacji!)
```

---

## ⚡ Najczęstsze Scenariusze

### ✅ REKOMENDOWANE

#### Scenariusz 1: Restauracja z Kuchnią
```
❌ Auto-print       → Nie drukuj automatycznie nowych
✅ Po akcept.       → Drukuj po zaakceptowaniu
✅ Auto DINE_IN     → Drukuj DINE_IN automatycznie
   └─ Kuchnia       → Na drukarce kuchennej
✅ Kuchnia          → Dodatkowo kuchnia po akceptacji

Rezultat DELIVERY: 1x Główna + 1x Kuchnia (po akceptacji)
Rezultat DINE_IN: 1x Kuchnia (natychmiast)
```

#### Scenariusz 2: Mały Lokal (1 drukarka)
```
✅ Auto-print       → Drukuj wszystkie nowe
❌ Po akcept.       → Nie duplikuj
✅ Auto DINE_IN     → Drukuj DINE_IN
   └─ Główna        → Na głównej drukarce
❌ Kuchnia          → Brak drukarki kuchennej

Rezultat DELIVERY: 1x Główna (natychmiast)
Rezultat DINE_IN: 1x Główna (natychmiast)
```

#### Scenariusz 3: Tylko Manualne
```
❌ Auto-print       → Czekaj na akceptację
✅ Po akcept.       → Drukuj po akcept.
❌ Auto DINE_IN     → DINE_IN też czeka
❌ Kuchnia          → Tylko główna

Rezultat DELIVERY: 1x Główna (po akceptacji)
Rezultat DINE_IN: 1x Główna (po akceptacji)
```

---

### ⚠️ NIEZALECANE (Duplikacje)

#### Konflikt A: Wszystko Włączone
```
✅ Auto-print       → +1 wydruk
✅ Po akcept.       → +2 wydruki
✅ Kuchnia          → (w ramach +2)

Rezultat DELIVERY: 3 WYDRUKI! (2x Główna + 1x Kuchnia)
Problem: Za dużo papieru!
```

#### Konflikt B: DINE_IN Duplikacja
```
✅ Auto-print       → (nie działa dla DINE_IN)
✅ Auto DINE_IN     → +1 wydruk
✅ Po akcept.       → +2 wydruki (jeśli zaakceptujesz)
✅ Kuchnia          → (w ramach +2)

Rezultat DINE_IN: 3 WYDRUKI! (jeśli zaakceptujesz ręcznie)
Problem: DINE_IN nie wymaga akceptacji!
```

---

## 🎬 Timeline Drukowania

### Przykład DELIVERY (Wszystko Włączone)

```
00:00 ─ Nowe zamówienie DELIVERY przychodzi (PROCESSING)
  │
  ├─► 00:01 ✅ Wydruk #1 (Auto-print) → GŁÓWNA
  │
05:00 ─ Pracownik akceptuje zamówienie (ACCEPTED)
  │
  ├─► 05:01 ✅ Wydruk #2 (Po akcept.) → GŁÓWNA
  │
  └─► 05:02 ✅ Wydruk #3 (Kuchnia) → KUCHNIA

Razem: 3 wydruki
```

### Przykład DINE_IN (Tylko Auto DINE_IN)

```
00:00 ─ Nowe zamówienie DINE_IN przychodzi (PROCESSING)
  │
  └─► 00:01 ✅ Wydruk #1 (Auto DINE_IN) → KUCHNIA

Razem: 1 wydruk
```

### Przykład DELIVERY (Przyszło Już Zaakceptowane)

```
00:00 ─ Nowe zamówienie DELIVERY przychodzi (już ACCEPTED - z Uber/Glovo)
  │
  └─► 00:01 ✅ Wydruk #1 (Auto-druk accepted) → GŁÓWNA
           └─► ✅ Wydruk #2 (Kuchnia, jeśli włączone) → KUCHNIA

Ustawienia: Po akcept. = ON, Kuchnia = ON
Razem: 2 wydruki (bez manualnej akceptacji!)
```

---

## 🔍 Diagnostyka Szybka

### Pytanie 1: Czy drukuje się wielokrotnie?

**TAK** → Sprawdź:
- [ ] Czy masz włączone "Auto-print" I "Po akcept."?
- [ ] Czy to DINE_IN i akceptujesz je ręcznie?
- [ ] Sprawdź logi `AUTO_PRINT_DINE_IN`

**Rozwiązanie:** Wyłącz jedną z opcji lub nie akceptuj DINE_IN ręcznie

---

### Pytanie 2: Czy nie drukuje się wcale?

**TAK** → Sprawdź:
- [ ] Czy drukarka jest włączona? (Zarządzanie → Drukarki)
- [ ] Czy masz włączoną przynajmniej jedną opcję auto-druku?
- [ ] Dla DINE_IN: Czy "Auto DINE_IN" jest ON?

**Rozwiązanie:** Włącz odpowiednie opcje lub drukarkę

---

### Pytanie 3: Czy drukuje na złej drukarce?

**TAK** → Sprawdź:
- [ ] DINE_IN: Wybór drukarki (Główna/Kuchnia) pod "Auto DINE_IN"
- [ ] Czy masz skonfigurowaną drukarkę kuchenną?

**Rozwiązanie:** Zmień wybór drukarki lub skonfiguruj drukarkę kuchenną

---

## 📞 Szybka Pomoc

### Chcę żeby DELIVERY drukował się 1 raz

```
OPCJA A: Przy nowym zamówieniu
✅ Auto-print
❌ Po akcept.
❌ Kuchnia

OPCJA B: Po zaakceptowaniu
❌ Auto-print
✅ Po akcept.
❌ Kuchnia
```

### Chcę żeby DINE_IN szedł od razu do kuchni

```
❌ Auto-print (nie wpływa)
✅ Auto DINE_IN → Kuchnia
❌ Po akcept. (jeśli nie akceptujesz)
```

### Chcę 2 wydruki dla DELIVERY (kasjer + kuchnia)

```
OPCJA A: Wszystko natychmiast
✅ Auto-print
❌ Po akcept.
+ RĘCZNIE: Wydrukuj na kuchni z menu

OPCJA B: Po zaakceptowaniu
❌ Auto-print
✅ Po akcept.
✅ Kuchnia
```

---

**Ostatnia aktualizacja:** 2026-02-04  
**Wersja:** 1.6 (po fixach duplikacji socket)  
**Pełna dokumentacja:** `DOKUMENTACJA_AUTOMATYCZNEGO_DRUKOWANIA.md`

## 📝 Historia Zmian

### v1.6 (2026-02-04)
- ✅ **NAPRAWIONO DUPLIKACJĘ:** Zmiana statusu przez socket (PROCESSING → ACCEPTED) drukuje tylko raz
- ✅ Synchroniczne dodawanie do `printedOrderIds` (przed viewModelScope.launch)
- ✅ Race condition handling - brak duplikacji między socketEventsRepo.orders i socketEventsRepo.statuses

### v1.5 (2026-02-04)
- ✅ Naprawiono drukowanie przy restarcie aplikacji
- ✅ Dodano `printedOrderIds` Set do śledzenia wydrukowanych zamówień

### v1.4 (2026-02-04)
- ✅ Naprawiono duplikację przy manualnej akceptacji
- ✅ Dodano ochronę `manuallyAcceptedOrders` Set
- ✅ Zaktualizowano wszystkie scenariusze
- ✅ Dodano szczegółowe wyjaśnienia krok po kroku

### v1.3 (2026-02-04)
- Dodano obsługę zewnętrznej akceptacji przez socket

### v1.2 (2026-02-04)
- Dodano obsługę zamówień przychodzących już jako ACCEPTED

### v1.1 (2026-02-03)
- Naprawiono duplikację DINE_IN
- Dodano wykluczenie DINE_IN z auto-print

