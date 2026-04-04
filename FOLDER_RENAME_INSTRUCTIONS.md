# 🔧 Instrukcja Krok po Kroku - Przeniesienie Folderów

**Cel:** Fizyczne przeniesienie folderów z literówkami  
**Czas:** 5-10 minut  
**Wymagane:** Android Studio

---

## 📋 Co Będziemy Przenosić

### 1. ui/utili → ui/util
**Lokalizacja:** `app/src/main/java/com/itsorderchat/ui/utili/`  
**Pliki w środku:**
- MapUtils.kt
- LogTimeFormatter.kt
- LocationUtils.kt

### 2. data/entity/datebase → data/entity/database
**Lokalizacja:** `app/src/main/java/com/itsorderchat/data/entity/datebase/`  
**Pliki w środku:**
- AppDatabase.kt

---

## 🎯 METODA 1: Refactor → Rename w Android Studio (ZALECANE)

### Krok 1: Przenieś ui/utili → ui/util

1. **Otwórz Android Studio**
   - Otwórz projekt: `L:\SHOP APP`

2. **Przejdź do folderu**
   - W **Project View** (po lewej stronie)
   - Rozwiń: `app` → `src` → `main` → `java` → `com` → `itsorderchat` → `ui`
   - Znajdź folder **`utili`**

3. **Refaktoryzuj nazwę**
   - **Kliknij PRAWYM** na folder `utili`
   - Wybierz: **Refactor** → **Rename** (lub naciśnij **Shift + F6**)
   - Pojawi się okno dialogowe

4. **Wpisz nową nazwę**
   - W polu tekstowym wpisz: **`util`** (bez cudzysłowów)
   - ✅ Upewnij się, że zaznaczone: **"Search in comments and strings"**
   - ✅ Upewnij się, że zaznaczone: **"Search for text occurrences"**

5. **Kliknij Refactor**
   - Android Studio pokaże podgląd zmian
   - Sprawdź czy wygląda OK (powinno pokazać zmiany w plikach)
   - Kliknij **"Do Refactor"**

6. **Poczekaj**
   - Android Studio automatycznie:
     - Zmieni nazwę folderu
     - Zaktualizuje wszystkie importy
     - Zaktualizuje package names
     - Zaktualizuje ścieżki w Gradle

✅ **Gotowe!** Folder `utili` jest teraz `util`

---

### Krok 2: Przenieś data/entity/datebase → data/entity/database

1. **Przejdź do folderu**
   - W **Project View**
   - Rozwiń: `app` → `src` → `main` → `java` → `com` → `itsorderchat` → `data` → `entity`
   - Znajdź folder **`datebase`**

2. **Refaktoryzuj nazwę**
   - **Kliknij PRAWYM** na folder `datebase`
   - Wybierz: **Refactor** → **Rename** (lub **Shift + F6**)

3. **Wpisz nową nazwę**
   - W polu tekstowym wpisz: **`database`**
   - ✅ Zaznacz: **"Search in comments and strings"**
   - ✅ Zaznacz: **"Search for text occurrences"**

4. **Kliknij Refactor**
   - Sprawdź podgląd zmian
   - Kliknij **"Do Refactor"**

✅ **Gotowe!** Folder `datebase` jest teraz `database`

---

### Krok 3: Rebuild Projektu

1. **Clean Project**
   - Menu: **Build** → **Clean Project**
   - Poczekaj aż się zakończy

2. **Rebuild Project**
   - Menu: **Build** → **Rebuild Project**
   - Poczekaj (~2-5 minut)

3. **Sprawdź błędy**
   - W dolnym panelu sprawdź zakładkę **Build**
   - Powinno być: **BUILD SUCCESSFUL**
   - Jeśli są błędy → zobacz sekcję "Troubleshooting" poniżej

---

### Krok 4: Weryfikacja

1. **Sprawdź strukturę**
   - W Project View sprawdź czy foldery mają poprawne nazwy:
     - ✅ `ui/util/` (nie `utili`)
     - ✅ `data/entity/database/` (nie `datebase`)

2. **Uruchom aplikację**
   - Kliknij **Run** (zielony trójkąt) lub **Shift + F10**
   - Aplikacja powinna się skompilować i uruchomić

3. **Przetestuj**
   - Otwórz kilka ekranów
   - Sprawdź czy wszystko działa

✅ **Jeśli aplikacja działa - SUKCES!**

---

## 🎯 METODA 2: Ręczne Przeniesienie (jeśli Refactor nie działa)

**⚠️ UWAGA:** Ta metoda jest bardziej ryzykowna!

### Krok 1: Zamknij Android Studio

### Krok 2: W Eksploratorze Windows:

1. **Przejdź do:**
   ```
   L:\SHOP APP\app\src\main\java\com\itsorderchat\ui\
   ```

