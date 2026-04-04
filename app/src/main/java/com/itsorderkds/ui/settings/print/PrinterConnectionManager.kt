package com.itsorderkds.ui.settings.print

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.connection.tcp.TcpConnection
import com.itsorderkds.data.model.Printer
import com.itsorderkds.data.model.PrinterConnectionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Zarządza cyklem życia połączenia z drukarką:
 * connect → write → flush → disconnect (serializowane przez Mutex).
 *
 * Obsługuje:
 * - Bluetooth (RFCOMM/SPP)
 * - Sieć (TCP/IP RAW printing port 9100)
 * - Wbudowana drukarka (Android Print Service)
 *
 * Używa Mutex zamiast AtomicBoolean - bardziej idiomatyczne dla coroutines.
 * Pozwala wielu kelnerom bezpiecznie drukować na tej samej drukarce.
 */
class PrinterConnectionManager {
    private val mutex = Mutex()

    /**
     * Uniwersalna funkcja do zarządzania połączeniem z drukarką.
     * Automatycznie dostosowuje strategię retry/timeout w zależności od typu połączenia.
     */
    @SuppressLint("MissingPermission")
    suspend fun <T> withConnection(
        connection: DeviceConnection,
        printer: Printer? = null, // Opcjonalny kontekst drukarki (dla optymalizacji)
        block: suspend (DeviceConnection) -> T
    ): T = withContext(Dispatchers.IO) {
        val connectionType = printer?.connectionType ?: PrinterConnectionType.BLUETOOTH

        mutex.withLock {
            Timber.d("🔒 PrinterConnectionManager: mutex locked (type=$connectionType)")

            // Strategia połączenia zależy od typu
            when (connectionType) {
                PrinterConnectionType.BLUETOOTH -> connectBluetooth(connection, block)
                PrinterConnectionType.NETWORK -> connectNetwork(connection, block)
                PrinterConnectionType.BUILTIN -> connectBuiltin(connection, block)
            }
        }
    }

    /**
     * Strategia dla Bluetooth: cancel discovery + retry z backoff.
     */
    @SuppressLint("MissingPermission")
    private suspend fun <T> connectBluetooth(
        connection: DeviceConnection,
        block: suspend (DeviceConnection) -> T
    ): T {
        Timber.d("📶 Bluetooth connection strategy")

        // 1. Cancel discovery - krytyczne dla stabilności RFCOMM
        try {
            BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery()
            Timber.d("✅ BT discovery cancelled")
        } catch (e: Exception) {
            Timber.w(e, "⚠️ cancelDiscovery failed (non-critical)")
        }

        var connected = false
        var lastError: Exception? = null
        val maxAttempts = 3

        // 2. Retry logic z progressive backoff
        for (attempt in 1..maxAttempts) {
            try {
                Timber.d("🔌 Connect attempt #$attempt/$maxAttempts...")
                connection.connect()
                connected = true
                Timber.d("✅ Connected on attempt #$attempt")
                break
            } catch (e: Exception) {
                lastError = e
                Timber.w("❌ Connect failed (attempt #$attempt): ${e.message}")

                // Cleanup przed retry
                try {
                    connection.disconnect()
                    Timber.d("🧹 Socket cleanup OK")
                } catch (_: Exception) {
                    Timber.d("🧹 Socket cleanup skipped (already closed)")
                }

                // Progressive backoff (200ms, 400ms, 600ms)
                if (attempt < maxAttempts) {
                    val backoffMs = 200L * attempt
                    Timber.d("⏳ Waiting ${backoffMs}ms before retry...")
                    delay(backoffMs)

                    // Re-cancel discovery przed kolejną próbą
                    try {
                        BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery()
                    } catch (_: Exception) {}
                }
            }
        }

        // 3. Fallback: długi cooldown + ostatnia próba (dla DUAL/BLE combo)
        if (!connected) {
            Timber.w("🔄 All attempts failed, trying fallback after cooldown...")
            try {
                BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery()
            } catch (_: Exception) {}

            delay(1500) // Dłuższy cooldown dla BT stack reset

            try {
                Timber.d("🔌 Fallback connect...")
                connection.connect()
                connected = true
                Timber.d("✅ Fallback connect succeeded!")
            } catch (e: Exception) {
                lastError = e
                Timber.e(e, "❌ Fallback connect failed")
            }
        }

        if (!connected) {
            Timber.e("💥 All BT connection attempts exhausted")
            throw Exception("BT connect failed after $maxAttempts retries + fallback", lastError)
        }

        return executeAndCleanup(connection, block, flushMs = 150, cleanupMs = 300)
    }

