package com.itsorderkds.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Nasza globalna, stylowana karta. Domyślnie nie ma cienia (elevation).
 * Możesz tu dodać inne wspólne style, np. domyślny kształt czy kolory.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    isLast: Boolean = false, // <-- DODANY PARAMETR
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            // Teraz `isLast` jest dostępne i błąd zniknie
            .padding(bottom = if (!isLast) 8.dp else 0.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}
@Composable
fun MainCardRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier.padding(start = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        content()
    }
}
/**
 * To jest nasz "globalny" komponent dla karty, która zawsze zawiera wiersz (Row).
 * Hermetyzuje styl zarówno zewnętrznej karty, jak i wewnętrznego layoutu.
 *
 * @param modifier Modyfikatory przekazywane do zewnętrznej karty.
 * @param onClick Opcjonalna akcja do wykonania po kliknięciu całej karty.
 * @param content Treść, która ma być wyświetlona wewnątrz wiersza.
 * Używamy `RowScope`, aby można było w środku używać np. `.weight()`.
 */
@Composable
fun AppCardRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    // Stosujemy modyfikator clickable do całej karty
    val finalModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    Card(
        modifier = finalModifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        // Tutaj możesz dodać więcej globalnych stylów dla karty
    ) {
        // Wewnątrz karty umieszczamy nasz standardowy, ostylowany wiersz
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Renderujemy treść przekazaną z zewnątrz
            content()
        }
    }
}