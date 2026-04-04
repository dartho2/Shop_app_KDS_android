# ⚙️ Ustawienia Drukowania - Instrukcja

## Przegląd

Ustawienia drukowania pozwalają skonfigurować:
1. Automatyczne drukowanie po zaakceptowaniu zamówienia
2. Automatyczne drukowanie na drukarce kuchennej
3. Listę dostępnych drukarek

## Gdzie Znaleźć Ustawienia?

1. Otwórz aplikację ItsOrderCart
2. Przejdź do **Ustawienia** (menu główne)
3. Kliknij **"Drukarki"**

## G��ówne Funkcjonalności

### 1. Drukowanie po zaakceptowaniu

**Przycisk: "Auto Print - Zaakceptowane"**

Jeśli włączone (ON):
- Po zaakceptowaniu zamówienia drukuje się automatycznie
- Drukuje się na drukarce **STANDARD**
- Format: Pełny paragon

Jeśli wyłączone (OFF):
- Brak automatycznego drukowania
- Musisz ręcznie kliknąć "Drukuj"

### 2. Drukowanie na kuchni

**Przycisk: "Auto Print - Kuchnia"**

Jeśli włączone (ON):
- Po zaakceptowaniu drukuje się RÓWNIEŻ na drukarce **KITCHEN**
- Format: Kompaktowy bilet kuchenny
- Drukuje się dodatkowo, nie zamiast standardowego

Jeśli wyłączone (OFF):
- Drukuje się tylko na drukarce standardowej (jeśli ta opcja jest włączona)

**Ważne:** Ta opcja działa TYLKO jeśli masz skonfigurowaną drukarkę typu KITCHEN!

### 3. Zarządzanie drukarkami

**Przycisk: "Drukarki"**

Przechodzi do listy drukarek gdzie możesz:
- Dodać nową drukarkę
- Edytować istniejące drukarki
- Usunąć drukarki
- Testować wydruk
- Sprawdzać diagnostykę

## Przepływ Drukowania

```
Zamówienie dotarło
    ↓
Klikasz "Zaakceptuj"
    ↓
[Auto Print - Zaakceptowane = ON]?
    ├─ TAK: Drukuje na drukarce STANDARD
    │   ↓
    │   [Auto Print - Kuchnia = ON]?
    │   ├─ TAK: Drukuje RÓWNIEŻ na drukarce KITCHEN
    │   └─ NIE: Koniec
    └─ NIE: Brak automatycznego drukowania
```

## Troubleshooting

### "Brak skonfigurowanej drukarki"
- Przejdź do **Drukarki** → **+Dodaj drukarkę**
- Skonfiguruj drukarkę (Bluetooth, Sieć, lub Wbudowana)
- Włącz drukarkę (przycisk ON)
- Uruchom test drukowania

### Auto Print nie działa
- Sprawdź czy przycisk jest ON (powinien być niebieski)
- Sprawdź czy drukarka jest skonfigurowana i włączona
- Przejdź do **Drukarki** i kliknij **Test Print**
- Sprawdź czy wydruk się pojawił

### Drukuje się tylko na jednej drukarce, a nie na obu
- Jeśli chcesz drukować na kuchni:
  - Musi być skonfigurowana drukarka z typem **KITCHEN**
  - Przycisk **Auto Print - Kuchnia** musi być ON
  - Oraz **Auto Print - Zaakceptowane** musi być ON

### Drukuje się na złej drukarce
- Przejdź do **Drukarki**
- Sprawdź typ każdej drukarki (STANDARD lub KITCHEN)
- Edytuj drukarkę i zmień typ jeśli trzeba

## Kodowanie i Codepage

W każdej drukarce można ustawić:

- **Encoding**: UTF-8 (domyślnie), Cp852 (dla polskich znaków), itp.
- **Codepage**: 13 dla CP852, null dla UTF-8

Jeśli znaki polskie drukują się jako krzaki:
1. Edytuj drukarkę
2. Zmień Encoding na **Cp852**
3. Zmień Codepage na **13**
4. Przetestuj

## Szablony Wydruku

Każda drukarka może używać innego szablonu:

- **TEMPLATE_STANDARD** - Pełny format paragon (standardowy)
- **TEMPLATE_COMPACT** - Kompaktowy format (kuchnia)

Dla drukarek KITCHEN domyślnie ustawiany jest TEMPLATE_COMPACT.

## Zaawansowane Opcje

### Automatyczne Cięcie Papieru

Jeśli drukarka to obsługuje, można włączyć automatyczne cięcie po wydruku:
- Edytuj drukarkę
- Przycisk **Auto Cut** = ON

### Profil Drukarki

Każda drukarka ma przypisany profil, który zawiera:
- Typ połączenia (Bluetooth, Sieć, Wbudowana)
- Encoding
- Codepage
- Inne ustawienia ESC/POS

## Po Co Resetować?

Jeśli coś pójdzie nie tak:
1. Przejdź do **Drukarki**
2. Usuń drukarkę (przycisk Usuń)
3. Dodaj ją ponownie
4. Skonfiguruj od nowa

Tym sposobem resetujesz wszystkie ustawienia drukarki.

## Wskazówki Pro

1. **Testuj regularnie** - Kliknij "Test Print" aby upewnić się że drukarka działa
2. **Oznacz druki** - W nazwie drukarki wskaż jej przeznaczenie (np. "Kuchnia-Błękitna")
3. **Przyporządkowanie drukarek** - STANDARD = Paragon dla klienta, KITCHEN = Bilet dla kuchni
4. **Opóźnienia** - System automatycznie czeka między drukami (BT potrzebuje czasu)


