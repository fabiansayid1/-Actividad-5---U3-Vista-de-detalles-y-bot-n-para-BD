package com.example.basededatos.model

import com.google.gson.annotations.SerializedName

data class BookResponse(
    @SerializedName("items") val items: List<BookItem>?
)

data class BookItem(
    @SerializedName("id") val id: String?,
    @SerializedName("volumeInfo") val volumeInfo: VolumeInfo?,
    @SerializedName("accessInfo") val accessInfo: BookAccessInfo? = null,
    @SerializedName("saleInfo") val saleInfo: BookSaleInfo? = null
)

data class VolumeInfo(
    @SerializedName("title") val title: String?,
    @SerializedName("subtitle") val subtitle: String?,
    @SerializedName("authors") val authors: List<String>?,
    @SerializedName("publisher") val publisher: String?,
    @SerializedName("publishedDate") val publishedDate: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("pageCount") val pageCount: Int?,
    @SerializedName("categories") val categories: List<String>?,
    @SerializedName("industryIdentifiers") val industryIdentifiers: List<BookIdentifier>?,
    @SerializedName("printType") val printType: String?,
    @SerializedName("maturityRating") val maturityRating: String?,
    @SerializedName("language") val language: String?,
    @SerializedName("averageRating") val averageRating: Double?,
    @SerializedName("ratingsCount") val ratingsCount: Int?,
    @SerializedName("imageLinks") val imageLinks: ImageLinks?,
    @SerializedName("previewLink") val previewLink: String?,
    @SerializedName("infoLink") val infoLink: String?,
    @SerializedName("canonicalVolumeLink") val canonicalVolumeLink: String?
)

data class BookIdentifier(
    @SerializedName("type") val type: String?,
    @SerializedName("identifier") val identifier: String?
)

data class ImageLinks(
    @SerializedName("thumbnail") val thumbnail: String?,
    @SerializedName("smallThumbnail") val smallThumbnail: String?,
    @SerializedName("small") val small: String?,
    @SerializedName("medium") val medium: String?,
    @SerializedName("large") val large: String?,
    @SerializedName("extraLarge") val extraLarge: String?
)

data class BookAccessInfo(
    @SerializedName("viewability") val viewability: String?,
    @SerializedName("accessViewStatus") val accessViewStatus: String?,
    @SerializedName("webReaderLink") val webReaderLink: String?,
    @SerializedName("publicDomain") val publicDomain: Boolean?,
    @SerializedName("pdf") val pdf: BookFileAccess?,
    @SerializedName("epub") val epub: BookFileAccess?
)

data class BookFileAccess(
    @SerializedName("isAvailable") val isAvailable: Boolean?
)

data class BookSaleInfo(
    @SerializedName("saleability") val saleability: String?,
    @SerializedName("isEbook") val isEbook: Boolean?
)
