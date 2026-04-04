package com.itsorderkds.ui.settings.print

import recieptservice.com.recieptservice.PrinterInterface
import timber.log.Timber

/**
 * Renderer wykonujący wywołania AIDL na podstawie sparsowanych segmentów formatowania.
 *
 * Obsługuje 3 typy interfejsów AIDL:
 * - CLONE (PrinterInterface) - klony Senraise używane w H10
 * - SENRAISE (IService) - oryginalna drukarka Senraise
 * - WOYOU (IWoyouService) - Sunmi i kompatybilne
 *
 * UŻYCIE:
 * ```kotlin
 * val segments = AidlFormattingParser.parse(formattedText)
 * val success = AidlFormattingRenderer.renderClone(cloneService, segments, autoCut = true)
 * ```
 */
object AidlFormattingRenderer {

    /**
     * Renderuje sformatowany tekst na drukarce CLONE (PrinterInterface).
     *
     * To jest główna metoda dla terminala H10 i podobnych klonów.
     *
     * @param service Instancja PrinterInterface (AIDL)
     * @param segments Lista sparsowanych segmentów z formatowaniem
     * @param autoCut Czy automatycznie ciąć papier po wydrukowaniu
     * @return true jeśli drukowanie powiodło się, false w przeciwnym razie
     */
    fun renderClone(
        service: PrinterInterface,
        segments: List<AidlFormattingParser.FormattedSegment>,
        autoCut: Boolean = false
    ): Boolean {
        return runCatching {
            Timber.d("🖨️ [CLONE RENDERER] Start: ${segments.size} segmentów, autoCut=$autoCut")

            service.beginWork()

            segments.forEachIndexed { index, segment ->

                // 1. Ustaw wyrównanie (0=left, 1=center, 2=right)
                try {
                    service.setAlignment(segment.alignment)
                } catch (e: Exception) {
                    Timber.w("⚠️ setAlignment failed: ${e.message}")
                }

                // 2. Ustaw pogrubienie
                try {
                    service.setTextBold(segment.bold)
                } catch (e: Exception) {
                    Timber.w("⚠️ setTextBold failed: ${e.message}")
                }

                // 3. Ustaw podwójną szerokość
                try {
                    service.setTextDoubleWidth(segment.doubleWidth)
                } catch (e: Exception) {
                    Timber.w("⚠️ setTextDoubleWidth failed: ${e.message}")
                }

                // 4. Ustaw podwójną wysokość
                try {
                    service.setTextDoubleHeight(segment.doubleHeight)
                } catch (e: Exception) {
                    Timber.w("⚠️ setTextDoubleHeight failed: ${e.message}")
                }

                // 5. Drukuj tekst segmentu
                try {
                    service.printText(segment.text)
                } catch (e: Exception) {
                    Timber.e(e, "❌ printText failed dla segmentu $index")
                    throw e  // Przerwij całe drukowanie
                }

                // 6. Reset formatowania po każdym segmencie (bezpieczniejsze dla starszych drukarek)
                try {
                    service.setTextBold(false)
                    service.setTextDoubleWidth(false)
                    service.setTextDoubleHeight(false)
                    service.setAlignment(0)  // powrót do left
                } catch (e: Exception) {
                    Timber.w("⚠️ reset formatowania failed: ${e.message}")
                    // Nie przerywaj - to nie jest krytyczne
                }
            }

            // 7. Dodaj odstęp przed cięciem (3 puste linie)
            try {
                service.nextLine(3)
            } catch (e: Exception) {
                Timber.w("⚠️ nextLine failed: ${e.message}")
            }

            service.endWork()

            // 8. Opcjonalne cięcie papieru
            if (autoCut) {
                try {
                    // CLONE może nie mieć dedykowanej metody cutPaper()
                    // Używamy printEpson() z komendą ESC/POS cięcia
                    val cutCommand = byteArrayOf(
                        0x1D.toByte(), 0x56.toByte(), 0x00.toByte()  // GS V 0 (full cut)
                    )
                    service.printEpson(cutCommand)
                    Timber.d("✂️ [CLONE] Papier przycięty")
                } catch (e: Exception) {
                    Timber.w(e, "⚠️ autoCut failed (może nie być wspierany)")
                    // Nie przerywaj - wydruk jest gotowy
                }
            }

            Timber.d("✅ [CLONE RENDERER] Sukces: ${segments.size} segmentów wydrukowanych")
            true

        }.onFailure { e ->
            Timber.e(e, "❌ [CLONE RENDERER] Błąd drukowania")
        }.getOrDefault(false)
    }

