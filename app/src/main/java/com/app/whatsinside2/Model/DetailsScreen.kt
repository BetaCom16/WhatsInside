package com.app.whatsinside2.Model

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.app.whatsinside2.DetailsViewModel
import com.app.whatsinside2.LocationEntity
import com.app.whatsinside2.Screen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    navController: NavController,
    barcode: String,
    productId: Int,
    // ViewModel Injection
    viewModel: DetailsViewModel = viewModel()
) {
    // Sobald sich im ViewModel etwas ändert, wird die UI automatisch neu gebaut
    val name by viewModel.name.collectAsState()
    val brand by viewModel.brand.collectAsState()
    val imageUrl by viewModel.imageUrl.collectAsState()
    val calories by viewModel.calories.collectAsState()

    val quantity by viewModel.quantity.collectAsState()
    val location by viewModel.location.collectAsState()
    val expirationDate by viewModel.expirationDate.collectAsState()

    // UI-Status
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val showManualDialog by viewModel.showManualDialog.collectAsState()

    val allLocations by viewModel.allLocations.collectAsState(initial = emptyList())

    // Lokaler State nur für UI-Elemente
    var showDatePicker by remember { mutableStateOf(false) }

    // Hilfs-Variable für den Datumstext inklusive Umwandlung des Datums
    val dateString = remember(expirationDate) {
        if(expirationDate != null){
            val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)
            formatter.format(Date(expirationDate!!))
        } else{
            "Kein Datum gewählt"
        }
    }

    // Lädt alle Daten beim Start
    LaunchedEffect(barcode, productId) {
        viewModel.loadData(barcode, productId)
    }

    if(showDatePicker){
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = expirationDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateDate(datePickerState.selectedDateMillis)
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Abbrechen") } }
        ) { DatePicker(state = datePickerState) }
    }

    if(showManualDialog){
        ManualProductDialog(
            barcode = if(barcode == "manual_entry") "" else barcode,
            availableLocations = allLocations,
            onDismiss = {
                viewModel.showManualDialog.value = false
                if(barcode == "manual_entry") navController.popBackStack()
            },
            onSave = { newName, newBrand, newPrice, newLocation, newDate ->
                // Hier wird die Funktion zum Speichern des Produkts aus dem DetailViewModel geholt
                viewModel.saveProduct(
                    idToSave = 0,
                    barcodeToSave = if(barcode == "manual_entry") "MANUAL_${System.currentTimeMillis()}" else barcode,
                    overrideName = newName,
                    overrideBrand = newBrand,
                    overridePrice = newPrice,
                    overrideLocation = newLocation,
                    overrideDate = newDate,
                    onSuccess = { navController.popBackStack(Screen.Home.route, inclusive = false) }
                )
            }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        /**
         * Folgender if-Block bis zum else nach dem else if dient der Anzeige von Informationen,
         * sobald ein Scan nicht erfolgreich war, z.B. weil das Produkt nicht über die API
         * in der OpenFoodFacts Datenbank gefunden wurde oder wegen eines Timeouts. Der Nutzer
         * bekommt die Möglichkeit, das Produkt einfach manuell einzutragen oder den Vorgang
         * abzubrechen
        */
        if (isLoading) {
            CircularProgressIndicator()
        } else if (errorMessage != null) {
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
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    text = errorMessage ?: "",
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(onClick = {
                    viewModel.showManualDialog.value = true
                }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Produkt anlegen"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = {
                    navController.popBackStack()
                }) {
                    Text(
                        text = "Abbrechen"
                    )
                }
            }
        } else {
            /**
             * Dieser if-Block beinhaltet die Darstellung einer normalen Detailseite, sofern ein Scan
             * des Barcodes und die daraufhin stattfindende Abfrage über die API an OpenFoodFacts
             * erfolgreich war. Der Nutzer kann nun die Menge, das MHD und den Lagerort angeben und
             * anschließend das Produkt speichern
             */
            if(barcode != "manual_entry"){
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Bild
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                        if (imageUrl != null) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Image,
                                    null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Text(
                                    text = "Kein Bild",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Zeigt die Marke an, sofern diese in der DB vorhanden ist
                    if (brand != null) {
                        Text(
                            text = brand ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Zeigt den Namen des Produkts an
                    Text(
                        text = name,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Menge des Produkts angeben
                    Text(
                        text = "Menge:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.updateQuantity(-1)
                            }) {
                            Icon(
                                Icons.Default.Remove,
                                "Weniger"
                            ) }
                        Text(
                            text = quantity.toString(),
                            style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier.padding(horizontal = 16.dp
                            )
                        )
                        IconButton(
                            onClick = {
                                viewModel.updateQuantity(1)
                            }) {
                            Icon(Icons.Default.Add,
                                "Mehr"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Lagerort des Produkts aus dem Dropdown oder neu vergeben
                    LocationDropdown(
                        options = allLocations,
                        selectedLocation = location,
                        onLocationChange = { viewModel.updateLocation(it) }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // MHD des Produkts festlegen
                    Text(
                        text = "Ablaufdatum:",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            showDatePicker = true
                        }) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = dateString
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Zeigt die Kalorien des Produkts auf 100g an, falls dieser Wert vorhanden ist
                    if (calories != null){
                        Text(
                            text = "Energie/100g: $calories kcal"
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    /**
                     * Folgender Codeblock ermöglicht dem Nutzer das Duplizieren eines bereits
                     * vorhandenen Produkts mithilfe eines Buttons. Anwendungsfall hierbei wäre,
                     * wenn das Produkt mehrfach vorhanden ist, jedoch mit unterschiedlichen
                     * Mindesthaltbarkeitsdaten
                     */
                    if (productId != -1) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = {
                                viewModel.saveProduct(
                                    idToSave = 0,
                                    barcodeToSave = barcode,
                                    onSuccess = { navController.popBackStack(Screen.Home.route, inclusive = false) }
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentCopy, null)
                            Text("Duplizieren")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Speichern des Produkts
                    Button(
                        onClick = {
                            viewModel.saveProduct(
                                idToSave = if (productId != -1) productId else 0,
                                barcodeToSave = barcode,
                                onSuccess = { navController.popBackStack(Screen.Home.route, inclusive = false) }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Save,
                            null
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Produkt bereits in der DB vorhanden - dann können Änderungen daran gespeichert werden
                        // Produkt zum ersten Mal hinzugefügt, dann nur die Möglichkeit das Produkt anzulegen
                        Text(
                            text = if (productId != -1) {
                                "Änderungen speichern"
                            } else {
                                "In den Vorratsschrank legen"
                            }
                        )
                    }
                }
            }
        }
    }
}

// Folgender Code beinhaltet Hilfsfunktionen für UI-Elemente
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
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedLocation,
            onValueChange = { onLocationChange(it) },
            label = {
                Text(
                    text = "Lagerort"
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
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
                        text = {
                            Text(
                                text = option.name
                            )
                        },
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


/**
 * Folgender Codebereich beinhaltet die UI-Elemente für den Dialog, um manuell Produkte anlegen
 * zu können
 */
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
                TextButton(onClick = {
                    selectedDateMillis = datePickerState.selectedDateMillis; showDatePicker = false
                }) {
                    Text(
                        text = "OK"
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDatePicker = false
                }) {
                    Text(
                        text = "Abbrechen")
                }
            }
        ) { DatePicker(
            state = datePickerState
        ) }
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

                // Eingabefeld für den Produktnamen
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = {
                        Text("Produktname *"
                        ) },
                    isError = name.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Eingabefeld für die Marke
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = {
                        Text("Marke (Optional)"
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Das Eingabefeld für den Preis des Produkts. Ändert die Tastatur auf Zahlen
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { if(it.all { char -> char.isDigit() || char == '.' || char == ',' }) priceText = it },
                    label = {
                        Text(text = "Preis € (Optional)"
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Dropdown-Menü für die Angabe des Lagerortes
                LocationDropdown(
                    options = availableLocations,
                    selectedLocation = location,
                    onLocationChange = { location = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Das Eingabefeld für das MHD des Produkts
                OutlinedButton(onClick = {
                    showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        null
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = dateString
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Zwei Buttons. Einer zum Abbrechen, der andere zum Speichern
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick =
                        onDismiss
                    ){
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