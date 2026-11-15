package br.com.araujosamuel.cieloticketsales.presentation.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.araujosamuel.cieloticketsales.domain.model.Purchase
import br.com.araujosamuel.cieloticketsales.domain.usecase.GetPurchasesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PurchaseResultUiState(
    val purchase: Purchase? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PurchaseResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPurchasesUseCase: GetPurchasesUseCase
) : ViewModel() {

    private val purchaseId: String = savedStateHandle["purchaseId"] ?: ""

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
}
