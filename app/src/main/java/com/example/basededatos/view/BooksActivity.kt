package com.example.basededatos.view

import android.os.Bundle
import android.content.Intent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.basededatos.databinding.ActivityBooksBinding
import com.example.basededatos.viewmodel.AppViewModelFactory
import com.example.basededatos.viewmodel.BooksViewModel
import com.example.basededatos.viewmodel.BookSearchMode
import com.example.basededatos.model.BookItem
import com.example.basededatos.viewmodel.FavoriteBooksViewModel
import com.google.android.material.snackbar.Snackbar

class BooksActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBooksBinding
    private lateinit var booksViewModel: BooksViewModel
    private lateinit var favoriteBooksViewModel: FavoriteBooksViewModel
    private lateinit var email: String
    private var visibleBooks: List<BookItem> = emptyList()
    private var favoriteIds: Set<String> = emptySet()
    private val bookAdapter by lazy {
        BookAdapter(
            onBookClick = ::openBookDetails,
            onFavoriteClick = { favoriteBooksViewModel.toggleFavorite(email, it) }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBooksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnVolver.setOnClickListener { finish() }
        binding.btnBuscarLibros.setOnClickListener { searchBooks() }

        booksViewModel = ViewModelProvider(
            this,
            AppViewModelFactory(application)
        )[BooksViewModel::class.java]
        favoriteBooksViewModel = ViewModelProvider(
            this,
            AppViewModelFactory(application)
        )[FavoriteBooksViewModel::class.java]
        binding.rvBooks.layoutManager = GridLayoutManager(this, 2)
        binding.rvBooks.adapter = bookAdapter

        booksViewModel.state.observe(this) { state ->
            visibleBooks = state.books
            refreshBookCards()
            binding.progressBooks.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            binding.rvBooks.visibility =
                if (!state.isLoading && state.books.isNotEmpty()) View.VISIBLE else View.GONE
            binding.tvListaVacia.visibility =
                if (!state.isLoading && state.isEmpty) View.VISIBLE else View.GONE
            state.errorMessage?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }

        email = intent.getStringExtra(ProfileExtras.EMAIL).orEmpty()
        favoriteBooksViewModel.state.observe(this) { state ->
            favoriteIds = state.books.mapNotNull { it.id }.toSet()
            refreshBookCards()
            state.message?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                favoriteBooksViewModel.clearMessage()
            }
        }
        val subjects = intent.getStringArrayListExtra(ProfileExtras.SUBJECTS).orEmpty()
        binding.tvMateriasBuscadas.text = if (subjects.isEmpty()) {
            "Tus materias aparecerán aquí"
        } else {
            "Libros relacionados con: " + subjects.joinToString(" · ")
        }
        booksViewModel.loadSubjects(subjects)
    }

    override fun onResume() {
        super.onResume()
        if (::favoriteBooksViewModel.isInitialized && ::email.isInitialized) {
            favoriteBooksViewModel.loadFavorites(email)
        }
    }

    private fun searchBooks() {
        val query = binding.etBookQuery.text?.toString()?.trim().orEmpty()
        if (query.isBlank()) {
            binding.tilBookQuery.error = "Escribe un título, autor o materia."
            return
        }
        binding.tilBookQuery.error = null
        val mode = when (binding.toggleBookSearchMode.checkedButtonId) {
            binding.btnSearchTitle.id -> BookSearchMode.TITLE
            binding.btnSearchAuthor.id -> BookSearchMode.AUTHOR
            else -> BookSearchMode.SUBJECT
        }
        binding.tvMateriasBuscadas.text = "Consulta Google Books: ${mode.apiOperator}:$query"
        booksViewModel.searchBooks(query, mode)
    }

    private fun openBookDetails(book: BookItem) {
        val volumeId = book.id
        if (volumeId.isNullOrBlank()) {
            Snackbar.make(binding.root, "Este resultado no tiene una ficha disponible.", Snackbar.LENGTH_LONG).show()
            return
        }
        startActivity(
            Intent(this, BookDetailActivity::class.java)
                .putExtra(BookDetailActivity.EXTRA_VOLUME_ID, volumeId)
                .putExtra(ProfileExtras.EMAIL, email)
        )
    }

    private fun refreshBookCards() {
        if (::binding.isInitialized) bookAdapter.submitList(visibleBooks, favoriteIds)
    }
}
