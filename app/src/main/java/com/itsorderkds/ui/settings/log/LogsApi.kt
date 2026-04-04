package com.itsorderkds.ui.settings.log

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface LogsApi {
    /**
     * Pobiera logi z serwera z możliwością filtrowania.
     * Wszystkie parametry są opcjonalne (nullowalne).
     *
     * @param tenantId ID tenanta, domyślnie "_system" na backendzie.
     * @param date Data w formacie "YYYY-MM-DD", domyślnie dzisiejszy dzień na backendzie.
     * @param level Poziom logu do filtrowania (np. "info", "error").
     * @param limit Maksymalna liczba logów do zwrócenia.
     * @param search Fraza do wyszukania w wiadomościach.
     */
    @GET("client/v3/api/admin/logs")
    suspend fun getLogs(
        @Query("tenantId") tenantId: String?,
        @Query("date") date: String?,
        @Query("level") level: String?,
        @Query("limit") limit: Int?,
        @Query("search") search: String?
    ): Response<List<LogEntry>>
}
