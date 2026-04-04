package com.itsorderkds.ui.settings.printer

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.itsorderkds.util.AppPrefs
import timber.log.Timber

/**
 * Uproszczony PrinterManager — tylko core funkcje do drukowania.
 */
object PrinterManager {

    private const val SPP_UUID = "00001101-0000-1000-8000-00805f9b34fb"

    /** Zwraca połączenie do skonfigurowanej drukarki */
    fun getConnection(ctx: Context): DeviceConnection? {
        val (type, id) = AppPrefs.getPrinter(ctx)
        Timber.d("PrinterManager.getConnection: type=$type, id=$id")

        if (type != "bluetooth" || id.isNullOrBlank()) {
            Timber.w("PrinterManager.getConnection: no configured printer (type=$type, id=$id)")
            return null
        }

        return try {
            val printers = BluetoothPrintersConnections().list
            Timber.d("PrinterManager.getConnection: found ${printers?.size ?: 0} BT printers")

            val conn = printers?.firstOrNull { it.device.address.equals(id, ignoreCase = true) }
            if (conn != null) {
                logDevice("getConnection(list)", conn.device)
                return if (hasSpp(conn.device)) {
                    Timber.d("PrinterManager.getConnection: SUCCESS - found printer with SPP")
                    conn
                } else {
                    Timber.w("PrinterManager.getConnection: printer has no SPP UUID")
                    null
                }
            }
            Timber.w("PrinterManager.getConnection: printer not found in BT list, trying fallback")
            null
        } catch (ex: Exception) {
            Timber.w(ex, "PrinterManager.getConnection: failed, try fallback")
            val fallback = findBt(ctx, id)
            fallback?.also { logDevice("getConnection(fallback)", it.device) }
            if (fallback != null && hasSpp(fallback.device)) fallback else null
        }
    }

    /** Zwraca połączenie do drukarki o konkretnym MAC (niezależnie od AppPrefs) */
    fun getConnectionById(ctx: Context, deviceId: String): DeviceConnection? {
        if (deviceId.isBlank()) return null
        return try {
            val printers = BluetoothPrintersConnections().list
            val conn = printers?.firstOrNull { it.device.address.equals(deviceId, ignoreCase = true) }
            if (conn != null) {
                logDevice("getConnectionById(list)", conn.device)
                return if (hasSpp(conn.device)) conn else null
            }
            // fallback
            val fb = findBt(ctx, deviceId)
            fb?.also { logDevice("getConnectionById(fallback)", it.device) }
            if (fb != null && hasSpp(fb.device)) fb else null
        } catch (t: Throwable) {
            Timber.w(t, "PrinterManager.getConnectionById: failed")
            val fb = findBt(ctx, deviceId)
            fb?.also { logDevice("getConnectionById(fallback-ex)", it.device) }
            if (fb != null && hasSpp(fb.device)) fb else null
        }
    }

    /** Wysyła dane na drukarkę */
    fun write(connection: DeviceConnection, bytes: ByteArray) {
        try {
            connection.javaClass.getMethod("write", ByteArray::class.java).invoke(connection, bytes)
            Timber.d("PrinterManager.write: wrote %d bytes", bytes.size)
        } catch (t: Throwable) {
            Timber.w(t, "PrinterManager.write: failed")
            throw t
        }
    }

    /** Wysyła ESC @ (reset drukarki) */
    fun sendInit(connection: DeviceConnection): Boolean {
        return try {
            write(connection, byteArrayOf(0x1B.toByte(), 0x40.toByte()))
            Timber.d("PrinterManager.sendInit: sent ESC @")
            true
        } catch (t: Throwable) {
            Timber.w(t, "PrinterManager.sendInit: failed")
            false
        }
    }

    /** Wysyła ESC t n (wybór codepage) */
    fun sendSelectCodepage(connection: DeviceConnection, n: Int): Boolean {
        return try {
            write(connection, byteArrayOf(0x1B.toByte(), 0x74.toByte(), n.toByte()))
            Timber.d("PrinterManager.sendSelectCodepage: sent ESC t %d", n)
            true
        } catch (t: Throwable) {
            Timber.w(t, "PrinterManager.sendSelectCodepage: failed")
            false
        }
    }

