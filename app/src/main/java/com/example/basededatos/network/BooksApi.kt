package com.example.basededatos.network

import com.example.basededatos.model.BookResponse
import com.example.basededatos.model.BookItem
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface BooksApi {
    @GET("volumes")
    suspend fun getBooks(
        @Query("q") query: String,
        @Query("maxResults") maxResults: Int = 10,
        @Query("key") apiKey: String? = null
    ): BookResponse

    @GET("volumes/{volumeId}")
    suspend fun getBookDetails(
        @Path("volumeId") volumeId: String,
        @Query("key") apiKey: String? = null,
        @Query("projection") projection: String = "full"
    ): BookItem
}
