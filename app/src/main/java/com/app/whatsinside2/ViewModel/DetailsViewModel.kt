package com.app.whatsinside2

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.whatsinside2.LocationEntity
import com.app.whatsinside2.ProductEntity
import com.app.whatsinside2.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = WhatsInsideDatabase.getDatabase(application)
    private val locationDao = db.locationDao()
    private val productDao = db.productDao()

    // Hier werden StateFlows genutzt, damit die UI Änderungen automatisch erkennt

    // Daten des Produkts
    val name = MutableStateFlow("Laden...")
    val brand = MutableStateFlow<String?>(null)
    val imageUrl = MutableStateFlow<String?>(null)
    val calories = MutableStateFlow<Double?>(null)

    // Eingabefelder
    val quantity = MutableStateFlow(1)
    val location = MutableStateFlow("")
    val expirationDate = MutableStateFlow<Long?>(null)

    // UI-Status
    val isLoading = MutableStateFlow(true)
    val errorMessage = MutableStateFlow<String?>(null)
    val showManualDialog = MutableStateFlow(false)

    // Liste der Orte für das Dropdown
    val allLocations = locationDao.getAllLocations()

    // Daten laden
    fun loadData(barcode: String, productId: Int) {
        viewModelScope.launch {
            // Folgender Codeblock ist dafür da, wenn ein Produkt manuell eingetragen wird
            if (barcode == "manual_entry") {
                isLoading.value = false
                showManualDialog.value = true
                return@launch
            }

            try {
                if (productId != -1) {
                    // Produkt existiert bereits
                    val existingProduct = productDao.getProductById(productId)
                    if (existingProduct != null) {
                        name.value = existingProduct.name
                        brand.value = existingProduct.brand
                        imageUrl.value = existingProduct.imageUrl?.replace("http://", "https://")
                        calories.value = existingProduct.calories
                        quantity.value = existingProduct.quantity
                        expirationDate.value = existingProduct.expirationDate
                        location.value = existingProduct.location ?: ""
                    } else {
                        errorMessage.value = "Produkt nicht in Datenbank gefunden."
                    }
                } else {
                    // Produkt wird aus der OpenFoodFacts Datenbank via API gezogen
                    val response = RetrofitInstance.api.getProduct(barcode)
                    if (response.status == 1 && response.product != null) {
                        name.value = response.product.product_name ?: "Unbekannt"
                        brand.value = response.product.brands
                        imageUrl.value = response.product.image_url?.replace("http://", "https://")
                        calories.value = response.product.nutriments?.energy_100g
                    } else {
                        errorMessage.value = "Produkt online nicht gefunden."
                    }
                }
            } catch (e: Exception) {
                errorMessage.value = "Fehler: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    // Folgender Block dient der Speicherung des Produkts
    fun saveProduct(
        idToSave: Int,
        barcodeToSave: String,
        overrideName: String? = null,
        overrideBrand: String? = null,
        overridePrice: Double? = null,
        overrideLocation: String? = null,
        overrideDate: Long? = null,
        onSuccess: () -> Unit // Callback, wenn fertig
    ) {
        viewModelScope.launch {
            // Entweder werden die übergebenen (aus dem Dialog) Daten oder die aktuellen (vom Screen) übernommen
            val finalName = overrideName ?: name.value
            val finalBrand = overrideBrand ?: brand.value
            val finalLocation = overrideLocation ?: location.value
            val finalDate = overrideDate ?: expirationDate.value

            // Wenn der Lagerort noch nicht existiert, wird dieser neu angelegt
            if (finalLocation.isNotBlank()) {
                val existingLoc = locationDao.getLocationByName(finalLocation)
                if (existingLoc == null) {
                    locationDao.insertLocation(LocationEntity(name = finalLocation))
                }
            }

            // Produkt endgültig speichern
            val entity = ProductEntity(
                id = idToSave,
                barcode = barcodeToSave,
                name = finalName,
                brand = finalBrand,
                imageUrl = imageUrl.value,
                calories = calories.value,
                quantity = quantity.value,
                location = finalLocation,
                expirationDate = finalDate,
                price = overridePrice
            )
            productDao.insertProduct(entity)

            // Feedback an den User via Toast-Nachricht
            Toast.makeText(getApplication(), "Gespeichert!", Toast.LENGTH_SHORT).show()
            onSuccess()
        }
    }

    // Einfache SetterMethoden für die UI, um Werte zu ändern
    fun updateQuantity(delta: Int) {
        val newValue = quantity.value + delta
        if (newValue >= 1) quantity.value = newValue
    }

    fun setQuantity(value: Int) {
        quantity.value = value
    }

    fun updateLocation(newLocation: String) {
        location.value = newLocation
    }

    fun updateDate(newDate: Long?) {
        expirationDate.value = newDate
    }
}