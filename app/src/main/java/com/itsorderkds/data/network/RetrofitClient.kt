package com.itsorderkds.data.network

//import android.content.Context
//import android.util.Log
//import com.itsorderkds.data.network.preferences.DataStoreTokenProvider
//import com.itsorderkds.util.AppPrefs
//import okhttp3.OkHttpClient
//import okhttp3.logging.HttpLoggingInterceptor
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//object RetrofitClient {
//
//    // Single source of truth: zawsze wywołuj tego jednego getRetrofit z Context
//    fun getRetrofit(context: Context): Retrofit {
//        // 1) Provider tokenów
//        val tokenProvider = DataStoreTokenProvider(context)
//
//        // 2) “Goły” authApi do refresh-ów (bez AuthInterceptor/Authenticator)
//        val refreshClient = OkHttpClient.Builder()
//            .addInterceptor(HttpLoggingInterceptor().apply {
//                level = HttpLoggingInterceptor.Level.BODY
//            })
//            .build()
//
//        val authApi = Retrofit.Builder()
//            .baseUrl(AppPrefs.getBaseUrl().ensureEndsWithSlash())
//            .client(refreshClient)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//            .create(AuthApi::class.java)
//
//        // 3) Główny klient z interceptorem i authenticator-em
//        val httpClient = OkHttpClient.Builder()
//            // dokleja header Authorization: Bearer <token>
//            .addInterceptor(AuthInterceptor(tokenProvider))
//            // automatyczne odświeżanie po 401
//            .authenticator(TokenAuthenticator(tokenProvider, authApi))
//            // logi request/response
//            .addInterceptor(HttpLoggingInterceptor().apply {
//                level = HttpLoggingInterceptor.Level.BODY
//            })
//            .build()
//
//        // 4) Finalny Retrofit
//        return Retrofit.Builder()
//            .baseUrl(AppPrefs.getBaseUrl().ensureEndsWithSlash())
//            .client(httpClient)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//    }
//
//    // helper do dopilnowania ukośnika
//    private fun String.ensureEndsWithSlash() =
//        if (endsWith("/")) this else "$this/"
//}
