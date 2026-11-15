package br.com.araujosamuel.cieloticketsales.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.EVENT_LIST
    ) {
        composable(Routes.EVENT_LIST) {
            val viewModel: EventListViewModel = hiltViewModel()
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

        composable(Routes.EVENT_DETAIL) {
            val viewModel: EventDetailViewModel = hiltViewModel()
            EventDetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToCheckout = { purchaseId ->
                    navController.navigate(Routes.checkout(purchaseId))
                }
            )
        }

        composable(Routes.CHECKOUT) {
            val viewModel: CheckoutViewModel = hiltViewModel()
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

        composable(Routes.PURCHASE_RESULT) {
            val viewModel: PurchaseResultViewModel = hiltViewModel()
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
            val viewModel: PurchaseHistoryViewModel = hiltViewModel()
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
