package br.com.araujosamuel.cieloticketsales.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Gera QR Code para ingressos aprovados usando a biblioteca ZXing Core.
 *
 * Decisão: ZXing Core (sem a camada Android) foi escolhida por ser leve,
 * madura e não adicionar Activities/Fragments desnecessários ao projeto.
 * A geração é feita puramente em código, sem dependência de UI.
 *
 * Trade-off: exige conversão manual BitMatrix → Bitmap, mas oferece
 * mais controle sobre o resultado e tamanho menor do APK.
 */
object QrCodeGenerator {

    /**
     * Gera um Bitmap de QR Code a partir do conteúdo fornecido.
     *
     * @param content conteúdo a ser encodado (ex: JSON do ingresso ou purchaseId)
     * @param size dimensão em pixels (largura = altura)
     * @return Bitmap com o QR Code
     */
    fun generate(content: String, size: Int = 512): Bitmap {
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    /**
     * Constrói o conteúdo do QR Code do ingresso.
     * Formato simples para leitura por validadores de entrada.
     */
    fun buildTicketContent(
        purchaseId: String,
        eventName: String,
        quantity: Int,
        authCode: String?
    ): String = "INGRESSO|$purchaseId|$eventName|QTD:$quantity|AUTH:${authCode ?: "N/A"}"
}
