package br.com.araujosamuel.cieloticketsales.presentation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import br.com.araujosamuel.cieloticketsales.MainActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity responsável por capturar o retorno do Cielo Smart via Deep Link.
 *
 * Quando o Cielo Smart conclui o pagamento, ele dispara uma intent para a URI
 * configurada como urlCallback: "order://response?response={base64}" ou
 * "order://response?error={base64}".
 *
 * Esta Activity:
 * 1. Captura a URI
 * 2. Emite no PaymentResultChannel para o CheckoutViewModel processar
 * 3. Redireciona para a MainActivity mantendo a stack de navegação
 * 4. Encerra a si mesma
 */
@AndroidEntryPoint
class ResponseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val uri: Uri = intent.data ?: run {
            redirectToMain(null)
            return
        }

        // Emite o resultado para o CheckoutViewModel via canal compartilhado
        PaymentResultChannel.emit(uri)

        redirectToMain(uri)
    }

    private fun redirectToMain(responseUri: Uri?) {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_PAYMENT_RESULT
            responseUri?.let { putExtra(EXTRA_RESPONSE_URI, it.toString()) }
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(mainIntent)
        finish()
    }

    companion object {
        const val ACTION_PAYMENT_RESULT = "br.com.araujosamuel.cieloticketsales.PAYMENT_RESULT"
        const val EXTRA_RESPONSE_URI = "extra_response_uri"
    }
}
