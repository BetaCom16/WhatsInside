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

            val currentProducts = productsFlow.first()

            if(currentProducts.isNotEmpty()){
                val result = recipeRepository.getRecipesFromGemini(currentProducts)

                result.onSuccess { recipes ->
                    _suggestedRecipes.value = recipes
                }.onFailure { exception ->
                    // Leere Liste, falls ein Fehler auftritt
                    _suggestedRecipes.value = emptyList()
                    recipeErrorMsg.value = exception.message ?: "Unbekannter Fehler"
                }
            } else {
                // Zeigt nichts an, wenn die DB leer ist
                _suggestedRecipes.value = emptyList()
                recipeErrorMsg.value = "Der Vorratsschrank ist leer!"
            }
            isLoadingRecipes.value = false
        }
    }


    // Funktion als Companion-Object, um es von außen für den Unittest StatisticsViewModelTest greifbar zu machen
    companion object {
        fun calculateStats(
            products: List<ProductEntity>,
            currentTimeMillis: Long = System.currentTimeMillis()
        ): PantryStats {

            val threeDaysInMillis = 3 * 24 * 60 * 60 * 1000L
            val warningThreshold = currentTimeMillis + threeDaysInMillis

            //Counter für gute, bald ablaufende und abgelaufene Produkte
            var expired = 0
            var soon = 0
            var good = 0

            products.forEach { product ->
                val expDate = product.expirationDate
                if (expDate != null) {
                    // Hier wird mit der übergebenen Zeit verglichen
                    // Zählt die jeweilige Kategorie hoch, wenn die Bedingung erfüllt ist
                    if (expDate < currentTimeMillis) {
                        expired++
                    } else if (expDate < warningThreshold) {
                        soon++
                    } else {
                        good++
                    }
                } else {
                    good++
                }
            }
            return PantryStats(products.size, expired, soon, good)
        }
    }
}