package com.itsorderkds.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itsorderkds.data.network.Resource
import com.itsorderkds.data.repository.AuthRepository
import com.itsorderkds.data.responses.UserRole
import com.itsorderkds.ui.theme.home.HomeActivity
import com.itsorderkds.util.AppPrefs // Zaimportuj swoje preferencje
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Rozbudowana klasa stanu, która przechowuje wszystkie dane dla UI
data class LoginUiState(
    val domain: String = "",
    val email: String = "",
    val pass: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loginSuccess: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: AuthRepository
) : ViewModel() {

    // Prywatny, mutowalny stan, zarządzany tylko przez ViewModel
    private val _uiState = MutableStateFlow(LoginUiState())
    // Publiczny, niemutowalny stan, który UI może obserwować
    val uiState = _uiState.asStateFlow()

    // --- Funkcje do obsługi akcji użytkownika ---

    fun onDomainChange(newValue: String) {
        _uiState.update { it.copy(domain = newValue, errorMessage = null) }
    }

    fun onEmailChange(newValue: String) {
        _uiState.update { it.copy(email = newValue, errorMessage = null) }
    }

    fun onPasswordChange(newValue: String) {
        _uiState.update { it.copy(pass = newValue, errorMessage = null) }
    }

    fun onLoginClick() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val s = _uiState.value

            // proste walidacje pól (opcjonalnie dopasuj komunikaty pod UI)
            if (s.domain.isBlank()) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Podaj adres serwera") }
                return@launch
            }
            if (s.email.isBlank() || s.pass.isBlank()) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Podaj e-mail i hasło") }
                return@launch
            }

            try {
                AppPrefs.setBaseUrl(s.domain) // może rzucić IllegalArgumentException
            } catch (e: IllegalArgumentException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Nieprawidłowy adres serwera"
                    )
                }
                return@launch
            }

            when (val response = repo.login(s.email, s.pass)) {
                is Resource.Success -> {
                    val data = response.value
                    saveAuthToken(
                        access = data.accessToken,
                        refresh = data.refreshToken,
                        refreshId = data.refreshTokenId,
                        tenantKey = data.tenantKey,
                        role = data.role,
                        userId = data.sub
                    )
                    _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
                }
                is Resource.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Wystąpił błąd: ${response.errorBody}"
                        )
                    }
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Nieznany błąd") }
                }
            }
        }
    }
    fun onNavigationDone() {
        _uiState.update { it.copy(loginSuccess = false) }
    }
    private fun saveAuthToken(access: String, refresh: String, refreshId: String, tenantKey: String, role: UserRole?, userId: String?) =
        viewModelScope.launch {
            repo.saveAuthToken(access, refresh, refreshId, tenantKey, role, userId)
        }
}
