# 📱 Dokumentacja Dolnej Belki Nawigacyjnej - Spis Treści

## 🎯 Przegląd

Dolna belka nawigacyjna (Bottom Navigation Bar) to kluczowy element interfejsu aplikacji dla pracowników restauracji. Ta dokumentacja zawiera kompletny przewodnik po jej funkcjonowaniu, implementacji i rozszerzaniu.

---

## 📚 Dokumenty

### 1. [DOLNA_BELKA_QUICK_START.md](./DOLNA_BELKA_QUICK_START.md) ⚡
**Dla kogo**: Deweloperzy potrzebujący szybkiego wprowadzenia

**Zawartość**:
- ✅ Podstawowe informacje o działaniu belki
- ✅ Szczegółowy opis 3 zakładek
- ✅ Przykłady użycia
- ✅ FAQ i rozwiązywanie problemów
- ✅ Ilustracje tekstowe

**Czas czytania**: ~15 minut

---

### 2. [DOKUMENTACJA_DOLNA_BELKA.md](./DOKUMENTACJA_DOLNA_BELKA.md) 📖
**Dla kogo**: Deweloperzy potrzebujący pełnej dokumentacji technicznej

**Zawartość**:
- ✅ Kompletna architektura systemu
- ✅ Szczegółowy przepływ danych
- ✅ Mechanizmy synchronizacji
- ✅ API i parametry komponentów
- ✅ Scenariusze testowe
- ✅ Best practices
- ✅ Historia zmian

**Czas czytania**: ~45 minut

---

### 3. [DOLNA_BELKA_DIAGRAMY.md](./DOLNA_BELKA_DIAGRAMY.md) 🎨
**Dla kogo**: Osoby uczące się wizualnie / prezentacje

**Zawartość**:
- ✅ Diagramy architektury
- ✅ Diagramy przepływu danych
- ✅ Diagramy sekwencji
- ✅ Wykresy stanów
- ✅ Wizualizacje UI
- ✅ Diagramy UML

**Czas czytania**: ~20 minut

---

## 🚀 Szybki Start

### Dla Nowych Deweloperów

Jeśli jesteś nowy w projekcie, zalecamy następującą ścieżkę:

```
1. DOLNA_BELKA_QUICK_START.md
   └─> Zrozumienie podstaw (15 min)
   
2. DOLNA_BELKA_DIAGRAMY.md
   └─> Zobaczenie jak to działa wizualnie (20 min)
   
3. DOKUMENTACJA_DOLNA_BELKA.md
   └─> Pogłębienie wiedzy technicznej (45 min)
```

### Dla Doświadczonych Deweloperów

Jeśli znasz już projekt i chcesz szybko dodać funkcjonalność:

```
1. DOLNA_BELKA_QUICK_START.md → Sekcja "Przykłady Użycia"
2. DOKUMENTACJA_DOLNA_BELKA.md → Sekcja "Rozszerzanie funkcjonalności"
```

---

## 📍 Pliki Źródłowe

### Główne komponenty w kodzie:

```
app/src/main/java/com/itsorderchat/ui/theme/home/
├── HomeActivity.kt          # Komponent StaffBottomBar (linie 1034-1096)
├── MainScreen.kt            # Definicja zakładek staffTabs (linie 1-95)
└── view/
    └── StaffView.kt         # Logika wyświetlania zawartości (linie 77-700)
```

### Zasoby tłumaczeń:

```
app/src/main/res/
├── values/strings.xml       # Polskie tłumaczenia
└── values-en/strings.xml    # Angielskie tłumaczenia
```

---

## 🔍 Co Znajdziesz w Dokumentacji?

### Architektura
- Model MVVM z jednokierunkowym przepływem danych
- Synchronizacja dwukierunkowa między belką a pagerem
- State hoisting w HomeActivity

### Komponenty
- `StaffBottomBar` - Komponent dolnej belki
- `StaffView` - Logika wyświetlania zawartości
- `OrderTab` - Model danych zakładki
- `staffTabs` - Konfiguracja 3 zakładek

### Funkcjonalności
- 3 zakładki: Zamówienia, Zakończone, Inne
- Wsparcie dla swipe gestures
- Zwijane sekcje w zakładkach
- Automatyczne tłumaczenia
- Material Design 3

### Integracje
- `OrdersViewModel` - Źródło danych
- `HorizontalPager` - Przewijanie zakładek
- `NavigationBar` - Material 3 component
- Jetpack Compose Navigation

---

## 🎓 Kluczowe Koncepcje

### 1. Stan hoistowany (Hoisted State)
```kotlin
// HomeActivity
var selectedStaffTabIndex by remember { mutableIntStateOf(0) }

// Przekazywany w dół do komponentów potomnych
StaffBottomBar(selectedTabIndex = selectedStaffTabIndex, ...)
StaffView(selectedTabIndex = selectedStaffTabIndex, ...)
```

