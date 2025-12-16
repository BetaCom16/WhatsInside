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
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

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

@Database(entities = [ProductEntity::class, LocationEntity::class], version = 4, exportSchema = false)
abstract class WhatsInsideDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun locationDao(): LocationDao

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

                    // Folgender Codeblock fügt beim Erstellen der DB Standardorte hinzu
                    .addCallback(object : RoomDatabase.Callback(){
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getDatabase(context)
                                val dao = database.locationDao()

                                dao.insertLocation(LocationEntity(name = "Vorratsschrank"))
                                dao.insertLocation(LocationEntity(name = "Kühlschrank"))
                                dao.insertLocation(LocationEntity(name = "Gefriertruhe"))
                                dao.insertLocation(LocationEntity(name = "Keller"))
                            }
                        }
                    })

                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}