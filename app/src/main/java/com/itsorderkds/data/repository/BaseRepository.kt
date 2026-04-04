package com.itsorderkds.data.repository

import android.util.Log
import com.itsorderkds.data.network.Resource
import com.itsorderkds.ui.theme.GlobalMessageManager
import com.itsorderkds.ui.theme.MessageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.slf4j.LoggerFactory
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
// POPRAWKA ARCHITEKTONICZNA: Klasa abstrakcyjna nie powinna mieć `@Inject constructor`.
// Zależności są przekazywane z konkretnych implementacji (np. ProductsRepositoryImpl).
abstract class BaseRepository(
    private val messageManager: GlobalMessageManager
) {

    private val TAG = "BaseRepository"

    suspend fun <T> safeApiCall(
        // NOWOŚĆ: Flaga do kontrolowania, czy pokazywać globalny komunikat o błędzie
        showGlobalMessageOnError: Boolean = true,
        apiCall: suspend () -> Response<T>
    ): Resource<T> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiCall()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Log.d(TAG, "Success, code: ${response.code()}")
                        Resource.Success(body)
                    } else {
                        val errorMsg = "Pusta odpowiedź serwera (code: ${response.code()})"
                        Log.w(TAG, errorMsg)
                        createErrorResource(
                            errorMessage = errorMsg,
                            errorCode = response.code(),
                            showGlobalMessage = showGlobalMessageOnError
                        )
                    }
                } else {
                    // REFAKTORYZACJA: Używamy nowej funkcji pomocniczej
                    createErrorResource(
                        response = response,
                        showGlobalMessage = showGlobalMessageOnError
                    )
                }
            } catch (throwable: Throwable) {
                when (throwable) {
                    is HttpException -> {
                        // REFAKTORYZACJA: Używamy nowej funkcji pomocniczej
                        createErrorResource(
                            exception = throwable,
                            showGlobalMessage = showGlobalMessageOnError
                        )
                    }
                    else -> { // Błędy sieciowe, np. IOException
                        val errorMsg = "Błąd połączenia. Sprawdź internet."
                        Log.e(TAG, "Network or other error", throwable)
                        if (showGlobalMessageOnError) {
                            messageManager.showMessage(errorMsg, MessageType.ERROR)
                        }
                        Resource.Failure(true, null, null, errorMsg)
                    }
                }
            }
        }
    }

    /**
     * NOWA FUNKCJA POMOCNICZA: Tworzy obiekt Resource.Failure, aby uniknąć powtarzania kodu.
     */
    private suspend fun createErrorResource(
        response: Response<*>? = null,
        exception: HttpException? = null,
        errorMessage: String? = null,
        errorCode: Int? = null,
        showGlobalMessage: Boolean
    ): Resource.Failure {
        val finalErrorCode = response?.code() ?: exception?.code() ?: errorCode
        val errorBody = response?.errorBody() ?: exception?.response()?.errorBody()
        val finalErrorMessage = errorMessage ?: parseErrorMessage(errorBody?.string())

        Log.e(TAG, "API Failure, code: $finalErrorCode, message: $finalErrorMessage")

        if (showGlobalMessage) {
            messageManager.showMessage(finalErrorMessage, MessageType.ERROR)
        }

        return Resource.Failure(false, finalErrorCode, errorBody, finalErrorMessage)
    }

    /**
     * POPRAWIONA FUNKCJA: Bezpieczna obsługa pustego lub niepoprawnego `errorBody`.
     */
    private fun parseErrorMessage(errorBody: String?): String {
        if (errorBody.isNullOrBlank()) {
            return "Wystąpił nieoczekiwany błąd."
        }
        return try {
            val jsonObj = JSONObject(errorBody)
            jsonObj.optString("message", jsonObj.optString("error", "Nieznany błąd serwera."))
        } catch (e: Exception) {
            // Jeśli `errorBody` nie jest JSON-em, zwróć go w całości (jeśli jest krótki)
            if (errorBody.length < 100) errorBody else "Błąd odpowiedzi serwera."
        }
    }
}