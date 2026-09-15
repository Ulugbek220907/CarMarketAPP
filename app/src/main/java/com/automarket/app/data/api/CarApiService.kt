package com.automarket.app.data.api

import com.automarket.app.data.model.Car
import com.automarket.app.data.model.ChatMessage
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CarApiService {

    @GET("health")
    suspend fun checkHealth(): Response<Map<String, Any>>

    @GET("api/cars")
    suspend fun getCars(
        @Query("search") search: String? = null,
        @Query("category") category: String? = null,
        @Query("sort") sort: String? = null,
        @Query("favorites") favorites: Boolean? = null,
        @Query("userListings") userListings: Boolean? = null
    ): Response<List<Car>>

    @GET("api/cars/{id}")
    suspend fun getCarById(@Path("id") id: Long): Response<Car>

    @POST("api/cars")
    suspend fun createCar(@Body car: Car): Response<Car>

    @POST("api/cars/{id}/favorite")
    suspend fun toggleFavorite(@Path("id") id: Long): Response<Map<String, Any>>

    @DELETE("api/cars/{id}")
    suspend fun deleteCar(@Path("id") id: Long): Response<Map<String, Any>>

    @GET("api/cars/{carId}/messages")
    suspend fun getMessages(@Path("carId") carId: Long): Response<List<ChatMessage>>

    @POST("api/cars/{carId}/messages")
    suspend fun sendMessage(
        @Path("carId") carId: Long,
        @Body message: ChatMessage
    ): Response<ChatMessage>

    @POST("api/cars/{carId}/messages/offers")
    suspend fun createOffer(
        @Path("carId") carId: Long,
        @Body offerData: Map<String, Any>
    ): Response<ChatMessage>

    @POST("api/seed")
    suspend fun reseedDatabase(): Response<Map<String, Any>>
}
