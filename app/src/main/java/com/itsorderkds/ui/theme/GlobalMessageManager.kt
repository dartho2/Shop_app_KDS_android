package com.itsorderkds.ui.theme

import androidx.compose.material3.SnackbarDuration
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

data class UiMessage(
    val text: String,
    val type: MessageType = MessageType.INFO,
    val duration: SnackbarDuration = SnackbarDuration.Short
)

enum class MessageType { INFO, SUCCESS, ERROR }

@Singleton
class GlobalMessageManager @Inject constructor() {
    private val _messages = MutableSharedFlow<UiMessage>()
    val messages = _messages.asSharedFlow()

    suspend fun showMessage(
        text: String,
        type: MessageType = MessageType.INFO
    ) {
        _messages.emit(UiMessage(text = text, type = type))
    }
}