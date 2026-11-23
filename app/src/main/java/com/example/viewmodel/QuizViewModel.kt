package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CardDifficulty
import com.example.data.CardEntity
import com.example.data.QuizMode
import com.example.data.SmartCardsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed interface QuizQuestion {
    val card: CardEntity

    data class MultipleChoice(
        override val card: CardEntity,
        val options: List<String>,
        val correctIndex: Int
    ) : QuizQuestion

    data class TrueFalse(
        override val card: CardEntity,
        val proposedAnswer: String,
        val isStatementTrue: Boolean
    ) : QuizQuestion
}

data class MissedQuizItem(
    val card: CardEntity,
    val userAnswerText: String
)

data class QuizUiState(
    val title: String = "Quiz",
    val mode: QuizMode = QuizMode.MULTIPLE_CHOICE,
    val isLoading: Boolean = true,
    val insufficientCards: Boolean = false,
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedChoiceIndex: Int? = null,
    val selectedTrueFalse: Boolean? = null,
    val isRevealed: Boolean = false,
    val correctCount: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val missedItems: List<MissedQuizItem> = emptyList(),
    val isCompleted: Boolean = false,
    val durationSeconds: Int = 0
) {
    val currentQuestion: QuizQuestion?
        get() = questions.getOrNull(currentIndex)
}

