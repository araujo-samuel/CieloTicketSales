package br.com.araujosamuel.cieloticketsales.di

import android.content.Context
import br.com.araujosamuel.cieloticketsales.data.cielo.CieloDeepLinkService
import br.com.araujosamuel.cieloticketsales.data.local.AppDatabase
import br.com.araujosamuel.cieloticketsales.data.local.dao.PurchaseDao
import br.com.araujosamuel.cieloticketsales.data.repository.EventRepository
import br.com.araujosamuel.cieloticketsales.data.repository.EventRepositoryImpl
import br.com.araujosamuel.cieloticketsales.data.repository.PurchaseRepository
import br.com.araujosamuel.cieloticketsales.data.repository.PurchaseRepositoryImpl
import br.com.araujosamuel.cieloticketsales.domain.usecase.CreatePurchaseOrderUseCase
import br.com.araujosamuel.cieloticketsales.domain.usecase.GetEventsUseCase
import br.com.araujosamuel.cieloticketsales.domain.usecase.GetPurchasesUseCase
import br.com.araujosamuel.cieloticketsales.domain.usecase.UpdatePurchaseResultUseCase
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

    // --- Domain layer ---

    @Provides
    @Singleton
    fun provideGetEventsUseCase(eventRepository: EventRepository): GetEventsUseCase =
        GetEventsUseCase(eventRepository)

    @Provides
    @Singleton
    fun provideCreatePurchaseOrderUseCase(purchaseRepository: PurchaseRepository): CreatePurchaseOrderUseCase =
        CreatePurchaseOrderUseCase(purchaseRepository)

    @Provides
    @Singleton
    fun provideUpdatePurchaseResultUseCase(purchaseRepository: PurchaseRepository): UpdatePurchaseResultUseCase =
        UpdatePurchaseResultUseCase(purchaseRepository)

    @Provides
    @Singleton
    fun provideGetPurchasesUseCase(purchaseRepository: PurchaseRepository): GetPurchasesUseCase =
        GetPurchasesUseCase(purchaseRepository)
}
