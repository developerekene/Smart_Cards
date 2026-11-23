package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CardDifficulty
import com.example.data.CardEntity
import com.example.data.DeckEntity
import com.example.data.ExportFormat
import com.example.data.QuizMode
import com.example.ui.theme.DeckAccentPalette
import com.example.ui.theme.LocalSmartCardsColors

@Composable
fun DifficultyBadge(
    difficulty: CardDifficulty,
    modifier: Modifier = Modifier
) {
    val ext = LocalSmartCardsColors.current
    val (label, bg, fg) = when (difficulty) {
        CardDifficulty.EASY -> Triple("Easy", ext.easyContainer, ext.easy)
        CardDifficulty.DIFFICULT -> Triple("Difficult", ext.difficultContainer, ext.difficult)
        CardDifficulty.UNMARKED -> Triple(
            "New",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun SubjectPill(
    subject: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = accentColor.copy(alpha = 0.14f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = subject,
            style = MaterialTheme.typography.labelMedium,
            color = accentColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun DeckEditorDialog(
    initialDeck: DeckEntity? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, subject: String, description: String, accentIndex: Int) -> Unit
) {
    var title by remember { mutableStateOf(initialDeck?.title.orEmpty()) }
    var subject by remember { mutableStateOf(initialDeck?.subject.orEmpty()) }
    var description by remember { mutableStateOf(initialDeck?.description.orEmpty()) }
    var accentIndex by remember { mutableIntStateOf(initialDeck?.accentIndex ?: 0) }
    var showError by remember { mutableStateOf(false) }

    val quickSubjects = listOf("Science", "History", "Languages", "Math", "Computer Science", "Medicine")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialDeck == null) "Create New Deck" else "Edit Deck",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        showError = false
                    },
                    label = { Text("Deck Title") },
                    placeholder = { Text("e.g., Organic Chemistry") },
                    singleLine = true,
                    isError = showError && title.isBlank(),
                    supportingText = if (showError && title.isBlank()) {
                        { Text("Please enter a deck title") }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deck_title_input")
                )

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject / Topic") },
                    placeholder = { Text("e.g., Chemistry") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deck_subject_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickSubjects.take(3).forEach { tag ->
                        FilterChip(
                            selected = subject.equals(tag, ignoreCase = true),
                            onClick = { subject = tag },
                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    placeholder = { Text("What will you master in this deck?") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deck_description_input")
                )

                Text(
                    text = "Deck Color",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DeckAccentPalette.forEachIndexed { idx, color ->
                        val selected = idx == accentIndex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (selected) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { accentIndex = idx }
                                .semantics { contentDescription = "Color option ${idx + 1}" },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        showError = true
                    } else {
                        onSave(title, subject.ifBlank { "General" }, description, accentIndex)
                    }
                },
                modifier = Modifier.testTag("save_deck_button")
            ) {
                Text(if (initialDeck == null) "Create" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CardEditorDialog(
    initialCard: CardEntity? = null,
    onDismiss: () -> Unit,
    onSave: (question: String, answer: String, topic: String, difficulty: CardDifficulty) -> Unit
) {
    var question by remember { mutableStateOf(initialCard?.question.orEmpty()) }
    var answer by remember { mutableStateOf(initialCard?.answer.orEmpty()) }
    var topic by remember { mutableStateOf(initialCard?.topic.orEmpty()) }
    var difficulty by remember { mutableStateOf(initialCard?.difficulty ?: CardDifficulty.UNMARKED) }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialCard == null) "Add Flashcard" else "Edit Flashcard",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = question,
                    onValueChange = {
                        question = it
                        showError = false
                    },
                    label = { Text("Question (Front)") },
                    placeholder = { Text("Enter your question or prompt") },
                    minLines = 2,
                    maxLines = 4,
                    isError = showError && question.isBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_question_input")
                )

                OutlinedTextField(
                    value = answer,
                    onValueChange = {
                        answer = it
                        showError = false
                    },
                    label = { Text("Answer (Back)") },
                    placeholder = { Text("Enter the correct answer") },
                    minLines = 2,
                    maxLines = 4,
                    isError = showError && answer.isBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_answer_input")
                )

                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Sub-topic Tag (optional)") },
                    placeholder = { Text("e.g., Chapter 3, Enzymes") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_topic_input")
                )

                Text(
                    text = "Difficulty Tag",
                    style = MaterialTheme.typography.labelLarge
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val items = listOf(
                        CardDifficulty.UNMARKED to "New",
                        CardDifficulty.EASY to "Easy",
                        CardDifficulty.DIFFICULT to "Difficult"
                    )
                    items.forEachIndexed { idx, (diff, label) ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = items.size),
                            onClick = { difficulty = diff },
                            selected = difficulty == diff
                        ) {
                            Text(label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (question.isBlank() || answer.isBlank()) {
                        showError = true
                    } else {
                        onSave(question, answer, topic, difficulty)
                    }
                },
                modifier = Modifier.testTag("save_card_button")
            ) {
                Text(if (initialCard == null) "Add Card" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuizSetupDialog(
    deckTitle: String,
    totalCards: Int,
    difficultCards: Int,
    onDismiss: () -> Unit,
    onStartQuiz: (mode: QuizMode, difficultOnly: Boolean, questionLimit: Int) -> Unit
) {
    var mode by remember { mutableStateOf(QuizMode.MULTIPLE_CHOICE) }
    var difficultOnly by remember { mutableStateOf(false) }
    var questionLimit by remember { mutableIntStateOf(0) } // 0 = All

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Quiz Setup", style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = deckTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Text("Quiz Mode", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = mode == QuizMode.MULTIPLE_CHOICE,
                        onClick = { mode = QuizMode.MULTIPLE_CHOICE },
                        label = { Text("Multiple Choice") },
                        leadingIcon = {
                            Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quiz_mode_mc")
                    )
                    FilterChip(
                        selected = mode == QuizMode.TRUE_FALSE,
                        onClick = { mode = QuizMode.TRUE_FALSE },
                        label = { Text("True / False") },
                        leadingIcon = {
                            Icon(Icons.Default.Rule, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quiz_mode_tf")
                    )
                }

                if (difficultCards > 0) {
                    Text("Card Pool", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !difficultOnly,
                            onClick = { difficultOnly = false },
                            label = { Text("All Cards ($totalCards)") }
                        )
                        FilterChip(
                            selected = difficultOnly,
                            onClick = { difficultOnly = true },
                            label = { Text("Difficult Only ($difficultCards)") },
                            leadingIcon = {
                                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }

                Text("Question Count (Randomized)", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "All", 5 to "5", 10 to "10", 20 to "20").forEach { (limit, label) ->
                        FilterChip(
                            selected = questionLimit == limit,
                            onClick = { questionLimit = limit },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onStartQuiz(mode, difficultOnly, questionLimit) },
                enabled = if (difficultOnly) difficultCards > 0 else totalCards > 0,
                modifier = Modifier.testTag("start_quiz_confirm_button")
            ) {
                Text("Start Quiz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ImportExportDialog(
    title: String = "Import & Export Flashcards",
    hasCardsToExport: Boolean,
    onDismiss: () -> Unit,
    onImportClick: () -> Unit,
    onExportClick: (ExportFormat) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Back up your flashcard decks or import sets from JSON or CSV files.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        onDismiss()
                        onImportClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_file_button")
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Import from JSON or CSV")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onExportClick(ExportFormat.JSON)
                        },
                        enabled = hasCardsToExport,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_json_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Export JSON")
                    }

                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onExportClick(ExportFormat.CSV)
                        },
                        enabled = hasCardsToExport,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_csv_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Export CSV")
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Supported Formats",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "• CSV: Question,Answer (or Deck,Subject,Question,Answer,Topic,Difficulty)\n• JSON: SmartCards export or [{\"question\":\"...\",\"answer\":\"...\"}]",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
