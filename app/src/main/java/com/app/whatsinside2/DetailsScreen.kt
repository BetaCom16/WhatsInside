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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.collectAsState
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
    val db = WhatsInsideDatabase.getDatabase(context)

    // Holt alle Lagerorte aus der Datenbank
    val allLocations by db.locationDao().getAllLocations().collectAsState(initial = emptyList())

    var name by remember { mutableStateOf("Laden...") }
    var brand by remember { mutableStateOf<String?>(null) }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var calories by remember { mutableStateOf<Double?>(null) }
    var location by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showManualDialog by remember { mutableStateOf(false) }

    var quantity by remember { mutableIntStateOf(1) }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateString = remember(selectedDateMillis) {
        if(selectedDateMillis != null){
            val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)
            formatter.format(Date(selectedDateMillis!!))
        } else{
            "Kein Datum gewählt"
        }
    }

    LaunchedEffect(barcode, productId) {
        if(barcode == "manual_entry"){
            isLoading = false
            showManualDialog = true
            return@LaunchedEffect
        }

        try {
            if (productId != -1) {
                // Bearbeiten aus vorhandener DB
                val existingProduct = db.productDao().getProductById(productId)
                if (existingProduct != null) {
                    name = existingProduct.name
                    brand = existingProduct.brand
                    // HTTPS erzwingen für DB-Bilder
                    imageUrl = existingProduct.imageUrl?.replace("http://", "https://")
                    calories = existingProduct.calories
                    quantity = existingProduct.quantity
                    selectedDateMillis = existingProduct.expirationDate
                    location = existingProduct.location ?: ""
                } else {
                    errorMessage = "Produkt nicht mehr in Datenbank gefunden."
                }
            } else {
                // Neu von der API übernommen
                val response = RetrofitInstance.api.getProduct(barcode)
                if (response.status == 1 && response.product != null) {
                    name = response.product.product_name ?: "Unbekannt"
                    brand = response.product.brands
                    // HTTPS erzwingen für API-Bilder
                    imageUrl = response.product.image_url?.replace("http://", "https://")
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

    fun saveProduct(
        idToSave: Int,
        barcodeToSave: String,
        nameToSave: String,
        brandToSave: String?,
        imgToSave: String?,
        calToSave: Double?,
        qtyToSave: Int,
        locToSave: String?,
        dateToSave: Long?
    ) {
        scope.launch{
            if(!locToSave.isNullOrBlank()){
                val existingLoc = db.locationDao().getLocationByName(locToSave)
                if(existingLoc == null){
                    db.locationDao().insertLocation(LocationEntity(name = locToSave))
                }
            }

            val entity = ProductEntity(
                id = idToSave,
                barcode = barcodeToSave,
                name = nameToSave,
                brand = brandToSave,
                imageUrl = imgToSave,
                calories = calToSave,
                quantity = qtyToSave,
                location = locToSave,
                expirationDate = dateToSave
            )
            db.productDao().insertProduct(entity)

            Toast.makeText(context, "Gespeichert!", Toast.LENGTH_SHORT).show()
            navController.popBackStack(Screen.Home.route, inclusive = false)
        }
    }

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
            barcode = if(barcode == "manual_entry") "" else barcode,
            availableLocations = allLocations,
            onDismiss = {
                showManualDialog = false
                if(barcode == "manual_entry") navController.popBackStack()
            },
            onSave = { newName, newBrand, newPrice, newLocation, newDate ->
                scope.launch {
                    saveProduct(
                        idToSave = 0,
                        barcodeToSave = if(barcode == "manual_entry") "MANUAL_${System.currentTimeMillis()}" else barcode,
                        nameToSave = newName,
                        brandToSave = newBrand,
                        imgToSave = null,
                        calToSave = null,
                        qtyToSave = 1,
                        locToSave = newLocation,
                        dateToSave = newDate
                    )
                }
            }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (errorMessage != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Hoppla!", style = MaterialTheme.typography.headlineSmall)
                Text(text = errorMessage ?: "", textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { showManualDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Produkt anlegen")
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { navController.popBackStack() }) { Text(text = "Abbrechen") }
            }
        } else {
            if(barcode != "manual_entry"){
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Zeigt das Produktbild an
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                        if (imageUrl != null) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Produktbild",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Platzhalter, wenn kein Bild da ist
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Image, // Standard Icon
                                    contentDescription = "Kein Bild",
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Text("Kein Bild", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if(brand != null){
                        Text(
                            text = brand ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                    }

                    Text(text = name, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Menge:", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        IconButton(onClick = { if(quantity > 1) quantity-- }) { Icon(Icons.Default.Remove, "Weniger") }
                        Text(text = quantity.toString(), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(horizontal = 16.dp))
                        IconButton(onClick = { quantity++ }) { Icon(Icons.Default.Add, "Mehr") }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    LocationDropdown(
                        options = allLocations,
                        selectedLocation = location,
                        onLocationChange = { location = it }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

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

                    if (productId != -1) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = {
                                saveProduct(
                                    idToSave = 0,
                                    barcodeToSave = barcode,
                                    nameToSave = name,
                                    brandToSave = brand,
                                    imgToSave = imageUrl,
                                    calToSave = calories,
                                    qtyToSave = quantity,
                                    locToSave = location,
                                    dateToSave = selectedDateMillis
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentCopy, null)
                            Text("Duplizieren")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            saveProduct(
                                idToSave = if (productId != -1) productId else 0,
                                barcodeToSave = barcode,
                                nameToSave = name,
                                brandToSave = brand,
                                imgToSave = imageUrl,
                                calToSave = calories,
                                qtyToSave = quantity,
                                locToSave = location,
                                dateToSave = selectedDateMillis
                            )
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationDropdown(
    options: List<LocationEntity>,
    selectedLocation: String,
    onLocationChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(0.5f)
    ) {
        OutlinedTextField(
            value = selectedLocation,
            onValueChange = { onLocationChange(it) },
            label = { Text("Lagerort") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )

        if(options.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.name) },
                        onClick = {
                            onLocationChange(option.name)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualProductDialog(
    barcode: String,
    availableLocations: List<LocationEntity>,
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
                if (barcode.isNotEmpty()) {
                    Text(
                        text = "Barcode: $barcode",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Produktname *") },
                    isError = name.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Marke (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = priceText,
                    onValueChange = {
                        if(it.all { char -> char.isDigit() || char == '.' || char == ',' }) {
                            priceText = it
                        }
                    },
                    label = { Text("Preis € (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                LocationDropdown(
                    options = availableLocations,
                    selectedLocation = location,
                    onLocationChange = { location = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                    TextButton(onClick = onDismiss){ Text(text = "Abbrechen") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val price = priceText.replace(',', '.').toDoubleOrNull()
                            val brandToSave = if(brand.isBlank()) null else brand
                            val locToSave = if(location.isBlank()) null else location

                            onSave(name, brandToSave, price, locToSave, selectedDateMillis)
                        },
                        enabled = name.isNotBlank()
                    ) { Text(text = "Speichern") }
                }
            }
        }
    }
}