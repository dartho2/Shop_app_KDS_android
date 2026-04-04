package com.itsorderkds.data.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itsorderkds.data.model.Product
import com.itsorderkds.data.network.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/* ─────────────────────────  Abstrakcyjna baza  ───────────────────────── */

/**
 * Generyczny ViewModel obsługujący pobieranie listy elementów typu [T].
 * Dzięki niemu w klasach dziedziczących nie trzeba już ręcznie pisać
 * `fold(onSuccess / onFailure)` ani zarządzać flagą `isLoading`.
 */
abstract class BaseListViewModel<T> : ViewModel() {

    protected val itemsFlow      = MutableStateFlow<List<T>>(emptyList())
    protected val isLoadingFlow  = MutableStateFlow(false)
    protected val errorFlow      = MutableStateFlow<Resource.Failure?>(null)

    /**
     * Uruchamia [call] w IO‑dispatcherze i automatycznie:
     *  • aktualizuje `isLoadingFlow`,
     *  • zapisuje wynik do `itemsFlow`,
     *  • rejestruje błąd w `errorFlow`.
     */
    protected fun request(
        scope: CoroutineScope,
        call: suspend () -> Resource<List<T>>
    ) {
        scope.launch(Dispatchers.IO) {
            isLoadingFlow.value = true
            call().fold(
                onSuccess = {
                    itemsFlow.value = it
                    errorFlow.value = null
                },
                onFailure = {
                    itemsFlow.value = emptyList()
                    errorFlow.value = it
                }
            )
            isLoadingFlow.value = false
        }
    }

    open fun errorShown() { errorFlow.value = null }
}