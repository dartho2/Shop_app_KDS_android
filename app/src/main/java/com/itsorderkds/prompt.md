MAMY KLONA DRUKARKI H10: /system/priv-app/SRPrinter/SRPrinter.apk
Z JADX mam zdekompilowany interfejs:
`public interface PrinterInterface extends IInterface { ... }`
z metodami m.in. `beginWork()`, `endWork()`, `printText(String)`, `nextLine(int)`, `printEpson(byte[])`, itd.

CEL:
Sprawić, żeby drukarka fizycznie drukowała / przesuwała papier.

ZADANIE DLA CIEBIE (COPILOT):
1) NIE używaj żadnych metod typu updatePrinterState()/printerStatus dla CLONE – to NIE jest w tym AIDL.
2) Zamiast tego zrób poprawny AIDL w projekcie na podstawie zdekompilowanego interfejsu:
    - utwórz `app/src/main/aidl/recieptservice/com/recieptservice/PrinterInterface.aidl`
    - utwórz `app/src/main/aidl/recieptservice/com/recieptservice/PSAMCallback.aidl` (minimalny stub, żeby kompilowało)
    - kolejność metod ma być identyczna jak w dekompilacji (bo odpowiada TRANSACTION_*)

3) Zmień AidlPrinterService tak, aby CLONE drukował ZAWSZE w sesji:
    - s.beginWork()
    - s.printText("HELLO\n")
    - s.nextLine(5)
    - s.endWork()
      (w tej kolejności)

4) Dodaj drugi test: printEpson() z prostymi bajtami ESC/POS:
   ESC @, "HELLO\n", feed.
   To ma iść przez `s.printEpson(byte[])` wewnątrz beginWork/endWork.

5) Dodaj logi do każdego kroku i obsłuż wyjątki.

KOD DO WDROŻENIA:

A) PrinterInterface.aidl:
package recieptservice.com.recieptservice;

import android.graphics.Bitmap;
import recieptservice.com.recieptservice.PSAMCallback;

interface PrinterInterface {
void printEpson(in byte[] data);                 // 1
String getServiceVersion();                      // 2
void printText(String text);                     // 3
void printBitmap(in Bitmap bitmap);              // 4
void printBarCode(String str, int a, int b, int c); // 5
void printQRCode(String str, int a, int b);      // 6
void setAlignment(int a);                        // 7
void setTextSize(float f);                       // 8
void nextLine(int lines);                        // 9
void printTableText(in String[] text, in int[] width, in int[] align); // 10
void setTextBold(boolean b);                     // 11
void beginWork();                                // 12
void endWork();                                  // 13
void setDark(int i);                             // 14
void setLineHeight(float f);                     // 15
void setTextDoubleWidth(boolean b);              // 16
void setTextDoubleHeight(boolean b);             // 17
void printPDF417Code(String str, int a, int b);   // 18
void setCode(String str);                        // 19
void print128BarCode(String str, int a, int b, int c); // 20
boolean getScannerStatus();                      // 21
void checkPSAMCard(int slot, in PSAMCallback cb);      // 22
void activatePSAMCard(int slot, in PSAMCallback cb);   // 23
void deactivatePSAMCard(int slot, in PSAMCallback cb); // 24
void transmitPSAMCard(int slot, in byte[] data, in PSAMCallback cb); // 25
}

B) PSAMCallback.aidl (minimalnie):
package recieptservice.com.recieptservice;
interface PSAMCallback {
void onResult(in byte[] data);
}

C) handleClonePrint() w AidlPrinterService (ważne: beginWork/endWork):
private fun handleClonePrint(text: String, autoCut: Boolean): Boolean {
val s = cloneService ?: return false
return runCatching {
Timber.d("[CLONE] version=${s.getServiceVersion()}")
s.beginWork()

        // opcjonalnie: kodowanie i ustawienia
        // s.setCode("UTF-8")  // jeśli nie działa, testuj "GBK"
        s.setAlignment(0)
        s.setTextBold(false)

        s.printText(text)
        s.nextLine(5)

        s.endWork()
        true
    }.onFailure {
        Timber.e(it, "[CLONE] print failed")
        runCatching { s.endWork() }
    }.getOrDefault(false)
}

D) test tylko feed:
fun feedOnlyTestfeedOnlyTest(lines: Int = 5): Boolean {
val s = cloneService ?: return false
return runCatching {
s.beginWork()
s.nextLine(lines)
s.endWork()
true
}.getOrDefault(false)
}

E) test printEpson:
fun printEpsonHello(): Boolean {
val s = cloneService ?: return false
val data = byteArrayOf(
0x1B, 0x40,                    // ESC @ init
0x48, 0x45, 0x4C, 0x4C, 0x4F, 0x0A, // HELLO\n
0x0A, 0x0A, 0x0A
)
return runCatching {
s.beginWork()
s.printEpson(data)
s.endWork()
true
}.getOrDefault(false)
}

DODATKOWE:
Jeśli beginWork/printText/nextLine/endWork nie rusza papieru, a log w SRPrinter pokazuje "MCU power off 2", to problem jest po stronie sterownika/sprzętu (MCU/drukarka wyłączona, brak papieru, otwarta klapka, itd.). Wtedy potrzebujemy sprawdzić czy systemowa apka SRPrinter potrafi zrobić self-test.
