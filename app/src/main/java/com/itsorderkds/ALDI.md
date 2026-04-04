ZADANIE: przygotuj pełną dokumentację + wdrożenie + sprzątanie kodu dla integracji drukarki wbudowanej H10 (SRPrinter / recieptservice) przez AIDL, z pełnym formatowaniem przez DantSu.

KONTEKST:
- Udało się wydrukować "HELLO" przez AIDL do serwisu: recieptservice.com.recieptservice.service.PrinterService
- APK sterownika jest systemowy: /system/priv-app/SRPrinter/SRPrinter.apk
- Używamy AIDL z projektu:
  app/src/main/aidl/recieptservice/com/recieptservice/PrinterInterface.aidl
  app/src/main/aidl/recieptservice/com/recieptservice/PSAMCallback.aidl
- Kluczowe metody, które muszą być użyte w sesji:
  beginWork() -> (formatowanie + printText/printEpson/nextLine) -> endWork()
- Wcześniej były różne eksperymenty (printImage/raw socket/updatePrinterState itp.) – teraz tego NIE używamy.
- Docelowo chcemy ładny wydruk z tagów DantSu (np. [C], [L], [R], <b>, <font size='big'> itp.).

KLUCZOWA ZASADA (DantSu):
- DantSu ma posłużyć do generowania ESC/POS bajtów (render), a NIE do wysyłania po BT/USB.
- ESC/POS bajty wygenerowane przez DantSu wysyłamy do SRPrinter przez AIDL: PrinterInterface.printEpson(byte[]).
- printText() może pozostać jako fallback (awaryjny), ale preferowane jest DantSu -> printEpson.

CO MASZ ZROBIĆ (3 ETAPY):

========================
ETAP 0 — INTEGRACJA DANTSU (KRYTYCZNE)
========================
1) Dodaj zależność DantSu ESCPOS-ThermalPrinter-Android (JitPack) lub równoważną z pakietu DantSu.
2) Zaimplementuj renderer, który bierze dokument ze znacznikami DantSu i zwraca ByteArray ESC/POS, bez drukowania na urządzeniu:
    - stwórz klasę BufferDeviceConnection (lub podobną) która “udaje” DeviceConnection i zapisuje wszystkie wysyłane bajty do ByteArrayOutputStream.
    - użyj klas DantSu (PrinterTextParser / EscPosPrinterCommands / EscPosPrinter) tak, aby wynikowo mieć ByteArray ESC/POS.
    - przygotuj publiczne API:
      fun renderToEscPosBytes(
      formattedText: String,
      dpi: Int = 203,
      paperWidthMm: Float = 58f,
      charsPerLine: Int = 32,
      charset: Charset = Charset.forName("CP852")
      ): ByteArray
3) W rendererze zadbaj o:
    - polskie znaki (CP852 / CP1250 / Windows-1250) + dobór codepage kompatybilny z tą drukarką
    - brak niekontrolowanych komend, które mogą crashować driver:
        - domyślnie wyłącz generowanie barcode/qrcode jeśli nie jest potrzebne
        - jeśli barcode/qrcode jest potrzebne, generuj tylko przez DantSu tagi, ale dodaj walidację inputów (null/empty) żeby uniknąć crashy sterownika
4) Dodaj opcję “safe mode”:
    - jeśli render DantSu się nie powiedzie -> fallback na printText + proste metody AIDL setAlignment/setTextBold/setTextSize.

========================
ETAP 1 — DOKUMENTACJA “JAK TO DZIAŁA”
========================
Napisz dokumentację techniczną w pliku markdown:
docs/printer/h10-aidl.md

Ma zawierać:
1) Opis architektury:
    - Dlaczego AIDL
    - Jaki serwis: component name + package
    - Jak działa bindService + ServiceConnection
    - Dlaczego potrzebne jest beginWork/endWork (sesja)
    - Różnica między printText() a printEpson(byte[]) (ESC/POS)
    - Jak DantSu generuje ESC/POS i dlaczego wysyłamy to przez printEpson
2) Opis AIDL:
    - Lokalizacja plików .aidl
    - Jakie metody są używane w naszej aplikacji (minimum)
    - Co jest opcjonalne (barcode, qrcode, PSAM)
3) Flow drukowania (PROD):
    - connect()
    - beginWork()
    - render dokumentu zamówienia w formacie DantSu -> ESC/POS bytes
    - printEpson(bytes) (chunking + delay)
    - nextLine/feed
    - endWork()
    - disconnect (kiedy i dlaczego)
