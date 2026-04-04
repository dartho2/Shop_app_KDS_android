package com.itsorderkds

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect // <-- Ważny import
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.itsorderkds.ui.auth.AuthViewModel
import com.itsorderkds.ui.login.LoginScreen
import com.itsorderkds.ui.theme.ItsOrderChatTheme
import com.itsorderkds.ui.theme.home.HomeActivity // Upewnij się, że import jest poprawny
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : ComponentActivity() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ItsOrderChatTheme {
                // Pobieramy stan z ViewModelu
                val uiState by viewModel.uiState.collectAsState()

                // Używamy LaunchedEffect do obsługi jednorazowych zdarzeń (side-effects)
                LaunchedEffect(key1 = uiState.loginSuccess) {
                    if (uiState.loginSuccess) {
                        // Jeśli logowanie się udało, uruchom nową aktywność
                        startActivity(Intent(this@LoginActivity, HomeActivity::class.java))

                        // Zakończ LoginActivity, aby użytkownik nie mógł do niej wrócić przyciskiem "wstecz"
                        finish()

                        // Zresetuj stan w ViewModel, aby uniknąć ponownej nawigacji
                        viewModel.onNavigationDone()
                    }
                }

                LoginScreen(viewModel = viewModel)
            }
        }
    }
}