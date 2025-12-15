package com.app.whatsinside2

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY addedAt DESC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    //Abfrage zum Laden eines Produkts, um es zu bearbeiten
    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Int): ProductEntity?

    //Wenn die ID bereits existiert, wird es aktualisiert
    //Wenn ID=0, dann wird ein neues Produkt angelegt
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)
}

@Database(entities = [ProductEntity::class], version = 3, exportSchema = false)
abstract class WhatsInsideDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao

    companion object {
        @Volatile
        private var INSTANCE: WhatsInsideDatabase? = null

        fun getDatabase(context: Context): WhatsInsideDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WhatsInsideDatabase::class.java,
                    "whats_inside_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}