//package com.itsorderkds.ui.settings.print
//
//import android.content.ComponentName
//import android.content.Context
//import android.content.Intent
//import android.content.ServiceConnection
//import android.os.IBinder
//import timber.log.Timber
//import java.nio.charset.Charset
//
///**
// * Adapter dla drukarek Sunmi H10.
// * Komunikuje się z serwisem drukowania Sunmi poprzez AIDL.
// *
// * Sunmi drukarka H10 dostarcza serwis drukowania dostępny w systemie.
// */
//class SunmiPrinterAdapter(private val context: Context) {
//
//    private var printerService: ISunmiPrinterService? = null
//    private var isConnected = false
//    private val serviceConnection = object : ServiceConnection {
//        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
//            Timber.d("✅ Sunmi printer service connected")
//            printerService = ISunmiPrinterService.Stub.asInterface(service)
//            isConnected = true
//        }
//
//        override fun onServiceDisconnected(name: ComponentName?) {
//            Timber.d("❌ Sunmi printer service disconnected")
//            isConnected = false
//        }
//    }
//
//    fun connect(): Boolean {
//        return try {
//            val intent = Intent().apply {
//                setPackage("com.sunmi.assistant")
//                action = "com.sunmi.thermal.printerservice"
//            }
//            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
//            Timber.d("🔌 Connecting to Sunmi printer service...")
//            true
//        } catch (e: Exception) {
//            Timber.e(e, "❌ Failed to connect to Sunmi printer service")
//            false
//        }
//    }
//
//    fun printText(text: String, encoding: String = "UTF-8"): Boolean {
//        return try {
//            if (!isConnected) {
//                Timber.e("❌ Sunmi printer service not connected")
//                return false
//            }
//
//            val service = printerService ?: return false
//
//            // Konwertuj tekst na bajty
//            val bytes = text.toByteArray(Charset.forName(encoding))
//
//            Timber.d("📋 Printing ${bytes.size} bytes via Sunmi service")
//
//            // Wyślij do serwisu
//            service.printRaw(bytes)
//
//            Timber.d("✅ Sunmi print sent")
//            true
//        } catch (e: Exception) {
//            Timber.e(e, "❌ Sunmi print failed")
//            false
//        }
//    }
//
//    fun cutPaper(): Boolean {
//        return try {
//            if (!isConnected) return false
//            printerService?.cutPaper()
//            Timber.d("✂️ Paper cut command sent")
//            true
//        } catch (e: Exception) {
//            Timber.e(e, "❌ Cut paper failed")
//            false
//        }
//    }
//
//    fun disconnect() {
//        try {
//            if (isConnected) {
//                context.unbindService(serviceConnection)
//                isConnected = false
//                Timber.d("🔌 Disconnected from Sunmi printer service")
//            }
//        } catch (e: Exception) {
//            Timber.w(e, "⚠️ Error disconnecting from Sunmi service")
//        }
//    }
//}
//
///**
// * Interfejs AIDL dla serwisu drukarki Sunmi.
// * To jest stub - faktyczne AIDL jest w Sunmi SDK.
// * Tu mamy minimalne interfejsy do drukowania.
// */
//interface ISunmiPrinterService {
//    fun printRaw(data: ByteArray?)
//    fun cutPaper()
//    fun addString(text: String?)
//    fun printString(text: String?)
//
//    abstract class Stub : ISunmiPrinterService {
//        companion object {
//            fun asInterface(obj: IBinder?): ISunmiPrinterService {
//                return obj as? ISunmiPrinterService
//                    ?: throw IllegalArgumentException("Invalid AIDL binder")
//            }
//        }
//    }
//}
//
