package com.itsorderkds.ui.settings.log

import com.itsorderkds.data.network.Resource
import com.itsorderkds.data.repository.BaseRepository
import com.itsorderkds.ui.theme.GlobalMessageManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // Repozytoria zazwyczaj powinny być Singletonami
class LogsRepository @Inject constructor(
    private val api: LogsApi,
    messageManager: GlobalMessageManager
) : BaseRepository(messageManager) {

    // ───── API CALLS ───────────────────────────────────────────────────────

    suspend fun getLogs(
        tenantId: String?,
        date: String?,
        level: String?,
        limit: Int?,
        search: String?
    ): Resource<List<LogEntry>> =
        safeApiCall { api.getLogs(tenantId, date, level, limit, search) }
}