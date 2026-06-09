package com.app.digitalwallet.di

import com.app.digitalwallet.data.remote.api.AuthApiService
import com.app.digitalwallet.data.remote.api.KycApiService
import com.app.digitalwallet.data.remote.api.QRApiService
import com.app.digitalwallet.data.remote.api.WalletApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideQRApiService(retrofit: Retrofit): QRApiService {
        return retrofit.create(QRApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideWalletApiService(retrofit: Retrofit): WalletApiService {
        return retrofit.create(WalletApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideKycApiService(retrofit: Retrofit): KycApiService {
        return retrofit.create(KycApiService::class.java)
    }
}
