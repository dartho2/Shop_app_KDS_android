//package com.itsorderkds.ui.settings.print
//
//import com.dantsu.escposprinter.connection.DeviceConnection
//import timber.log.Timber
//
///** Minimal ESC/POS helpers, only safe commands */
//object EscPosCommandBuilder {
//    fun init(connection: DeviceConnection) {
//        write(connection, byteArrayOf(0x1B, 0x40)) // ESC @
//    }
//    fun selectCodepage(connection: DeviceConnection, n: Int) {
//        write(connection, byteArrayOf(0x1B, 0x74, n.toByte())) // ESC t n
//    }
//    fun feed(connection: DeviceConnection, lines: Int = 3) {
//        write(connection, ByteArray(lines) { 0x0A })
//    }
//    fun cut(conn: DeviceConnection) {
//        // Feed papieru: ESC d 10 (10 linii)
//        write(conn, byteArrayOf(0x1B, 0x64, 0x0A)) // ESC d 10
//        Thread.sleep(250)
//
//        // Cięcie papieru: GS V 0 (full cut)
//        write(conn, byteArrayOf(0x1D, 0x56, 0x00)) // GS V 0
//        Thread.sleep(500)
//        Timber.d("EscPosCommandBuilder: cut performed (ESC d 10 + GS V 0)")
//    }
//    private fun write(connection: DeviceConnection, bytes: ByteArray) {
//        try {
//            connection.javaClass.getMethod("write", ByteArray::class.java).invoke(connection, bytes)
//            Timber.d("EscPosCommandBuilder: wrote %d bytes", bytes.size)
//        } catch (t: Throwable) {
//            Timber.w(t, "EscPosCommandBuilder: write failed")
//            throw t
//        }
//    }
//}
