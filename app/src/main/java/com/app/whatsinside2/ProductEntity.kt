package com.app.whatsinside2

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey
    val barcode: String,

    val name: String,
    val imageUrl: String?,
    val calories: Double?,

    val addedAt: Long = System.currentTimeMillis()
)