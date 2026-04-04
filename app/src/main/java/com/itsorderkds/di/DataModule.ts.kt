// app/src/main/java/com/itsorderchat/di/DataModule.kt
package com.itsorderkds.di

import android.content.Context
import com.itsorderkds.data.preferences.UserPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides @Singleton
    fun provideUserPreferences(
        @ApplicationContext ctx: Context
    ): UserPreferences = UserPreferences(ctx)
//
//    @Provides @Singleton
//    fun provideProfileRepository(
//        authApi: com.itsorderkds.data.network.ApiItsorder,
//        prefs: UserPreferences
//    ): ProfileRepository =
//        ProfileRepository(authApi, prefs)
}
