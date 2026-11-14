package br.com.araujosamuel.cieloticketsales.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.com.araujosamuel.cieloticketsales.domain.model.Purchase
import br.com.araujosamuel.cieloticketsales.domain.usecase.GetPurchasesUseCase
import kotlinx.coroutines.flow.Flow

class PurchaseHistoryViewModel(
    getPurchasesUseCase: GetPurchasesUseCase
) : ViewModel() {

    val purchases: Flow<List<Purchase>> = getPurchasesUseCase()

    class Factory(private val getPurchasesUseCase: GetPurchasesUseCase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PurchaseHistoryViewModel(getPurchasesUseCase) as T
    }
}
