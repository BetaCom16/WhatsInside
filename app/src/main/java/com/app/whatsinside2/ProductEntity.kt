package com.app.whatsinside2

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val barcode: String,
    val name: String,
    val imageUrl: String?,
    val calories: Double?,

    //Anzahl und Ablaufdatum
    val quantity: Int,
    val expirationDate: Long?,
    val addedAt: Long = System.currentTimeMillis(),

    //Marke, Preis und Lagerort
    val brand: String? = null,
    val price: Double? = null,
    val location: String? = null
)