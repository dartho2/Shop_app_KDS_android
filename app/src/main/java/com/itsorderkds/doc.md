ZADANIE: Dodaj obsługę obcinania papieru (cut) dla drukarek ESC/POS, ale tylko jeśli drukarka to wspiera.
Jeśli wspiera — w ustawieniach drukarki ma się pojawić opcja (Switch/Checkbox) “Obcinaj papier po wydruku”.
Opcja ma być zapisywana per-drukarka (po MAC / printerId) i używana w każdym wydruku “receipt”.

KONTEKST:
- Android + Jetpack Compose (PrinterSettingsScreen)
- Drukowanie przez Bluetooth (RFCOMM / DantSu escposprinter)
- Mamy AppPrefs i PrinterManager oraz PrinterTestHelper/PrinterService

WYMAGANIA UI:
1) W PrinterSettingsScreen dodaj sekcję “Papier” / “Cut”:
    - Switch: “Obcinaj papier po wydruku”
    - Switch jest widoczny tylko jeśli drukarka ma cutter (hasCutter == true).
    - Jeśli drukarka NIE ma cutter → pokaż tekst pomocniczy “Ta drukarka nie obsługuje obcinania papieru”.
2) Skąd brać hasCutter?
    - Najprościej: ustawienie per drukarka (manual) + opcjonalna detekcja.
    - Dodaj możliwość ręcznego zaznaczenia “Drukarka ma obcinak (cutter)” w manual mapping (obok encoding/codepage).
    - Zapisz w AppPrefs per printerId: hasCutter.
3) Dodaj też przełącznik “Drukarka ma obcinak (cutter)” (manual) aby użytkownik mógł wymusić.

APP PREFS:
- Dodaj metody:
    - getPrinterHasCutterFor(printerId: String?): Boolean
    - setPrinterHasCutterFor(printerId: String, value: Boolean)
    - getAutoCutEnabledFor(printerId: String?): Boolean  (czy obcinać po wydruku)
    - setAutoCutEnabledFor(printerId: String, value: Boolean)

IMPLEMENTACJA DRUKU (Receipt):
1) W miejscu gdzie finalizujesz wydruk (po wysłaniu tekstu/obrazu/formatowania) zrób:
    - jeśli (hasCutter && autoCutEnabled) → wyślij komendę cut
    - inaczej → tylko feed (kilka LF)
2) Implementacja komendy cut:
    - Preferuj standard ESC/POS:
        - GS V 0  (0x1D 0x56 0x00) pełne cięcie
        - GS V 1  (0x1D 0x56 0x01) częściowe
    - Zrób metodę w PrinterManager:
        - fun cut(connection): Boolean -> próbuje kilka sekwencji i zwraca true jeśli write nie rzuci wyjątku
    - Wysyłaj cut dopiero po krótkim feed (np. 3–6 LF) i flush.
3) DantSu EscPosPrinter:
    - Jeśli używamy EscPosPrinter, można po printFormattedText dopisać raw cut przez connection.write(...)
      (albo użyć PrinterManager.sendCutPaper(connection)).
    - Jeśli drukujemy przez RFCOMM OutputStream, dopisz out.write(cutBytes) na końcu.

DETEKCJA (opcjonalnie):
- Nie da się “pewnie” wykryć cut, bo write zwykle nie zwraca ACK.
- Możesz dodać w ustawieniach przycisk “Test obcinania”:
    - wydrukuj “CUT TEST” + feed + cut
    - user ocenia czy ucięło.
- Jeśli user potwierdzi, ustaw hasCutter=true.

DODATKOWO:
- Upewnij się, że logika bierze printerId z AppPrefs.getPrinter(context)
- Obsłuż sytuację gdy printerId null → ukryj opcje per urządzenie.
- Dodaj logi Timber: autoCutEnabled/hasCutter, oraz czy wysłano cut.

PROSZĘ:
- Zaimplementuj to end-to-end:
    - AppPrefs + UI + PrinterManager.cut + użycie w wydruku receipt.
- Daj gotowe fragmenty kodu w Kotlinie.
