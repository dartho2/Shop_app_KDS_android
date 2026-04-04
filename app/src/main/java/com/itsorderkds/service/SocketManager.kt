// app/src/main/java/com/itsorderchat/data/network/SocketManager.kt
package com.itsorderkds.service

import com.itsorderkds.data.network.preferences.TokenProvider
import com.itsorderkds.util.AppPrefs
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import io.socket.engineio.client.EngineIOException
import io.socket.engineio.client.transports.WebSocket
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.IOException

object SocketManager {
    private var socket: Socket? = null

    /** Flaga blokująca auto-reconnect po błędzie autoryzacji */
    @Volatile private var blockedByAuthError: Boolean = false

    /** Wywoływane, gdy wygasł token / błąd autoryzacji */
    var onAuthExpired: (() -> Unit)? = null

    /** Wywoływane, gdy socket się połączył */
    var onConnect: (() -> Unit)? = null

    /** Wywoływane, gdy socket się rozłączył */
    var onDisconnect: (() -> Unit)? = null

    /**
     * Inicjalizuje Socket.IO pod danym namespace'em.
     * Zwraca true gdy jest token i poprawnie skonfigurowano.
     * Zwraca false gdy tokenu brak.
     */
    fun init(
        namespace: String,
        client: OkHttpClient,
        tokenProv: TokenProvider
    ): Boolean {
        // Czyścimy poprzednią instancję
        clear()

        val token = runBlocking { tokenProv.getAccessToken() }
        if (token.isNullOrBlank()) {
            Timber.tag(TAG).i("Brak tokenu – pomijam init() dla $namespace")
            return false
        }

        blockedByAuthError = false

        val base = AppPrefs.getBaseUrl()
            .removeSuffix("/")
            .replace("/api$", "")
        val url = "$base$namespace"
        Timber.tag(TAG).i("Inicjalizuję Socket.IO pod: $url")

        val opts = IO.Options().apply {
            transports = arrayOf(WebSocket.NAME)
            path = "/socket.io/v3/" // musi być identyczna jak na serwerze
            callFactory = client
            webSocketFactory = client
            // Socket.IO posiada własny mechanizm reconnect; ręcznego nie dodajemy
            reconnection = true
            auth = mapOf("token" to token)
        }

        socket = IO.socket(url, opts).apply {
            on(Socket.EVENT_CONNECT) {
                blockedByAuthError = false
                Timber.tag(TAG).i("✅ CONNECTED -> $namespace id=${id()}")
                onConnect?.invoke()
            }

            on(Socket.EVENT_CONNECT_ERROR) { args ->
                val raw = args.getOrNull(0)
                val err = raw as? Throwable
                val msg = err?.message ?: raw?.toString().orEmpty()
                Timber.tag(TAG).i("❌ CONNECT_ERROR: $msg")
                // HTTP 401/403 (Engine.IO handshake) albo komunikat z serwera WS
                val httpCauseMsg = (err as? EngineIOException)?.cause?.message.orEmpty()
                val isHttpUnauthorized =
                    httpCauseMsg.startsWith("HTTP 401") || httpCauseMsg.startsWith("HTTP 403")
                val isWsUnauthorized =
                    msg.contains("unauthorized", ignoreCase = true) ||
                            msg.contains("invalid token", ignoreCase = true)

                if (isHttpUnauthorized || isWsUnauthorized) {
                    handleUnauthorized(this)
                }
            }

            // Niestandardowy event z serwera
            on("unauthorized") {
                Timber.tag(TAG).i("🚫 RECEIVED unauthorized event")
                handleUnauthorized(this)
            }

            on(Socket.EVENT_DISCONNECT) { args ->
                Timber.tag(TAG).i("🔌 DISCONNECTED: ${args.getOrNull(0)}")
                onDisconnect?.invoke()
                // UWAGA: nie robimy ręcznego reconnectu (żadnych thread { connect() }).
                // Jeśli nie było błędu auth, zadziała wbudowany reconnection Socket.IO.
                // Jeśli był błąd auth, zablokujemy reconnection w handleUnauthorized().
                if (blockedByAuthError) {
                    try { io().reconnection(false) } catch (_: Throwable) {}
                }
            }

            on("error") { args ->
                Timber.tag(TAG).i("⚠️ SOCKET ERROR: ${args.getOrNull(0)}")
            }
        }

        return true
    }

    /** Czyści i zamyka socket całkowicie */
    fun clear() {
        try { socket?.io()?.reconnection(false) } catch (_: Throwable) {}
        try { socket?.off() } catch (_: Throwable) {}
        try { socket?.disconnect() } catch (_: Throwable) {}
        socket = null
    }

    /** Łączy (jeśli `init()` było OK) */
    fun connect() = socket?.connect()

    /** Rozłącza */
    fun disconnect() = socket?.disconnect()

    /** Rejestruje listener */
    fun on(event: String, listener: Emitter.Listener) = socket?.on(event, listener)
    fun isConnected(): Boolean = socket?.connected() == true

    /** Wysyła zdarzenie */
    fun emit(event: String, vararg args: Any) = socket?.emit(event, *args)

    // --- Prywatne ---

    private fun handleUnauthorized(s: Socket) {
        if (blockedByAuthError) return
        blockedByAuthError = true
        Timber.tag(TAG).i("🔒 Auth error – wyłączam auto-reconnect i czyszczę socket, proszę o refresh tokenu.")
        try { s.io().reconnection(false) } catch (_: Throwable) {}
        try { s.off() } catch (_: Throwable) {}
        try { s.disconnect() } catch (_: Throwable) {}
        onAuthExpired?.invoke()
    }

    private const val TAG = "SocketManager"
}
