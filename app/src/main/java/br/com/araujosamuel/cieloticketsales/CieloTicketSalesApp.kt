package br.com.araujosamuel.cieloticketsales

import android.app.Application
import br.com.araujosamuel.cieloticketsales.di.AppContainer

class CieloTicketSalesApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
