package com.app.whatsinside2

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.whatsinside2.ProductEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class ProductDaoTest {

    private lateinit var db: WhatsInsideDatabase
    private lateinit var productDao: ProductDao

    // Vorbereitungen für den eigentlichen Test
    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Erstellt wird eine In-Memory-Datenbank, die nur im Arbeitsspeicher existiert
        // Nach dem Herunterfahren des Emulators verschwindet die Datenbank wieder
        db = Room.inMemoryDatabaseBuilder(context, WhatsInsideDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        productDao = db.productDao()
    }

    // Aktionen nachdem der Test ausgeführt wurde, quasi Aufräumarbeiten
    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    // Folgender Test fügt ein Produkt zur Datenbank hinzu und prüft damit die Funktionalität der DB
    // Folgede erste Zeilen definieren das Produkt
    @Test
    @Throws(Exception::class)
    fun writeProductAndReadInList() = runBlocking {
        val product = ProductEntity(
            barcode = "12345",
            name = "Test Nudeln",
            imageUrl = null,
            calories = 350.0,
            quantity = 1,
            expirationDate = null
        )

        // Schreibt das Produkt in die DB
        productDao.insertProduct(product)

        // Liest aus der DB aus
        val allProducts = productDao.getAllProducts().first()

        // Prüft, ob genau ein Produkt in der DB ist, in dem Fall ist es nur das neu erstellte Produkt
        assertEquals(1, allProducts.size)

        // Prüft, ob der Name des Testprodukts wirklich "Test Nudeln" lautet
        assertEquals("Test Nudeln", allProducts[0].name)
    }


    @Test
    @Throws(Exception::class)
    fun deleteProduct() = runBlocking {
        // Erstellen des Test-Produkts
        val product = ProductEntity(
            barcode = "999",
            name = "Weg damit",
            imageUrl = null,
            calories = null,
            quantity = 1,
            expirationDate = null
        )
        productDao.insertProduct(product)

        // Das gespeicherte Produkt aus der DB holen
        val listBefore = productDao.getAllProducts().first()
        val savedProduct = listBefore[0]

        // Das Produkt löschen
        productDao.deleteProduct(savedProduct)

        // Prüft, ob das Produkt wirklich gelöscht wurde
        val listAfter = productDao.getAllProducts().first()
        assertEquals(0, listAfter.size)
    }

    @Test
    @Throws(Exception::class)
    fun updateProduct() = runBlocking {
        // Test-Produkt anlegen
        val original = ProductEntity(
            barcode = "111",
            name = "Altes Brot",
            imageUrl = null,
            calories = null,
            quantity = 1,
            expirationDate = null
        )
        productDao.insertProduct(original)

        // Produkt aus der DB holen
        val savedProduct = productDao.getAllProducts().first()[0]

        // Daten des Produkts veröndern, um die Funktionalität der Datenänderung zu prüfen
        val update = savedProduct.copy(name = "Frisches Brot", quantity = 5)
        productDao.insertProduct(update)

        // Holt das Produkt aus der DB
        val listAfter = productDao.getAllProducts().first()

        // Prüft, ob nur ein Produkt da ist und kein Duplikat
        assertEquals(1, listAfter.size)
        // Prüft, dass der Name auch den neuen Namen hat
        assertEquals("Frisches Brot", listAfter[0].name)
        assertEquals(5, listAfter[0].quantity)
    }

    @Test
    @Throws(Exception::class)
    fun getProductById() = runBlocking {
        // Produkt speichern
        val product = ProductEntity(
            barcode = "555",
            name = "Gesuchtes Produkt",
            imageUrl = null,
            calories = null,
            quantity = 1,
            expirationDate = null
        )
        productDao.insertProduct(product)

        // Produkt aus der DB holen
        val savedProduct = productDao.getAllProducts().first()[0]
        val id = savedProduct.id

        // Nach der ID suchen
        val foundProduct = productDao.getProductById(id)

        // Prüfen
        assertNotNull(foundProduct)
        assertEquals("Gesuchtes Produkt", foundProduct?.name)
    }


    /*
    Folgender Test versucht 3 unterschiedliche Produkte in die Datenbank zu schreiben.
    Dies stellt sicher, dass ein "gefüllter Vorratsschrank" existieren kann. Dies ist relevant
    für die Statistik der Produkte
     */
    @Test
    @Throws(Exception::class)
    fun verifyDataIntegrityForStatistics() = runBlocking {
        // Die Produkte definieren
        val product1 = ProductEntity(barcode = "A", name = "Milch", imageUrl = null, calories = null, quantity = 1, expirationDate = 10000L)
        val product2 = ProductEntity(barcode = "B", name = "Eier", imageUrl = null, calories = null, quantity = 1, expirationDate = 20000L)
        val product3 = ProductEntity(barcode = "C", name = "Mehl", imageUrl = null, calories = null, quantity = 1, expirationDate = null)

        // Die Produkte in die DB schreiben
        productDao.insertProduct(product1)
        productDao.insertProduct(product2)
        productDao.insertProduct(product3)

        // Die Produkte aus der DB holen
        val list = productDao.getAllProducts().first()

        // Prüfung, ob alle Produkte vorhanden sind. Wäre dies nicht der Fall, würde
        // die Statistik falsche Daten anzeigen
        assertEquals(3, list.size)
    }
}