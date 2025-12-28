package com.app.whatsinside2

import android.util.Log
import com.app.whatsinside2.ProductEntity
import com.google.ai.client.generativeai.GenerativeModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.app.whatsinside2.BuildConfig

class RecipeRepository {

    private val apiKey = BuildConfig.GEMINI_API_KEY

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash-lite",
        apiKey = apiKey
    )
    suspend fun getRecipesFromGemini(pantryList: List<ProductEntity>): Result<List<Recipe>> {
        if (pantryList.size < 2) return Result.success(emptyList())

        return withContext(Dispatchers.IO) {
            try {
                if (apiKey.isBlank()) {
                    return@withContext Result.failure(Exception("API Key fehlt in local.properties!"))
                }

                val ingredientsString = pantryList.joinToString(", ") { it.name }

                val prompt = """
                    Ich habe folgende Produkte in meinem Vorratsschrank: $ingredientsString.
                    
                    Erstelle mir 3 Rezeptvorschläge, die als Hauptspeisen gesehen werden können und die ich damit 
                    (und mit Standard-Gewürzen) kochen kann.
                    Ignoriere Zutaten, die offensichtlich nicht zusammenpassen. Ignoriere auch Zutaten, die ganz offensichtlich
                    nicht in normalen Mahlzeiten verarbeitet werden sollten, wie z.B. Knabberkram wie Chips, klassische 
                    Süßigkeiten, etc.
                    
                    Antworte AUSSCHLIESSLICH mit einem JSON-Array. Kein weiterer Text davor oder danach. Gib bei den Zutaten auch die
                    benötigte Menge für zwei Portionen an. Gib bei der Beschreibung, sofern erforderlich, auch die Koch- und/oder Backzeit
                    mit Temperatur und Einstellung an.
                    Halte dich exakt an dieses Format für jedes Rezept:
                    [
                      {
                        "id": 0,
                        "name": "Name des Gerichts",
                        "ingredients": ["Zutat 1", "Zutat 2"],
                        "description": "Eine kurze Anleitung (max 50 Wörter, wenn erforderlich in Schritte unterteilt).",
                        "missingIngredients": ["Fehlende Zutat A"]
                      }
                    ]
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                val responseText = response.text ?: return@withContext Result.success(emptyList())

                val jsonString = responseText
                    .replace("```json", "")
                    .replace("```", "")
                    .trim()

                val gson = Gson()
                val listType = object : TypeToken<List<Recipe>>() {}.type
                val recipes: List<Recipe> = gson.fromJson(jsonString, listType)

                val numberedRecipes = recipes.mapIndexed { index, recipe ->
                    recipe.copy(id = index + 100)
                }

                Result.success(numberedRecipes)

            } catch (e: Exception) {
                Log.e("RecipeRepository", "Fehler: ${e.message}")
                //Fehlerausgabe
                Result.failure(e)
            }
        }
    }
}