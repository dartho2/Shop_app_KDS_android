# Migracja na nowy system drukarek

Celem jest pełne przejście na listę drukarek (`Printer/PrinterProfile`) i wyłączenie legacy ustawień STANDARD/KITCHEN (`SETTINGS_PRINTER`).

## Kroki wdrożenia
1. **Migracja danych**
   - Dodać migrator (np. `PrinterMigration`) wywoływany raz przy starcie (Application/`HomeActivity`) przed pierwszym drukowaniem.
   - Wejście: stare prefs STANDARD/KITCHEN (MAC, encoding, template, autoCut, kolejność).
   - Wyjście: lista `Printer` zapisana w `PrinterPreferences` (z `printerType`, profilem, encoding, codepage, template, autoCut, order).
   - Po sukcesie: wyczyścić legacy klucze, ustawić flagę migracji.

2. **Refaktor logiki druku**
   - `PrinterService` i wszystkie wywołania druku (np. `OrdersViewModel`, `SocketStaffEventsHandler`, akcja po akceptacji) muszą iterować po `PrinterPreferences.getPrinters()` i drukować sekwencyjnie wg `printer.order`.
   - Usunąć użycie `AppPrefs.getPrinterSettings(...)` i podobnych helperów legacy.
   - Zamykać połączenie po każdej drukarce; dla dual-mode dodać dłuższy timeout/retry.

3. **UI ustawień**
   - Zastąpić stare `SETTINGS_PRINTER` przekierowaniem do `PrintersListScreen` (dodaj/edytuj/usuwaj drukarki, wybór profilu, encoding, template, autoCut, `printerType`).
   - Ukryć/usunąć stare fragmenty/ekrany legacy.

4. **Porządki w kodzie**
   - W `AppPrefs.kt` usunąć legacy klucze/metody drukarek; zostawić tylko helpery potrzebne nowemu modelowi.
   - Usunąć lub zarchiwizować stare layouty/nav/enumy powiązane z dwiema drukarkami.

5. **Testy (manual / automaty)**
   - Migracja: start z legacy prefs → po migracji lista drukarek poprawna; stare klucze wyczyszczone.
   - Druk 1 drukarka: poprawny wydruk, brak crashy.
   - Druk 2 drukarki: sekwencyjne połączenia, brak dublowania, zamykane połączenia między drukami.
   - Dual-mode (BLE/BT): dłuższy timeout, brak błędów połączenia.
   - Błąd MAC: obsłużony exception, logi, brak crash.
   - UI: dodanie/edycja/usuwanie drukarki, zmiana typu/profilu/encoding/template, zapis i użycie w druku.

## Zmiany w plikach (propozycja)
- `data/preferences/PrinterMigration.kt` (nowy/uzupełnić) + wywołanie w Application/`HomeActivity`.
- `ui/settings/print/PrinterService.kt` + wywołania w `OrdersViewModel`, `SocketStaffEventsHandler`: pętla po `PrinterPreferences` zamiast legacy.
- `ui/settings/printer/PrintersListScreen.kt`, `AddEditPrinterDialog.kt`: jedyny ekran konfiguracji.
- `util/AppPrefs.kt`: usunąć legacy drukarki.
- Nav/menu: przekierowanie `SETTINGS_PRINTER` → `PrintersListScreen`.

## Notatki wykonawcze
- Migrację uruchomić jak najwcześniej w cyklu życia, aby druk zawsze używał nowej listy.
- Po migracji legacy dane czyścić, aby uniknąć ponownej migracji.
- Przy problemach z dual-mode wprowadzić minimalne opóźnienie między drukarkami i pełne zamknięcie socketu.

