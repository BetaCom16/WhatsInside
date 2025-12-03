package com.app.whatsinside2

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun DetailsScreen(
    navController: NavController,
    barcode: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var product by remember { mutableStateOf<Product?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(barcode) {
        try {
            val response = RetrofitInstance.api.getProduct(barcode)

            if (response.status == 1 && response.product != null) {
                product = response.product
            } else {
                errorMessage = "Produkt nicht gefunden"
            }
        } catch (e: Exception) {
            errorMessage = "Fehler beim Laden: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (errorMessage != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Fehler!", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error)
                Text(errorMessage ?: "")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { navController.popBackStack() }) {
                    Text("Zurück")
                }
            }
        } else {
            val currentProduct = product
            if (currentProduct != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    AsyncImage(
                        model = currentProduct.image_url,
                        contentDescription = "Produktbild",
                        modifier = Modifier.size(200.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = currentProduct.product_name ?: "Unbekanntes Produkt",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Text(
                        text = currentProduct.brands ?: "Marke unbekannt",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val kcal = currentProduct.nutriments?.energy_100g ?: 0.0
                    Text(text = "Energie/100g: $kcal kcal")

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(onClick = {
                        scope.launch {
                            val db = WhatsInsideDatabase.getDatabase(context)

                            val entity = ProductEntity(
                                barcode = barcode,
                                name = currentProduct.product_name ?: "Unbekannt",
                                imageUrl = currentProduct.image_url,
                                calories = currentProduct.nutriments?.energy_100g
                            )

                            db.productDao().insertProduct(entity)

                            Toast.makeText(context, "Gespeichert!", Toast.LENGTH_SHORT).show()

                            navController.popBackStack(Screen.Home.route, inclusive = false)
                        }
                    }) {
                        Text("In den Vorratsschrank legen" )
                    }
                }
            }
        }
    }
}