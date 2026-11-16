package br.com.araujosamuel.cieloticketsales.di

import android.content.Context
import br.com.araujosamuel.cieloticketsales.data.cielo.CieloDeepLinkService
import br.com.araujosamuel.cieloticketsales.data.local.AppDatabase
import br.com.araujosamuel.cieloticketsales.data.local.dao.PurchaseDao
import br.com.araujosamuel.cieloticketsales.data.repository.EventRepository
import br.com.araujosamuel.cieloticketsales.data.repository.EventRepositoryImpl
import br.com.araujosamuel.cieloticketsales.data.repository.PurchaseRepository
import br.com.araujosamuel.cieloticketsales.data.repository.PurchaseRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // --- Data layer ---

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getInstance(context)

    @Provides
    @Singleton
    fun providePurchaseDao(database: AppDatabase): PurchaseDao =
        database.purchaseDao()

    @Provides
    @Singleton
    fun provideEventRepository(): EventRepository =
        EventRepositoryImpl()

    @Provides
    @Singleton
    fun providePurchaseRepository(purchaseDao: PurchaseDao): PurchaseRepository =
        PurchaseRepositoryImpl(purchaseDao)

    @Provides
    @Singleton
    fun provideCieloDeepLinkService(): CieloDeepLinkService =
        CieloDeepLinkService()
}
