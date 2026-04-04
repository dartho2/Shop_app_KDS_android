package com.itsorderkds.ui.settings.print

import timber.log.Timber

/**
 * Parser konwertujący tagi ESC/POS (używane przez DantSu) na strukturę segmentów
 * gotowych do wykonania przez AIDL interface.
 *
 * WSPIERANE TAGI:
 * - [C] - center alignment
 * - [L] - left alignment (domyślne)
 * - [R] - right alignment
 * - <b>...</b> - bold text
 * - <u>...</u> - underline
 * - <font size='wide'>...</font> - double width
 * - <font size='tall'>...</font> - double height
 * - <font size='big'>...</font> - double width + height
 *
 * PRZYKŁAD:
 * ```
 * val input = "[C]<b>Z-12345</b>\n[L]Test"
 * val segments = AidlFormattingParser.parse(input)
 * // → [
 * //     FormattedSegment("Z-12345\n", alignment=1, bold=true),
 * //     FormattedSegment("Test\n", alignment=0, bold=false)
 * //   ]
 * ```
 */
object AidlFormattingParser {

    /**
     * Reprezentuje pojedynczy segment tekstu z pełnym stanem formatowania.
     */
    data class FormattedSegment(
        val text: String,
        val alignment: Int = 0,          // 0=left, 1=center, 2=right
        val bold: Boolean = false,
        val underline: Boolean = false,
        val doubleWidth: Boolean = false,
        val doubleHeight: Boolean = false
    )

    /**
     * Główna metoda parsująca - konwertuje tekst z tagami ESC/POS na listę segmentów.
     */
    fun parse(input: String): List<FormattedSegment> {
        val lines = input.split("\n")
        val segments = mutableListOf<FormattedSegment>()

        Timber.d("🔍 Parser: Przetwarzam ${lines.size} linii")

        for ((lineIndex, line) in lines.withIndex()) {
            if (line.trim().isEmpty()) {
                // Pusta linia - zachowaj jako newline
                segments.add(FormattedSegment("\n"))
                continue
            }

            // Wykryj alignment na początku linii
            val alignment = when {
                line.trimStart().startsWith("[C]") -> 1
                line.trimStart().startsWith("[R]") -> 2
                line.trimStart().startsWith("[L]") -> 0
                else -> 0
            }

            // Usuń prefix alignment
            val withoutAlign = when {
                line.trimStart().startsWith("[C]") -> line.trimStart().substring(3)
                line.trimStart().startsWith("[R]") -> line.trimStart().substring(3)
                line.trimStart().startsWith("[L]") -> line.trimStart().substring(3)
                else -> line.trimStart()
            }

            // Parsuj tagi inline (<b>, <font>, etc.)
            val lineSegments = parseInlineTags(withoutAlign, alignment)
            segments.addAll(lineSegments)

         }

        Timber.d("✅ Parser: Wygenerowano ${segments.size} segmentów")
        return segments
    }

    /**
     * Parsuje tagi inline w obrębie jednej linii.
     */
    private fun parseInlineTags(
        text: String,
        alignment: Int
    ): List<FormattedSegment> {
        if (text.isEmpty()) {
            return listOf(FormattedSegment("\n", alignment = alignment))
        }

        val segments = mutableListOf<FormattedSegment>()

        // Stan formatowania (może się zmieniać w trakcie linii)
        var currentBold = false
        var currentUnderline = false
        var currentDoubleWidth = false
        var currentDoubleHeight = false

        // Regex do wykrywania tagów HTML-like
        val tagRegex = Regex("<(/?)([^>]+)>")
        var lastIndex = 0

        tagRegex.findAll(text).forEach { match ->
            // 1. Dodaj tekst PRZED tagiem (jeśli istnieje)
            if (match.range.first > lastIndex) {
                val plainText = text.substring(lastIndex, match.range.first)
                if (plainText.isNotEmpty()) {
                    segments.add(FormattedSegment(
                        text = plainText,
                        alignment = alignment,
                        bold = currentBold,
                        underline = currentUnderline,
                        doubleWidth = currentDoubleWidth,
                        doubleHeight = currentDoubleHeight
                    ))
                }
            }

            // 2. Przetwórz sam tag (zmień stan formatowania)
            val isClosing = match.groupValues[1] == "/"
            val tagContent = match.groupValues[2].lowercase()

            when {
                tagContent == "b" -> {
                    currentBold = !isClosing
                 }
                tagContent == "u" -> {
                    currentUnderline = !isClosing
                 }
                tagContent.startsWith("font") -> {
                    if (isClosing) {
                        // Zamykający tag <font> - resetuj rozmiar
                        currentDoubleWidth = false
                        currentDoubleHeight = false
                      } else {
                        // Otwierający tag - sprawdź atrybut size
                        when {
                            tagContent.contains("size='wide'") || tagContent.contains("size=\"wide\"") -> {
                                currentDoubleWidth = true
                              }
                            tagContent.contains("size='tall'") || tagContent.contains("size=\"tall\"") -> {
                                currentDoubleHeight = true
                             }
                            tagContent.contains("size='big'") || tagContent.contains("size=\"big\"") -> {
                                currentDoubleWidth = true
                                currentDoubleHeight = true
                             }
                        }
                    }
                }
                else -> {
                    Timber.v("      Tag nieznany: $tagContent")
                }
            }

            lastIndex = match.range.last + 1
        }

        // 3. Dodaj pozostały tekst PO ostatnim tagu
        if (lastIndex < text.length) {
            val remainingText = text.substring(lastIndex)
            if (remainingText.isNotEmpty()) {
                segments.add(FormattedSegment(
                    text = remainingText,
                    alignment = alignment,
                    bold = currentBold,
                    underline = currentUnderline,
                    doubleWidth = currentDoubleWidth,
                    doubleHeight = currentDoubleHeight
                ))
            }
        }

        // 4. Jeśli nie znaleziono żadnych tagów, zwróć całą linię jako jeden segment
        if (segments.isEmpty()) {
            segments.add(FormattedSegment(
                text = text,
                alignment = alignment
            ))
        }

        // 5. Dodaj newline na końcu linii (do ostatniego segmentu)
        if (segments.isNotEmpty()) {
            val last = segments.removeAt(segments.lastIndex)
            segments.add(last.copy(text = last.text + "\n"))
        }

        return segments
    }

    /**
     * Pomocnicza funkcja do usuwania WSZYSTKICH tagów (fallback dla starych drukarek).
     */
    fun stripAllTags(input: String): String {
        return input
            .replace(Regex("""\[C]|\[L]|\[R]"""), "")  // usuń alignment
            .replace(Regex("<[^>]+>"), "")              // usuń tagi HTML-like
            .trim()
    }
}

