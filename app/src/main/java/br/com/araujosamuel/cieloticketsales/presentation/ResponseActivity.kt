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
 * 2. Redireciona para a MainActivity passando a URI via Intent
 * 3. Encerra a si mesma
 *
 * A emissão no PaymentResultChannel é feita exclusivamente pela MainActivity,
 * que é o ponto de entrada único — evita double-emission no canal compartilhado.
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
