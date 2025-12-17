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
    // Live-Statistiken
    val stats by viewModel.stats.collectAsState()

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

        // Hier folgt der Code für das Diagramm zur Anzeige der Statistik
        if(stats.totalItems > 0){
            PantryPieChart(
                stats = stats,
                modifier = Modifier.size(200.dp)
            )
        } else{
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Kein Daten",
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Die Legende des Diagramms
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendItem(
                color = Color(0xFF4CAF50),
                text = "In Ordnung (${stats.goodItems})" // Grüne Farbe für noch haltbare Lebensmittel
            )
            LegendItem(
                color = Color(0xFFFFC107),
                text = "Läuft bald ab (${stats.expiringSoonItems})" // Gelbe Farbe für bald ablaufende Lebensmittel
            )
            LegendItem(
                color = Color(0xFFF44336),
                text = "Abgelaufen (${stats.expiredItems})" // Rote Farbe für abgelaufene Lebensmittel
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Gesamt: ${stats.totalItems} Produkte",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(48.dp))

    }
}

// Hilfsfunktionen für das Ring-Diagramm
@Composable
fun PantryPieChart(
    stats: PantryStats,
    modifier: Modifier = Modifier,
    thickness: Dp = 30.dp
){
    // Hier werden die Farben definiert
    val colorGood = Color(0xFF4CAF50) // Grün
    val colorSoon = Color(0xFFFFC107) // Geld
    val colorExpired = Color(0xFFF44336) // Rot
    val colorEmpty = Color.LightGray

    Canvas(modifier = modifier){
        val total = stats.totalItems.toFloat()

        if(total == 0f){
            drawArc(
                color = colorEmpty,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = thickness.toPx())
            )
            return@Canvas
        }

        val angleGood = (stats.goodItems / total) * 360f
        val angleSoon = (stats.expiringSoonItems / total) * 360f
        val angleExpired = (stats.expiredItems / total) * 360f

        // Beginn der Aufteilung mittig oben
        var currentAngle = -90f

        // Tortenstück für noch gute Lebensmittel
        if(angleGood > 0){
            drawArc(
                color = colorGood,
                startAngle = currentAngle,
                sweepAngle = angleGood,
                useCenter = false,
                style = Stroke(width = thickness.toPx(), cap = StrokeCap.Butt)
            )
            currentAngle += angleGood
        }

        // Tortenstück für bald ablaufende Lebensmittel
        if(angleSoon > 0){
            drawArc(
                color = colorSoon,
                startAngle = currentAngle,
                sweepAngle = angleSoon,
                useCenter = false,
                style = Stroke(width = thickness.toPx(), cap = StrokeCap.Butt)
            )
            currentAngle += angleSoon
        }

        // Tortenstück für abgelaufene Lebensmittels
        if(angleExpired > 0){
            drawArc(
                color = colorExpired,
                startAngle = currentAngle,
                sweepAngle = angleExpired,
                useCenter = false,
                style = Stroke(width = thickness.toPx(), cap = StrokeCap.Butt)
            )
        }
    }
}

@Composable
fun LegendItem(color: Color, text: String){
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, shape = CircleShape)
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall
        )
    }
}