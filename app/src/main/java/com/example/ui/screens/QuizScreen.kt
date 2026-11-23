package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CardDifficulty
import com.example.data.QuizMode
import com.example.ui.theme.LocalSmartCardsColors
import com.example.viewmodel.MissedQuizItem
import com.example.viewmodel.QuizQuestion
import com.example.viewmodel.QuizUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    uiState: QuizUiState,
    onBack: () -> Unit,
    onSelectChoice: (Int) -> Unit,
    onSelectTrueFalse: (Boolean) -> Unit,
    onNext: () -> Unit,
    onRetake: () -> Unit,
    onMarkDifficult: (Long) -> Unit,
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
                        Text(
                            text = if (uiState.mode == QuizMode.MULTIPLE_CHOICE) {
                                "Multiple Choice Quiz"
                            } else {
                                "True or False Quiz"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("quiz_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit Quiz")
                    }
                },
                actions = {
                    if (uiState.currentStreak > 0 && !uiState.isCompleted) {
                        Surface(
                            color = ext.amberContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = "Streak",
                                    tint = ext.amber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "${uiState.currentStreak}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.amber
                                )
                            }
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
            contentAlignment = Alignment.TopCenter
        ) {
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.insufficientCards -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("No Cards Available", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                text = "Add flashcards to this deck before starting a quiz.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = onBack) { Text("Back") }
                        }
                    }
                }
                uiState.isCompleted -> {
                    QuizCompletedView(
                        uiState = uiState,
                        onMarkDifficult = onMarkDifficult,
                        onRetake = onRetake,
                        onDone = onBack
                    )
                }
                else -> {
                    val question = uiState.currentQuestion ?: return@Box
                    val progress = (uiState.currentIndex + 1).toFloat() / uiState.questions.size.toFloat()

                    Column(
                        modifier = Modifier
                            .widthIn(max = 640.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Question ${uiState.currentIndex + 1} of ${uiState.questions.size}",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = "Score: ${uiState.correctCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.easy
                                )
                            }

                            LinearProgressIndicator(
                                progress = { progress },
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            when (question) {
                                is QuizQuestion.MultipleChoice -> {
                                    item {
                                        QuestionPromptCard(
                                            label = "QUESTION",
                                            text = question.card.question,
                                            topic = question.card.topic
                                        )
                                    }
                                    question.options.forEachIndexed { idx, option ->
                                        item(key = "mc_$idx") {
                                            MultipleChoiceOptionCard(
                                                letter = ('A' + idx).toString(),
                                                text = option,
                                                isSelected = uiState.selectedChoiceIndex == idx,
                                                isCorrectOption = idx == question.correctIndex,
                                                isRevealed = uiState.isRevealed,
                                                onClick = { onSelectChoice(idx) },
                                                modifier = Modifier.testTag("mc_option_$idx")
                                            )
                                        }
                                    }
                                }

                                is QuizQuestion.TrueFalse -> {
                                    item {
                                        TrueFalseStatementCard(
                                            questionText = question.card.question,
                                            proposedAnswer = question.proposedAnswer,
                                            isRevealed = uiState.isRevealed,
                                            isStatementTrue = question.isStatementTrue,
                                            actualAnswer = question.card.answer
                                        )
                                    }
                                    item {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            TrueFalseButton(
                                                label = "True",
                                                value = true,
                                                selectedValue = uiState.selectedTrueFalse,
                                                correctValue = question.isStatementTrue,
                                                isRevealed = uiState.isRevealed,
                                                onClick = { onSelectTrueFalse(true) },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("tf_true_button")
                                            )
                                            TrueFalseButton(
                                                label = "False",
                                                value = false,
                                                selectedValue = uiState.selectedTrueFalse,
                                                correctValue = question.isStatementTrue,
                                                isRevealed = uiState.isRevealed,
                                                onClick = { onSelectTrueFalse(false) },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("tf_false_button")
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = onNext,
                            enabled = uiState.isRevealed,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("quiz_next_button")
                        ) {
                            Text(
                                text = if (uiState.currentIndex + 1 >= uiState.questions.size) {
                                    "See Results"
                                } else {
                                    "Next Question"
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionPromptCard(
    label: String,
    text: String,
    topic: String
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                if (topic.isNotBlank()) {
                    Text(
                        text = topic,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = text,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Composable
private fun MultipleChoiceOptionCard(
    letter: String,
    text: String,
    isSelected: Boolean,
    isCorrectOption: Boolean,
    isRevealed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalSmartCardsColors.current
    val targetBg = when {
        !isRevealed -> MaterialTheme.colorScheme.surface
        isCorrectOption -> ext.easyContainer
        isSelected -> ext.difficultContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val bgColor by animateColorAsState(targetValue = targetBg, label = "mc_bg")

    val borderColor = when {
        !isRevealed -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        isCorrectOption -> ext.easy
        isSelected -> ext.difficult
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    Card(
        onClick = onClick,
        enabled = !isRevealed,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = bgColor,
            disabledContainerColor = bgColor
        ),
        border = BorderStroke(1.5.dp, borderColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = letter,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (isRevealed) {
                if (isCorrectOption) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Correct", tint = ext.easy)
                } else if (isSelected) {
                    Icon(Icons.Default.Cancel, contentDescription = "Incorrect", tint = ext.difficult)
                }
            }
        }
    }
}

@Composable
private fun TrueFalseStatementCard(
    questionText: String,
    proposedAnswer: String,
    isRevealed: Boolean,
    isStatementTrue: Boolean,
    actualAnswer: String
) {
    val ext = LocalSmartCardsColors.current
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "IS THIS MATCH TRUE OR FALSE?",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Text(
                text = questionText,
                style = MaterialTheme.typography.titleLarge
            )

            HorizontalDivider(color = ext.cardRuledLine)

            Text(
                text = "Proposed Answer:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = proposedAnswer,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )

            if (isRevealed && !isStatementTrue) {
                Surface(
                    color = ext.easyContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Correct Answer:",
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.easy
                        )
                        Text(
                            text = actualAnswer,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrueFalseButton(
    label: String,
    value: Boolean,
    selectedValue: Boolean?,
    correctValue: Boolean,
    isRevealed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalSmartCardsColors.current
    val bg = when {
        !isRevealed -> MaterialTheme.colorScheme.surface
        value == correctValue -> ext.easyContainer
        selectedValue == value -> ext.difficultContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val border = when {
        !isRevealed -> MaterialTheme.colorScheme.outline
        value == correctValue -> ext.easy
        selectedValue == value -> ext.difficult
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    Card(
        onClick = onClick,
        enabled = !isRevealed,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = bg, disabledContainerColor = bg),
        border = BorderStroke(1.5.dp, border),
        modifier = modifier.height(72.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun QuizCompletedView(
    uiState: QuizUiState,
    onMarkDifficult: (Long) -> Unit,
    onRetake: () -> Unit,
    onDone: () -> Unit
) {
    val ext = LocalSmartCardsColors.current
    val total = uiState.questions.size.coerceAtLeast(1)
    val pct = ((uiState.correctCount * 100f) / total).toInt()
    val ringColor = when {
        pct >= 80 -> ext.easy
        pct >= 50 -> ext.amber
        else -> ext.difficult
    }

    LazyColumn(
        modifier = Modifier.widthIn(max = 600.dp).fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Quiz Complete!", style = MaterialTheme.typography.headlineMedium)

                    Box(
                        modifier = Modifier.size(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val trackColor = MaterialTheme.colorScheme.surfaceVariant
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 14.dp.toPx()
                            drawArc(
                                color = trackColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = ringColor,
                                startAngle = -90f,
                                sweepAngle = (pct / 100f) * 360f,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$pct%",
                                style = MaterialTheme.typography.displaySmall,
                                color = ringColor
                            )
                            Text(
                                text = "${uiState.correctCount}/$total",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${uiState.bestStreak}", style = MaterialTheme.typography.titleLarge)
                            Text("Best Streak", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${uiState.durationSeconds}s", style = MaterialTheme.typography.titleLarge)
                            Text("Time Taken", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRetake,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quiz_retake_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Retake")
                        }
                        Button(
                            onClick = onDone,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quiz_done_button")
                        ) {
                            Text("Done")
                        }
                    }
                }
            }
        }

        if (uiState.missedItems.isNotEmpty()) {
            item {
                Text(
                    text = "Review Missed Cards (${uiState.missedItems.size})",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            items(uiState.missedItems) { item ->
                MissedItemCard(item = item, onMarkDifficult = { onMarkDifficult(item.card.id) })
            }
        }
    }
}

@Composable
private fun MissedItemCard(
    item: MissedQuizItem,
    onMarkDifficult: () -> Unit
) {
    val ext = LocalSmartCardsColors.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(item.card.question, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Your pick: ${item.userAnswerText}",
                style = MaterialTheme.typography.bodySmall,
                color = ext.difficult
            )
            Text(
                text = "Correct answer: ${item.card.answer}",
                style = MaterialTheme.typography.bodyMedium,
                color = ext.easy,
                fontWeight = FontWeight.SemiBold
            )
            if (item.card.difficulty != CardDifficulty.DIFFICULT) {
                OutlinedButton(
                    onClick = onMarkDifficult,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ext.difficult)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Mark as Difficult")
                }
            } else {
                Text(
                    text = "Marked Difficult for review",
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.difficult
                )
            }
        }
    }
}
