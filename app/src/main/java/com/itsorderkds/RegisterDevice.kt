package com.itsorderkds

import android.util.Log
import javax.inject.Inject


class RegisterDevice @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
) {
    suspend operator fun invoke(mode: Mode) {
        when (mode) {
            Mode.IF_NEEDED -> {
                sendToken()

            }
            Mode.FORCEFULLY -> sendToken()
        }
    }

    private suspend fun sendToken() {
        val token = appPrefsWrapper.getFCMToken()
    }

    enum class Mode {
        IF_NEEDED, FORCEFULLY
    }
}
