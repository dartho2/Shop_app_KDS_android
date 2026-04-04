# 📊 Metryki Jakości Kodu - Obecny Stan i Cele

## 🎯 Obecny Stan (Styczeń 2025)

### Struktura Pakietów
- ❌ **Spójność:** 4/10
  - Mieszana lokalizacja Repository (niektóre w `ui/`, inne w `data/`)
  - Brak warstwy Domain
  - Literówki w nazwach pakietów

### Rozmiery Plików
- ⚠️ **OrdersViewModel.kt:** ~900 linii
  - **Cel:** Max 300 linii na ViewModel
  - **Akcja:** Podzielić na 3-4 mniejsze ViewModels

- ⚠️ **AcceptOrderSheetContent.kt:** ~600 linii
  - **Cel:** Max 300 linii na Composable
  - **Akcja:** Wydzielić komponenty

### Duplikacja Kodu
- ❌ **Estimated Duplication:** 15-20%
  - Powtarzające się wzorce obsługi błędów
  - Duplikacja `?.orEmpty()` / `?: "—"`
  - **Cel:** < 5% duplikacji

### Testowanie
- ❌ **Test Coverage:** ~0%
  - Brak testów jednostkowych dla ViewModels
  - Brak testów dla Use Cases (bo ich nie ma)
  - **Cel:** > 70% coverage dla logiki biznesowej

### Dokumentacja
- ❌ **KDoc Coverage:** ~10%
  - Większość publicznych API bez dokumentacji
  - **Cel:** 100% KDoc dla public API

### Warnings/Errors
- ⚠️ **Compiler Warnings:** ~50+
  - Unused imports
  - Unnecessary safe calls
  - Deprecated APIs
  - **Cel:** 0 warnings

---

## 📈 Cele na Q1 2025

### Tydzień 1-2: Quick Wins
- [x] Naprawić literówki w nazwach
- [ ] Dodać extension functions
- [ ] Usunąć nieużywane kody
- [ ] Naprawić wszystkie warnings
- **Target:** Warnings: 0, Duplication: 12%

### Tydzień 3-4: Warstwa Domain
- [ ] Utworzyć domain models
- [ ] Utworzyć repository interfaces
- [ ] Utworzyć pierwsze Use Cases
- **Target:** 10 Use Cases, 50% test coverage

### Tydzień 5-6: Refactor ViewModels
- [ ] Podzielić OrdersViewModel
- [ ] Wprowadzić sealed classes dla stanów
- [ ] Dodać testy
- **Target:** Max 300 linii/VM, 70% test coverage

### Tydzień 7-8: Infrastruktura
- [ ] Przenieść Services
- [ ] Oddzielić logikę biznesową
- [ ] Code review
- **Target:** Clean architecture, 80% coverage

---

## 🔍 Narzędzia do Monitorowania

### 1. Detekt (Static Analysis)
```gradle
// build.gradle
plugins {
    id 'io.gitlab.arturbosch.detekt' version '1.23.4'
}

detekt {
    config = files("config/detekt.yml")
    buildUponDefaultConfig = true
}
```

**Metryki:**
- Złożoność cyklomatyczna
- Długość metod/klas
- Zagnieżdżenie
- Duplikacja kodu

### 2. SonarQube (Code Quality)
```bash
./gradlew sonarqube \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=admin
```

**Metryki:**
- Code smells
- Bugs
- Vulnerabilities
- Technical debt

### 3. JaCoCo (Test Coverage)
```gradle
apply plugin: 'jacoco'

jacoco {
    toolVersion = "0.8.10"
}

tasks.withType(Test) {
    jacoco.includeNoLocationClasses = true
}
```

### 4. Android Lint
```bash
./gradlew lint
```

**Sprawdza:**
- Potential bugs
- Performance issues
- Accessibility
- Internationalization

---

## 📊 Dashboard Metryk (Propozycja)

