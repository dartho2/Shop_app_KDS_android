package com.itsorderkds.data.network

import okhttp3.OkHttpClient


object WebSocketProvider {
    // Możesz dodać tu interceptory, SSL itp.
    val client: OkHttpClient = OkHttpClient.Builder().build()
}