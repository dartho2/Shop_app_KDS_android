# 🚨 NAPRAWIONO: Błąd migracji bazy danych 21→22

## ❌ PROBLEM

```
java.lang.IllegalStateException: A migration from 21 to 22 was required but not found.
```

**Przyczyna**: Dodanie pola `couponTotalDiscount` do `OrderEntity` zwiększyło wersję bazy danych z 21 do 22, ale brakuje migracji.

---

## ✅ ROZWIĄZANIE - WYKONANE

### 1. Utworzono migrację w AppDatabase.kt

**Plik**: `l:\SHOP APP\app\src\main\java\com\itsorderchat\data\entity\database\AppDatabase.kt`

```kotlin
companion object {
    // MIGRACJA: dodanie kolumny 'type' (TEXT NULL)
    val MIGRATION_20_21 = object : Migration(20, 21) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE orders ADD COLUMN type TEXT")
        }
    }

    // MIGRACJA: dodanie kolumny 'couponTotalDiscount' (REAL NOT NULL DEFAULT 0.0)
    val MIGRATION_21_22 = object : Migration(21, 22) {  // ← NOWE
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE orders ADD COLUMN couponTotalDiscount REAL NOT NULL DEFAULT 0.0")
        }
    }
}
```

### 2. Dodano migrację do Room Database Builder

**Plik**: `l:\SHOP APP\app\src\main\java\com\itsorderchat\di\NetworkModule.kt`

```kotlin
Room.databaseBuilder(
    appContext, AppDatabase::class.java, "app.db"
)
    .addMigrations(AppDatabase.MIGRATION_20_21, AppDatabase.MIGRATION_21_22)  // ← DODANE
    .build()
```

### 3. Zwiększono wersję bazy danych

**Plik**: `AppDatabase.kt`

```kotlin
@Database(
    entities = [OrderEntity::class],
    version = 22,  // ← ZMIENIONE z 21 na 22
    exportSchema = false
)
```

---

## 🔧 JAK ZAINSTALOWAĆ NOWĄ WERSJĘ

### KROK 1: Zbuduj APK (✅ WYKONANE)

Aplikacja została już zbudowana:
```
L:\SHOP APP\app\build\outputs\apk\debug\app-debug.apk
```

**Status**: ✅ BUILD SUCCESSFUL

---

### KROK 2: Zainstaluj aplikację na urządzeniu

#### OPCJA A: Przez Android Studio
1. Podłącz urządzenie przez USB
2. Kliknij: **Run** → **Run 'app'**
3. Wybierz urządzenie z listy

#### OPCJA B: Przez ADB (komenda)
```bash
adb install -r L:\SHOP APP\app\build\outputs\apk\debug\app-debug.apk
```

#### OPCJA C: Ręcznie (jeśli ADB nie działa)
1. Skopiuj plik `app-debug.apk` na urządzenie (przez USB/email/cloud)
2. Na urządzeniu: otwórz plik APK
3. Kliknij "Zainstaluj" (może wymagać włączenia "Nieznane źródła")

---

### KROK 3A: Migracja automatyczna (zalecane)

Po zainstalowaniu nowej wersji, **uruchom aplikację normalnie**.

**Co się stanie**:
- Room wykryje wersję bazy 21 na urządzeniu
- Automatycznie wykona migrację 21→22
- Doda kolumnę `couponTotalDiscount` z wartością domyślną `0.0`
- Aplikacja uruchomi się poprawnie ✅

**Logi w logcat**:
```
D/RoomDatabase: Migration from 21 to 22 successful
```

---

### KROK 3B: Wyczyść dane (jeśli migracja nie działa)

**UWAGA**: To usunie wszystkie lokalne zamówienia!

#### Sposób 1: Przez ustawienia Androida
1. Ustawienia → Aplikacje → ItsOrderChat
2. Kliknij "Pamięć"
3. Kliknij "Wyczyść dane"
4. Uruchom aplikację ponownie

#### Sposób 2: Przez ADB
```bash
adb shell pm clear com.itsorderchat
```

