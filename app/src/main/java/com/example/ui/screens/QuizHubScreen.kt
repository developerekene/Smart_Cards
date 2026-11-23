package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DeckWithStats
import com.example.data.QuizMode
import com.example.data.QuizSessionEntity
import com.example.ui.components.SubjectPill
import com.example.ui.theme.DeckAccentPalette
import com.example.ui.theme.LocalSmartCardsColors
import com.example.viewmodel.LibraryUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizHubScreen(
    libraryState: LibraryUiState,
    recentSessions: List<QuizSessionEntity>,
    onStartQuiz: (deckId: Long?, mode: QuizMode, difficultOnly: Boolean, questionLimit: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMode by remember { mutableStateOf(QuizMode.MULTIPLE_CHOICE) }
    var questionLimit by remember { mutableIntStateOf(10) }
    val ext = LocalSmartCardsColors.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Quiz Studio",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "quiz_config_card") {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Choose Quiz Format", style = MaterialTheme.typography.titleLarge)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FilterChip(
                                    selected = selectedMode == QuizMode.MULTIPLE_CHOICE,
                                    onClick = { selectedMode = QuizMode.MULTIPLE_CHOICE },
                                    label = { Text("Multiple Choice") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(18.dp))
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("hub_mode_mc")
                                )
                                FilterChip(
                                    selected = selectedMode == QuizMode.TRUE_FALSE,
                                    onClick = { selectedMode = QuizMode.TRUE_FALSE },
                                    label = { Text("True or False") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Rule, contentDescription = null, modifier = Modifier.size(18.dp))
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("hub_mode_tf")
                                )
                            }

                            Text("Questions Per Session", style = MaterialTheme.typography.labelLarge)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(5 to "5", 10 to "10", 20 to "20", 0 to "All").forEach { (limit, label) ->
                                    FilterChip(
                                        selected = questionLimit == limit,
                                        onClick = { questionLimit = limit },
                                        label = { Text(label) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { onStartQuiz(null, selectedMode, false, questionLimit) },
                                    enabled = libraryState.totalCards > 0,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("hub_random_mix_quiz_button")
                                ) {
                                    Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("All Decks Mix")
                                }

                                if (libraryState.difficultCards > 0) {
                                    OutlinedButton(
                                        onClick = { onStartQuiz(null, selectedMode, true, questionLimit) },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ext.difficult),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("hub_difficult_quiz_button")
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Difficult (${libraryState.difficultCards})")
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "quiz_by_deck_header") {
                    Text(
                        text = "Quiz a Specific Deck",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                val nonEmptyDecks = libraryState.allDecks.filter { it.totalCards > 0 }
                if (nonEmptyDecks.isEmpty()) {
                    item(key = "no_quiz_decks") {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Create a deck and add flashcards in the Library tab to unlock quizzes.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                } else {
                    items(nonEmptyDecks, key = { "quiz_deck_${it.id}" }) { deck ->
                        QuizDeckRow(
                            deck = deck,
                            onStart = { onStartQuiz(deck.id, selectedMode, false, questionLimit) }
                        )
                    }
                }

                if (recentSessions.isNotEmpty()) {
                    item(key = "recent_scores_header") {
                        Text(
                            text = "Recent Quiz Scores",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(recentSessions.take(5), key = { "recent_quiz_${it.id}" }) { session ->
                        QuizScoreRow(session = session)
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun QuizDeckRow(
    deck: DeckWithStats,
    onStart: () -> Unit
) {
    val accent = DeckAccentPalette[deck.accentIndex.coerceIn(0, DeckAccentPalette.lastIndex)]
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onStart)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SubjectPill(subject = deck.subject, accentColor = accent)
                Text(
                    text = deck.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${deck.totalCards} cards · ${deck.difficultCards} difficult",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Button(onClick = onStart) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Start")
            }
        }
    }
}

@Composable
fun QuizScoreRow(
    session: QuizSessionEntity,
    modifier: Modifier = Modifier
) {
    val ext = LocalSmartCardsColors.current
    val pct = session.percentage
    val (bg, fg) = when {
        pct >= 80 -> ext.easyContainer to ext.easy
        pct >= 50 -> ext.amberContainer to ext.amber
        else -> ext.difficultContainer to ext.difficult
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = session.deckTitle,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = (if (session.quizMode == QuizMode.MULTIPLE_CHOICE) "Multiple Choice" else "True / False") +
                        " · ${session.correctCount}/${session.totalCount} correct · ${session.durationSeconds}s",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(10.dp))
            Surface(color = bg, shape = RoundedCornerShape(10.dp)) {
                Text(
                    text = "$pct%",
                    style = MaterialTheme.typography.titleSmall,
                    color = fg,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
