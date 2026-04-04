package com.itsorderkds.data.model

import com.google.gson.annotations.SerializedName
import com.itsorderkds.data.enums.OrderScope
import com.itsorderkds.data.enums.RestaurantStatus
import com.itsorderkds.data.enums.SourceOrder
import java.io.Serializable

data class TimeRangeOpen(
    @SerializedName("start") val start: String,  // "HH:mm"
    @SerializedName("end")   val end: String     // "HH:mm"
) : Serializable

data class ExceptionDay(
    @SerializedName("date")    val date: String,                 // "YYYY-MM-DD"
    @SerializedName("closed")  val closed: Boolean? = null,
    @SerializedName("ranges")  val ranges: List<TimeRangeOpen>? = null
) : Serializable

// Struktura pauzy przechowywana w dokumencie godzin (IOpenHours.current_pause)
data class CurrentPause(
    @SerializedName("untilIso") val untilIso: String?,           // np. "2025-10-13T21:00:00Z"
    @SerializedName("scope")    val scope: OrderScope?,          // all/delivery/pickup
    @SerializedName("reason")   val reason: String?              // opcjonalny opis
) : Serializable

data class AvailabilityNowDto(
    @SerializedName("accepting")    val accepting: Boolean,
    @SerializedName("reason")       val reason: String,          // np. "TEMPORARY-PAUSE"
    @SerializedName("source")       val source: SourceOrder?,    // np. "PAUSE" | "MANUAL" | "SCHEDULE"
    @SerializedName("until")        val until: String?,          // ISO 8601 lub null
    @SerializedName("nextChangeAt") val nextChangeAt: String?    // ISO 8601 lub null
) : Serializable

data class OpenHoursDto(
    @SerializedName("timezone")      val timezone: String,
    @SerializedName("is_open")       val isOpen: Boolean,
    @SerializedName("weekly")        val weekly: Map<String, List<TimeRange>>?, // patrz uwaga wyżej
    @SerializedName("exceptions")    val exceptions: List<ExceptionDay>?,
    @SerializedName("current_pause") val currentPause: CurrentPause?
) : Serializable

data class OpenHoursAdminDto(
    @SerializedName("_id")           val id: String?,
    @SerializedName("timezone")      val timezone: String,
    @SerializedName("is_open")       val isOpen: Boolean,
    @SerializedName("weekly")        val weekly: Map<String, List<TimeRange>>,
    @SerializedName("exceptions")    val exceptions: List<ExceptionDay>,
    @SerializedName("current_pause") val currentPause: CurrentPause?,
    @SerializedName("createdAt")     val createdAt: String?,   // ISO (jeśli masz Date adapter, możesz zmienić na Date)
    @SerializedName("updatedAt")     val updatedAt: String?
) : Serializable

data class PauseRequest(
    @SerializedName("minutes") val minutes: Int,               // 1..1440
    @SerializedName("scope")   val scope: OrderScope = OrderScope.ALL,
    @SerializedName("reason")  val reason: String? = null,      // max 200, opcjonalnie
    @SerializedName("portals") val portals: List<String>? = null
) : Serializable

// DELETE /admin/openhours/pause – brak body

// POST /admin/openhours/status  (jeśli używasz ręcznego zamknięcia/otwarcia)
data class ManualStatusRequest(
    @SerializedName("closed") val closed: Boolean              // true=zamknij, false=otwórz
) : Serializable

// PUT /admin/openhours  (częściowy update zgodnie z Zod)
data class UpdateOpenHoursRequest(
    @SerializedName("timezone")   val timezone: String? = null,
    @SerializedName("is_open")    val isOpen: Boolean? = null,
    @SerializedName("weekly")     val weekly: Map<String, List<TimeRangeOpen>>? = null,
    @SerializedName("exceptions") val exceptions: List<ExceptionDay>? = null,
    @SerializedName("portals") val portals: List<String>? = null
) : Serializable
