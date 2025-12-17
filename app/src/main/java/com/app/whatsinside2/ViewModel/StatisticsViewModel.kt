package com.app.whatsinside2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class PantryStats(
    val totalItems: Int = 0,
    val expiredItems: Int = 0,
    val expiringSoonItems: Int = 0,
    val goodItems: Int = 0
)

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = WhatsInsideDatabase.getDatabase(application)
    private val productDao = db.productDao()

    val stats: StateFlow<PantryStats> = productDao.getAllProducts()
        .map { products ->
            val now = System.currentTimeMillis()
            val threeDaysInMillis = 3 * 24 * 60 * 60 * 1000L
            val warningThreshold = now + threeDaysInMillis

            var expired = 0
            var soon = 0
            var good = 0

            products.forEach { product ->
                val expDate = product.expirationDate

                if(expDate != null) {
                    if(expDate < now) {
                        expired ++
                    } else if(expDate < warningThreshold) {
                        soon++
                    } else {
                        good++
                    }
                } else{
                    good++
                }
            }

            PantryStats(
                totalItems = products.size,
                expiredItems = expired,
                expiringSoonItems = soon,
                goodItems = good
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PantryStats()
        )
}