    /**
     * Renderuje sformatowany tekst na drukarce SENRAISE (IService - oryginał).
     *
     * UWAGA: Senraise ma prostsze API - nie wszystkie funkcje są dostępne.
     *
     * @param service Instancja com.senraise.printer.IService (AIDL)
     * @param segments Lista sparsowanych segmentów
     * @param autoCut Czy automatycznie ciąć papier
     * @return true jeśli sukces
     */
    fun renderSenraise(
        service: com.senraise.printer.IService,
        segments: List<AidlFormattingParser.FormattedSegment>,
        autoCut: Boolean = false
    ): Boolean {
        return runCatching {
            Timber.d("🖨️ [SENRAISE RENDERER] Start: ${segments.size} segmentów")

            service.updatePrinterState()

            segments.forEachIndexed { index, segment ->

                // 1. Ustaw wyrównanie (0=left, 1=center, 2=right)
                try {
                    service.setAlign(segment.alignment)
                } catch (e: Exception) {
                    Timber.w("⚠️ setAlign failed: ${e.message}")
                }

                // 2. Symuluj pogrubienie przez większy font
                //    Senraise nie ma setTextBold() - używamy setFont()
                try {
                    val fontSize = when {
                        segment.bold || segment.doubleWidth || segment.doubleHeight -> 1  // większy
                        else -> 0  // normalny
                    }
                    service.setFont(fontSize)
                } catch (e: Exception) {
                    Timber.w("⚠️ setFont failed: ${e.message}")
                }

                // 3. Drukuj tekst
                try {
                    service.printText(segment.text)
                } catch (e: Exception) {
                    Timber.e(e, "❌ printText failed")
                    throw e
                }

                // 4. Reset
                try {
                    service.setFont(0)
                    service.setAlign(0)
                } catch (e: Exception) {
                    Timber.w("⚠️ reset failed: ${e.message}")
                }
            }

            // 5. Odstęp
            try {
                service.nextLine(3)
            } catch (e: Exception) {
                Timber.w("⚠️ nextLine failed: ${e.message}")
            }

            // 6. Cięcie (Senraise ma dedykowaną metodę)
            if (autoCut) {
                try {
                    service.cutPaper()
                    Timber.d("✂️ [SENRAISE] Papier przycięty")
                } catch (e: Exception) {
                    Timber.w(e, "⚠️ cutPaper failed")
                }
            }

            Timber.d("✅ [SENRAISE RENDERER] Sukces")
            true

        }.onFailure { e ->
            Timber.e(e, "❌ [SENRAISE RENDERER] Błąd")
        }.getOrDefault(false)
    }

    /**
     * Renderuje sformatowany tekst na drukarce WOYOU/SUNMI (IWoyouService).
     *
     * UWAGA: Woyou używa innego API - printTextWithFont() zamiast setFont().
     * Wyrównanie trzeba symulować przez spacje (brak setAlignment()).
     *
     * @param service Instancja woyou.aidlservice.jiuiv5.IWoyouService
     * @param segments Lista sparsowanych segmentów
     * @param autoCut Czy automatycznie ciąć papier
     * @return true jeśli sukces
     */
    fun renderWoyou(
        service: woyou.aidlservice.jiuiv5.IWoyouService,
        segments: List<AidlFormattingParser.FormattedSegment>,
        autoCut: Boolean = false
    ): Boolean {
        return runCatching {
            Timber.d("🖨️ [WOYOU RENDERER] Start: ${segments.size} segmentów")

            service.printerInit(null)

            segments.forEachIndexed { index, segment ->

                // 1. Oblicz rozmiar fontu na podstawie formatowania
                val fontSize = when {
                    segment.doubleWidth && segment.doubleHeight -> 48f  // BIG
                    segment.doubleWidth -> 36f                          // WIDE
                    segment.doubleHeight -> 36f                         // TALL
                    segment.bold -> 30f                                 // BOLD (trochę większy)
                    else -> 24f                                         // NORMAL
                }

                // 2. Symuluj wyrównanie przez padding spacjami
                //    (Woyou nie ma setAlignment())
                val lineWidth = 32  // zakładamy drukarkę 58mm = ~32 znaki
                val alignedText = when (segment.alignment) {
                    1 -> alignCenter(segment.text, lineWidth)   // center
                    2 -> alignRight(segment.text, lineWidth)    // right
                    else -> segment.text                         // left (bez zmian)
                }

                // 3. Drukuj z fontowaniem
                try {
                    service.printTextWithFont(
                        alignedText,
                        "monospace",  // typeface (monospace dla równego wyrównania)
                        fontSize,
                        null          // callback (synchroniczne drukowanie)
                    )
                } catch (e: Exception) {
                    Timber.e(e, "❌ printTextWithFont failed")
                    throw e
                }
            }

            // 4. Odstęp
            try {
                service.printText("\n\n\n", null)
            } catch (e: Exception) {
                Timber.w("⚠️ spacing failed: ${e.message}")
            }

            // 5. Cięcie
            if (autoCut) {
                try {
                    service.paperCut(null)
                    Timber.d("✂️ [WOYOU] Papier przycięty")
                } catch (e: Exception) {
                    Timber.w(e, "⚠️ paperCut failed")
                }
            }

            Timber.d("✅ [WOYOU RENDERER] Sukces")
            true

        }.onFailure { e ->
            Timber.e(e, "❌ [WOYOU RENDERER] Błąd")
        }.getOrDefault(false)
    }

    /**
     * Wyśrodkowuje tekst przez dodanie spacji na początku.
     */
    private fun alignCenter(text: String, lineWidth: Int): String {
        val lines = text.split("\n")
        return lines.joinToString("\n") { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                ""
            } else {
                val padding = ((lineWidth - trimmed.length) / 2).coerceAtLeast(0)
                " ".repeat(padding) + trimmed
            }
        }
    }

    /**
     * Wyrównuje tekst do prawej przez dodanie spacji na początku.
     */
    private fun alignRight(text: String, lineWidth: Int): String {
        val lines = text.split("\n")
        return lines.joinToString("\n") { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                ""
            } else {
                val padding = (lineWidth - trimmed.length).coerceAtLeast(0)
                " ".repeat(padding) + trimmed
            }
        }
    }
}

