package com.itsorderkds.data.network

import com.itsorderkds.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * API dla Kitchen Display System (KDS)
 * Zgodnie z dokumentacją KDS.md
 */
interface KdsApi {

    // ========== Endpointy odczytu (staff) ==========

    /**
     * Pobiera listę ticketów z opcjonalnymi filtrami
     * GET /staff/kds/tickets
     */
    @GET("client/v3/api/staff/kds/tickets")
    suspend fun getTickets(
        @Query("state") state: String? = null,        // NEW, ACKED, IN_PROGRESS, READY, etc.
        @Query("from") from: String? = null,          // ISO 8601
        @Query("to") to: String? = null,              // ISO 8601
        @Query("priority") priority: String? = null,  // normal | rush
        @Query("limit") limit: Int? = 100,
        @Query("skip") skip: Int? = 0
    ): Response<KdsTicketsResponse>

    /**
     * Pobiera pojedynczy ticket razem ze wszystkimi pozycjami
     * GET /staff/kds/tickets/:ticketId
     */
    @GET("client/v3/api/staff/kds/tickets/{ticketId}")
    suspend fun getTicketWithItems(
        @Path("ticketId") ticketId: String
    ): Response<KdsTicketWithItemsResponse>

    /**
     * Pobiera listę aktywnych stanowisk kuchni
     * GET /staff/kds/stations
     */
    @GET("client/v3/api/staff/kds/stations")
    suspend fun getStations(
        @Query("all") all: Boolean? = null  // Jeśli true, zwraca też nieaktywne
    ): Response<KdsStationsResponse>

    // ========== Komendy ticketu (staff) ==========

    /**
     * Potwierdź przyjęcie ticketu. NEW → ACKED
     * POST /staff/kds/tickets/:ticketId/ack
     */
    @POST("client/v3/api/staff/kds/tickets/{ticketId}/ack")
    suspend fun ackTicket(
        @Path("ticketId") ticketId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: Map<String, String> = emptyMap()
    ): Response<KdsTicket>

    /**
     * Rozpocznij przygotowanie. NEW|ACKED → IN_PROGRESS
     * POST /staff/kds/tickets/:ticketId/start
     */
    @POST("client/v3/api/staff/kds/tickets/{ticketId}/start")
    suspend fun startTicket(
        @Path("ticketId") ticketId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: Map<String, String> = emptyMap()
    ): Response<KdsTicket>

    /**
     * Oznacz ticket jako gotowy. * → READY
     * POST /staff/kds/tickets/:ticketId/ready
     */
    @POST("client/v3/api/staff/kds/tickets/{ticketId}/ready")
    suspend fun readyTicket(
        @Path("ticketId") ticketId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: Map<String, String> = emptyMap()
    ): Response<KdsTicket>

    /**
     * Wydaj zamówienie. READY → HANDED_OFF
     * POST /staff/kds/tickets/:ticketId/handoff
     */
    @POST("client/v3/api/staff/kds/tickets/{ticketId}/handoff")
    suspend fun handoffTicket(
        @Path("ticketId") ticketId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: Map<String, String> = emptyMap()
    ): Response<KdsTicket>

    /**
     * Anuluj ticket. * → CANCELLED
     * POST /staff/kds/tickets/:ticketId/cancel
     * Wymaga pola "reason" w body (obowiązkowe, min 1, max 200 znaków)
     */
    @POST("client/v3/api/staff/kds/tickets/{ticketId}/cancel")
    suspend fun cancelTicket(
        @Path("ticketId") ticketId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: Map<String, String>  // { "reason": "..." }
    ): Response<KdsTicket>

    // ========== Komendy pozycji (staff) ==========

    /**
     * Rozpocznij gotowanie pozycji. QUEUED → COOKING
     * POST /staff/kds/items/:itemId/start
     */
    @POST("client/v3/api/staff/kds/items/{itemId}/start")
    suspend fun startItem(
        @Path("itemId") itemId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: Map<String, String> = emptyMap()
    ): Response<KdsTicketItem>

    /**
     * Oznacz pozycję jako gotową. * → READY
     * POST /staff/kds/items/:itemId/ready
     */
    @POST("client/v3/api/staff/kds/items/{itemId}/ready")
    suspend fun readyItem(
        @Path("itemId") itemId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: Map<String, String> = emptyMap()
    ): Response<KdsTicketItem>
}

