package com.app.whatsinside2

data class Recipe(
    val id: Int,
    val name: String,
    val ingredients: List<String>, // Liste der benötigten Zutaten
    val description: String,       // Kurze Beschreibung

    val availableIngredientsCount: Int = 0,

    val missingIngredients: List<String> = emptyList()
)