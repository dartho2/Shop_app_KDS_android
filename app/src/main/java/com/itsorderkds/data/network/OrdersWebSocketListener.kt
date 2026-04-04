package com.itsorderkds.data.network


import android.util.Log
import com.google.gson.Gson
import com.itsorderkds.data.model.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class OrdersWebSocketListener(
    private val ordersFlow: MutableSharedFlow<Order>
) : WebSocketListener() {
    private val gson = Gson()

    override fun onOpen(ws: WebSocket, response: Response) {
        Log.d("WS", "Connected to orders WS")
    }

    override fun onMessage(ws: WebSocket, text: String) {
        try {
            val order = gson.fromJson(text, Order::class.java)
            CoroutineScope(Dispatchers.IO).launch {
                ordersFlow.emit(order)
            }
        } catch (e: Exception) {
            Log.e("WS", "Failed to parse order JSON", e)
        }
    }

    override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
        Log.e("WS", "WebSocket failure", t)
    }

    override fun onClosing(ws: WebSocket, code: Int, reason: String) {
        ws.close(code, reason)
        Log.d("WS", "Closing: $code / $reason")
    }

    override fun onClosed(ws: WebSocket, code: Int, reason: String) {
        Log.d("WS", "Closed: $code / $reason")
    }
}