#### Sposób 3: Odinstaluj i zainstaluj ponownie
```bash
adb uninstall com.itsorderchat
adb install L:\SHOP APP\app\build\outputs\apk\debug\app-debug.apk
```

---

## 📊 CO ZOSTAŁO ZMIENIONE W BAZIE DANYCH

### Tabela: `orders`

**PRZED (wersja 21)**:
```sql
CREATE TABLE orders (
    ...
    additionalFeeTotal REAL NOT NULL,
    deliveryType TEXT NOT NULL,
    type TEXT
);
```

**PO (wersja 22)**:
```sql
CREATE TABLE orders (
    ...
    additionalFeeTotal REAL NOT NULL,
    couponTotalDiscount REAL NOT NULL DEFAULT 0.0,  -- ← NOWE
    deliveryType TEXT NOT NULL,
    type TEXT
);
```

**Migracja SQL**:
```sql
ALTER TABLE orders ADD COLUMN couponTotalDiscount REAL NOT NULL DEFAULT 0.0
```

---

## 🧪 WERYFIKACJA CZY DZIAŁA

### TEST 1: Sprawdź czy aplikacja się uruchamia

1. Zainstaluj nową wersję APK
2. Uruchom aplikację
3. **Oczekiwany rezultat**: Aplikacja uruchamia się bez crashu ✅

### TEST 2: Sprawdź migrację w logcat

```bash
adb logcat | findstr "Migration"
```

**Oczekiwany log**:
```
D/RoomDatabase: Migration from 21 to 22 successful
```

### TEST 3: Sprawdź czy wydruk działa

1. Zaakceptuj zamówienie
2. Sprawdź wydruk
3. **Oczekiwany rezultat**: Wydruk zawiera linię "Zniżka:" (jeśli rabat > 0)

---

## 🔄 ALTERNATYWA: Destruktywna migracja (NIE ZALECANE)

Jeśli migracja nie działa, możesz użyć destruktywnej migracji (TYLKO DO DEVELOPMENTU!):

**Plik**: `NetworkModule.kt`

```kotlin
Room.databaseBuilder(
    appContext, AppDatabase::class.java, "app.db"
)
    .fallbackToDestructiveMigration()  // ← usuwa bazę i tworzy od nowa
    .build()
```

⚠️ **UWAGA**: To usunie WSZYSTKIE dane lokalne przy każdej aktualizacji!

---

## ✅ PODSUMOWANIE

### Zmiany w kodzie:
- [x] `AppDatabase.kt` - dodano migrację 21→22
- [x] `NetworkModule.kt` - dodano migrację do buildera
- [x] Wersja bazy danych: 21 → 22
- [x] Build successful ✅

### Następne kroki:
1. ⏳ Zainstaluj APK na urządzeniu
2. ⏳ Uruchom aplikację
3. ⏳ Sprawdź czy migracja przeszła
4. ⏳ Przetestuj wydruk z rabatem

---

## 📞 TROUBLESHOOTING

### Problem: Aplikacja nadal crashuje po instalacji

**Rozwiązanie 1**: Wyczyść dane aplikacji (Settings → Apps → ItsOrderChat → Clear data)

**Rozwiązanie 2**: Sprawdź logi:
```bash
adb logcat -s SQLiteDatabase:* RoomDatabase:*
```

**Rozwiązanie 3**: Odinstaluj całkowicie i zainstaluj ponownie

### Problem: Migracja nie wykonuje się

**Sprawdź**:
1. Czy wersja w `@Database` to 22?
2. Czy `MIGRATION_21_22` jest dodana do `.addMigrations()`?
3. Czy APK został poprawnie zbudowany (sprawdź datę pliku)?

### Problem: "Column couponTotalDiscount not found"

**Rozwiązanie**: Wyczyść dane aplikacji lub odinstaluj i zainstaluj ponownie.

---

**Data utworzenia**: 2026-01-27  
**Autor**: GitHub Copilot  
**Status**: ✅ KOD GOTOWY - WYMAGA INSTALACJI NA URZĄDZENIU  
**Następny krok**: Zainstaluj APK i przetestuj

