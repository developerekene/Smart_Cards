package com.example.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CardDifficulty
import com.example.ui.components.DifficultyBadge
import com.example.ui.theme.LocalSmartCardsColors
import com.example.viewmodel.StudyUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    uiState: StudyUiState,
    onBack: () -> Unit,
    onFlip: () -> Unit,
    onMarkDifficulty: (CardDifficulty) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onRestart: (difficultOnly: Boolean, shuffled: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalSmartCardsColors.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.title,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (uiState.cards.isNotEmpty() && !uiState.isCompleted) {
                            Text(
                                text = "Card ${uiState.currentIndex + 1} of ${uiState.cards.size}" +
                                    if (uiState.difficultOnly) " · Difficult Review" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("study_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit Study")
                    }
                },
                actions = {
                    if (uiState.cards.isNotEmpty()) {
                        IconButton(
                            onClick = onToggleShuffle,
                            modifier = Modifier.testTag("study_shuffle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Toggle Shuffle",
                                tint = if (uiState.isShuffled) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
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
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator()
                }
                uiState.cards.isEmpty() -> {
                    Column(
                        modifier = Modifier.widthIn(max = 460.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = ext.easy,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = if (uiState.difficultOnly) {
                                "No Difficult Cards Left!"
                            } else {
                                "No Flashcards to Study"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (uiState.difficultOnly) {
                                "All cards in this set have been reviewed or marked Easy."
                            } else {
                                "Add cards to this deck to begin studying."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = onBack) {
                            Text("Go Back")
                        }
                    }
                }
                uiState.isCompleted -> {
                    StudyCompletedCard(
                        uiState = uiState,
                        onReviewDifficult = { onRestart(true, true) },
                        onStudyAgainShuffled = { onRestart(false, true) },
                        onDone = onBack
                    )
                }
                else -> {
                    val card = uiState.currentCard!!
                    val progress = (uiState.currentIndex + 1).toFloat() / uiState.cards.size.toFloat()

                    Column(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            LinearProgressIndicator(
                                progress = { progress },
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DifficultyBadge(difficulty = card.difficulty)
                                if (uiState.isShuffled) {
                                    Surface(
                                        color = ext.amberContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Randomized",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ext.amber,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 3D Flip Card
                        FlipFlashcard(
                            question = card.question,
                            answer = card.answer,
                            topic = card.topic,
                            isFlipped = uiState.isFlipped,
                            onFlip = onFlip,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 16.dp)
                                .testTag("study_flip_card")
                        )

                        // Study Controls
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { onMarkDifficulty(CardDifficulty.DIFFICULT) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ext.difficultContainer,
                                        contentColor = ext.difficult
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .testTag("mark_difficult_button")
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Difficult")
                                }

                                Button(
                                    onClick = { onMarkDifficulty(CardDifficulty.EASY) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ext.easyContainer,
                                        contentColor = ext.easy
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .testTag("mark_easy_button")
                                ) {
                                    Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Easy")
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = onPrevious,
                                    enabled = uiState.currentIndex > 0,
                                    modifier = Modifier.testTag("study_prev_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Prev")
                                }

                                OutlinedButton(
                                    onClick = onFlip,
                                    modifier = Modifier.testTag("study_flip_button")
                                ) {
                                    Icon(Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (uiState.isFlipped) "Show Question" else "Reveal Answer")
                                }

                                OutlinedButton(
                                    onClick = onNext,
                                    modifier = Modifier.testTag("study_next_button")
                                ) {
                                    Text("Next")
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlipFlashcard(
    question: String,
    answer: String,
    topic: String,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalSmartCardsColors.current
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "card_flip_rotation"
    )
    val showBack = rotation > 90f

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (showBack) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable(onClick = onFlip)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (showBack) rotationY = 180f
                }
        ) {
            // Index card ruled lines on front face
            if (!showBack) {
                val ruledColor = ext.cardRuledLine
                val accentRed = ext.difficult.copy(alpha = 0.3f)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val headerLineY = 64.dp.toPx()
                    drawLine(
                        color = accentRed,
                        start = Offset(0f, headerLineY),
                        end = Offset(size.width, headerLineY),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    var y = headerLineY + 36.dp.toPx()
                    while (y < size.height - 32.dp.toPx()) {
                        drawLine(
                            color = ruledColor,
                            start = Offset(24.dp.toPx(), y),
                            end = Offset(size.width - 24.dp.toPx(), y),
                            strokeWidth = 1.dp.toPx()
                        )
                        y += 36.dp.toPx()
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (showBack) ext.easyContainer else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (showBack) "ANSWER" else "QUESTION",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (showBack) ext.easy else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    if (topic.isNotBlank()) {
                        Text(
                            text = topic,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (showBack) answer else question,
                        style = if (showBack) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (showBack) "Tap to flip back" else "Tap card to reveal answer",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyCompletedCard(
    uiState: StudyUiState,
    onReviewDifficult: () -> Unit,
    onStudyAgainShuffled: () -> Unit,
    onDone: () -> Unit
) {
    val ext = LocalSmartCardsColors.current
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = ext.easy,
                modifier = Modifier.size(64.dp)
            )
            Text("Session Complete!", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "You reviewed ${uiState.cards.size} card(s) in ${uiState.title}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = ext.easyContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${uiState.sessionEasyCount}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = ext.easy
                        )
                        Text("Marked Easy", style = MaterialTheme.typography.labelMedium, color = ext.easy)
                    }
                }
                Surface(
                    color = ext.difficultContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${uiState.sessionDifficultCount}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = ext.difficult
                        )
                        Text("Marked Difficult", style = MaterialTheme.typography.labelMedium, color = ext.difficult)
                    }
                }
            }

            if (uiState.sessionDifficultCount > 0) {
                Button(
                    onClick = onReviewDifficult,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ext.difficult,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Review Difficult Cards")
                }
            }

            OutlinedButton(
                onClick = onStudyAgainShuffled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Study Again (Shuffled)")
            }

            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Finish")
            }
        }
    }
}
