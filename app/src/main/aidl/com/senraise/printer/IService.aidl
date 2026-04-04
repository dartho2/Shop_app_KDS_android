package com.senraise.printer;

interface IService {
    void updatePrinterState();
    void printText(String text);
    void printBarCode(String data, int symbology, int height, int width);
    void printQrCode(String data, int moduleSize, int errorLevel);
    void printImage(in byte[] data);
    void setAlign(int align); // 0: left, 1: center, 2: right
    void nextLine(int line);
    void setFont(int size);
    void cutPaper();
    int getPrinterStatus();
}

