package com.itsorderkds.ui.settings.printer


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.itsorderkds.ui.theme.ItsOrderChatTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PrinterSettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Zakładając, że masz zdefiniowany motyw aplikacji
            ItsOrderChatTheme {
                PrinterSettingsScreen(
                )
            }
        }
    }
}