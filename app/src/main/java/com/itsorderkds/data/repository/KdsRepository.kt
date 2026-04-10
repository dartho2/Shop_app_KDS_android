package com.itsorderkds.data.repository

import com.itsorderkds.data.model.*
import com.itsorderkds.data.network.KdsApi
import com.itsorderkds.data.network.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository dla Kitchen Display System (KDS)
 * Obsługuje wszystkie operacje związane z ticketami KDS
 */
@Singleton
class KdsRepository @Inject constructor(
    private val kdsApi: KdsApi
) {

    /**
     * Generuje unikalny klucz idempotencji (UUID v4)
     */
    private fun generateIdempotencyKey(): String = UUID.randomUUID().toString()

    // ========== Operacje odczytu ==========

    /**
     * Pobiera listę ticketów z opcjonalnymi filtrami
     */
    suspend fun getTickets(
        state: String? = null,
        from: String? = null,
        to: String? = null,
        priority: String? = null,
        limit: Int = 100,
        skip: Int = 0
    ): Resource<KdsTicketsResponse> = withContext(Dispatchers.IO) {
        try {
            val response = kdsApi.getTickets(state, from, to, priority, limit, skip)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Failure(false, errorMessage = response.message() ?: "Błąd pobierania ticketów")
            }
        } catch (e: Exception) {
            Resource.Failure(false, errorMessage = e.message ?: "Nieznany błąd")
        }
    }

    /**
     * Pobiera historię ticketów (HANDED_OFF + CANCELLED) z dzisiejszego dnia.
     * Używane przez panel historii w KDS.
     */
    suspend fun getHistoryTickets(limit: Int = 50): Resource<KdsTicketsResponse> =
        getTickets(state = "HANDED_OFF", limit = limit)

    suspend fun getCancelledTickets(limit: Int = 20): Resource<KdsTicketsResponse> =
        getTickets(state = "CANCELLED", limit = limit)


    suspend fun getTicketWithItems(ticketId: String): Resource<KdsTicketWithItemsResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = kdsApi.getTicketWithItems(ticketId)
                if (response.isSuccessful && response.body() != null) {
                    Resource.Success(response.body()!!)
                } else {
                    Resource.Failure(false, errorMessage = response.message() ?: "Błąd pobierania ticketu")
                }
            } catch (e: Exception) {
                Resource.Failure(false, errorMessage = e.message ?: "Nieznany błąd")
            }
        }

    /**
     * Pobiera listę stanowisk kuchni
     */
    suspend fun getStations(includeInactive: Boolean = false): Resource<KdsStationsResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = kdsApi.getStations(if (includeInactive) true else null)
                if (response.isSuccessful && response.body() != null) {
                    Resource.Success(response.body()!!)
                } else {
                    Resource.Failure(false, errorMessage = response.message() ?: "Błąd pobierania stanowisk")
                }
            } catch (e: Exception) {
                Resource.Failure(false, errorMessage = e.message ?: "Nieznany błąd")
            }
        }

    // ========== Komendy ticketu ==========

    /**
     * Potwierdź przyjęcie ticketu (NEW → ACKED)
     */
    suspend fun ackTicket(
        ticketId: String,
        note: String? = null,
        idempotencyKey: String = generateIdempotencyKey()
    ): Resource<KdsTicket> = withContext(Dispatchers.IO) {
        try {
            val body = note?.let { mapOf("note" to it) } ?: emptyMap()
            val response = kdsApi.ackTicket(ticketId, idempotencyKey, body)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Failure(false, errorMessage = response.message() ?: "Błąd potwierdzania ticketu")
            }
        } catch (e: Exception) {
            Resource.Failure(false, errorMessage = e.message ?: "Nieznany błąd")
        }
    }

    /**
     * Rozpocznij przygotowanie ticketu (NEW|ACKED → IN_PROGRESS)
     */
    suspend fun startTicket(
        ticketId: String,
        note: String? = null,
        idempotencyKey: String = generateIdempotencyKey()
    ): Resource<KdsTicket> = withContext(Dispatchers.IO) {
        try {
            val body = note?.let { mapOf("note" to it) } ?: emptyMap()
            val response = kdsApi.startTicket(ticketId, idempotencyKey, body)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Failure(false, errorMessage = response.message() ?: "Błąd rozpoczynania ticketu")
            }
        } catch (e: Exception) {
            Resource.Failure(false, errorMessage = e.message ?: "Nieznany błąd")
        }
    }

    /**
     * Oznacz ticket jako gotowy (* → READY)
     */
    suspend fun readyTicket(
        ticketId: String,
        idempotencyKey: String = generateIdempotencyKey()
    ): Resource<KdsTicket> = withContext(Dispatchers.IO) {
        try {
            val response = kdsApi.readyTicket(ticketId, idempotencyKey, emptyMap())
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Failure(false, errorMessage = response.message() ?: "Błąd oznaczania ticketu jako gotowy")
            }
        } catch (e: Exception) {
            Resource.Failure(false, errorMessage = e.message ?: "Nieznany błąd")
        }
    }

    /**
     * Wydaj zamówienie (READY → HANDED_OFF)
     */
    suspend fun handoffTicket(
        ticketId: String,
        idempotencyKey: String = generateIdempotencyKey()
    ): Resource<KdsTicket> = withContext(Dispatchers.IO) {
        try {
            val response = kdsApi.handoffTicket(ticketId, idempotencyKey, emptyMap())
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Failure(false, errorMessage = response.message() ?: "Błąd wydawania ticketu")
            }
        } catch (e: Exception) {
            Resource.Failure(false, errorMessage = e.message ?: "Nieznany błąd")
        }
    }

    /**
     * Anuluj ticket (* → CANCELLED)
     * @param reason Powód anulowania (wymagane, min 1, max 200 znaków)
     */
    suspend fun cancelTicket(
        ticketId: String,
        reason: String,
        idempotencyKey: String = generateIdempotencyKey()
    ): Resource<KdsTicket> = withContext(Dispatchers.IO) {
        try {
            if (reason.isBlank() || reason.length > 200) {
                return@withContext Resource.Failure(false, errorMessage = "Powód anulowania musi mieć 1-200 znaków")
            }
            val body = mapOf("reason" to reason)
            val response = kdsApi.cancelTicket(ticketId, idempotencyKey, body)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Failure(false, errorMessage = response.message() ?: "Błąd anulowania ticketu")
            }
        } catch (e: Exception) {
            Resource.Failure(false, errorMessage = e.message ?: "Nieznany błąd")
        }
    }

    // ========== Komendy pozycji ==========

    /**
     * Rozpocznij gotowanie pozycji (QUEUED → COOKING)
     */
    suspend fun startItem(
        itemId: String,
        idempotencyKey: String = generateIdempotencyKey()
    ): Resource<KdsTicketItem> = withContext(Dispatchers.IO) {
        try {
            val response = kdsApi.startItem(itemId, idempotencyKey, emptyMap())
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Failure(false, errorMessage = response.message() ?: "Błąd rozpoczynania pozycji")
            }
        } catch (e: Exception) {
            Resource.Failure(false, errorMessage = e.message ?: "Nieznany błąd")
        }
    }

    /**
     * Oznacz pozycję jako gotową (* → READY)
     */
    suspend fun readyItem(
        itemId: String,
        idempotencyKey: String = generateIdempotencyKey()
    ): Resource<KdsTicketItem> = withContext(Dispatchers.IO) {
        try {
            val response = kdsApi.readyItem(itemId, idempotencyKey, emptyMap())
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Failure(false, errorMessage = response.message() ?: "Błąd oznaczania pozycji jako gotowa")
            }
        } catch (e: Exception) {
            Resource.Failure(false, errorMessage = e.message ?: "Nieznany błąd")
        }
    }
}

