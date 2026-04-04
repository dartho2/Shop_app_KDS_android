# 📋 Plan Reorganizacji Struktury - Wykonanie

**Status:** ⏳ W TRAKCIE - Wykonuję pozostałe zadania...

## Etap 1: Naprawienie literówek ✅

### 1.1 ui/utili → ui/util
- [x] MapUtils.kt ✅ (package zmieniony)
- [x] LogTimeFormatter.kt ✅ (package zmieniony)
- [x] LocationUtils.kt ✅ (package zmieniony)
- ⚠️ WYMAGA: Fizyczne przeniesienie folderu w Android Studio

### 1.2 data/entity/datebase → data/entity/database
- [ ] AppDatabase.kt ⏳ (w trakcie)
- ⚠️ WYMAGA: Fizyczne przeniesienie folderu w Android Studio

## Etap 2: Przeniesienie Repository

### Z ui/product/ do data/repository/
- [ ] ProductsRepository.kt
- [ ] ProductsViewModel.kt (ZOSTAJE w ui/)

### Z ui/settings/ do data/repository/
- [ ] SettingsRepository.kt

### Z ui/vehicle/ do data/repository/
- [ ] VehicleRepository.kt

## Etap 3: Przeniesienie API

### Z ui/product/ do data/network/
- [ ] ProductApi.kt

### Z ui/vehicle/ do data/network/
- [ ] VehicleApi.kt

## Etap 4: Dodanie Extension Functions

### Utworzenie util/extensions/
- [ ] StringExtensions.kt (NOWY)

## Etap 5: Dodanie Constants

- [ ] util/Constants.kt (NOWY)

## Etap 6: Dodanie .editorconfig

- [ ] .editorconfig (NOWY w root)

---

## Uwagi:
- Każda zmiana wymaga aktualizacji importów
- Android Studio może automatycznie zaktualizować niektóre importy
- Po zakończeniu uruchomić: Build → Rebuild Project