    /**
     * Strategia dla sieci: szybsze timeout, mniej retry (sieć jest stabilniejsza).
     */
    private suspend fun <T> connectNetwork(
        connection: DeviceConnection,
        block: suspend (DeviceConnection) -> T
    ): T {
        Timber.d("🌐 Network connection strategy")

        var connected = false
        var lastError: Exception? = null
        val maxAttempts = 2 // Sieć: mniej retry (albo działa, albo nie)

        for (attempt in 1..maxAttempts) {
            try {
                Timber.d("🔌 Network connect attempt #$attempt/$maxAttempts...")
                connection.connect()
                connected = true
                Timber.d("✅ Network connected on attempt #$attempt")
                break
            } catch (e: Exception) {
                lastError = e
                Timber.w("❌ Network connect failed (attempt #$attempt): ${e.message}")

                try { connection.disconnect() } catch (_: Exception) {}

                if (attempt < maxAttempts) {
                    delay(500) // Krótki backoff dla sieci
                }
            }
        }

        if (!connected) {
            Timber.e("💥 Network connection failed")
            throw Exception("Network connect failed after $maxAttempts retries", lastError)
        }

        return executeAndCleanup(connection, block, flushMs = 100, cleanupMs = 200)
    }

    /**
     * Strategia dla wbudowanej drukarki: brak retry (synchroniczne API Androida).
     */
    private suspend fun <T> connectBuiltin(
        connection: DeviceConnection,
        block: suspend (DeviceConnection) -> T
    ): T {
        Timber.d("🖨️ Builtin printer strategy")

        try {
            connection.connect()
            Timber.d("✅ Builtin printer connected")
        } catch (e: Exception) {
            Timber.e(e, "❌ Builtin printer connect failed")
            throw Exception("Builtin printer not available", e)
        }

        return executeAndCleanup(connection, block, flushMs = 0, cleanupMs = 100)
    }

    /**
     * Wspólna logika: wykonaj block, flush buffer, disconnect.
     */
    private suspend fun <T> executeAndCleanup(
        connection: DeviceConnection,
        block: suspend (DeviceConnection) -> T,
        flushMs: Long,
        cleanupMs: Long
    ): T {
        // 1. Execute user block (drukowanie)
        val result = try {
            Timber.d("📄 Executing print block...")
            block(connection)
        } catch (e: Exception) {
            Timber.e(e, "❌ Print block failed")
            throw e
        }

        // 2. Flush buffer (jeśli potrzebny)
        if (flushMs > 0) {
            try {
                Timber.d("⏳ Flushing buffer (${flushMs}ms)...")
                delay(flushMs)
            } catch (_: Exception) {}
        }

        // 3. Disconnect + cooldown
        try {
            connection.disconnect()
            if (cleanupMs > 0) {
                Timber.d("✅ Disconnected, waiting ${cleanupMs}ms for cleanup...")
                delay(cleanupMs)
            }
        } catch (e: Exception) {
            Timber.w(e, "⚠️ Disconnect warning (non-critical)")
            if (cleanupMs > 0) delay(cleanupMs)
        }

        Timber.d("🔓 PrinterConnectionManager: mutex released")
        return result
    }

    companion object {
        /**
         * Factory: tworzy odpowiednie połączenie na podstawie konfiguracji drukarki.
         */
        fun createConnection(printer: Printer): DeviceConnection {
            return when (printer.connectionType) {
                PrinterConnectionType.BLUETOOTH -> {
                    // DantSu obsługuje to przez BluetoothPrintersConnections().list
                    throw IllegalStateException("Use BluetoothPrintersConnections to get BT connection")
                }
                PrinterConnectionType.NETWORK -> {
                    val ip = printer.networkIp ?: throw IllegalArgumentException("Network IP not set")
                    val port = printer.networkPort
                    Timber.d("Creating TCP connection: $ip:$port")
                    TcpConnection(ip, port)
                }
                PrinterConnectionType.BUILTIN -> {
                    // Android Print Service - wymaga dedykowanej implementacji
                    throw UnsupportedOperationException("Builtin printer not yet implemented")
                }
            }
        }
    }
}

