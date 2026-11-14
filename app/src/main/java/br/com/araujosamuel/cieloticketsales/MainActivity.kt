package br.com.araujosamuel.cieloticketsales

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import br.com.araujosamuel.cieloticketsales.presentation.PaymentResultChannel
import br.com.araujosamuel.cieloticketsales.presentation.ResponseActivity
import br.com.araujosamuel.cieloticketsales.presentation.navigation.AppNavigation
import br.com.araujosamuel.cieloticketsales.ui.theme.CieloTicketSalesTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Processa resultado de pagamento se a Activity foi retomada pela ResponseActivity
        handlePaymentResultIntent(intent)

        setContent {
            CieloTicketSalesTheme {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    app = application as CieloTicketSalesApp
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handlePaymentResultIntent(intent)
    }

    private fun handlePaymentResultIntent(intent: Intent) {
        if (intent.action == ResponseActivity.ACTION_PAYMENT_RESULT) {
            val uriString = intent.getStringExtra(ResponseActivity.EXTRA_RESPONSE_URI) ?: return
            val uri = Uri.parse(uriString)
            PaymentResultChannel.emit(uri)
        }
    }
}
