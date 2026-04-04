2026-02-04 17:45:19.364  4227-4287  SocketStaf...ntsHandler com.itsorderchat                     D  Auto-print (new order) enabled=false for KR-145
2026-02-04 17:45:25.664  4227-4227  DUPSKO                  com.itsorderchat                     D  PrintDebug Button +15 min clicked. Wywołuję onTimeSelect i onPrintOrder
2026-02-04 17:45:25.667  4227-4227  DUPSKO                  com.itsorderchat                     D  PrintDebug TIME +2026-02-04T18:00:25.665753+01:00 min clicked. Wywołuję onTimeSelect i onPrintOrder
2026-02-04 17:45:25.668  4227-4227  DUPSKO                  com.itsorderchat                     D  PrintDebug Button +2026-02-04T18:00:25.665753+01:00 min clicked
2026-02-04 17:45:26.057  4227-4227  AUTO_PRINT...TUS_CHANGE com.itsorderchat                     D  🔔 Status zmieniony na ACCEPTED przez socket: 6983779f47a521e2eb337e60
2026-02-04 17:45:26.130  4227-4227  OrdersView...kAsPrinted com.itsorderchat                     D  💾 Zapisano wydrukowane zamówienie do DataStore: 6983779f47a521e2eb337e60
2026-02-04 17:45:26.219  4227-4227  OrdersView...rderUpdate com.itsorderchat                     D  🖨️ OrdersViewModel: Wywołuję printAfterOrderAccepted dla KR-145
2026-02-04 17:45:26.307  4227-4227  PrinterService          com.itsorderchat                     D  📋 printAfterOrderAccepted: order=KR-145, autoAccepted=true, autoKitchen=true
2026-02-04 17:45:26.316  4227-4227  PrinterPreferences      com.itsorderchat                     D  PrinterPreferences: Wczytano 2 drukarek
2026-02-04 17:45:26.394  4227-4227  PrinterService          com.itsorderchat                     D  🖨️ Drukuję na standardowej: wbudowana
2026-02-04 17:45:26.396  4227-4227  PrinterService          com.itsorderchat                     D  🖨️ Drukarka wbudowana (serial port)
2026-02-04 17:45:26.397  4227-4227  PrinterService          com.itsorderchat                     D  🖨️ Serial Port Print: port=builtin, printer=wbudowana
2026-02-04 17:45:26.408  4227-4735  TICKET_PRINT            com.itsorderchat                     D  Order #KR-145: source=null, sourceLabel=''
2026-02-04 17:45:26.408  4227-4735  TICKET_PRINT            com.itsorderchat                     D  ⚠️ Source label is blank - not added to ticket
2026-02-04 17:45:26.426  4227-4735  PrinterSer...tOneSerial com.itsorderchat                     D  📋 Serial: Dokument przygotowany (638 znaków)
2026-02-04 17:45:26.433  4227-4735  PrinterSer...tOneSerial com.itsorderchat                     D  ⚙️ Serial: encoding=UTF-8, autoCut=false
2026-02-04 17:45:26.438  4227-4735  PrinterSer...tOneSerial com.itsorderchat                     D  📤 Wysyłam zawartość do DantSu...
2026-02-04 17:45:26.442  4227-4735  SerialPortPrinter       com.itsorderchat                     D  🚀 Wykryto builtin -> Uruchamiam AidlPrinterService
2026-02-04 17:45:26.714  4227-4735  om.itsorderchat         com.itsorderchat                     W  Verification of void com.itsorderchat.ui.settings.printer.AidlPrinterService.access$setCloneService$p(com.itsorderchat.ui.settings.printer.AidlPrinterService, recieptservice.com.recieptservice.PrinterInterface) took 270.813ms (11.08 bytecodes/s) (728B approximate peak alloc)
2026-02-04 17:45:26.722  4227-4735  AidlPrinterService      com.itsorderchat                     D  🔄 Łączenie z usługą...
2026-02-04 17:45:26.724  4227-4735  AidlPrinterService      com.itsorderchat                     D  🔄 Łączenie z usługą...
2026-02-04 17:45:26.728  4227-4735  AidlPrinterService      com.itsorderchat                     D     Próba Senraise: false
2026-02-04 17:45:26.739  4227-4735  AidlPrinterService      com.itsorderchat                     D     Próba Klon H10: true
2026-02-04 17:45:26.822  4227-4227  AidlPrinte...connection com.itsorderchat                     D  🔗 ===== POŁĄCZENIE Z DRUKARKĄ AIDL =====
2026-02-04 17:45:26.824  4227-4227  AidlPrinte...connection com.itsorderchat                     D  📦 Pakiet: recieptservice.com.recieptservice
2026-02-04 17:45:26.825  4227-4227  AidlPrinte...connection com.itsorderchat                     D  ✅ Typ: KLON (recieptservice)
2026-02-04 17:45:26.828  4227-4227  PrinterService          com.itsorderchat                     D  📋 printAfterOrderAccepted: order=KR-145, autoAccepted=true, autoKitchen=true
2026-02-04 17:45:27.542  4227-4735  AidlPrinterService      com.itsorderchat                     D  🖨️ AIDL print start, serviceType=CLONE
2026-02-04 17:45:27.557  4227-4735  AidlPrinterService      com.itsorderchat                     D  🧾 [CLONE] version=202
2026-02-04 17:45:27.561  4227-4735  AidlPrinterService      com.itsorderchat                     D  📋 [CLONE] Używam parsera formatowania AIDL
2026-02-04 17:45:27.642  4227-4735  AidlPrinterService      com.itsorderchat                     D  ✅ [CLONE] Sparsowano 18 segmentów
2026-02-04 17:45:27.732  4227-4735  AidlPrinterService      com.itsorderchat                     D  ✅ [CLONE] Renderowanie zakończone sukcesem
2026-02-04 17:45:27.736  4227-4735  AidlPrinterService      com.itsorderchat                     D  🔌 Rozłączono
2026-02-04 17:45:27.736  4227-4735  SerialPortPrinter       com.itsorderchat                     D  ✅ Wydrukowano pomyślnie przez AIDL
2026-02-04 17:45:27.737  4227-4735  PrinterSer...tOneSerial com.itsorderchat                     D  ✅ Serial print zakończony pomyślnie
2026-02-04 17:45:27.757  4227-4227  PrinterService          com.itsorderchat                     D  ⏱️ Print duration (BUILTIN): 1361ms, printer=wbudowana
2026-02-04 17:45:28.260  4227-4227  PrinterService          com.itsorderchat                     D  ✅ Drukowanie standardowe zakończone
2026-02-04 17:45:28.267  4227-4227  PrinterPreferences      com.itsorderchat                     D  PrinterPreferences: Wczytano 2 drukarek
2026-02-04 17:45:28.272  4227-4227  PrinterService          com.itsorderchat                     D  🍳 Drukuję na kuchni (inna drukarka, bez opóźnienia)
2026-02-04 17:45:28.274  4227-4227  PrinterService          com.itsorderchat                     D  🍳 Drukuję na kuchni: kuchnia
2026-02-04 17:45:28.276  4227-4227  PrinterService          com.itsorderchat                     D  🔒 PrinterSTEP: [ENTRY] target=KITCHEN order=KR-145
2026-02-04 17:45:28.277  4227-4227  PrinterService          com.itsorderchat                     D  📍 getConnectionFor: type=BLUETOOTH, deviceId=AB:0D:6F:E2:85:D7
2026-02-04 17:45:28.279  4227-4227  PrinterService          com.itsorderchat                     D  📶 Łączę się przez Bluetooth: AB:0D:6F:E2:85:D7
2026-02-04 17:45:28.309  4227-4227  PrinterManager          com.itsorderchat                     D  getConnectionById(list): POS-8390_BLE (AB:0D:6F:E2:85:D7) type=DUAL uuids=00000000-0000-1000-8000-00805f9b34fb, 00001101-0000-1000-8000-00805f9b34fb
2026-02-04 17:45:28.321  4227-4227  PrinterManager          com.itsorderchat                     D  hasSpp: checking device POS-8390_BLE (AB:0D:6F:E2:85:D7)
2026-02-04 17:45:28.327  4227-4227  PrinterManager          com.itsorderchat                     D  hasSpp: device.type=3, uuids=00000000-0000-1000-8000-00805f9b34fb, 00001101-0000-1000-8000-00805f9b34fb
2026-02-04 17:45:28.331  4227-4227  PrinterManager          com.itsorderchat                     D  hasSpp: hasSppUuid=true
2026-02-04 17:45:28.332  4227-4227  PrinterService          com.itsorderchat                     D  ✅ getConnectionFor: Bluetooth OK
2026-02-04 17:45:28.340  4227-4227  TICKET_PRINT            com.itsorderchat                     D  Order #KR-145: source=null, sourceLabel=''
2026-02-04 17:45:28.340  4227-4227  TICKET_PRINT            com.itsorderchat                     D  ⚠️ Source label is blank - not added to ticket
2026-02-04 17:45:28.356  4227-4227  PrinterService          com.itsorderchat                     D  ✅ Dokument gotowy (template=template_standard, autoCut=true)
2026-02-04 17:45:28.358  4227-4287  PrinterCon...Connection com.itsorderchat                     D  🔒 PrinterConnectionManager: mutex locked (type=BLUETOOTH)
2026-02-04 17:45:28.360  4227-4287  PrinterCon...ionManager com.itsorderchat                     D  📶 Bluetooth connection strategy
2026-02-04 17:45:28.364  4227-4287  PrinterCon...ionManager com.itsorderchat                     D  ✅ BT discovery cancelled
2026-02-04 17:45:28.365  4227-4287  PrinterCon...ionManager com.itsorderchat                     D  🔌 Connect attempt #1/3...
2026-02-04 17:45:29.335  4227-4287  PrinterCon...ionManager com.itsorderchat                     D  ✅ Connected on attempt #1
2026-02-04 17:45:29.338  4227-4287  PrinterCon...ionManager com.itsorderchat                     D  📄 Executing print block...
2026-02-04 17:45:29.697  4227-4287  PrinterCon...ionManager com.itsorderchat                     D  ⏳ Flushing buffer (150ms)...
2026-02-04 17:45:29.856  4227-4287  PrinterCon...ionManager com.itsorderchat                     D  ✅ Disconnected, waiting 300ms for cleanup...
2026-02-04 17:45:30.161  4227-4287  PrinterCon...ionManager com.itsorderchat                     D  🔓 PrinterConnectionManager: mutex released
2026-02-04 17:45:30.165  4227-4227  PrinterService          com.itsorderchat                     D  ✅ Wydruk zakończony kuchnia
2026-02-04 17:45:30.166  4227-4227  PrinterService          com.itsorderchat                     D  ⏱️ Print duration (SUCCESS): 1889ms, printer=kuchnia, type=BLUETOOTH
2026-02-04 17:45:30.375  4227-4227  PrinterService          com.itsorderchat                     D  🔓 PrinterSTEP: [EXIT] target=KITCHEN
2026-02-04 17:45:30.878  4227-4227  PrinterService          com.itsorderchat                     D  ✅ Drukowanie kuchenne zakończone
2026-02-04 17:45:30.885  4227-4227  PrinterPreferences      com.itsorderchat                     D  PrinterPreferences: Wczytano 2 drukarek
2026-02-04 17:45:30.890  4227-4227  PrinterService          com.itsorderchat                     D  🖨️ Drukuję na standardowej: wbudowana
2026-02-04 17:45:30.891  4227-4227  PrinterService          com.itsorderchat                     D  🖨️ Drukarka wbudowana (serial port)
2026-02-04 17:45:30.892  4227-4227  PrinterService          com.itsorderchat                     D  🖨️ Serial Port Print: port=builtin, printer=wbudowana
2026-02-04 17:45:30.898  4227-4287  TICKET_PRINT            com.itsorderchat                     D  Order #KR-145: source=unknown, sourceLabel=''
2026-02-04 17:45:30.898  4227-4287  TICKET_PRINT            com.itsorderchat                     D  ⚠️ Source label is blank - not added to ticket
2026-02-04 17:45:30.905  4227-4227  OrdersView...kAsPrinted com.itsorderchat                     D  💾 Zapisano wydrukowane zamówienie do DataStore: 6983779f47a521e2eb337e60
2026-02-04 17:45:30.918  4227-4287  PrinterSer...tOneSerial com.itsorderchat                     D  📋 Serial: Dokument przygotowany (638 znaków)
2026-02-04 17:45:30.920  4227-4287  PrinterSer...tOneSerial com.itsorderchat                     D  ⚙️ Serial: encoding=UTF-8, autoCut=false
2026-02-04 17:45:30.923  4227-4287  PrinterSer...tOneSerial com.itsorderchat                     D  📤 Wysyłam zawartość do DantSu...
2026-02-04 17:45:30.925  4227-4287  SerialPortPrinter       com.itsorderchat                     D  🚀 Wykryto builtin -> Uruchamiam AidlPrinterService
2026-02-04 17:45:30.927  4227-4287  AidlPrinterService      com.itsorderchat                     D  🔄 Łączenie z usługą...
2026-02-04 17:45:30.929  4227-4287  AidlPrinterService      com.itsorderchat                     D  🔄 Łączenie z usługą...
2026-02-04 17:45:30.933  4227-4287  AidlPrinterService      com.itsorderchat                     D     Próba Senraise: false
2026-02-04 17:45:30.941  4227-4227  AidlPrinte...connection com.itsorderchat                     D  🔗 ===== POŁĄCZENIE Z DRUKARKĄ AIDL =====
2026-02-04 17:45:30.941  4227-4287  AidlPrinterService      com.itsorderchat                     D     Próba Klon H10: true
2026-02-04 17:45:30.942  4227-4227  AidlPrinte...connection com.itsorderchat                     D  📦 Pakiet: recieptservice.com.recieptservice
2026-02-04 17:45:30.943  4227-4227  AidlPrinte...connection com.itsorderchat                     D  ✅ Typ: KLON (recieptservice)
2026-02-04 17:45:31.744  4227-4287  AidlPrinterService      com.itsorderchat                     D  🖨️ AIDL print start, serviceType=CLONE
2026-02-04 17:45:31.747  4227-4287  AidlPrinterService      com.itsorderchat                     D  🧾 [CLONE] version=202
2026-02-04 17:45:31.749  4227-4287  AidlPrinterService      com.itsorderchat                     D  📋 [CLONE] Używam parsera formatowania AIDL
2026-02-04 17:45:31.828  4227-4287  AidlPrinterService      com.itsorderchat                     D  ✅ [CLONE] Sparsowano 18 segmentów
2026-02-04 17:45:31.901  4227-4287  AidlPrinterService      com.itsorderchat                     D  ✅ [CLONE] Renderowanie zakończone sukcesem
2026-02-04 17:45:31.904  4227-4287  AidlPrinterService      com.itsorderchat                     D  🔌 Rozłączono
2026-02-04 17:45:31.905  4227-4287  SerialPortPrinter       com.itsorderchat                     D  ✅ Wydrukowano pomyślnie przez AIDL
2026-02-04 17:45:31.906  4227-4287  PrinterSer...tOneSerial com.itsorderchat                     D  ✅ Serial print zakończony pomyślnie
2026-02-04 17:45:31.912  4227-4227  PrinterService          com.itsorderchat                     D  ⏱️ Print duration (BUILTIN): 1021ms, printer=wbudowana
2026-02-04 17:45:32.415  4227-4227  PrinterService          com.itsorderchat                     D  ✅ Drukowanie standardowe zakończone
2026-02-04 17:45:32.423  4227-4227  PrinterPreferences      com.itsorderchat                     D  PrinterPreferences: Wczytano 2 drukarek
2026-02-04 17:45:32.430  4227-4227  PrinterService          com.itsorderchat                     D  🍳 Drukuję na kuchni (inna drukarka, bez opóźnienia)
2026-02-04 17:45:32.432  4227-4227  PrinterService          com.itsorderchat                     D  🍳 Drukuję na kuchni: kuchnia
2026-02-04 17:45:32.435  4227-4227  PrinterService          com.itsorderchat                     D  🔒 PrinterSTEP: [ENTRY] target=KITCHEN order=KR-145
2026-02-04 17:45:32.438  4227-4227  PrinterService          com.itsorderchat                     D  📍 getConnectionFor: type=BLUETOOTH, deviceId=AB:0D:6F:E2:85:D7
2026-02-04 17:45:32.439  4227-4227  PrinterService          com.itsorderchat                     D  📶 Łączę się przez Bluetooth: AB:0D:6F:E2:85:D7
2026-02-04 17:45:32.470  4227-4227  PrinterManager          com.itsorderchat                     D  getConnectionById(list): POS-8390_BLE (AB:0D:6F:E2:85:D7) type=DUAL uuids=00000000-0000-1000-8000-00805f9b34fb, 00001101-0000-1000-8000-00805f9b34fb
2026-02-04 17:45:32.484  4227-4227  PrinterManager          com.itsorderchat                     D  hasSpp: checking device POS-8390_BLE (AB:0D:6F:E2:85:D7)
2026-02-04 17:45:32.492  4227-4227  PrinterManager          com.itsorderchat                     D  hasSpp: device.type=3, uuids=00000000-0000-1000-8000-00805f9b34fb, 00001101-0000-1000-8000-00805f9b34fb
2026-02-04 17:45:32.499  4227-4227  PrinterManager          com.itsorderchat                     D  hasSpp: hasSppUuid=true
2026-02-04 17:45:32.501  4227-4227  PrinterService          com.itsorderchat                     D  ✅ getConnectionFor: Bluetooth OK
2026-02-04 17:45:32.507  4227-4227  TICKET_PRINT            com.itsorderchat                     D  Order #KR-145: source=unknown, sourceLabel=''
2026-02-04 17:45:32.507  4227-4227  TICKET_PRINT            com.itsorderchat                     D  ⚠️ Source label is blank - not added to ticket
2026-02-04 17:45:32.523  4227-4227  PrinterService          com.itsorderchat                     D  ✅ Dokument gotowy (template=template_standard, autoCut=true)
2026-02-04 17:45:32.526  4227-4287  PrinterCon...Connection com.itsorderchat                     D  🔒 PrinterConnectionManager: mutex locked (type=BLUETOOTH)
2026-02-04 17:45:32.527  4227-4287  PrinterCon...ionManager com.itsorderchat                     D  📶 Bluetooth connection strategy
2026-02-04 17:45:32.532  4227-4287  PrinterCon...ionManager com.itsorderchat                     D  ✅ BT discovery cancelled
2026-02-04 17:45:32.534  4227-4287  PrinterCon...ionManager com.itsorderchat                     D  🔌 Connect attempt #1/3...
2026-02-04 17:45:32.702  4227-4287  PrinterCon...ionManager com.itsorderchat                     D  ✅ Connected on attempt #1
2026-02-04 17:45:32.705  4227-4287  PrinterCon...ionManager com.itsorderchat                     D  📄 Executing print block...
2026-02-04 17:45:33.082  4227-4287  PrinterCon...ionManager com.itsorderchat                     D  ⏳ Flushing buffer (150ms)...
2026-02-04 17:45:33.238  4227-4287  PrinterCon...ionManager com.itsorderchat                     D  ✅ Disconnected, waiting 300ms for cleanup...
2026-02-04 17:45:33.544  4227-4287  PrinterCon...ionManager com.itsorderchat                     D  🔓 PrinterConnectionManager: mutex released
2026-02-04 17:45:33.548  4227-4227  PrinterService          com.itsorderchat                     D  ✅ Wydruk zakończony kuchnia
2026-02-04 17:45:33.549  4227-4227  PrinterService          com.itsorderchat                     D  ⏱️ Print duration (SUCCESS): 1113ms, printer=kuchnia, type=BLUETOOTH
2026-02-04 17:45:33.751  4227-4227  PrinterService          com.itsorderchat                     D  🔓 PrinterSTEP: [EXIT] target=KITCHEN
2026-02-04 17:45:34.254  4227-4227  PrinterService          com.itsorderchat                     D  ✅ Drukowanie kuchenne zakończone