### 2. Synchronizacja dwukierunkowa
```
StaffBottomBar ←──────────→ HomeActivity ←──────────→ StaffView
  (onClick)      selectedTabIndex      (swipe gesture)
```

### 3. Filtrowanie zamówień
```kotlin
// Każda zakładka definiuje swoje statusy
OrderTab(
    titleRes = R.string.order_newOrder,
    statuses = setOf(PROCESSING, ACCEPTED, OUT_FOR_DELIVERY)
)
```

---

## 🛠️ Typowe Zadania

### Jak dodać nową zakładkę?
Zobacz: `DOKUMENTACJA_DOLNA_BELKA.md` → Sekcja "Rozszerzanie funkcjonalności"

### Jak zmienić domyślną zakładkę?
Zobacz: `DOLNA_BELKA_QUICK_START.md` → Sekcja "FAQ"

### Jak debugować problemy?
Zobacz: `DOKUMENTACJA_DOLNA_BELKA.md` → Sekcja "Znane problemy i ograniczenia"

### Jak przetestować zmiany?
Zobacz: `DOKUMENTACJA_DOLNA_BELKA.md` → Sekcja "Testowanie"

---

## 📊 Statystyki Kodu

| Metryka | Wartość |
|---------|---------|
| Pliki źródłowe | 3 |
| Linie kodu (belka) | ~63 |
| Linie kodu (widok) | ~700 |
| Liczba zakładek | 3 |
| Liczba statusów zamówień | 5 |
| Wspierane języki | 2 (PL, EN) |

---

## 🔗 Powiązane Dokumenty

### W projekcie:
- `AIDL_FLOW_DIAGRAM.md` - Ogólny diagram przepływu
- `EXECUTIVE_SUMMARY.md` - Podsumowanie projektu
- `FINAL_STATUS.md` - Status implementacji

### Zewnętrzne zasoby:
- [Material Design 3 - Navigation Bar](https://m3.material.io/components/navigation-bar)
- [Jetpack Compose - HorizontalPager](https://developer.android.com/jetpack/compose/layouts/pager)
- [State Management in Compose](https://developer.android.com/jetpack/compose/state)

---

## 🤝 Kontrybutorzy

### Autorzy dokumentacji:
- AI Documentation Generator (2026-02-02)

### Code maintainers:
- Team ItsOrderChat

---

## 📝 Historia Wersji

| Wersja | Data | Zmiany |
|--------|------|--------|
| 1.0 | 2026-02-02 | Pierwsza wersja dokumentacji |
| - | - | • Utworzono 3 dokumenty |
| - | - | • Dodano diagramy i wizualizacje |
| - | - | • Dodano komentarze w kodzie |

---

## 💡 Wskazówki

### Dla czytających dokumentację:
1. Zacznij od Quick Start jeśli jesteś nowy
2. Użyj diagramów do wizualizacji przepływów
3. Przeczytaj pełną dokumentację dla szczegółów
4. Sprawdź kod źródłowy z komentarzami

### Dla edytujących kod:
1. Aktualizuj dokumentację przy zmianach
2. Dodawaj komentarze KDoc do nowych funkcji
3. Testuj wszystkie scenariusze po zmianach
4. Sprawdź czy tłumaczenia są kompletne

---

## 📞 Pomoc

### Masz pytania?
1. Sprawdź sekcję FAQ w `DOLNA_BELKA_QUICK_START.md`
2. Zobacz "Znane problemy" w `DOKUMENTACJA_DOLNA_BELKA.md`
3. Przejrzyj diagramy w `DOLNA_BELKA_DIAGRAMY.md`

### Znalazłeś błąd?
1. Sprawdź czy nie jest już znany
2. Dodaj do sekcji "Znane problemy"
3. Zaktualizuj dokumentację jeśli naprawiłeś

---

## ✅ Checklist dla Deweloperów

Przed wprowadzeniem zmian w dolnej belce:

- [ ] Przeczytałem Quick Start
- [ ] Zrozumiałem synchronizację dwukierunkową
- [ ] Sprawdziłem powiązane komponenty
- [ ] Przetestowałem na różnych zakładkach
- [ ] Zaktualizowałem tłumaczenia
- [ ] Dodałem/zaktualizowałem komentarze
- [ ] Przetestowałem swipe gestures
- [ ] Sprawdziłem tryb ciemny/jasny
- [ ] Zaktualizowałem dokumentację

---

**Ostatnia aktualizacja**: 2026-02-02  
**Wersja**: 1.0  
**Status**: ✅ Kompletna

