package com.itsorderkds.ui.settings.printer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text

@Composable
fun PrinterSettingsScreen(modifier: Modifier = Modifier, navController: androidx.navigation.NavController? = null) {
    LaunchedEffect(Unit) {
        navController?.navigate(com.itsorderkds.ui.theme.home.AppDestinations.PRINTERS_LIST)
    }
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Przeniesiono ustawienia drukarek do nowego ekranu.")
    }
}
