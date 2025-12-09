package com.app.whatsinside2

import android.widget.Toast
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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

    //Menge des jeweiligen Produkts - Standard: 1
    var quantity by remember { mutableStateOf(1) }

    //Aktueller Zeitpunkt in Millisenkunden
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }

    //Steuerung des Kalenders
    var showDatePicker by remember { mutableStateOf(false) }

    val dateString = remember(selectedDateMillis) {
        if(selectedDateMillis != null){
            val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)
            formatter.format(Date(selectedDateMillis!!))
        } else{
            "Kein Datum gewählt"
        }
    }

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

    //Der Kalender
    if(showDatePicker){
        val datePickerState = rememberDatePickerState()

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Abbrechen")
                }
            }
        ) {
            DatePicker(state = datePickerState)
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
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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

                    Text("Menge:", style = MaterialTheme.typography.titleMedium)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { if(quantity > 1) quantity-- }) {
                            Icon(Icons.Default.Remove, "Weniger")
                        }

                        Text(
                            text = quantity.toString(),
                            style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        IconButton(onClick = { quantity++ }) {
                            Icon(Icons.Default.Add, "Mehr")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Ablaufdatum:", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showDatePicker = true }
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = dateString)

                    }

                    Spacer(modifier = Modifier.height(32.dp))

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
                                calories = currentProduct.nutriments?.energy_100g,
                                quantity = quantity,
                                expirationDate = selectedDateMillis
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