//package com.itsorderkds.data.network
//
//import android.content.Context
//import com.itsorderkds.ItsChat
//import com.itsorderkds.data.model.NetworkModule
//import com.itsorderkds.util.AppPrefs
//
//class RemoteDataSource(context: Context) {
//    private val client  = (context.applicationContext as ItsChat).okHttpClient
//    private val baseUrl = AppPrefs.getBaseUrl()
//
//    fun <Api> buildApi(api: Class<Api>): Api =
//        NetworkModule.provideRetrofit(baseUrl, client, api)
//}
