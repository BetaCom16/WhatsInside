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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    navController: NavController,
    barcode: String,
    productId: Int
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Speichern der Daten von der API oder DB direkt in Variablen
    var name by remember { mutableStateOf("Laden...") }
    var brand by remember { mutableStateOf<String?>(null) }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var calories by remember { mutableStateOf<Double?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showManualDialog by remember { mutableStateOf(false) }

    // Eingabefelder
    var quantity by remember { mutableIntStateOf(1) }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Datum Text Formatierung
    val dateString = remember(selectedDateMillis) {
        if(selectedDateMillis != null){
            val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)
            formatter.format(Date(selectedDateMillis!!))
        } else{
            "Kein Datum gewählt"
        }
    }

    LaunchedEffect(barcode, productId) {
        val db = WhatsInsideDatabase.getDatabase(context)

        try {
            if (productId != -1) {
                // Wenn Product-ID nicht -1 ist, wird das Produkt aus der DB geladen und bearbeitet
                val existingProduct = db.productDao().getProductById(productId)
                if (existingProduct != null) {
                    name = existingProduct.name
                    brand = existingProduct.brand
                    imageUrl = existingProduct.imageUrl
                    calories = existingProduct.calories
                    quantity = existingProduct.quantity
                    selectedDateMillis = existingProduct.expirationDate
                } else {
                    errorMessage = "Produkt nicht mehr in Datenbank gefunden."
                }
            } else {
                // Hat die Product-ID den Wert -1, existiert das Produkt nicht und wird neu erstellt
                val response = RetrofitInstance.api.getProduct(barcode)
                if (response.status == 1 && response.product != null) {
                    name = response.product.product_name ?: "Unbekannt"
                    brand = response.product.brands
                    imageUrl = response.product.image_url
                    calories = response.product.nutriments?.energy_100g
                } else {
                    errorMessage = "Produkt online nicht gefunden."
                }
            }
        } catch (e: Exception) {
            errorMessage = "Fehler: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    // Kalender
    if(showDatePicker){
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { selectedDateMillis = datePickerState.selectedDateMillis; showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Abbrechen") } }
        ) { DatePicker(state = datePickerState) }
    }

    if(showManualDialog){
        ManualProductDialog(
            barcode = barcode,
            onDismiss = { showManualDialog = false },
            onSave = { newName, newBrand, newPrice, newLocation, newDate ->
                scope.launch {
                    val db = WhatsInsideDatabase.getDatabase(context)
                    val entity = ProductEntity(
                        barcode = barcode,
                        name = newName,
                        brand = newBrand,
                        price = newPrice,
                        location = newLocation,
                        expirationDate = newDate,
                        quantity = 1,
                        imageUrl = null,
                        calories = null
                    )
                    db.productDao().insertProduct(entity)
                    Toast.makeText(context, "Produkt hinzugefügt!", Toast.LENGTH_SHORT).show()
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (errorMessage != null) {
            // Wenn das Produkt nicht gefunden wurde, erscheint folgender Dialog zum manuellen Anlegen
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Hoppla!",
                    style = MaterialTheme.typography.headlineSmall,
                )

                Text(
                    text = errorMessage ?: "",
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(onClick = { showManualDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Produkt manuell anlegen")
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = { navController.popBackStack() }) {
                    Text(
                        text = "Abbrechen")
                }
            }
        } else {
            // Wenn Produkt mittels Barcode gefunden, dann wird die normale Detailseite angezeigt
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.size(200.dp))
                Spacer(modifier = Modifier.height(16.dp))

                Text(text = name, style = MaterialTheme.typography.headlineMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))

                // Die Menge des Produkts
                Text("Menge:", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    IconButton(onClick = { if(quantity > 1) quantity-- }) { Icon(Icons.Default.Remove, "Weniger") }
                    Text(text = quantity.toString(), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(horizontal = 16.dp))
                    IconButton(onClick = { quantity++ }) { Icon(Icons.Default.Add, "Mehr") }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Datum
                Text("Ablaufdatum:", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = dateString)
                }

                Spacer(modifier = Modifier.height(32.dp))
                if (calories != null) Text(text = "Energie/100g: $calories kcal")
                Spacer(modifier = Modifier.height(32.dp))


                // Duplizieren-Button - nur sichtbar wenn ein Produkt bearbeitet wird
                if (productId != -1) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val db = WhatsInsideDatabase.getDatabase(context)

                                val entity = ProductEntity(
                                    id = 0, // 0 bedeutet "neuen Eintrag anlegen"
                                    barcode = barcode,
                                    name = name,
                                    brand = brand,
                                    imageUrl = imageUrl,
                                    calories = calories,
                                    quantity = quantity, // Die aktuell eingestellte Menge
                                    expirationDate = selectedDateMillis // Das aktuell eingestellte Datum
                                )
                                db.productDao().insertProduct(entity)

                                Toast.makeText(context, "Kopie gespeichert!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack(Screen.Home.route, inclusive = false)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.ContentCopy, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Produkt duplizieren")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Button zum Speichern der Änderungen
                Button(
                    onClick = {
                        scope.launch {
                            val db = WhatsInsideDatabase.getDatabase(context)

                            val entity = ProductEntity(
                                // Wenn productId != -1, wird diese ID verwendet
                                // Wenn productId == -1, wird 0 genommen
                                id = if (productId != -1) productId else 0,
                                barcode = barcode,
                                name = name,
                                imageUrl = imageUrl,
                                calories = calories,
                                quantity = quantity,
                                expirationDate = selectedDateMillis
                            )

                            db.productDao().insertProduct(entity)

                            Toast.makeText(context, "Gespeichert!", Toast.LENGTH_SHORT).show()
                            navController.popBackStack(Screen.Home.route, inclusive = false)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (productId != -1) "Änderungen speichern" else "In den Vorratsschrank legen")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualProductDialog(
    barcode: String,
    onDismiss: () -> Unit,
    onSave: (name: String, brand: String?, price: Double?, location: String?, date: Long?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateString = remember(selectedDateMillis) {
        if (selectedDateMillis != null) {
            val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)
            formatter.format(Date(selectedDateMillis!!))
        } else {
            "Kein Datum"
        }
    }

    if(showDatePicker){
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { selectedDateMillis = datePickerState.selectedDateMillis; showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Abbrechen") } }
        ) { DatePicker(state = datePickerState) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Produkt manuell anlegen",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Barcode: $barcode",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Eingabefeld Name (Pflichtfeld)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Produktname *") },
                    isError = name.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Eingabefeld Marke (Optional)
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Marke (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Eingabefeld Preis (Optional)
                OutlinedTextField(
                    value = priceText,
                    onValueChange = {
                        // NUr Zahlen und Punkte/Kommas zulassen
                        if(it.all { char -> char.isDigit() || char == '.' || char == ',' }) {
                            priceText = it
                        }
                    },
                    label = { Text("Preis € (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Eingabefeld Lagerort (Optional)
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Lagerort (Optional)") },
                    placeholder = { Text("z.B. Küche - Hängeschrank rechts") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                //Eingabefeld MHD (Optional)
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DateRange, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(dateString)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss){
                        Text(
                            text = "Abbrechen"
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val price = priceText.replace(',', '.').toDoubleOrNull()
                            val brandToSave = if(brand.isBlank()) null else brand
                            val locToSave = if(location.isBlank()) null else location

                            onSave(name, brandToSave, price, locToSave, selectedDateMillis)
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Text(
                            text = "Speichern"
                        )
                    }
                }
            }
        }
    }
}