class QuizViewModel(
    private val deckId: Long?,
    private val quizMode: QuizMode,
    private val difficultOnly: Boolean,
    private val questionLimit: Int,
    private val repository: SmartCardsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState(mode = quizMode))
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private var startTimeMs: Long = System.currentTimeMillis()
    private var resolvedDeckTitle: String = "Quiz"

    init {
        startQuiz()
    }

    fun startQuiz() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isCompleted = false) }
            resolvedDeckTitle = when {
                deckId != null -> repository.getDeckById(deckId)?.title ?: "Deck Quiz"
                difficultOnly -> "Difficult Cards Quiz"
                else -> "Random Mix Quiz"
            }

            val sourceCards = repository.getCardsForStudy(deckId, difficultOnly).shuffled()
            val allLibraryCards = repository.getAllCardsOnce()

            if (sourceCards.isEmpty()) {
                _uiState.update {
                    it.copy(
                        title = resolvedDeckTitle,
                        isLoading = false,
                        insufficientCards = true,
                        questions = emptyList()
                    )
                }
                return@launch
            }

            val selectedCards = if (questionLimit > 0) {
                sourceCards.take(questionLimit)
            } else {
                sourceCards
            }

            val distractorPool = (sourceCards + allLibraryCards)
                .map { it.answer.trim() }
                .filter { it.isNotEmpty() }
                .distinct()

            val generated = when (quizMode) {
                QuizMode.MULTIPLE_CHOICE -> generateMultipleChoiceQuestions(selectedCards, distractorPool)
                QuizMode.TRUE_FALSE -> generateTrueFalseQuestions(selectedCards, distractorPool)
            }

            startTimeMs = System.currentTimeMillis()
            _uiState.update {
                QuizUiState(
                    title = resolvedDeckTitle,
                    mode = quizMode,
                    isLoading = false,
                    insufficientCards = generated.isEmpty(),
                    questions = generated,
                    currentIndex = 0
                )
            }
        }
    }

    private fun generateMultipleChoiceQuestions(
        cards: List<CardEntity>,
        distractorPool: List<String>
    ): List<QuizQuestion.MultipleChoice> {
        val fallbackAnswers = listOf(
            "Mitochondria", "Photosynthesis", "Constant Velocity", "Parliamentary System",
            "Dynamic Equilibrium", "Binary Search", "Atmospheric Pressure", "Primary Key"
        )

        return cards.map { card ->
            val correct = card.answer.trim()
            val candidates = distractorPool
                .filter { !it.equals(correct, ignoreCase = true) }
                .shuffled()
                .take(3)
                .toMutableList()

            if (candidates.size < 3) {
                fallbackAnswers
                    .filter { fb ->
                        !fb.equals(correct, ignoreCase = true) &&
                            candidates.none { it.equals(fb, ignoreCase = true) }
                    }
                    .take(3 - candidates.size)
                    .forEach { candidates.add(it) }
            }

            val options = (candidates + correct).shuffled()
            val correctIndex = options.indexOf(correct)
            QuizQuestion.MultipleChoice(
                card = card,
                options = options,
                correctIndex = correctIndex
            )
        }
    }

    private fun generateTrueFalseQuestions(
        cards: List<CardEntity>,
        distractorPool: List<String>
    ): List<QuizQuestion.TrueFalse> {
        return cards.mapIndexed { index, card ->
            val correct = card.answer.trim()
            val otherAnswers = distractorPool.filter { !it.equals(correct, ignoreCase = true) }
            val shouldBeTrue = if (otherAnswers.isEmpty()) {
                true
            } else {
                (index % 2 == 0) xor Random.nextBoolean()
            }

            val proposed = if (shouldBeTrue) {
                correct
            } else {
                otherAnswers.random()
            }

            QuizQuestion.TrueFalse(
                card = card,
                proposedAnswer = proposed,
                isStatementTrue = shouldBeTrue
            )
        }
    }

    fun selectMultipleChoice(index: Int) {
        val state = _uiState.value
        if (state.isRevealed || state.isCompleted) return
        val question = state.currentQuestion as? QuizQuestion.MultipleChoice ?: return

        val isCorrect = index == question.correctIndex
        val newStreak = if (isCorrect) state.currentStreak + 1 else 0
        val newMissed = if (isCorrect) {
            state.missedItems
        } else {
            state.missedItems + MissedQuizItem(question.card, question.options.getOrElse(index) { "" })
        }

        _uiState.update {
            it.copy(
                selectedChoiceIndex = index,
                isRevealed = true,
                correctCount = it.correctCount + if (isCorrect) 1 else 0,
                currentStreak = newStreak,
                bestStreak = maxOf(it.bestStreak, newStreak),
                missedItems = newMissed
            )
        }
    }

    fun selectTrueFalse(answerIsTrue: Boolean) {
        val state = _uiState.value
        if (state.isRevealed || state.isCompleted) return
        val question = state.currentQuestion as? QuizQuestion.TrueFalse ?: return

        val isCorrect = answerIsTrue == question.isStatementTrue
        val newStreak = if (isCorrect) state.currentStreak + 1 else 0
        val newMissed = if (isCorrect) {
            state.missedItems
        } else {
            state.missedItems + MissedQuizItem(
                question.card,
                if (answerIsTrue) "Answered: True" else "Answered: False"
            )
        }

        _uiState.update {
            it.copy(
                selectedTrueFalse = answerIsTrue,
                isRevealed = true,
                correctCount = it.correctCount + if (isCorrect) 1 else 0,
                currentStreak = newStreak,
                bestStreak = maxOf(it.bestStreak, newStreak),
                missedItems = newMissed
            )
        }
    }

    fun nextQuestion() {
        val state = _uiState.value
        if (!state.isRevealed) return

        val nextIdx = state.currentIndex + 1
        if (nextIdx >= state.questions.size) {
            val elapsedSec = ((System.currentTimeMillis() - startTimeMs) / 1000L).toInt().coerceAtLeast(1)
            _uiState.update {
                it.copy(
                    isCompleted = true,
                    durationSeconds = elapsedSec
                )
            }
            viewModelScope.launch {
                repository.recordQuizSession(
                    deckId = deckId,
                    deckTitle = resolvedDeckTitle,
                    quizMode = quizMode,
                    correctCount = state.correctCount,
                    totalCount = state.questions.size,
                    durationSeconds = elapsedSec
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    currentIndex = nextIdx,
                    selectedChoiceIndex = null,
                    selectedTrueFalse = null,
                    isRevealed = false
                )
            }
        }
    }

    fun markMissedCardDifficult(cardId: Long) {
        viewModelScope.launch {
            repository.setCardDifficulty(cardId, CardDifficulty.DIFFICULT)
            _uiState.update { state ->
                state.copy(
                    missedItems = state.missedItems.map { item ->
                        if (item.card.id == cardId) {
                            item.copy(card = item.card.copy(difficulty = CardDifficulty.DIFFICULT))
                        } else {
                            item
                        }
                    }
                )
            }
        }
    }

    class Factory(
        private val deckId: Long?,
        private val quizMode: QuizMode,
        private val difficultOnly: Boolean,
        private val questionLimit: Int,
        private val repository: SmartCardsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return QuizViewModel(deckId, quizMode, difficultOnly, questionLimit, repository) as T
        }
    }
}