4) Typowe błędy i troubleshooting:
    - bind ok, ale brak druku -> brak beginWork/endWork
    - printText działa, a printEpson nie -> problem codepage / bytes / chunking
    - crash drivera przez złe bajty -> zasady bezpiecznego ESC/POS + walidacja barcode
    - “MCU power off” / brak papieru / otwarta klapka
    - jak włączyć logi / Timber tagi
5) Wymagania i kompatybilność:
    - działa na tym konkretnym H10/SRPrinter
    - fallbacki: jeśli brak serwisu -> zwróć błąd (bez raw socket)

========================
ETAP 2 — DOKUMENTACJA WDROŻENIA “JAK TO URUCHOMIĆ W PRODUKCJI”
========================
Napisz docs/printer/h10-deployment.md:

Ma zawierać checklistę wdrożeniową:
1) Weryfikacja urządzenia:
    - pm path recieptservice.com.recieptservice
    - sprawdzenie czy PrinterService istnieje i jest exported
2) Wymagania w aplikacji:
    - obecność plików AIDL w app/src/main/aidl
    - zależności DantSu (JitPack) + konfiguracja Gradle
    - minSdk / proguard/R8 (jeśli trzeba keep rules dla DantSu / parserów)
    - uprawnienia (jeśli nie są potrzebne, napisz że nie są potrzebne)
3) Test minimalny po wdrożeniu:
    - test “HELLO” + test “DantSu formatted sample” (developer-only, nie w UI dla usera)
4) Monitoring:
    - co logować w release (tylko error)
    - jak zbierać logcat jeśli klient zgłasza problem
5) Procedura aktualizacji:
    - co sprawdzić po aktualizacji SRPrinter / firmware

========================
ETAP 3 — POSPRZĄTANIE KODU I USUNIĘCIE TESTÓW
========================
1) Usuń wszystkie testowe przyciski z UI:
    - Test AIDL / HELLO / FEED / RAW SOCKET / diagnostyczne ikonki
2) Usuń/wyłącz testowe metody i stare eksperymenty w AidlPrinterService:
    - printViaRawSocket(), testRawSocket()
    - printImage/buildRawBytes/strip html jeśli niepotrzebne
    - debug dumpPackageServices/runFullDiagnostics jeśli było tylko do debugowania
3) Zostaw tylko produkcyjny, czysty interfejs:
    - connect()
    - disconnect()
    - printOrder(document: PrintDocument): Result
    - getStatus() (tylko jeśli realnie działa i jest potrzebne)
4) Uporządkuj threading:
    - nie używaj Thread.sleep() w UI
    - druk wykonuj na Dispatchers.IO
    - zapewnij synchronizację: jedna kolejka wydruków na raz (Mutex / single-thread queue)
5) Ujednolić nazewnictwo + pakiety:
    - jeden pakiet: com.itsorderchat.printing (albo podobnie)
    - usuń duplikaty plików / settings.print vs settings.printer
6) Dodaj testy jednostkowe tam gdzie ma sens:
    - test renderera DantSu: formattedText -> ESC/POS bytes (snapshot / length / sanity)
    - mock AIDL do walidacji flow (beginWork -> printEpson -> endWork)
7) Finalnie wykonaj refactor:
    - usuń nieużywane importy, martwy kod
    - spójne logi (tag: PrinterService)
    - public API klasy drukarki ma być minimalistyczne

WYNIK KOŃCOWY:
- Renderer DantSu -> ESC/POS bytes -> AIDL printEpson działa i daje ładne formatowanie (centrowanie, bold, rozmiary)
- Dwa pliki dokumentacji w docs/printer/
- Kod drukowania działa w produkcji bez testowych elementów
- UI nie pokazuje żadnych przycisków testowych
- AidlPrinterService jest czysty, utrzymywalny, z kolejką wydruków i poprawną obsługą sesji beginWork/endWork.

DODATKOWO (ZASADY BEZPIECZNEGO ESC/POS):
- printEpson() wysyłaj w chunkach (np. 512–2048 bajtów) z krótkim delay (np. 20–50ms) aby nie przepełnić bufora sterownika.
- NIE używaj printImage() do bajtów ESC/POS.
- Barcode/QRCode tylko po walidacji danych (brak null/empty), inaczej sterownik może crashować.
- Domyślnie używaj paperWidth 58mm, dpi 203, charsPerLine 32 (dostosuj jeśli wydruk jest obcięty).

Wygeneruj gotowe pliki Kotlin (jeśli trzeba):
- DantsuEscPosRenderer.kt
- BufferDeviceConnection.kt
- PrinterQueue.kt / PrinterMutex.kt
- zmiany w AidlPrinterService.kt
- dokumentacje md w docs/printer/
- ewentualne proguard-rules.pro (keep rules)