    /** Wysyła komendę ucięcia papieru (GS V 0) */
    fun sendCutPaper(connection: DeviceConnection): Boolean {
        return try {
            write(connection, byteArrayOf(0x1D.toByte(), 0x56.toByte(), 0x00.toByte()))
            Timber.d("PrinterManager.sendCutPaper: sent GS V")
            true
        } catch (t: Throwable) {
            Timber.w(t, "PrinterManager.sendCutPaper: failed")
            false
        }
    }

    /** Wysyła LF + GS V (feed + cut) */
    fun feedAndCut(connection: DeviceConnection, feedLines: Int = 3): Boolean {
        return try {
            // Feed
            write(connection, ByteArray(feedLines) { 0x0A })
            Thread.sleep(100)
            // Cut (GS V)
            write(connection, byteArrayOf(0x1D.toByte(), 0x56.toByte(), 0x00.toByte()))
            Timber.d("PrinterManager.feedAndCut: success")
            true
        } catch (t: Throwable) {
            Timber.w(t, "PrinterManager.feedAndCut: failed")
            false
        }
    }

    /** Wysyła surowe bajty (dla kompatybilności) */
    fun sendRaw(connection: DeviceConnection, bytes: ByteArray, keepConnectionOpen: Boolean = false): Boolean {
        return try {
            write(connection, bytes)
            true
        } catch (t: Throwable) {
            Timber.w(t, "PrinterManager.sendRaw: failed")
            false
        }
    }

    /** Wrapper dla sendSelectCodepage (dla kompatybilności) */
    fun sendSelectCodepageVariants(connection: DeviceConnection, n: Int): Boolean {
        return sendSelectCodepage(connection, n)
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun hasSpp(device: BluetoothDevice?): Boolean {
        if (device == null) return false

        val uuids = device.uuids
        Timber.d("hasSpp: checking device ${device.name} (${device.address})")
        Timber.d("hasSpp: device.type=${device.type}, uuids=${uuids?.joinToString { it.uuid.toString() } ?: "null"}")

        // DEVICE_TYPE_LE = 2 (BLE only) - odrzuć
        // DEVICE_TYPE_CLASSIC = 1 - akceptuj
        // DEVICE_TYPE_DUAL = 3 - akceptuj
        // DEVICE_TYPE_UNKNOWN = 0 - akceptuj (mogą być drukarki, które raportują typ 0)
        val isBleOnly = device.type == BluetoothDevice.DEVICE_TYPE_LE

        if (isBleOnly) {
            Timber.w("hasSpp: device is BLE-only (type=${device.type}), rejecting")
            return false
        }

        // Jeśli brak UUID (niektóre drukarki nie raportują), próbuj i tak
        if (uuids == null || uuids.isEmpty()) {
            Timber.d("hasSpp: no UUIDs reported, accepting device anyway (type=${device.type})")
            return true
        }

        // Sprawdź czy ma SPP UUID
        val hasSppUuid = uuids.any { it.uuid.toString().equals(SPP_UUID, ignoreCase = true) }
        Timber.d("hasSpp: hasSppUuid=$hasSppUuid")
        return hasSppUuid
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun logDevice(tag: String, device: BluetoothDevice?) {
        if (device == null) return
        val typeStr = when (device.type) {
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> "CLASSIC"
            BluetoothDevice.DEVICE_TYPE_LE -> "LE"
            BluetoothDevice.DEVICE_TYPE_DUAL -> "DUAL"
            else -> "UNKNOWN"
        }
        val uuids = device.uuids?.joinToString { it.uuid.toString() } ?: "none"
        Timber.d("%s: %s (%s) type=%s uuids=%s", tag, device.name ?: device.address, device.address, typeStr, uuids)
    }

    /* Fallback BT discovery */
    private fun findBt(ctx: Context, macOrName: String): BluetoothConnection? {
        @Suppress("DEPRECATION")
        val adapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter() ?: return null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        val device = if (macOrName.contains(":"))
            adapter.getRemoteDevice(macOrName)
        else
            adapter.bondedDevices.firstOrNull { it.name == macOrName }

        return device?.let { BluetoothConnection(it) }
    }
}
