package com.itsorderkds.ui.settings.print

import android_serialport_api.SerialPort
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.exceptions.EscPosConnectionException
import java.io.File
import java.io.IOException

/**
 * Niestandardowe połączenie dla biblioteki DantSu, obsługujące port szeregowy (Serial Port).
 * Wersja dostosowana do biblioteki io.github.xmaihh:serialport:2.1.1
 */
class SerialConnection(
    private val portName: String = "/dev/ttyS1", // Domyślny port dla H10
    private val baudRate: Int = 115200           // Standard dla H10
) : DeviceConnection() {

    private var serialPort: SerialPort? = null

    /**
     * Otwiera port szeregowy.
     */
    override fun connect(): DeviceConnection {
        if (isConnected()) {
            return this
        }
        try {
            val device = File(portName)
            if (!device.exists()) {
                throw EscPosConnectionException("Plik portu nie istnieje: $portName")
            }

            // Konstruktor biblioteki io.github.xmaihh:serialport:2.1.1 wymaga 7 parametrów:
            // 1. File (urządzenie)
            // 2. Baudrate
            // 3. DataBits (8)
            // 4. StopBits (1)
            // 5. Parity (0 - None)
            // 6. FlowControl (0 - wyłączone dla drukarek termicznych)
            // 7. Flags (0)
            val port = SerialPort(device, baudRate, 8, 1, 0, 0, 0)

            serialPort = port

            // KLUCZOWE: Przypisanie strumienia wyjściowego do klasy rodzica (DeviceConnection)
            // Bez tego DantSu nie wie, gdzie pisać!
            this.outputStream = port.outputStream

            if (this.outputStream == null) {
                throw EscPosConnectionException("Otwarto port, ale OutputStream jest null")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            serialPort = null
            throw EscPosConnectionException("Błąd otwarcia portu: ${e.message}")
        }
        return this
    }

    /**
     * Zamyka port.
     */
    override fun disconnect(): DeviceConnection {
        try {
            serialPort?.close()
        } catch (_: Exception) {
            // Ignorujemy błędy przy zamykaniu
        } finally {
            this.serialPort = null
            this.outputStream = null
        }
        return this
    }

    /**
     * Główna metoda zapisu.
     */
    override fun write(bytes: ByteArray) {
        try {
            this.outputStream?.write(bytes)
            this.outputStream?.flush()
        } catch (e: IOException) {
            e.printStackTrace()
            throw EscPosConnectionException("Błąd zapisu do portu: ${e.message}")
        }
    }

    override fun isConnected(): Boolean {
        return serialPort != null && outputStream != null
    }
}
