package com.app.whatsinside2.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.whatsinside2.ProductEntity
import com.app.whatsinside2.WhatsInsideDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
* Diese Datei enthält die Logik für die Datei HomeScreen.kt
*
*/
class HomeViewModel(application: Application) : AndroidViewModel(application) {

   // Verbindung zur Datenbank herstellen
   private val db = WhatsInsideDatabase.Companion.getDatabase(application)
   private val productDao = db.productDao()

   // Das UI aus HomeScreen.kt beobachtet die folgende Liste
   val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()

   // Die folgende Funktion wird aufgerufen, sobald auf dem UI auf den Mülleimer geklickt wird
   fun deleteProduct(product: ProductEntity) {
       // viewModelScope sorgt dafür, dass diese Funktion automatisch
       // beendet wird, wenn das ViewModel nicht mehr gebraucht wird
       viewModelScope.launch {
           productDao.deleteProduct(product)
       }
   }
}