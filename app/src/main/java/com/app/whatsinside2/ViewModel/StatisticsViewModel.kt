package com.app.whatsinside2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PantryStats(
    val totalItems: Int = 0,
    val expiredItems: Int = 0,
    val expiringSoonItems: Int = 0,
    val goodItems: Int = 0
)

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = WhatsInsideDatabase.getDatabase(application)
    private val productDao = db.productDao()
    private val recipeRepository = RecipeRepository()

    private val productsFlow = productDao.getAllProducts()

    val stats: StateFlow<PantryStats> = productsFlow
        .map { products -> calculateStats(products) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PantryStats()
        )

    private val _suggestedRecipes = MutableStateFlow<List<Recipe>>(emptyList())
    val suggestedRecipes: StateFlow<List<Recipe>> = _suggestedRecipes

    val isLoadingRecipes = MutableStateFlow(false)
    val recipeErrorMsg = MutableStateFlow<String?>(null)

    fun generateRecipes() {
        viewModelScope.launch {
            isLoadingRecipes.value = true
            recipeErrorMsg.value = null

            //Einmaliges "holen" des aktuellen Vorratsschranks
            val currentProducts = productsFlow.first()

            if(currentProducts.isNotEmpty()){

                val result = recipeRepository.getRecipesFromGemini(currentProducts)

                result.onSuccess { recipes ->
                    _suggestedRecipes.value = recipes
                }.onFailure { exception ->
                    _suggestedRecipes.value = emptyList()
                    recipeErrorMsg.value = exception.message ?: "Unbekannter Fehler"
                }
            } else {
                _suggestedRecipes.value = emptyList()
                recipeErrorMsg.value = "Der Vorratsschrank ist leer!"
            }
            isLoadingRecipes.value = false
        }
    }
}

private fun calculateStats(products: List<ProductEntity>): PantryStats {
    val now = System.currentTimeMillis()
    val threeDaysInMillis = 3 * 24 * 60 * 60 * 1000L
    val warningThreshold = now + threeDaysInMillis

    var expired = 0
    var soon = 0
    var good = 0

    products.forEach { product ->
        val expDate = product.expirationDate
        if (expDate != null) {
            if (expDate < now) expired++
            else if (expDate < warningThreshold) soon++
            else good++
        } else {
            good++
        }
    }
    return PantryStats(products.size, expired, soon, good)
}