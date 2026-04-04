package com.itsorderkds.ui


import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.itsorderkds.data.network.Resource


// Przyjmujemy generyczny ViewModel, który ma pole `error` i funkcję `errorShown`
interface UiStateWithError {
    val error: Resource.Failure?
}

interface ViewModelWithError {
    fun errorShown()
}

@Composable
fun HandleApiError(
    errorState: Resource.Failure?,
    snackbarHostState: SnackbarHostState,
    onErrorShown: () -> Unit,
    onUnauthorized: () -> Unit = {}
) {
    val context = LocalContext.current

    LaunchedEffect(errorState) {
        if (errorState != null) {
            val message = when {
                errorState.isNetworkError -> "Brak połączenia z internetem"
                errorState.errorCode == 401 -> {
                    onUnauthorized()
                    "Błąd autoryzacji. Proszę zalogować się ponownie."
                }

                errorState.errorCode == 422 -> "Nieprawidłowe dane"
                (errorState.errorCode ?: 0) in 500..599 -> "Błąd serwera (${errorState.errorCode})"
                else -> errorState.errorMessage ?: "Nieznany błąd"
            }

            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long
            )
            // "Konsumujemy" błąd, aby nie pokazywał się ponownie
            onErrorShown()
        }
    }
}