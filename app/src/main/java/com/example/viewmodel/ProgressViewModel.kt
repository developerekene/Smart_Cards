package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CardDifficulty
import com.example.data.DeckWithStats
import com.example.data.QuizSessionEntity
import com.example.data.SmartCardsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SubjectProgress(
    val subject: String,
    val totalCards: Int,
    val easyCards: Int,
    val difficultCards: Int
) {
    val masteryFraction: Float
        get() = if (totalCards > 0) easyCards.toFloat() / totalCards.toFloat() else 0f
}

data class ProgressUiState(
    val totalDecks: Int = 0,
    val totalCards: Int = 0,
    val easyCards: Int = 0,
    val difficultCards: Int = 0,
    val unmarkedCards: Int = 0,
    val masteryPercent: Int = 0,
    val quizzesTaken: Int = 0,
    val averageQuizPercent: Int = 0,
    val bestQuizPercent: Int = 0,
    val subjectBreakdown: List<SubjectProgress> = emptyList(),
    val recentSessions: List<QuizSessionEntity> = emptyList()
)

class ProgressViewModel(repository: SmartCardsRepository) : ViewModel() {

    val uiState: StateFlow<ProgressUiState> = combine(
        repository.observeDecksWithStats(),
        repository.observeAllQuizSessions()
    ) { decks: List<DeckWithStats>, sessions: List<QuizSessionEntity> ->
        val totalCards = decks.sumOf { it.totalCards }
        val easyCards = decks.sumOf { it.easyCards }
        val difficultCards = decks.sumOf { it.difficultCards }
        val unmarkedCards = decks.sumOf { it.unmarkedCards }
        val masteryPercent = if (totalCards > 0) ((easyCards * 100f) / totalCards).toInt() else 0

        val avgQuiz = if (sessions.isNotEmpty()) {
            sessions.map { it.percentage }.average().toInt()
        } else {
            0
        }
        val bestQuiz = sessions.maxOfOrNull { it.percentage } ?: 0

        val bySubject = decks
            .groupBy { it.subject.trim().ifEmpty { "General" } }
            .map { (subject, group) ->
                SubjectProgress(
                    subject = subject,
                    totalCards = group.sumOf { it.totalCards },
                    easyCards = group.sumOf { it.easyCards },
                    difficultCards = group.sumOf { it.difficultCards }
                )
            }
            .sortedByDescending { it.totalCards }

        ProgressUiState(
            totalDecks = decks.size,
            totalCards = totalCards,
            easyCards = easyCards,
            difficultCards = difficultCards,
            unmarkedCards = unmarkedCards,
            masteryPercent = masteryPercent,
            quizzesTaken = sessions.size,
            averageQuizPercent = avgQuiz,
            bestQuizPercent = bestQuiz,
            subjectBreakdown = bySubject,
            recentSessions = sessions
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProgressUiState())

    class Factory(private val repository: SmartCardsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProgressViewModel(repository) as T
        }
    }
}
