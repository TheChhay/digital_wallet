package com.app.digitalwallet.di

import com.app.digitalwallet.data.repository.AuthRepositoryImpl
import com.app.digitalwallet.data.repository.KycRepositoryImpl
import com.app.digitalwallet.data.repository.QRRepositoryImpl
import com.app.digitalwallet.data.repository.WalletRepositoryImpl
import com.app.digitalwallet.domain.repository.IAuthRepository
import com.app.digitalwallet.domain.repository.IKycRepository
import com.app.digitalwallet.domain.repository.IQRRepository
import com.app.digitalwallet.domain.repository.IWalletRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWalletRepository(
        walletRepository: WalletRepositoryImpl
    ): IWalletRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepository: AuthRepositoryImpl
    ): IAuthRepository

    @Binds
    @Singleton
    abstract fun bindKycRepository(
        kycRepository: KycRepositoryImpl
    ): IKycRepository

    @Binds
    @Singleton
    abstract fun bindQRRepository(
        qrRepository: QRRepositoryImpl
    ): IQRRepository
}
