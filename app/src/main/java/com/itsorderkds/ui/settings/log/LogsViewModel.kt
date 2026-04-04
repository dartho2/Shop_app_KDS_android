package com.itsorderkds.ui.settings.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itsorderkds.data.network.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

// Zalecenie: Każda z tych klas powinna być w osobnym pliku dla porządku.
enum class LogSource(val displayName: String) {
    API("API"),
    APP("Aplikacja")
}

data class AppLogsUiState(
    val logs: List<LogEntry> = emptyList(),
    val activeSource: LogSource = LogSource.API,
    val isFilterPanelVisible: Boolean = false,
    val hasSearched: Boolean = false,
    val isLoading: Boolean = false,
    val error: Resource.Failure? = null
)


@HiltViewModel
class LogsViewModel @Inject constructor(
    private val apiLogsRepository: LogsRepository,
    private val localLogSource: LocalLogSource
) : ViewModel() {

    private val _apiLogs = MutableStateFlow<List<LogEntry>>(emptyList())
    private val _appLogs = MutableStateFlow<List<LogEntry>>(emptyList())
    private val _activeSource = MutableStateFlow(LogSource.API)
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<Resource.Failure?>(null)
    private val _filterPanelVisible = MutableStateFlow(false)
    private val _hasSearched = MutableStateFlow(false)

    val state: StateFlow<AppLogsUiState> =
        combine(
            _apiLogs,
            _appLogs,
            _activeSource,
            _isLoading,
            _error
        ) { apiLogs, appLogs, source, loading, err ->
            val logsToShow = when (source) {
                LogSource.API -> apiLogs
                LogSource.APP -> appLogs
            }

            AppLogsUiState(
                logs = logsToShow,
                activeSource = source,
                isLoading = loading,
                error = err
            )
        }.combine(_filterPanelVisible) { ui, panelVisible ->
            ui.copy(isFilterPanelVisible = panelVisible)
        }.combine(_hasSearched) { ui, searched ->
            ui.copy(hasSearched = searched)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppLogsUiState()
        )
    fun loadLogs(tenantId: String? = null,
                 date:     String? = null,
                 level:    String? = null,
                 limit:    Int?    = 200,
                 search:   String? = null) {
        if (date.isNullOrBlank()) return

        _hasSearched.value = true
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val apiDeferred = async {
                    val result = apiLogsRepository.getLogs(tenantId, date, level, limit, search)
                    if (result is Resource.Success) result.value else emptyList()
                }
                val appDeferred = async {
                    localLogSource.getLogsForDate(date).filter { log ->
                        (level == null || log.level?.lowercase() == level) &&
                                (search == null || log.message?.contains(search, ignoreCase = true) == true)
                    }
                }
                _apiLogs.value = apiDeferred.await()
                    .sortedByDescending { runCatching { Instant.parse(it.timestamp) }.getOrNull() }
                _appLogs.value = appDeferred.await()
                    .sortedByDescending { runCatching { Instant.parse(it.timestamp) }.getOrNull() }
            } catch (e: Exception) {
                _error.value = Resource.fromException(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setActiveSource(source: LogSource) {
        _activeSource.value = source
    }

    fun toggleFilterVisibility() = _filterPanelVisible.update { !it }
    fun errorShown() { _error.value = null }
}