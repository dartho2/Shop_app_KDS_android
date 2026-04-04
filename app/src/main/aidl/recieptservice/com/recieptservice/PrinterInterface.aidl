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
