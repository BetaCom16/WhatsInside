package com.app.whatsinside2

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

data class ProductResponse(
    val product : Product?,
    val status: Int,
    val status_verbose: String?
)

data class Product(
    val product_name: String?,
    val brands: String?,
    val image_url: String?,
    val nutriments: Nutriments?
)

data class Nutriments(
    val energy_100g: Double?,
    val sugars_100g: Double?
)

interface OpenFoodFactsService{
    @GET("api/v0/product/{barcode}.json")
    suspend fun getProduct(@Path("barcode") barcode: String): ProductResponse
}

object RetrofitInstance {
    private const val BASE_URL = "https://world.openfoodfacts.org/"

    val api: OpenFoodFactsService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenFoodFactsService::class.java)
    }
}