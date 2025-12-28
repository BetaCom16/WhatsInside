package com.app.whatsinside2

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun StatisticsScreen(
    navController: NavController,
    viewModel: StatisticsViewModel = viewModel()
) {
    val stats by viewModel.stats.collectAsState()
    val recipes by viewModel.suggestedRecipes.collectAsState()
    val isRecipeLoading by viewModel.isLoadingRecipes.collectAsState()

    val errorMessage by viewModel.recipeErrorMsg.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Übersicht",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (stats.totalItems > 0) {
            PantryPieChart(stats = stats, modifier = Modifier.size(200.dp))
        } else {
            Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                Text("Keine Daten", color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            LegendItem(color = Color(0xFF4CAF50), text = "Gut (${stats.goodItems})")
            LegendItem(color = Color(0xFFFFC107), text = "Bald fällig (${stats.expiringSoonItems})")
            LegendItem(color = Color(0xFFF44336), text = "Abgelaufen (${stats.expiredItems})")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Rezeptvorschläge",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        IconButton(onClick = { viewModel.generateRecipes() }) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Neue Rezeptvorschläge"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isRecipeLoading) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Der Koch überlegt...", style = MaterialTheme.typography.bodySmall)
            }
        } else if (errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WifiOff, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Fehler beim Laden",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = errorMessage ?: "Unbekannter Fehler",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Button(onClick = { viewModel.generateRecipes() }) {
                            Text(
                                text = "Erneut versuchen"
                            )
                        }
                    }
                }
            }
        } else if (recipes.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = "Lass dir anhand deines Vorratsschranks Rezepte vorschlagen",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { viewModel.generateRecipes() }) {
                    Text(
                        text = "Rezepte generieren"
                    )
                }
            }
        } else {
            recipes.forEach { recipe ->
                RecipeItem(recipe = recipe)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun RecipeItem(recipe: Recipe) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = recipe.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Zutaten: ${recipe.ingredients.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = recipe.description,
                style = MaterialTheme.typography.bodyMedium
            )

            if (recipe.missingIngredients.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Fehlt evtl.: ${recipe.missingIngredients.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun PantryPieChart(
    stats: PantryStats,
    modifier: Modifier = Modifier,
    thickness: Dp = 30.dp
){
    val colorGood = Color(0xFF4CAF50)
    val colorSoon = Color(0xFFFFC107)
    val colorExpired = Color(0xFFF44336)
    val colorEmpty = Color.LightGray

    Canvas(modifier = modifier){
        val total = stats.totalItems.toFloat()

        if(total == 0f){
            drawArc(color = colorEmpty, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = thickness.toPx()))
            return@Canvas
        }

        val angleGood = (stats.goodItems / total) * 360f
        val angleSoon = (stats.expiringSoonItems / total) * 360f
        val angleExpired = (stats.expiredItems / total) * 360f

        var currentAngle = -90f

        if(angleGood > 0){
            drawArc(color = colorGood, startAngle = currentAngle, sweepAngle = angleGood, useCenter = false, style = Stroke(width = thickness.toPx(), cap = StrokeCap.Butt))
            currentAngle += angleGood
        }
        if(angleSoon > 0){
            drawArc(color = colorSoon, startAngle = currentAngle, sweepAngle = angleSoon, useCenter = false, style = Stroke(width = thickness.toPx(), cap = StrokeCap.Butt))
            currentAngle += angleSoon
        }
        if(angleExpired > 0){
            drawArc(color = colorExpired, startAngle = currentAngle, sweepAngle = angleExpired, useCenter = false, style = Stroke(width = thickness.toPx(), cap = StrokeCap.Butt))
        }
    }
}

@Composable
fun LegendItem(color: Color, text: String){
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).background(color, shape = CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall)
    }
}