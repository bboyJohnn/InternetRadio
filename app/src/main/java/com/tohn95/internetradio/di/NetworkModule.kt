package com.tohn95.internetradio.di

import android.content.Context
import com.tohn95.internetradio.data.remote.RadioBrowserClient
import com.tohn95.internetradio.data.remote.ServerProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton
    fun serverProvider(@ApplicationContext ctx: Context): ServerProvider = ServerProvider(ctx)

    @Provides @Singleton
    fun radioBrowserClient(sp: ServerProvider): RadioBrowserClient = RadioBrowserClient(sp)
}
