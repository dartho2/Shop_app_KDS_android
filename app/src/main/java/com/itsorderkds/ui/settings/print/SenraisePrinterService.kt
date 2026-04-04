package com.itsorderkds.ui.settings.print

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import timber.log.Timber
import com.senraise.printer.IService

class SenraisePrinterService(private val context: Context) {

    private var printerService: IService? = null
    private var isConnected = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            printerService = IService.Stub.asInterface(service)
            isConnected = true
            Timber.d("✅ Połączono z usługą Senraise Printer")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            printerService = null
            isConnected = false
            Timber.w("❌ Rozłączono usługę Senraise")
        }
    }

    fun connect() {
        if (isConnected) return
        val intent = Intent().apply {
            setPackage("com.senraise.printer")
            action = "com.senraise.printer.IService"
        }

        try {
            val result = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            if (!result) {
                Timber.e("❌ Nie udało się połączyć z com.senraise.printer. Próbuję fallback...")
                connectFallback()
            }
        } catch (e: Exception) {
            Timber.e(e, "Błąd bindService Senraise")
        }
    }

    private fun connectFallback() {
        val intent = Intent().apply {
            setPackage("recieptservice.com.recieptservice")
            action = "com.senraise.printer.IService"
        }
        try {
            val result = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            if (result) Timber.d("✅ Połączono przez fallback (recieptservice)!")
            else Timber.e("❌ Fallback też nie zadziałał.")
        } catch (e: Exception) {
            Timber.e(e, "Błąd fallback")
        }
    }

    fun disconnect() {
        if (isConnected) {
            try { context.unbindService(connection) } catch (_: Exception) {}
            isConnected = false
        }
    }

    fun printText(text: String, autoCut: Boolean = false): Boolean {
        if (printerService == null) {
            connect()
            Thread.sleep(500)
            if (printerService == null) return false
        }

        return try {
            val service = printerService!!
            service.updatePrinterState()
            service.setAlign(1) // center
            service.setFont(24)
            service.printText(text + "\n\n\n")
            if (autoCut) service.cutPaper()
            Timber.d("✅ Wysłano do Senraise AIDL")
            true
        } catch (e: Exception) {
            Timber.e(e, "Błąd druku Senraise")
            false
        }
    }

    fun getStatus(): Int {
        return try {
            printerService?.getPrinterStatus() ?: -1
        } catch (e: Exception) {
            Timber.e(e, "Błąd getPrinterStatus")
            -1
        }
    }
}

