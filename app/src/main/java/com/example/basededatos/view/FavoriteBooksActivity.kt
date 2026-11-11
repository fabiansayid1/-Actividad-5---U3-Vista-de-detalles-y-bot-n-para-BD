package com.example.basededatos.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.basededatos.databinding.ActivityFavoriteBooksBinding
import com.example.basededatos.model.BookItem
import com.example.basededatos.viewmodel.AppViewModelFactory
import com.example.basededatos.viewmodel.FavoriteBooksViewModel
import com.google.android.material.snackbar.Snackbar

class FavoriteBooksActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFavoriteBooksBinding
    private lateinit var viewModel: FavoriteBooksViewModel
    private lateinit var email: String
    private val adapter by lazy {
        BookAdapter(
            onBookClick = ::openBookDetails,
            onFavoriteClick = { viewModel.toggleFavorite(email, it) }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoriteBooksBinding.inflate(layoutInflater)
        setContentView(binding.root)
        email = intent.getStringExtra(ProfileExtras.EMAIL).orEmpty()
        binding.btnBackFavorites.setOnClickListener { finish() }
        binding.rvFavoriteBooks.layoutManager = GridLayoutManager(this, 2)
        binding.rvFavoriteBooks.adapter = adapter

        viewModel = ViewModelProvider(
            this,
            AppViewModelFactory(application)
        )[FavoriteBooksViewModel::class.java]
        viewModel.state.observe(this) { state ->
            val favoriteIds = state.books.mapNotNull { it.id }.toSet()
            adapter.submitList(state.books, favoriteIds)
            binding.progressFavorites.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            binding.rvFavoriteBooks.visibility = if (state.books.isNotEmpty()) View.VISIBLE else View.GONE
            binding.tvFavoritesEmpty.visibility =
                if (!state.isLoading && state.books.isEmpty()) View.VISIBLE else View.GONE
            state.message?.let {
                Snackbar.make(binding.rootFavorites, it, Snackbar.LENGTH_SHORT).show()
                viewModel.clearMessage()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) viewModel.loadFavorites(email)
    }

    private fun openBookDetails(book: BookItem) {
        val volumeId = book.id
        if (volumeId.isNullOrBlank()) {
            Snackbar.make(binding.rootFavorites, "Este libro no tiene una ficha disponible.", Snackbar.LENGTH_LONG).show()
            return
        }
        startActivity(
            Intent(this, BookDetailActivity::class.java)
                .putExtra(BookDetailActivity.EXTRA_VOLUME_ID, volumeId)
                .putExtra(ProfileExtras.EMAIL, email)
        )
    }
}
