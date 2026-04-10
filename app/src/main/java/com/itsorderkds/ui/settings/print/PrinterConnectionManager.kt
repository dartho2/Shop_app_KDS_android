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
     * Strategia dla sieci: timeout 10s, 3 próby z backoffem.
     * ECONNREFUSED = drukarka włączona ale port 9100 zablokowany lub zły adres IP.
     * SocketTimeoutException = drukarka niedostępna / zły IP / firewall.
     */
    private suspend fun <T> connectNetwork(
        connection: DeviceConnection,
        block: suspend (DeviceConnection) -> T
    ): T {
        Timber.d("🌐 Network connection strategy")

        var connected = false
        var lastError: Exception? = null
        val maxAttempts = 3

        for (attempt in 1..maxAttempts) {
            try {
                Timber.d("🔌 Network connect attempt #$attempt/$maxAttempts...")
                connection.connect()
                connected = true
                Timber.d("✅ Network connected on attempt #$attempt")
                break
            } catch (e: Exception) {
                lastError = e
                val hint = when {
                    e.message?.contains("ECONNREFUSED") == true ->
                        "ECONNREFUSED — sprawdź: czy port 9100 jest otwarty, czy drukarka jest gotowa do drukowania"
                    e.message?.contains("timeout") == true || e.message?.contains("SocketTimeout") == true ->
                        "Timeout — sprawdź: adres IP drukarki, czy drukarka jest w tej samej sieci Wi-Fi"
                    else -> e.message ?: "nieznany błąd"
                }
                Timber.w("❌ Network connect failed (attempt #$attempt): $hint")

                try { connection.disconnect() } catch (_: Exception) {}

                if (attempt < maxAttempts) {
                    val backoffMs = 1000L * attempt  // 1s, 2s
                    Timber.d("⏳ Retry za ${backoffMs}ms...")
                    delay(backoffMs)
                }
            }
        }

        if (!connected) {
            val errorHint = when {
                lastError?.message?.contains("ECONNREFUSED") == true ->
                    "Połączenie odrzucone (ECONNREFUSED) — port 9100 zablokowany lub drukarka nie akceptuje RAW print"
                lastError?.message?.contains("timeout") == true || lastError?.message?.contains("SocketTimeout") == true ->
                    "Timeout połączenia — sprawdź adres IP i czy drukarka jest w tej samej sieci"
                else ->
                    "Network connect failed after $maxAttempts prób"
            }
            Timber.e("💥 Network connection failed: $errorHint")
            throw Exception(errorHint, lastError)
        }

        return executeAndCleanup(connection, block, flushMs = 200, cleanupMs = 300)
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
                    throw IllegalStateException("Use BluetoothPrintersConnections to get BT connection")
                }
                PrinterConnectionType.NETWORK -> {
                    val ip = printer.networkIp ?: throw IllegalArgumentException("Network IP not set")
                    val port = printer.networkPort
                    Timber.d("Creating TCP connection: $ip:$port (timeout=10000ms)")
                    TcpConnection(ip, port, 10000)
                }
                PrinterConnectionType.BUILTIN -> {
                    throw UnsupportedOperationException("Builtin printer not yet implemented")
                }
            }
        }

        /**
         * Drukuje czysty tekst przez TCP socket — dla zwykłych drukarek sieciowych
         * (biurowe, laserowe, HP, Canon, Brother itp.) które nie obsługują ESC/POS.
         *
         * Wysyła tekst jako bajty UTF-8 przez port 9100 (lub skonfigurowany).
         * Większość drukarek sieciowych obsługuje ten tryb jako "RAW text".
         */
        suspend fun printPlainTextOverNetwork(
            ip: String,
            port: Int,
            text: String,
            timeoutMs: Int = 10000
        ) = withContext(Dispatchers.IO) {
            Timber.d("🌐 Plain text print: $ip:$port (${text.length} znaków)")
            val socket = java.net.Socket()
            try {
                socket.connect(java.net.InetSocketAddress(ip, port), timeoutMs)
                socket.soTimeout = timeoutMs
                val out = socket.getOutputStream()
                // Wyślij tekst jako UTF-8
                out.write(text.toByteArray(Charsets.UTF_8))
                // Form feed — wymusza wydruk na niektórych drukarkach
                out.write(0x0C)
                out.flush()
                Timber.d("✅ Plain text wysłany do $ip:$port")
            } finally {
                try { socket.close() } catch (_: Exception) {}
            }
        }
    }
}

