package woyou.aidlservice.jiuiv5;

import woyou.aidlservice.jiuiv5.IWoyouServiceCallback;

interface IWoyouService {
    void printerInit(in IWoyouServiceCallback callback);
    void printerSelfChecking(in IWoyouServiceCallback callback);
    void printText(String text, in IWoyouServiceCallback callback);
    void printTextWithFont(String text, String typeface, float fontsize, in IWoyouServiceCallback callback);
    void printBarCode(String data, int symbology, int height, int width, int textposition, in IWoyouServiceCallback callback);
    void printQrCode(String data, int modulesize, int errorlevel, in IWoyouServiceCallback callback);
    void paperCut(in IWoyouServiceCallback callback);
    int getPrinterStatus();
}