2. **Zmień nazwę folderu:**
   - Kliknij prawym na `utili`
   - Wybierz **Rename**
   - Wpisz: `util`

3. **Przejdź do:**
   ```
   L:\SHOP APP\app\src\main\java\com\itsorderchat\data\entity\
   ```

4. **Zmień nazwę folderu:**
   - Kliknij prawym na `datebase`
   - Wybierz **Rename**
   - Wpisz: `database`

### Krok 3: Otwórz Android Studio

1. **Otwórz projekt**
2. **Gradle Sync**
   - Menu: **File** → **Sync Project with Gradle Files**
3. **Invalidate Caches**
   - Menu: **File** → **Invalidate Caches / Restart**
   - Wybierz: **Invalidate and Restart**

### Krok 4: Rebuild

```
Build → Clean Project
Build → Rebuild Project
```

---

## 🔧 Troubleshooting - Problemy i Rozwiązania

### Problem 1: "Cannot rename - folder is in use"

**Rozwiązanie:**
1. Zamknij Android Studio
2. Zamknij wszystkie terminale/command prompts
3. Spróbuj ponownie

---

### Problem 2: Po rebuild są błędy kompilacji

**Rozwiązanie:**
1. **Sprawdź importy ręcznie:**
   - Otwórz każdy plik z błędem
   - Naciśnij **Alt + Enter** na czerwonym kodzie
   - Wybierz **"Import"**

2. **Wyczyść cache:**
   ```
   File → Invalidate Caches / Restart
   ```

3. **Usuń build folder:**
   - Zamknij Android Studio
   - Usuń folder: `L:\SHOP APP\app\build\`
   - Otwórz Android Studio i rebuild

---

### Problem 3: Git pokazuje dużo zmian

**To normalne!** Jeśli używasz Git:

```bash
# Sprawdź zmiany
git status

# Powinno pokazać:
# renamed: ui/utili/... → ui/util/...
# renamed: data/entity/datebase/... → data/entity/database/...

# Commituj zmiany:
git add .
git commit -m "refactor: fix typos in folder names (utili→util, datebase→database)"
```

---

## ✅ Checklist - Co Zrobić po Przeniesieniu

- [ ] Foldery mają poprawne nazwy (`util`, `database`)
- [ ] Projekt się kompiluje (BUILD SUCCESSFUL)
- [ ] Aplikacja się uruchamia
- [ ] Przetestowano główne funkcje
- [ ] Zcommitowano zmiany do Git (jeśli używasz)

---

## 🗑️ Opcjonalnie: Usuń Stare Deprecated Pliki

Po weryfikacji, że wszystko działa, usuń 5 plików deprecated:

1. **W Android Studio - Project View:**
   - `ui/product/ProductsRepository.kt` → Kliknij prawym → Delete
   - `ui/product/ProductApi.kt` → Delete
   - `ui/settings/SettingsRepository.kt` → Delete
   - `ui/vehicle/VehicleRepository.kt` → Delete
   - `ui/vehicle/VehicleApi.kt` → Delete

2. **Rebuild:**
   ```
   Build → Rebuild Project
   ```

3. **Sprawdź czy wszystko działa**

---

## 🎯 Oczekiwany Rezultat

### Przed:
```
ui/
└── utili/              ❌ Literówka
    ├── MapUtils.kt
    ├── LogTimeFormatter.kt
    └── LocationUtils.kt

data/entity/
└── datebase/           ❌ Literówka
    └── AppDatabase.kt
```

### Po:
```
ui/
└── util/               ✅ Poprawnie
    ├── MapUtils.kt
    ├── LogTimeFormatter.kt
    └── LocationUtils.kt

data/entity/
└── database/           ✅ Poprawnie
    └── AppDatabase.kt
```

---

## 📞 Potrzebujesz Pomocy?

### Jeśli coś nie działa:

1. **Sprawdź Build Output:**
   - Android Studio → Build panel (na dole)
   - Szukaj konkretnych błędów

2. **Sprawdź Event Log:**
   - Android Studio → Event Log (prawy dolny róg)
   - Szukaj ostrzeżeń o Refactor

3. **Stack Overflow:**
   - Szukaj: "Android Studio rename package folder"

---

## ⏱️ Szacowany Czas

- **Refactor → Rename:** 2-3 minuty
- **Rebuild:** 2-5 minut
- **Weryfikacja:** 2-3 minuty
- **TOTAL:** 5-10 minut

---

## 🎉 Gratulacje!

Po wykonaniu tych kroków:
- ✅ Brak literówek w nazwach folderów
- ✅ Struktura profesjonalna
- ✅ Package names zgodne z folderami
- ✅ Kod gotowy do dalszego rozwoju

---

**Status:** 📋 Instrukcja gotowa  
**Następny krok:** Wykonaj Refactor → Rename w Android Studio  
**Powodzenia!** 🚀

