package recieptservice.com.recieptservice;

import android.graphics.Bitmap;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import recieptservice.com.recieptservice.PSAMCallback;

/* loaded from: classes2.dex */
public interface PrinterInterface extends IInterface {
public static final String DESCRIPTOR = "recieptservice.com.recieptservice.PrinterInterface";

    public static class Default implements PrinterInterface {
        @Override // recieptservice.com.recieptservice.PrinterInterface
        public void activatePSAMCard(int i, PSAMCallback pSAMCallback) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }

        @Override // recieptservice.com.recieptservice.PrinterInterface
        public void beginWork() throws RemoteException {
        }

        @Override // recieptservice.com.recieptservice.PrinterInterface
        public void checkPSAMCard(int i, PSAMCallback pSAMCallback) throws RemoteException {
        }

        @Override // recieptservice.com.recieptservice.PrinterInterface
        public void deactivatePSAMCard(int i, PSAMCallback pSAMCallback) throws RemoteException {
        }

        @Override // recieptservice.com.recieptservice.PrinterInterface
        public void endWork() throws RemoteException {
        }

        @Override // recieptservice.com.recieptservice.PrinterInterface
        public boolean getScannerStatus() throws RemoteException {
            return false;
        }

        @Override // recieptservice.com.recieptservice.PrinterInterface
        public String getServiceVersion() throws RemoteException {
            return null;
        }

        @Override // recieptservice.com.recieptservice.PrinterInterface
        public void nextLine(int i) throws RemoteException {
        }

        @Override // recieptservice.com.recieptservice.PrinterInterface
        public void print128BarCode(String str, int i, int i2, int i3) throws RemoteException {
        }

        @Override // recieptservice.com.recieptservice.PrinterInterface
        public void printBarCode(String str, int i, int i2, int i3) throws RemoteException {
        }

        @Override // recieptservice.com.recieptservice.PrinterInterface
        public void printBitmap(Bitmap bitmap) throws RemoteException {
        }

        @Override // recieptservice.com.recieptservice.PrinterInterface
        public void printEpson(byte[] bArr) throws RemoteException {
        }

        @Override // recieptservice.com.recieptservice.PrinterInterface
        public void printPDF417Code(String str, int i, int i2) throws RemoteException {
        }

        @Override // recieptservice.com.recieptservice.PrinterInterface
        public void printQRCode(String str, int i, int i2) throws RemoteException {
        }

        @Override // recieptservice.com.recieptservice.PrinterInterface
        public void printTableText(String[] strArr, int[] iArr, int[] iArr2) throws RemoteException {
        }

        @Override // recieptservice.com.recieptservice.PrinterInterface
        public void printText(String str) throws RemoteException {
        }

        @Override // recieptservice.com.recieptservice.PrinterInterface
        public void setAlignment(int i) throws RemoteException {
        }

        @Override // recieptservice.com.recieptservice.PrinterInterface
        public void setCode(String str) throws RemoteException {
        }

        @Override // recieptservice.com.recieptservice.PrinterInterface
        public void setDark(int i) throws RemoteException {
        }

        @Override // recieptservice.com.recieptservice.PrinterInterface
        public void setLineHeight(float f) throws RemoteException {
        }

        @Override // recieptservice.com.recieptservice.PrinterInterface
        public void setTextBold(boolean z) throws RemoteException {
        }

        @Override // recieptservice.com.recieptservice.PrinterInterface
        public void setTextDoubleHeight(boolean z) throws RemoteException {
        }

        @Override // recieptservice.com.recieptservice.PrinterInterface
        public void setTextDoubleWidth(boolean z) throws RemoteException {
        }

        @Override // recieptservice.com.recieptservice.PrinterInterface
        public void setTextSize(float f) throws RemoteException {
        }

