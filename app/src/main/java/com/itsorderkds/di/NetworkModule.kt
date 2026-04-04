package com.itsorderkds.di

import android.content.Context
import androidx.room.Room
import com.itsorderkds.BuildConfig
import com.itsorderkds.data.core.lang.LanguageStore
import com.itsorderkds.data.dao.OrderDao
import com.itsorderkds.data.database.AppDatabase
import com.itsorderkds.data.network.AuthApi
import com.itsorderkds.data.network.AuthInterceptor
import com.itsorderkds.data.network.BaseUrlInterceptor
import com.itsorderkds.data.network.KdsApi
import com.itsorderkds.data.network.LanguageInterceptor
import com.itsorderkds.data.network.OrderApi
import com.itsorderkds.data.network.SettingsApi
import com.itsorderkds.data.network.TokenAuthenticator
import com.itsorderkds.data.network.preferences.DataStoreTokenProvider
import com.itsorderkds.data.network.preferences.TokenProvider
import com.itsorderkds.data.preferences.AppPreferencesManager
import com.itsorderkds.ui.settings.log.LogsApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // --- PODSTAWOWE ZALEŻNOŚCI SIECIOWE ---

    @Provides
    @Singleton
    fun provideTokenProvider(preferencesManager: AppPreferencesManager): TokenProvider =
        DataStoreTokenProvider(preferencesManager)

    @Provides
    @Singleton
    fun provideBaseUrlInterceptor(): BaseUrlInterceptor = BaseUrlInterceptor()

    @Provides @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

    /**
     * Centralny provider dla Retrofit.Builder.
     * Konfiguruje wspólne elementy (placeholder baseUrl, konwerter Gson).
     * Dzięki temu unikamy powtarzania kodu.
     */
    @Provides
    @Singleton
    fun provideRetrofitBuilder(): Retrofit.Builder {
        return Retrofit.Builder()
            .baseUrl("http://placeholder.com/") // Ten URL i tak zostanie nadpisany przez BaseUrlInterceptor
            .addConverterFactory(GsonConverterFactory.create())
    }

    // --- KONFIGURACJA DLA ZAPYTAŃ BEZ AUTORYZACJI ---
    @Provides
    @Singleton
    fun provideLogsApi(@Named("auth_retrofit") retrofit: Retrofit): LogsApi {
        return retrofit.create(LogsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideSettingsApi(@Named("auth_retrofit") retrofit: Retrofit): SettingsApi {
        return retrofit.create(SettingsApi::class.java)
    }

    @Provides
    @Singleton
    @Named("no_auth")
    fun provideNoAuthClient(
        loggingInterceptor: HttpLoggingInterceptor, baseUrlInterceptor: BaseUrlInterceptor
    ): OkHttpClient =
        OkHttpClient.Builder().addInterceptor(baseUrlInterceptor).addInterceptor(loggingInterceptor)
            .build()

    @Provides
    @Singleton
    @Named("no_auth_retrofit")
    fun provideNoAuthRetrofit(
        builder: Retrofit.Builder, // Wstrzykujemy gotowy builder
        @Named("no_auth") client: OkHttpClient
    ): Retrofit = builder.client(client) // Dołączamy tylko specyficzny klient
        .build()


    // --- KONFIGURACJA DLA ZAPYTAŃ Z AUTORYZACJĄ ---

    @Provides
    @Singleton
    fun provideTokenAuthenticator(
        tokenProvider: TokenProvider, authApi: AuthApi // AuthApi zależy od klienta "no_auth"
    ): TokenAuthenticator = TokenAuthenticator(tokenProvider, authApi)

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        tokenProvider: TokenProvider
    ) = AuthInterceptor(tokenProvider)


    @Provides
    @Singleton
    @Named("auth")          // ← jeśli rozróżniasz kilka klientów
    fun provideAuthClient(
        baseUrlInterceptor: BaseUrlInterceptor,
        authInterceptor   : AuthInterceptor,      // <- wstrzykiwany, NIE tworzony ręcznie
        tokenAuthenticator: TokenAuthenticator,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(baseUrlInterceptor)
        .addInterceptor(LanguageInterceptor { LanguageStore.get() })
        .addInterceptor(authInterceptor)      // token + obsługa 401
        .authenticator(tokenAuthenticator)    // odświeżanie tokenu
        .addInterceptor(loggingInterceptor)
        .build()

    @Provides
    @Singleton
    @Named("auth_retrofit")
    fun provideAuthRetrofit(
        builder: Retrofit.Builder, // Wstrzykujemy ten sam gotowy builder
        @Named("auth") client: OkHttpClient
    ): Retrofit = builder.client(client) // Dołączamy klienta z autoryzacją
        .build()


    // --- DOSTARCZANIE KONKRETNYCH KLAS API ---

    @Provides
    @Singleton
    fun provideAuthApi(@Named("no_auth_retrofit") retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideOrderApi(@Named("auth_retrofit") retrofit: Retrofit): OrderApi =
        retrofit.create(OrderApi::class.java)
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): AppDatabase {
        val builder = Room.databaseBuilder(
            appContext, AppDatabase::class.java, "app.db"
        )

        // W wersji DEBUG: automatycznie usuń bazę przy zmianie wersji (dla developmentu)
        // W wersji RELEASE: wykonaj migrację (dla produkcji)
        if (BuildConfig.DEBUG) {
            builder.fallbackToDestructiveMigration()
        } else {
            builder.addMigrations(AppDatabase.MIGRATION_20_21, AppDatabase.MIGRATION_21_22)
        }

        return builder.build()
    }

    @Provides
    fun provideOrderDao(db: AppDatabase): OrderDao = db.orderDao()
}
