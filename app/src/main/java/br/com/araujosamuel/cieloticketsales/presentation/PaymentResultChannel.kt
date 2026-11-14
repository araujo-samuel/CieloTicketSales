package br.com.araujosamuel.cieloticketsales.presentation

import android.net.Uri
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Canal singleton para comunicar o resultado do pagamento Cielo da
 * ResponseActivity para o CheckoutViewModel.
 *
 * Padrão escolhido: SharedFlow com extraBufferCapacity=1 para não perder
 * o evento caso o ViewModel ainda não esteja coletando.
 *
 * Alternativas consideradas:
 * - Intent extras na MainActivity: acoplamento entre Activities
 * - EventBus: biblioteca extra desnecessária
 * - SharedViewModel: exigiria escopo de Activity compartilhado
 */
object PaymentResultChannel {
    private val _results = MutableSharedFlow<Uri>(extraBufferCapacity = 1)
    val results: SharedFlow<Uri> = _results.asSharedFlow()

    fun emit(uri: Uri): Boolean = _results.tryEmit(uri)
}
