package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CardDifficulty
import com.example.data.CardWithDeck
import com.example.data.DeckWithStats
import com.example.data.ExportFormat
import com.example.data.ImportExportManager
import com.example.data.SmartCardsRepository
import com.example.data.ThemeMode
import com.example.data.ThemePreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val allDecks: List<DeckWithStats> = emptyList(),
    val filteredDecks: List<DeckWithStats> = emptyList(),
    val matchingCards: List<CardWithDeck> = emptyList(),
    val subjects: List<String> = emptyList(),
    val selectedSubject: String? = null,
    val searchQuery: String = "",
    val totalCards: Int = 0,
    val easyCards: Int = 0,
    val difficultCards: Int = 0
)

class LibraryViewModel(
    private val repository: SmartCardsRepository,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedSubject = MutableStateFlow<String?>(null)
    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    val themeMode: StateFlow<ThemeMode> = themePreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val difficultCards: StateFlow<List<CardWithDeck>> = repository.observeDifficultCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<LibraryUiState> = combine(
        repository.observeDecksWithStats(),
        repository.observeAllCardsWithDeck(),
        _searchQuery,
        _selectedSubject
    ) { decks, cardsWithDeck, query, subject ->
        val trimmedQuery = query.trim().lowercase()
        val subjects = decks.map { it.subject.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()

        val filteredDecks = decks.filter { deck ->
            val matchesSubject = subject == null || deck.subject.equals(subject, ignoreCase = true)
            val matchesQuery = trimmedQuery.isEmpty() ||
                deck.title.lowercase().contains(trimmedQuery) ||
                deck.subject.lowercase().contains(trimmedQuery) ||
                deck.description.lowercase().contains(trimmedQuery) ||
                cardsWithDeck.any { card ->
                    card.deckId == deck.id && (
                        card.question.lowercase().contains(trimmedQuery) ||
                            card.answer.lowercase().contains(trimmedQuery) ||
                            card.topic.lowercase().contains(trimmedQuery)
                        )
                }
            matchesSubject && matchesQuery
        }

        val matchingCards = if (trimmedQuery.isEmpty()) {
            emptyList()
        } else {
            cardsWithDeck.filter { card ->
                val matchesSubject = subject == null || card.deckSubject.equals(subject, ignoreCase = true)
                matchesSubject && (
                    card.question.lowercase().contains(trimmedQuery) ||
                        card.answer.lowercase().contains(trimmedQuery) ||
                        card.topic.lowercase().contains(trimmedQuery) ||
                        card.deckTitle.lowercase().contains(trimmedQuery)
                    )
            }
        }

        LibraryUiState(
            allDecks = decks,
            filteredDecks = filteredDecks,
            matchingCards = matchingCards,
            subjects = subjects,
            selectedSubject = subject,
            searchQuery = query,
            totalCards = decks.sumOf { it.totalCards },
            easyCards = decks.sumOf { it.easyCards },
            difficultCards = decks.sumOf { it.difficultCards }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryUiState())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectSubject(subject: String?) {
        _selectedSubject.value = if (_selectedSubject.value == subject) null else subject
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themePreferences.setThemeMode(mode)
        }
    }

    fun cycleThemeMode() {
        val next = when (themeMode.value) {
            ThemeMode.SYSTEM -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.SYSTEM
        }
        setThemeMode(next)
    }

    fun createDeck(title: String, subject: String, description: String, accentIndex: Int) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.createDeck(title, subject, description, accentIndex)
            _snackbarEvents.tryEmit("Deck \"${title.trim()}\" created")
        }
    }

    fun updateDeck(deckId: Long, title: String, subject: String, description: String, accentIndex: Int) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.updateDeck(deckId, title, subject, description, accentIndex)
            _snackbarEvents.tryEmit("Deck updated")
        }
    }

    fun deleteDeck(deckId: Long, title: String) {
        viewModelScope.launch {
            repository.deleteDeck(deckId)
            _snackbarEvents.tryEmit("Deleted \"$title\"")
        }
    }

    fun setCardDifficulty(cardId: Long, difficulty: CardDifficulty) {
        viewModelScope.launch {
            repository.setCardDifficulty(cardId, difficulty)
        }
    }

    fun loadStarterDecks() {
        viewModelScope.launch {
            repository.loadStarterDecks()
            _snackbarEvents.tryEmit("Starter decks added to your library")
        }
    }

    suspend fun generateExportText(format: ExportFormat): String {
        return repository.exportAllDecks(format)
    }

    fun importFromText(rawContent: String, fallbackTitle: String = "Imported Deck") {
        viewModelScope.launch {
            try {
                val payloads = ImportExportManager.parseImportContent(rawContent, fallbackTitle)
                if (payloads.isEmpty()) {
                    _snackbarEvents.tryEmit("No valid flashcards found in file")
                    return@launch
                }
                val summary = repository.importPayloads(payloads)
                _snackbarEvents.tryEmit(
                    "Imported ${summary.cardsImported} cards across ${summary.decksCreated} deck(s)"
                )
            } catch (e: Exception) {
                _snackbarEvents.tryEmit("Import failed: ${e.localizedMessage ?: "Invalid format"}")
            }
        }
    }

    fun emitMessage(message: String) {
        _snackbarEvents.tryEmit(message)
    }

    class Factory(
        private val repository: SmartCardsRepository,
        private val themePreferences: ThemePreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LibraryViewModel(repository, themePreferences) as T
        }
    }
}
