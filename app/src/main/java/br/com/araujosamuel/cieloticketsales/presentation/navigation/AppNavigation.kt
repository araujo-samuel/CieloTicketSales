package br.com.araujosamuel.cieloticketsales.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import br.com.araujosamuel.cieloticketsales.CieloTicketSalesApp
import br.com.araujosamuel.cieloticketsales.presentation.checkout.CheckoutScreen
import br.com.araujosamuel.cieloticketsales.presentation.checkout.CheckoutViewModel
import br.com.araujosamuel.cieloticketsales.presentation.eventdetail.EventDetailScreen
import br.com.araujosamuel.cieloticketsales.presentation.eventdetail.EventDetailViewModel
import br.com.araujosamuel.cieloticketsales.presentation.eventlist.EventListScreen
import br.com.araujosamuel.cieloticketsales.presentation.eventlist.EventListViewModel
import br.com.araujosamuel.cieloticketsales.presentation.history.PurchaseHistoryScreen
import br.com.araujosamuel.cieloticketsales.presentation.history.PurchaseHistoryViewModel
import br.com.araujosamuel.cieloticketsales.presentation.result.PurchaseResultScreen
import br.com.araujosamuel.cieloticketsales.presentation.result.PurchaseResultViewModel

object Routes {
    const val EVENT_LIST = "events"
    const val EVENT_DETAIL = "events/{eventId}"
    const val CHECKOUT = "checkout/{purchaseId}"
    const val PURCHASE_RESULT = "result/{purchaseId}"
    const val PURCHASE_HISTORY = "history"

    fun eventDetail(eventId: String) = "events/$eventId"
    fun checkout(purchaseId: String) = "checkout/$purchaseId"
    fun purchaseResult(purchaseId: String) = "result/$purchaseId"
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    app: CieloTicketSalesApp
) {
    val container = app.container

    NavHost(
        navController = navController,
        startDestination = Routes.EVENT_LIST
    ) {
        composable(Routes.EVENT_LIST) {
            val viewModel: EventListViewModel = viewModel(
                factory = EventListViewModel.Factory(container.getEventsUseCase)
            )
            EventListScreen(
                viewModel = viewModel,
                onEventClick = { eventId ->
                    navController.navigate(Routes.eventDetail(eventId))
                },
                onHistoryClick = {
                    navController.navigate(Routes.PURCHASE_HISTORY)
                }
            )
        }

        composable(Routes.EVENT_DETAIL) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
            val viewModel: EventDetailViewModel = viewModel(
                factory = EventDetailViewModel.Factory(
                    eventId = eventId,
                    eventRepository = container.eventRepository,
                    createPurchaseOrderUseCase = container.createPurchaseOrderUseCase
                )
            )
            EventDetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToCheckout = { purchaseId ->
                    navController.navigate(Routes.checkout(purchaseId))
                }
            )
        }

        composable(Routes.CHECKOUT) { backStackEntry ->
            val purchaseId = backStackEntry.arguments?.getString("purchaseId") ?: return@composable
            val viewModel: CheckoutViewModel = viewModel(
                factory = CheckoutViewModel.Factory(
                    purchaseId = purchaseId,
                    getPurchasesUseCase = container.getPurchasesUseCase,
                    updatePurchaseResultUseCase = container.updatePurchaseResultUseCase,
                    cieloDeepLinkService = container.cieloDeepLinkService
                )
            )
            CheckoutScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToResult = { resultId ->
                    navController.navigate(Routes.purchaseResult(resultId)) {
                        // Remove checkout da backstack — não faz sentido voltar para ele após o resultado
                        popUpTo(Routes.CHECKOUT) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.PURCHASE_RESULT) { backStackEntry ->
            val purchaseId = backStackEntry.arguments?.getString("purchaseId") ?: return@composable
            val viewModel: PurchaseResultViewModel = viewModel(
                factory = PurchaseResultViewModel.Factory(
                    purchaseId = purchaseId,
                    getPurchasesUseCase = container.getPurchasesUseCase
                )
            )
            PurchaseResultScreen(
                viewModel = viewModel,
                onBackToHome = {
                    navController.navigate(Routes.EVENT_LIST) {
                        popUpTo(Routes.EVENT_LIST) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.PURCHASE_HISTORY) {
            val viewModel: PurchaseHistoryViewModel = viewModel(
                factory = PurchaseHistoryViewModel.Factory(container.getPurchasesUseCase)
            )
            PurchaseHistoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onPurchaseClick = { purchaseId ->
                    navController.navigate(Routes.purchaseResult(purchaseId))
                }
            )
        }
    }
}
