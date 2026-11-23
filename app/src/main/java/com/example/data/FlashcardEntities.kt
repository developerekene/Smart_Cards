package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class CardDifficulty {
    UNMARKED,
    EASY,
    DIFFICULT
}

enum class QuizMode {
    MULTIPLE_CHOICE,
    TRUE_FALSE
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

@Entity(tableName = "decks")
data class DeckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val subject: String,
    val description: String = "",
    val accentIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = DeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["deckId"])]
)
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val deckId: Long,
    val question: String,
    val answer: String,
    val topic: String = "",
    val difficulty: CardDifficulty = CardDifficulty.UNMARKED,
    val reviewCount: Int = 0,
    val lastReviewedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "quiz_sessions")
data class QuizSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val deckId: Long?,
    val deckTitle: String,
    val quizMode: QuizMode,
    val correctCount: Int,
    val totalCount: Int,
    val durationSeconds: Int,
    val completedAt: Long = System.currentTimeMillis()
) {
    val percentage: Int
        get() = if (totalCount > 0) ((correctCount * 100f) / totalCount).toInt() else 0
}

data class DeckWithStats(
    val id: Long,
    val title: String,
    val subject: String,
    val description: String,
    val accentIndex: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val totalCards: Int,
    val easyCards: Int,
    val difficultCards: Int,
    val unmarkedCards: Int
) {
    val masteryFraction: Float
        get() = if (totalCards > 0) easyCards.toFloat() / totalCards.toFloat() else 0f
}

data class CardWithDeck(
    val id: Long,
    val deckId: Long,
    val deckTitle: String,
    val deckSubject: String,
    val question: String,
    val answer: String,
    val topic: String,
    val difficulty: CardDifficulty,
    val reviewCount: Int,
    val lastReviewedAt: Long?,
    val createdAt: Long
)

class SmartCardsConverters {
    @TypeConverter
    fun fromDifficulty(value: CardDifficulty): String = value.name

    @TypeConverter
    fun toDifficulty(value: String): CardDifficulty =
        enumValues<CardDifficulty>().firstOrNull { it.name == value } ?: CardDifficulty.UNMARKED

    @TypeConverter
    fun fromQuizMode(value: QuizMode): String = value.name

    @TypeConverter
    fun toQuizMode(value: String): QuizMode =
        enumValues<QuizMode>().firstOrNull { it.name == value } ?: QuizMode.MULTIPLE_CHOICE
}
