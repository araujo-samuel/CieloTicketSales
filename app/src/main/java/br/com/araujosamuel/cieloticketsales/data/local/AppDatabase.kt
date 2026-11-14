package br.com.araujosamuel.cieloticketsales.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import br.com.araujosamuel.cieloticketsales.data.local.dao.PurchaseDao
import br.com.araujosamuel.cieloticketsales.data.local.entity.PurchaseEntity

@Database(
    entities = [PurchaseEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun purchaseDao(): PurchaseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cielo_ticket_sales.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
