package com.itsorderkds.util       // ← upewnij się, że pakiet jest prawidłowy
//    (ten sam, którego używasz w MainActivity)

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 *  Globalny kanał „jednokierunkowy” do wyświetlania Snackbarów.
 *  W dowolnym ViewModel-u wywołujesz  →  snackbarManager.showMessage("…")
 *  a MainActivity (lub inny host) obserwuje [messages] i pokazuje Snackbara.
 */
@Singleton
class SnackbarManager @Inject constructor() {

    /**  rePlay = 0  → nie powtarzaj starych komunikatów po konfiguracji  */
    private val _messages = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val messages = _messages.asSharedFlow()

    suspend fun showMessage(msg: String) {
        _messages.emit(msg)
    }
}