```
┌─────────────────────────────────────────────────────────┐
│ ItsOrderChat - Code Quality Dashboard                  │
├─────────────────────────────────────────────────────────┤
│                                                         │
│ 📦 Struktura                                           │
│   ├─ Spójność pakietów:        ████░░░░░░ 40%         │
│   ├─ Separacja warstw:         ██░░░░░░░░ 20%         │
│   └─ Modularyzacja:            █░░░░░░░░░ 10%         │
│                                                         │
│ 🧪 Testowanie                                          │
│   ├─ Unit Test Coverage:       ░░░░░░░░░░  0%         │
│   ├─ Integration Tests:        ░░░░░░░░░░  0%         │
│   └─ UI Tests:                 ░░░░░░░░░░  0%         │
│                                                         │
│ 📝 Dokumentacja                                        │
│   ├─ KDoc Coverage:            █░░░░░░░░░ 10%         │
│   ├─ README:                   ████████░░ 80%         │
│   └─ Architecture Docs:        ░░░░░░░░░░  0%         │
│                                                         │
│ ⚠️  Jakość Kodu                                        │
│   ├─ Code Smells:              🔴 150+                 │
│   ├─ Duplikacja:               🟡 15%                  │
│   ├─ Złożoność:                🟡 Medium               │
│   └─ Warnings:                 🔴 50+                  │
│                                                         │
│ 📈 Trend (7 dni)                                       │
│   ├─ Commits:                  ↗️  +12                 │
│   ├─ Test Coverage:            →  0%                   │
│   ├─ Code Smells:              ↗️  +5                  │
│   └─ Tech Debt:                ↗️  +2h                 │
│                                                         │
└─────────────────────────────────────────────────────────┘

Legenda: 🔴 Krytyczne  🟡 Do poprawy  🟢 OK
```

---

## 🎯 Definicja Sukcesu

### Po 2 miesiącach (Marzec 2025):
```
✅ Struktura pakietów zgodna z Clean Architecture
✅ ViewModels < 300 linii każdy
✅ 70%+ test coverage dla logiki biznesowej
✅ 0 compiler warnings
✅ 100% KDoc dla public API
✅ < 5% duplikacji kodu
✅ Wszystkie Use Cases przetestowane
✅ CI/CD pipeline z automatycznymi testami
```

### Długoterminowe (6+ miesięcy):
```
✅ 90%+ test coverage
✅ Modularyzacja aplikacji (feature modules)
✅ Automatyczne deployments
✅ Performance monitoring
✅ Crash-free rate > 99.5%
✅ Build time < 2 min
```

---

## 📋 Raportowanie

### Cotygodniowy Raport
```markdown
# Code Quality Report - Week 1/2025

## Zmiany
- ✅ Naprawiono literówki w nazwach pakietów
- ✅ Dodano 15 extension functions
- ⚠️ W trakcie: Podział OrdersViewModel

## Metryki
- Test Coverage: 0% → 15% (+15%)
- Warnings: 52 → 35 (-17)
- Code Smells: 150 → 142 (-8)

## Następne Kroki
- [ ] Dokończyć podział ViewModels
- [ ] Dodać testy dla Use Cases
- [ ] Code review z zespołem
```

---

## 🔧 Automatyzacja

### GitHub Actions Workflow
```yaml
name: Code Quality Check

on: [pull_request]

jobs:
  quality:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Run Lint
        run: ./gradlew lint
        
      - name: Run Detekt
        run: ./gradlew detekt
        
      - name: Run Tests
        run: ./gradlew test
        
      - name: Generate Coverage Report
        run: ./gradlew jacocoTestReport
        
      - name: Upload Coverage
        uses: codecov/codecov-action@v3
        
      - name: Fail if Coverage < 70%
        run: |
          coverage=$(cat build/reports/jacoco/test/html/index.html | grep -oP '\d+%' | head -1)
          if [ "${coverage%?}" -lt 70 ]; then
            echo "Coverage ${coverage} is below 70%"
            exit 1
          fi
```

---

**Last Updated:** 2025-01-03  
**Next Review:** 2025-01-10