        @Override // recieptservice.com.recieptservice.PrinterInterface
        public void transmitPSAMCard(int i, byte[] bArr, PSAMCallback pSAMCallback) throws RemoteException {
        }
    }

    void activatePSAMCard(int i, PSAMCallback pSAMCallback) throws RemoteException;

    void beginWork() throws RemoteException;

    void checkPSAMCard(int i, PSAMCallback pSAMCallback) throws RemoteException;

    void deactivatePSAMCard(int i, PSAMCallback pSAMCallback) throws RemoteException;

    void endWork() throws RemoteException;

    boolean getScannerStatus() throws RemoteException;

    String getServiceVersion() throws RemoteException;

    void nextLine(int i) throws RemoteException;

    void print128BarCode(String str, int i, int i2, int i3) throws RemoteException;

    void printBarCode(String str, int i, int i2, int i3) throws RemoteException;

    void printBitmap(Bitmap bitmap) throws RemoteException;

    void printEpson(byte[] bArr) throws RemoteException;

    void printPDF417Code(String str, int i, int i2) throws RemoteException;

    void printQRCode(String str, int i, int i2) throws RemoteException;

    void printTableText(String[] strArr, int[] iArr, int[] iArr2) throws RemoteException;

    void printText(String str) throws RemoteException;

    void setAlignment(int i) throws RemoteException;

    void setCode(String str) throws RemoteException;

    void setDark(int i) throws RemoteException;

    void setLineHeight(float f) throws RemoteException;

    void setTextBold(boolean z) throws RemoteException;

    void setTextDoubleHeight(boolean z) throws RemoteException;

    void setTextDoubleWidth(boolean z) throws RemoteException;

    void setTextSize(float f) throws RemoteException;

    void transmitPSAMCard(int i, byte[] bArr, PSAMCallback pSAMCallback) throws RemoteException;

    public static abstract class Stub extends Binder implements PrinterInterface {
        static final int TRANSACTION_activatePSAMCard = 23;
        static final int TRANSACTION_beginWork = 12;
        static final int TRANSACTION_checkPSAMCard = 22;
        static final int TRANSACTION_deactivatePSAMCard = 24;
        static final int TRANSACTION_endWork = 13;
        static final int TRANSACTION_getScannerStatus = 21;
        static final int TRANSACTION_getServiceVersion = 2;
        static final int TRANSACTION_nextLine = 9;
        static final int TRANSACTION_print128BarCode = 20;
        static final int TRANSACTION_printBarCode = 5;
        static final int TRANSACTION_printBitmap = 4;
        static final int TRANSACTION_printEpson = 1;
        static final int TRANSACTION_printPDF417Code = 18;
        static final int TRANSACTION_printQRCode = 6;
        static final int TRANSACTION_printTableText = 10;
        static final int TRANSACTION_printText = 3;
        static final int TRANSACTION_setAlignment = 7;
        static final int TRANSACTION_setCode = 19;
        static final int TRANSACTION_setDark = 14;
        static final int TRANSACTION_setLineHeight = 15;
        static final int TRANSACTION_setTextBold = 11;
        static final int TRANSACTION_setTextDoubleHeight = 17;
        static final int TRANSACTION_setTextDoubleWidth = 16;
        static final int TRANSACTION_setTextSize = 8;
        static final int TRANSACTION_transmitPSAMCard = 25;

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, PrinterInterface.DESCRIPTOR);
        }

        public static PrinterInterface asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(PrinterInterface.DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof PrinterInterface)) {
                return (PrinterInterface) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i >= 1 && i <= 16777215) {
                parcel.enforceInterface(PrinterInterface.DESCRIPTOR);
            }
            if (i == 1598968902) {
                parcel2.writeString(PrinterInterface.DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    printEpson(parcel.createByteArray());
                    parcel2.writeNoException();
                    return true;
                case 2:
                    String serviceVersion = getServiceVersion();
                    parcel2.writeNoException();
                    parcel2.writeString(serviceVersion);
                    return true;
                case 3:
                    printText(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 4:
                    printBitmap((Bitmap) _Parcel.readTypedObject(parcel, Bitmap.CREATOR));
                    parcel2.writeNoException();
                    return true;
                case 5:
                    printBarCode(parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 6:
                    printQRCode(parcel.readString(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 7:
                    setAlignment(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 8:
                    setTextSize(parcel.readFloat());
                    parcel2.writeNoException();
                    return true;
                case 9:
                    nextLine(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 10:
                    printTableText(parcel.createStringArray(), parcel.createIntArray(), parcel.createIntArray());
                    parcel2.writeNoException();
                    return true;
                case 11:
                    setTextBold(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 12:
                    beginWork();
                    parcel2.writeNoException();
                    return true;
                case 13:
                    endWork();
                    parcel2.writeNoException();
                    return true;
                case 14:
                    setDark(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 15:
                    setLineHeight(parcel.readFloat());
                    parcel2.writeNoException();
                    return true;
                case 16:
                    setTextDoubleWidth(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 17:
                    setTextDoubleHeight(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 18:
                    printPDF417Code(parcel.readString(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 19:
                    setCode(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 20:
                    print128BarCode(parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 21:
                    boolean scannerStatus = getScannerStatus();
                    parcel2.writeNoException();
                    parcel2.writeInt(scannerStatus ? 1 : 0);
                    return true;
                case 22:
                    checkPSAMCard(parcel.readInt(), PSAMCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 23:
                    activatePSAMCard(parcel.readInt(), PSAMCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 24:
                    deactivatePSAMCard(parcel.readInt(), PSAMCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 25:
                    transmitPSAMCard(parcel.readInt(), parcel.createByteArray(), PSAMCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements PrinterInterface {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return PrinterInterface.DESCRIPTOR;
            }

            @Override // recieptservice.com.recieptservice.PrinterInterface
            public void printEpson(byte[] bArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(PrinterInterface.DESCRIPTOR);
                    parcelObtain.writeByteArray(bArr);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // recieptservice.com.recieptservice.PrinterInterface
            public String getServiceVersion() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(PrinterInterface.DESCRIPTOR);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // recieptservice.com.recieptservice.PrinterInterface
            public void printText(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(PrinterInterface.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // recieptservice.com.recieptservice.PrinterInterface
            public void printBitmap(Bitmap bitmap) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(PrinterInterface.DESCRIPTOR);
                    _Parcel.writeTypedObject(parcelObtain, bitmap, 0);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // recieptservice.com.recieptservice.PrinterInterface
            public void printBarCode(String str, int i, int i2, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(PrinterInterface.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // recieptservice.com.recieptservice.PrinterInterface
            public void printQRCode(String str, int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(PrinterInterface.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // recieptservice.com.recieptservice.PrinterInterface
            public void setAlignment(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(PrinterInterface.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // recieptservice.com.recieptservice.PrinterInterface
            public void setTextSize(float f) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(PrinterInterface.DESCRIPTOR);
                    parcelObtain.writeFloat(f);
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // recieptservice.com.recieptservice.PrinterInterface
            public void nextLine(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(PrinterInterface.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(9, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // recieptservice.com.recieptservice.PrinterInterface
            public void printTableText(String[] strArr, int[] iArr, int[] iArr2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(PrinterInterface.DESCRIPTOR);
                    parcelObtain.writeStringArray(strArr);
                    parcelObtain.writeIntArray(iArr);
                    parcelObtain.writeIntArray(iArr2);
                    this.mRemote.transact(10, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // recieptservice.com.recieptservice.PrinterInterface
            public void setTextBold(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(PrinterInterface.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(11, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // recieptservice.com.recieptservice.PrinterInterface
            public void beginWork() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(PrinterInterface.DESCRIPTOR);
                    this.mRemote.transact(12, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // recieptservice.com.recieptservice.PrinterInterface
            public void endWork() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(PrinterInterface.DESCRIPTOR);
                    this.mRemote.transact(13, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // recieptservice.com.recieptservice.PrinterInterface
            public void setDark(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(PrinterInterface.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(14, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // recieptservice.com.recieptservice.PrinterInterface
            public void setLineHeight(float f) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(PrinterInterface.DESCRIPTOR);
                    parcelObtain.writeFloat(f);
                    this.mRemote.transact(15, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // recieptservice.com.recieptservice.PrinterInterface
            public void setTextDoubleWidth(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(PrinterInterface.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(16, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // recieptservice.com.recieptservice.PrinterInterface
            public void setTextDoubleHeight(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(PrinterInterface.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(17, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // recieptservice.com.recieptservice.PrinterInterface
            public void printPDF417Code(String str, int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(PrinterInterface.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(18, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // recieptservice.com.recieptservice.PrinterInterface
            public void setCode(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(PrinterInterface.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(19, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // recieptservice.com.recieptservice.PrinterInterface
            public void print128BarCode(String str, int i, int i2, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(PrinterInterface.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(20, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // recieptservice.com.recieptservice.PrinterInterface
            public boolean getScannerStatus() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(PrinterInterface.DESCRIPTOR);
                    this.mRemote.transact(21, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // recieptservice.com.recieptservice.PrinterInterface
            public void checkPSAMCard(int i, PSAMCallback pSAMCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(PrinterInterface.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeStrongInterface(pSAMCallback);
                    this.mRemote.transact(22, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // recieptservice.com.recieptservice.PrinterInterface
            public void activatePSAMCard(int i, PSAMCallback pSAMCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(PrinterInterface.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeStrongInterface(pSAMCallback);
                    this.mRemote.transact(23, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // recieptservice.com.recieptservice.PrinterInterface
            public void deactivatePSAMCard(int i, PSAMCallback pSAMCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(PrinterInterface.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeStrongInterface(pSAMCallback);
                    this.mRemote.transact(24, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // recieptservice.com.recieptservice.PrinterInterface
            public void transmitPSAMCard(int i, byte[] bArr, PSAMCallback pSAMCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(PrinterInterface.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeStrongInterface(pSAMCallback);
                    this.mRemote.transact(25, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }

    public static class _Parcel {
        /* JADX INFO: Access modifiers changed from: private */
        public static <T> T readTypedObject(Parcel parcel, Parcelable.Creator<T> creator) {
            if (parcel.readInt() != 0) {
                return creator.createFromParcel(parcel);
            }
            return null;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static <T extends Parcelable> void writeTypedObject(Parcel parcel, T t, int i) {
            if (t != null) {
                parcel.writeInt(1);
                t.writeToParcel(parcel, i);
            } else {
                parcel.writeInt(0);
            }
        }
    }
}
