package br.com.araujosamuel.cieloticketsales.presentation.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.araujosamuel.cieloticketsales.domain.model.Purchase
import br.com.araujosamuel.cieloticketsales.domain.usecase.GetPurchasesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PurchaseResultUiState(
    val purchase: Purchase? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class PurchaseResultViewModel(
    private val purchaseId: String,
    private val getPurchasesUseCase: GetPurchasesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PurchaseResultUiState(isLoading = true))
    val uiState: StateFlow<PurchaseResultUiState> = _uiState.asStateFlow()

    init {
        loadPurchase()
    }

    private fun loadPurchase() {
        viewModelScope.launch {
            runCatching { getPurchasesUseCase.findById(purchaseId) }
                .onSuccess { purchase ->
                    if (purchase != null) {
                        _uiState.value = PurchaseResultUiState(purchase = purchase)
                    } else {
                        _uiState.value = PurchaseResultUiState(error = "Compra não encontrada")
                    }
                }
                .onFailure { error ->
                    _uiState.value = PurchaseResultUiState(error = error.message ?: "Erro ao carregar compra")
                }
        }
    }

    class Factory(
        private val purchaseId: String,
        private val getPurchasesUseCase: GetPurchasesUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PurchaseResultViewModel(purchaseId, getPurchasesUseCase) as T
    }
}
