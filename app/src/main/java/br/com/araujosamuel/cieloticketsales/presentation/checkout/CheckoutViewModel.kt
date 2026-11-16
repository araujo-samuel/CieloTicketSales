package br.com.araujosamuel.cieloticketsales.presentation.checkout

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.araujosamuel.cieloticketsales.data.cielo.CieloDeepLinkService
import dagger.hilt.android.lifecycle.HiltViewModel
import br.com.araujosamuel.cieloticketsales.domain.model.Purchase
import br.com.araujosamuel.cieloticketsales.domain.model.PurchaseStatus
import br.com.araujosamuel.cieloticketsales.domain.usecase.GetPurchasesUseCase
import br.com.araujosamuel.cieloticketsales.domain.usecase.UpdatePurchaseResultUseCase
import br.com.araujosamuel.cieloticketsales.presentation.PaymentResultChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CheckoutIntent {
    data class SelectPaymentCode(val code: String) : CheckoutIntent()
    object InitiatePayment : CheckoutIntent()
    object NavigationConsumed : CheckoutIntent()
    /** Enviado pela UI quando o Cielo Smart não está instalado no dispositivo */
    object CieloNotFound : CheckoutIntent()
}

/**
 * Eventos de efeito colateral emitidos pelo ViewModel para a UI executar.
 * A UI é responsável por chamar startActivity e packageManager — o ViewModel
 * apenas constrói a URI e delega a ação de navegação.
 */
sealed class CheckoutSideEffect {
    data class LaunchCieloPayment(val uri: Uri) : CheckoutSideEffect()
    data class CieloNotInstalled(val message: String) : CheckoutSideEffect()
}

data class CheckoutUiState(
    val purchase: Purchase? = null,
    val selectedPaymentCode: String = "CREDITO_AVISTA",
    val isLoading: Boolean = false,
    val isAwaitingCielo: Boolean = false,
    val error: String? = null,
    val navigateToResultId: String? = null
)

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPurchasesUseCase: GetPurchasesUseCase,
    private val updatePurchaseResultUseCase: UpdatePurchaseResultUseCase,
    private val cieloDeepLinkService: CieloDeepLinkService
) : ViewModel() {

    private val purchaseId: String = savedStateHandle["purchaseId"] ?: ""

    private val _uiState = MutableStateFlow(CheckoutUiState(isLoading = true))
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    private val _sideEffect = Channel<CheckoutSideEffect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

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

    fun processIntent(intent: CheckoutIntent) {
        when (intent) {
            is CheckoutIntent.SelectPaymentCode -> selectPaymentCode(intent.code)
            is CheckoutIntent.InitiatePayment -> initiatePayment()
            is CheckoutIntent.NavigationConsumed -> onNavigationConsumed()
            is CheckoutIntent.CieloNotFound -> onCieloNotFound()
        }
    }

    private fun selectPaymentCode(code: String) {
        _uiState.value = _uiState.value.copy(selectedPaymentCode = code)
    }

    /**
     * Constrói a URI de pagamento e emite [CheckoutSideEffect.LaunchCieloPayment].
     *
     * O ViewModel não chama startActivity nem acessa packageManager — essas são
     * responsabilidades da UI. A UI coleta o SideEffect, verifica se o Cielo está
     * instalado e chama startActivity. Se não estiver, envia [CheckoutIntent.CieloNotFound].
     *
     * Idempotência: a compra foi criada com UUID único em CreatePurchaseOrderUseCase.
     * Mesmo que o usuário clique em "Pagar" várias vezes, o orderId enviado ao Cielo
     * é sempre o mesmo UUID, o que previne cobranças duplicadas.
     */
    private fun initiatePayment() {
        val state = _uiState.value
        val purchase = state.purchase ?: return

        // Idempotência: não relança se já está aguardando resposta
        if (state.isAwaitingCielo) return

        val paymentUri = cieloDeepLinkService.buildPaymentUri(
            orderId = purchase.id,
            amountInCents = purchase.totalAmountInCents,
            paymentCode = state.selectedPaymentCode,
            eventName = purchase.eventName,
            quantity = purchase.quantity
        )

        _uiState.value = state.copy(isAwaitingCielo = true, error = null)

        viewModelScope.launch {
            _sideEffect.send(CheckoutSideEffect.LaunchCieloPayment(paymentUri))
        }
    }

    private fun onCieloNotFound() {
        _uiState.value = _uiState.value.copy(
            isAwaitingCielo = false,
            error = "Cielo Smart não encontrado. Instale o emulador Cielo para testar."
        )
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

    private fun onNavigationConsumed() {
        _uiState.value = _uiState.value.copy(navigateToResultId = null)
    }
}
