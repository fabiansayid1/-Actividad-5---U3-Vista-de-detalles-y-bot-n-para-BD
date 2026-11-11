package com.example.basededatos.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.basededatos.model.BookItem
import com.example.basededatos.network.UserStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class FavoriteBooksUiState(
    val isLoading: Boolean = false,
    val books: List<BookItem> = emptyList(),
    val message: String? = null
)

class FavoriteBooksViewModel(
    private val userStore: UserStore
) : ViewModel() {

    private val _state = MutableLiveData(FavoriteBooksUiState())
    val state: LiveData<FavoriteBooksUiState> = _state
    private val operationsMutex = Mutex()
    private var loadJob: Job? = null

    fun loadFavorites(email: String) {
        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail.isBlank()) {
            _state.value = FavoriteBooksUiState(message = "No se encontró la cuenta del estudiante.")
            return
        }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = _state.value?.copy(isLoading = true, message = null)
                ?: FavoriteBooksUiState(isLoading = true)
            try {
                val books = operationsMutex.withLock { userStore.getFavoriteBooks(normalizedEmail) }
                _state.value = FavoriteBooksUiState(books = books)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.value = FavoriteBooksUiState(message = "No se pudieron cargar tus libros favoritos.")
            }
        }
    }

    fun toggleFavorite(email: String, book: BookItem) {
        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail.isBlank() || book.id.isNullOrBlank()) {
            _state.value = _state.value?.copy(message = "No se pudo guardar este libro en favoritos.")
            return
        }
        loadJob?.cancel()
        viewModelScope.launch {
            _state.value = _state.value?.copy(isLoading = true, message = null)
                ?: FavoriteBooksUiState(isLoading = true)
            try {
                val (isFavorite, books) = operationsMutex.withLock {
                    val saved = userStore.toggleFavoriteBook(normalizedEmail, book)
                    saved to userStore.getFavoriteBooks(normalizedEmail)
                }
                val title = book.volumeInfo?.title?.takeIf { it.isNotBlank() } ?: "El libro"
                _state.value = FavoriteBooksUiState(
                    books = books,
                    message = if (isFavorite) "$title se agregó a favoritos." else "$title se quitó de favoritos."
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.value = _state.value?.copy(
                    isLoading = false,
                    message = "No se pudo actualizar la lista de favoritos."
                ) ?: FavoriteBooksUiState(message = "No se pudo actualizar la lista de favoritos.")
            }
        }
    }

    fun clearMessage() {
        _state.value = _state.value?.copy(message = null)
    }
}
