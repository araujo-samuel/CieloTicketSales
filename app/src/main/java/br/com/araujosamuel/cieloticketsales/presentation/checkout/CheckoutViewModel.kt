package br.com.araujosamuel.cieloticketsales.presentation.checkout

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.araujosamuel.cieloticketsales.data.cielo.CieloDeepLinkService
import br.com.araujosamuel.cieloticketsales.domain.model.Purchase
import br.com.araujosamuel.cieloticketsales.domain.model.PurchaseStatus
import br.com.araujosamuel.cieloticketsales.domain.usecase.GetPurchasesUseCase
import br.com.araujosamuel.cieloticketsales.domain.usecase.UpdatePurchaseResultUseCase
import br.com.araujosamuel.cieloticketsales.presentation.PaymentResultChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CheckoutUiState(
    val purchase: Purchase? = null,
    val selectedPaymentCode: String = "CREDITO_AVISTA",
    val isLoading: Boolean = false,
    val isAwaitingCielo: Boolean = false,
    val error: String? = null,
    val navigateToResultId: String? = null
)

class CheckoutViewModel(
    private val purchaseId: String,
    private val getPurchasesUseCase: GetPurchasesUseCase,
    private val updatePurchaseResultUseCase: UpdatePurchaseResultUseCase,
    private val cieloDeepLinkService: CieloDeepLinkService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState(isLoading = true))
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    init {
        loadPurchase()
        observePaymentResult()
    }

    private fun loadPurchase() {
        viewModelScope.launch {
            runCatching { getPurchasesUseCase.findById(purchaseId) }
                .onSuccess { purchase ->
                    if (purchase != null) {
                        _uiState.value = CheckoutUiState(
                            purchase = purchase,
                            // Se já tem resultado (ex: retomou após crash), vai direto para o resultado
                            navigateToResultId = if (purchase.status != PurchaseStatus.PENDING) purchaseId else null
                        )
                    } else {
                        _uiState.value = CheckoutUiState(error = "Pedido não encontrado")
                    }
                }
                .onFailure { error ->
                    _uiState.value = CheckoutUiState(error = error.message ?: "Erro ao carregar pedido")
                }
        }
    }

    private fun observePaymentResult() {
        viewModelScope.launch {
            PaymentResultChannel.results.collect { uri ->
                handlePaymentResponse(uri)
            }
        }
    }

    fun selectPaymentCode(code: String) {
        _uiState.value = _uiState.value.copy(selectedPaymentCode = code)
    }

    /**
     * Lança o Deep Link do Cielo Smart para processar o pagamento.
     *
     * Idempotência: a compra já foi criada com UUID único em CreatePurchaseOrderUseCase.
     * Mesmo que o usuário clique em "Pagar" várias vezes, o orderId enviado ao Cielo
     * é sempre o mesmo UUID, o que previne cobranças duplicadas.
     */
    fun initiatePayment(context: Context) {
        val state = _uiState.value
        val purchase = state.purchase ?: return

        // Idempotência: não relança se já está aguardando resposta
        if (state.isAwaitingCielo) return

        val paymentUri = cieloDeepLinkService.buildPaymentUri(
            orderId = purchase.id,
            amountInCents = purchase.totalAmountInCents,
            paymentCode = state.selectedPaymentCode
        )

        // Verifica se o Cielo Smart está instalado antes de lançar
        val intent = Intent(Intent.ACTION_VIEW, paymentUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val packageManager = context.packageManager
        val resolveInfo = packageManager.queryIntentActivities(intent, 0)

        if (resolveInfo.isNullOrEmpty()) {
            _uiState.value = state.copy(
                error = "Cielo Smart não encontrado. Instale o emulador Cielo para testar."
            )
            return
        }

        _uiState.value = state.copy(isAwaitingCielo = true, error = null)
        context.startActivity(intent)
    }

    private fun handlePaymentResponse(uri: Uri) {
        viewModelScope.launch {
            val response = cieloDeepLinkService.parsePaymentResponse(uri)

            runCatching {
                updatePurchaseResultUseCase(purchaseId, response)
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isAwaitingCielo = false,
                    navigateToResultId = purchaseId
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isAwaitingCielo = false,
                    error = "Erro ao salvar resultado: ${error.message}"
                )
            }
        }
    }

    fun onNavigationConsumed() {
        _uiState.value = _uiState.value.copy(navigateToResultId = null)
    }

    class Factory(
        private val purchaseId: String,
        private val getPurchasesUseCase: GetPurchasesUseCase,
        private val updatePurchaseResultUseCase: UpdatePurchaseResultUseCase,
        private val cieloDeepLinkService: CieloDeepLinkService
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CheckoutViewModel(
                purchaseId,
                getPurchasesUseCase,
                updatePurchaseResultUseCase,
                cieloDeepLinkService
            ) as T
    }
}
