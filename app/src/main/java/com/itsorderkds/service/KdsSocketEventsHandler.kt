package com.itsorderkds.service

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import com.itsorderkds.data.network.Resource
import com.itsorderkds.data.preferences.AppPreferencesManager
import com.itsorderkds.data.repository.KdsRepository
import com.itsorderkds.notifications.NotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handler eventów Socket.IO dla KDS.
 * Nasłuchuje KDS_TICKET_* i KDS_ITEM_* eventów i emituje je do KdsSocketEventsRepository.
 */
@Singleton
class KdsSocketEventsHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val kdsRepository: KdsRepository,
    private val kdsSocketEventsRepo: KdsSocketEventsRepository,
    private val appPreferencesManager: AppPreferencesManager
) {
    private val job = SupervisorJob()
    private val ioScope = CoroutineScope(job + Dispatchers.IO)
    private val gson = Gson()

    private val eventHandlers: Map<SocketEvent, (Array<Any>) -> Unit> = mapOf(
        SocketEvent.KDS_TICKET_CREATED  to ::handleTicketCreated,
        SocketEvent.KDS_TICKET_ACKED    to { args -> handleTicketStateChange(args, "ACKED") },
        SocketEvent.KDS_TICKET_STARTED  to { args -> handleTicketStateChange(args, "IN_PROGRESS") },
        SocketEvent.KDS_TICKET_READY    to { args -> handleTicketStateChange(args, "READY") },
        SocketEvent.KDS_TICKET_HANDOFF  to { args -> handleTicketStateChange(args, "HANDED_OFF") },
        SocketEvent.KDS_TICKET_CANCEL   to { args -> handleTicketStateChange(args, "CANCELLED") },
        SocketEvent.KDS_ITEM_STARTED    to { args -> handleItemStateChange(args, "COOKING") },
        SocketEvent.KDS_ITEM_READY      to { args -> handleItemStateChange(args, "READY") },
    )

    fun register() {
        eventHandlers.forEach { (event, handler) ->
            try {
                SocketManager.on(event.raw, handler)
                Timber.tag(TAG).d("Registered KDS handler for ${event.raw}")
            } catch (t: Throwable) {
                Timber.tag(TAG).e(t, "Failed to register KDS handler for ${event.raw}")
            }
        }
    }

    fun shutdown() {
        job.cancel()
    }

    // ─── KDS_TICKET_CREATED ─────────────────────────────────────────────────
    // Payload: { ticketId, orderId, orderNumber, state, itemCount, slaTargetAt }
    // Akcja: fetch pełnego ticketu (ticket + items) przez HTTP, potem emit
    private fun handleTicketCreated(args: Array<Any>) {
        val payload = parsePayload(args, KdsTicketCreatedPayload::class.java) ?: return
        Timber.tag(TAG).i("📋 KDS_TICKET_CREATED: ticketId=${payload.ticketId}, order=${payload.orderNumber}")

        ioScope.launch {
            runCatching {
                when (val res = kdsRepository.getTicketWithItems(payload.ticketId)) {
                    is Resource.Success -> {
                        Timber.tag(TAG).d("✅ Fetched ticket with items: ${payload.ticketId}")
                        kdsSocketEventsRepo.emitTicketCreated(res.value)

                        // Odczytaj ustawienia dźwięku z DataStore (synchronicznie — już na IO thread)
                        val isMuted  = runBlocking { appPreferencesManager.isNotificationSoundMuted("order_alarm") }
                        val soundUri = runBlocking { appPreferencesManager.getKdsNotificationSoundUri() }

                        // Powiadomienie heads-up — dźwięk gra raz zgodnie z ustawieniami
                        NotificationHelper.showNewKdsTicket(
                            context     = context,
                            orderNumber = res.value.ticket.displayNumber,
                            itemCount   = res.value.items.size,
                            isRush      = res.value.ticket.priority == "rush",
                            soundUri    = soundUri,
                            muted       = isMuted
                        )
                        Timber.tag(TAG).d("🔔 Powiadomienie KDS wysłane dla ${payload.ticketId} (muted=$isMuted, sound=${soundUri ?: "default"})")
                    }
                    is Resource.Failure -> {
                        Timber.tag(TAG).w("❌ Failed to fetch ticket ${payload.ticketId}: ${res.errorMessage}")
                        // Fallback: emituj minimalny ticket bez items
                        // (nie mamy pełnych danych, UI pokaże to co mamy z payloadu)
                    }
                    else -> Unit
                }
            }.onFailure {
                Timber.tag(TAG).e(it, "💥 handleTicketCreated failed for ${payload.ticketId}")
            }
        }
    }

    // ─── KDS_TICKET_* STATE CHANGE ──────────────────────────────────────────
    // Payload: { ticketId, state, startedAt?, readyAt?, handedOffAt?, cancelledAt?, actorId? }
    private fun handleTicketStateChange(args: Array<Any>, expectedState: String) {
        val payload = parsePayload(args, KdsTicketStatePayload::class.java) ?: return
        Timber.tag(TAG).i("🔄 KDS ticket state → $expectedState: ticketId=${payload.ticketId}")

        val event = KdsTicketStateEvent(
            ticketId     = payload.ticketId,
            newState     = payload.state ?: expectedState,
            startedAt    = payload.startedAt,
            readyAt      = payload.readyAt,
            handedOffAt  = payload.handedOffAt,
            cancelledAt  = payload.cancelledAt,
            actorId      = payload.actorId
        )
        kdsSocketEventsRepo.emitTicketStateChanged(event)
    }

    // ─── KDS_ITEM_* STATE CHANGE ────────────────────────────────────────────
    // Payload: { itemId, ticketId, state, firedAt?, doneAt?, actorId? }
    private fun handleItemStateChange(args: Array<Any>, expectedState: String) {
        val payload = parsePayload(args, KdsItemStatePayload::class.java) ?: return
        Timber.tag(TAG).i("🔄 KDS item state → $expectedState: itemId=${payload.itemId}, ticketId=${payload.ticketId}")

        val event = KdsItemStateEvent(
            itemId   = payload.itemId,
            ticketId = payload.ticketId,
            newState = payload.state ?: expectedState,
            firedAt  = payload.firedAt,
            doneAt   = payload.doneAt,
            actorId  = payload.actorId
        )
        kdsSocketEventsRepo.emitItemStateChanged(event)
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private fun <T> parsePayload(args: Array<Any>, type: Class<T>): T? {
        val json = args.firstOrNull()?.toString() ?: run {
            Timber.tag(TAG).w("KDS socket event payload was null for ${type.simpleName}")
            return null
        }
        return try {
            gson.fromJson(json, type)
        } catch (e: JsonSyntaxException) {
            Timber.tag(TAG).e(e, "Failed to parse KDS JSON for ${type.simpleName}: $json")
            null
        }
    }

    private companion object {
        const val TAG = "KdsSocketEventsHandler"
    }
}

// ─── Payload modele (tylko do parsowania Socket.IO JSON) ────────────────────

private data class KdsTicketCreatedPayload(
    @SerializedName("ticketId")    val ticketId: String,
    @SerializedName("orderId")     val orderId: String? = null,
    @SerializedName("orderNumber") val orderNumber: String? = null,
    @SerializedName("state")       val state: String? = null,
    @SerializedName("itemCount")   val itemCount: Int? = null,
    @SerializedName("slaTargetAt") val slaTargetAt: String? = null
)

private data class KdsTicketStatePayload(
    @SerializedName("ticketId")    val ticketId: String,
    @SerializedName("state")       val state: String? = null,
    @SerializedName("startedAt")   val startedAt: String? = null,
    @SerializedName("readyAt")     val readyAt: String? = null,
    @SerializedName("handedOffAt") val handedOffAt: String? = null,
    @SerializedName("cancelledAt") val cancelledAt: String? = null,
    @SerializedName("reason")      val reason: String? = null,
    @SerializedName("actorId")     val actorId: String? = null
)

private data class KdsItemStatePayload(
    @SerializedName("itemId")   val itemId: String,
    @SerializedName("ticketId") val ticketId: String,
    @SerializedName("state")    val state: String? = null,
    @SerializedName("firedAt")  val firedAt: String? = null,
    @SerializedName("doneAt")   val doneAt: String? = null,
    @SerializedName("actorId")  val actorId: String? = null
)



