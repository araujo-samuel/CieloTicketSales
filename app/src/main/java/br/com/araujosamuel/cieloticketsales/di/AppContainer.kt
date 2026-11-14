package br.com.araujosamuel.cieloticketsales.di

import android.content.Context
import br.com.araujosamuel.cieloticketsales.data.cielo.CieloDeepLinkService
import br.com.araujosamuel.cieloticketsales.data.local.AppDatabase
import br.com.araujosamuel.cieloticketsales.data.repository.EventRepository
import br.com.araujosamuel.cieloticketsales.data.repository.EventRepositoryImpl
import br.com.araujosamuel.cieloticketsales.data.repository.PurchaseRepository
import br.com.araujosamuel.cieloticketsales.data.repository.PurchaseRepositoryImpl
import br.com.araujosamuel.cieloticketsales.domain.usecase.CreatePurchaseOrderUseCase
import br.com.araujosamuel.cieloticketsales.domain.usecase.GetEventsUseCase
import br.com.araujosamuel.cieloticketsales.domain.usecase.GetPurchasesUseCase
import br.com.araujosamuel.cieloticketsales.domain.usecase.UpdatePurchaseResultUseCase

/**
 * Container de injeção de dependências manual.
 *
 * Decisão técnica: DI manual foi escolhido em vez de Hilt/Koin porque:
 * 1. Simplicidade — sem annotation processing extra (já temos KSP para Room)
 * 2. Transparência — todas as dependências visíveis em um único lugar
 * 3. Mais explícito para code review — sem "magia" de frameworks
 * 4. Escopo adequado para o tamanho do projeto
 *
 * Trade-off: em projetos maiores, Hilt oferece melhor gerenciamento de ciclo
 * de vida e escopos automáticos (Activity, Fragment, ViewModel).
 */
class AppContainer(context: Context) {

    // --- Data layer ---
    private val database = AppDatabase.getInstance(context)
    private val purchaseDao = database.purchaseDao()

    val eventRepository: EventRepository = EventRepositoryImpl()
    val purchaseRepository: PurchaseRepository = PurchaseRepositoryImpl(purchaseDao)
    val cieloDeepLinkService = CieloDeepLinkService()

    // --- Domain layer (use cases) ---
    val getEventsUseCase = GetEventsUseCase(eventRepository)
    val createPurchaseOrderUseCase = CreatePurchaseOrderUseCase(purchaseRepository)
    val updatePurchaseResultUseCase = UpdatePurchaseResultUseCase(purchaseRepository)
    val getPurchasesUseCase = GetPurchasesUseCase(purchaseRepository)
}
