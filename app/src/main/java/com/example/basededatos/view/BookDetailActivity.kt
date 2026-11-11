package com.example.basededatos.view

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.basededatos.R
import com.example.basededatos.databinding.ActivityBookDetailBinding
import com.example.basededatos.model.BookItem
import com.example.basededatos.viewmodel.AppViewModelFactory
import com.example.basededatos.viewmodel.BookDetailViewModel
import com.example.basededatos.viewmodel.FavoriteBooksViewModel
import com.google.android.material.snackbar.Snackbar
import java.util.Locale

class BookDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookDetailBinding
    private lateinit var viewModel: BookDetailViewModel
    private lateinit var favoriteBooksViewModel: FavoriteBooksViewModel
    private var email: String = ""
    private var currentBook: BookItem? = null
    private var favoriteIds: Set<String> = emptySet()
    private var readerUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBackDetail.setOnClickListener { finish() }
        binding.btnOpenBookReader.setOnClickListener { openReader() }
        binding.btnFavoriteDetail.setOnClickListener {
            currentBook?.let { favoriteBooksViewModel.toggleFavorite(email, it) }
        }
        email = intent.getStringExtra(ProfileExtras.EMAIL).orEmpty()

        val factory = AppViewModelFactory(application)
        viewModel = ViewModelProvider(this, factory)[BookDetailViewModel::class.java]
        favoriteBooksViewModel = ViewModelProvider(this, factory)[FavoriteBooksViewModel::class.java]

        viewModel.state.observe(this) { state ->
            binding.progressBookDetail.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            binding.contentBookDetail.visibility = if (state.book != null) View.VISIBLE else View.GONE
            binding.tvBookDetailError.visibility =
                if (!state.isLoading && state.errorMessage != null) View.VISIBLE else View.GONE
            state.errorMessage?.let { binding.tvBookDetailError.text = it }
            state.book?.let { book ->
                currentBook = book
                renderBook(book)
                renderFavoriteButton()
            }
        }
        favoriteBooksViewModel.state.observe(this) { state ->
            favoriteIds = state.books.mapNotNull { it.id }.toSet()
            renderFavoriteButton()
            state.message?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                favoriteBooksViewModel.clearMessage()
            }
        }

        favoriteBooksViewModel.loadFavorites(email)
        viewModel.loadBook(intent.getStringExtra(EXTRA_VOLUME_ID).orEmpty())
    }

    private fun renderBook(book: BookItem) {
        val info = book.volumeInfo
        val title = info?.title?.takeIf { it.isNotBlank() } ?: "Libro sin título"
        binding.tvBookDetailTitle.text = title
        binding.tvBookDetailSubtitle.text = info?.subtitle.orEmpty()
        binding.tvBookDetailSubtitle.visibility =
            if (info?.subtitle.isNullOrBlank()) View.GONE else View.VISIBLE
        binding.tvBookDetailAuthors.text = info?.authors?.joinToString(", ")
            ?.takeIf { it.isNotBlank() } ?: "Autor desconocido"
        binding.ivBookDetailCover.contentDescription = "Portada de $title"

        val coverUrl = info?.imageLinks?.extraLarge
            ?: info?.imageLinks?.large
            ?: info?.imageLinks?.medium
            ?: info?.imageLinks?.thumbnail
            ?: info?.imageLinks?.smallThumbnail
        Glide.with(binding.ivBookDetailCover)
            .load(coverUrl?.replace("http://", "https://"))
            .placeholder(R.drawable.bg_book_placeholder)
            .error(R.drawable.bg_book_placeholder)
            .fitCenter()
            .into(binding.ivBookDetailCover)

        val metadata = mutableListOf<String>()
        info?.publisher?.takeIf { it.isNotBlank() }?.let { metadata += "Editorial: $it" }
        info?.publishedDate?.takeIf { it.isNotBlank() }?.let { metadata += "Publicado: $it" }
        info?.pageCount?.let { metadata += "Páginas: $it" }
        info?.categories?.takeIf { it.isNotEmpty() }?.let {
            metadata += "Categorías: ${it.joinToString(", ")}"
        }
        info?.language?.takeIf { it.isNotBlank() }?.let { metadata += "Idioma: ${it.uppercase(Locale.getDefault())}" }
        info?.averageRating?.let { rating ->
            val count = info.ratingsCount?.let { " ($it valoraciones)" }.orEmpty()
            metadata += "Calificación: ${String.format(Locale.getDefault(), "%.1f", rating)}/5$count"
        }
        info?.printType?.takeIf { it.isNotBlank() }?.let { metadata += "Formato: $it" }
        info?.maturityRating?.takeIf { it.isNotBlank() }?.let { metadata += "Clasificación: $it" }
        info?.industryIdentifiers?.mapNotNull { identifier ->
            val type = identifier.type?.replace('_', ' ')
            val value = identifier.identifier
            if (!type.isNullOrBlank() && !value.isNullOrBlank()) "$type: $value" else null
        }?.let { metadata.addAll(it) }
        val availability = when {
            book.accessInfo?.publicDomain == true -> "Acceso: dominio público"
            book.accessInfo?.viewability == "ALL_PAGES" -> "Acceso: texto completo disponible"
            book.accessInfo?.viewability == "PARTIAL" -> "Acceso: vista previa parcial"
            book.accessInfo?.viewability == "NO_PAGES" -> "Acceso: ficha sin páginas de lectura"
            else -> book.accessInfo?.accessViewStatus?.let { "Acceso: ${it.replace('_', ' ').lowercase(Locale.getDefault())}" }
        }
        availability?.let { metadata += it }
        binding.tvBookDetailMetadata.text = metadata.joinToString("\n")
            .ifBlank { "Google Books no proporcionó más datos bibliográficos." }

        val description = info?.description
        binding.tvBookDetailDescription.text = if (description.isNullOrBlank()) {
            "Google Books no proporciona una descripción para este libro."
        } else {
            HtmlCompat.fromHtml(description, HtmlCompat.FROM_HTML_MODE_COMPACT)
        }

        readerUrl = book.accessInfo?.webReaderLink
            ?: info?.previewLink
            ?: info?.infoLink
            ?: info?.canonicalVolumeLink
        binding.btnOpenBookReader.isEnabled = !readerUrl.isNullOrBlank()
        binding.btnOpenBookReader.text = when {
            readerUrl.isNullOrBlank() -> "Vista previa no disponible"
            book.accessInfo?.viewability == "ALL_PAGES" || book.accessInfo?.publicDomain == true ->
                "Abrir libro en Google Books"
            else -> "Abrir vista previa en Google Books"
        }
    }

    private fun openReader() {
        val url = readerUrl ?: return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            Snackbar.make(binding.root, "No hay una aplicación disponible para abrir Google Books.", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun renderFavoriteButton() {
        val book = currentBook ?: return
        val isFavorite = book.id?.let(favoriteIds::contains) == true
        binding.btnFavoriteDetail.setImageResource(
            if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_outline
        )
        binding.btnFavoriteDetail.contentDescription = if (isFavorite) {
            "Quitar de favoritos"
        } else {
            "Agregar a favoritos"
        }
        binding.btnFavoriteDetail.isEnabled = email.isNotBlank()
    }

    companion object {
        const val EXTRA_VOLUME_ID = "book_volume_id"
    }
}
