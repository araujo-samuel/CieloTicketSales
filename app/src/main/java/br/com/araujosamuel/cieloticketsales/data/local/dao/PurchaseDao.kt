package br.com.araujosamuel.cieloticketsales.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.araujosamuel.cieloticketsales.data.local.entity.PurchaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(purchase: PurchaseEntity)

    @Query("SELECT * FROM purchases WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): PurchaseEntity?

    /**
     * Busca compra PENDING para um evento específico.
     * Usado pela estratégia de idempotência: se já existe uma compra pendente
     * para este evento, reutiliza ao invés de criar uma nova.
     */
    @Query("SELECT * FROM purchases WHERE eventId = :eventId AND status = 'PENDING' LIMIT 1")
    suspend fun findPendingByEventId(eventId: String): PurchaseEntity?

    @Query("SELECT * FROM purchases ORDER BY createdAt DESC")
    fun getAllPurchases(): Flow<List<PurchaseEntity>>

    /**
     * Atualiza o resultado de uma compra após retorno do Cielo.
     * Operação separada do insert para garantir que apenas os campos
     * de resultado sejam alterados, sem risco de sobrescrever dados originais.
     */
    @Query(
        """
        UPDATE purchases
        SET status = :status,
            authCode = :authCode,
            cieloCode = :cieloCode,
            cardBrand = :cardBrand,
            errorReason = :errorReason
        WHERE id = :id
        """
    )
    suspend fun updateResult(
        id: String,
        status: String,
        authCode: String?,
        cieloCode: String?,
        cardBrand: String?,
        errorReason: String?
    )
}
