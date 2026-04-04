//package com.itsorderkds.ui.settings.print
//
//import android.content.ComponentName
//import android.content.Context
//import android.content.Intent
//import android.content.ServiceConnection
//import android.os.IBinder
//import timber.log.Timber
//import java.lang.reflect.Method
//
///**
// * AIDL Printer Service - drukowanie na drukarkach wbudowanych (H10, Sunmi, itp.)
// *
// * Ta klasa komunikuje się z usługą systemową drukowania (recieptservice.com.recieptservice)
// * za pośrednictwem refleksji (bo interfejs AIDL nie jest dostępny w systemie).
// */
//class AidlPrinterService(private val context: Context) {
//
//    private var printerInterface: Any? = null
//    private var isConnected = false
//
//    private val connection = object : ServiceConnection {
//        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
//            Timber.d("🔗 ===== POŁĄCZENIE Z DRUKARKĄ AIDL =====")
//            Timber.d("���� Pakiet: ${name?.packageName}")
//
//            try {
//                // Refleksja: konwertuj IBinder na interfejs PrinterInterface
//                val serviceClass = Class.forName("recieptservice.com.recieptservice.service.PrinterInterface")
//                val stubClass = Class.forName("recieptservice.com.recieptservice.service.PrinterInterface\$Stub")
//
//                val asInterfaceMethod = stubClass.getDeclaredMethod("asInterface", IBinder::class.java)
//                printerInterface = asInterfaceMethod.invoke(null, service)
//
//                isConnected = true
//                Timber.d("✅ Typ: KLON (recieptservice)")
//                try {
//                    Timber.d("AIDL descriptor: ${service?.interfaceDescriptor}")
//                } catch (_: Exception) {
//                    // ignore descriptor issues
//                }
//
//                // 🔥 Diagnostyka: wypisz dostępne metody interfejsu PrinterInterface
//                try {
//                    val methodsDump = PrinterInterface::class.java.methods
//                        .sortedBy { it.name }
//                        .joinToString("\n") { mm ->
//                            val params = mm.parameterTypes.joinToString(",") { it.simpleName }
//                            "${mm.name}(${params}) : ${mm.returnType.simpleName}"
//                        }
//                    Timber.d("📋 PrinterInterface methods:\n$methodsDump")
//                } catch (e: Exception) {
//                    Timber.e(e, "Nie udało się wypisać metod PrinterInterface")
//                }
//            } catch (e: Exception) {
//                Timber.e(e, "❌ Błąd konwersji interfejsu")
//                isConnected = false
//            }
//        }
//
//        override fun onServiceDisconnected(name: ComponentName?) {
//            Timber.d("❌ ROZŁĄCZENIE Z DRUKARKĄ AIDL")
//            printerInterface = null
//            isConnected = false
//        }
//    }
//
//    fun connect() {
//        if (isConnected) return
//
//        Timber.d("🔄 Łączenie z usługą...")
//        val intent = Intent()
//        intent.setPackage("recieptservice.com.recieptservice")
//        intent.action = "recieptservice.com.recieptservice.service.PrinterInterface"
//
//        try {
//            Timber.d("     Próba Klon H10: ${context.bindService(intent, connection, Context.BIND_AUTO_CREATE)}")
//        } catch (e: Exception) {
//            Timber.e(e, "❌ Błąd bindService")
//        }
//    }
//
//    fun disconnect() {
//        if (isConnected) {
//            try {
//                context.unbindService(connection)
//            } catch (_: Exception) {}
//            isConnected = false
//            printerInterface = null
//        }
//    }
//
//    fun getPrinterStatus(): Int {
//        if (printerInterface == null) return -1
//
//        return try {
//            val method = printerInterface!!.javaClass.getMethod("getPrinterStatus")
//            method.invoke(printerInterface) as Int
//        } catch (e: Exception) {
//            Timber.w("⚠️ Błąd getPrinterStatus: ${e.message}")
//            -1
//        }
//    }
//
//    fun printOrder(formattedContent: String, autoCut: Boolean = false): Boolean {
//        if (printerInterface == null) {
//            connect()
//            Thread.sleep(800)
//            if (printerInterface == null) return false
//        }
//
//        Timber.d("🖨️ [KLON] Rozpoczynam drukowanie (PRINTTEXT)...")
//
//        return try {
//            val printer = printerInterface ?: return false
//
//            // 1. Inicjalizacja
//            invokeMethod(printer, "updatePrinterState")
//            Thread.sleep(300)
//
//            // 2. Usuń tagi - czysty tekst
//            val cleanText = stripAllTags(formattedContent)
//            Timber.v("📝 Czysty tekst (${cleanText.length} znaków):\n$cleanText")
//
//            // 3. ✅ Druk tekstu metodą printText()
//            invokeMethod(printer, "printText", cleanText)
//            Timber.d("✅ Wysłano ${cleanText.length} znaków metodą printText()")
//
//            // 4. Feed
//            Thread.sleep(300)
//            invokeMethod(printer, "nextLine", 3)
//
//            // 5. Opcjonalne cięcie
//            if (autoCut) {
//                Thread.sleep(100)
//                invokeMethod(printer, "cutPaper")
//            }
//
//            Thread.sleep(500)
//            true
//        } catch (e: Exception) {
//            Timber.e(e, "❌ Błąd drukowania")
//            false
//        }
//    }
//
//    /**
//     * Usuwa WSZYSTKIE tagi DantSu i HTML
//     */
//    private fun stripAllTags(text: String): String {
//        var result = text
//
//        // Usuń tagi DantSu: [C], [L], [R], [B], itp.
//        result = result.replace(Regex("\\[[A-Z]+\\]"), "")
//
//        // Usuń tagi HTML: <b>, <font>, <center>, itp.
//        result = result.replace(Regex("<[^>]+>"), "")
//
//        // Usuń nadmiarowe puste linie (max 2 pod rząd)
//        result = result.replace(Regex("\n{3,}"), "\n\n")
//
//        return result.trim()
//    }
//
//    /**
//     * Helper do wywoływania metod przez refleksję
//     */
//    private fun invokeMethod(obj: Any, methodName: String, vararg args: Any?): Any? {
//        return try {
//            val argTypes = args.map {
//                when (it) {
//                    is String -> String::class.java
//                    is Int -> Int::class.javaPrimitiveType ?: Integer::class.java
//                    is Boolean -> Boolean::class.javaPrimitiveType ?: java.lang.Boolean::class.java
//                    else -> it?.javaClass ?: Any::class.java
//                }
//            }.toTypedArray()
//
//            val method: Method? = obj.javaClass.getMethod(methodName, *argTypes)
//            return method?.invoke(obj, *args)
//        } catch (e: Exception) {
//            Timber.w("⚠️ Błąd metody $methodName: ${e.message}")
//            null
//        }
//    }
//
