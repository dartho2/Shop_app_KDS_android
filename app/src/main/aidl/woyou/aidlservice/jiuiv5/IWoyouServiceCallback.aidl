package woyou.aidlservice.jiuiv5;

interface IWoyouServiceCallback {
    void onRunResult(boolean isSuccess);
    void onReturnString(String result);
    void onRaiseException(int code, String msg);
    void onPrintResult(int code, String msg);
}

