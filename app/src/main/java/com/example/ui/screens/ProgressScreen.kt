package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ExportFormat
import com.example.data.ThemeMode
import com.example.ui.theme.LocalSmartCardsColors
import com.example.viewmodel.ProgressUiState
import com.example.viewmodel.SubjectProgress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    uiState: ProgressUiState,
    themeMode: ThemeMode,
    onSetThemeMode: (ThemeMode) -> Unit,
    onReviewDifficult: () -> Unit,
    onImportClick: () -> Unit,
    onExportClick: (ExportFormat) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalSmartCardsColors.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Learning Progress",
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
                // Mastery Donut Overview
                item(key = "mastery_donut") {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("Overall Card Mastery", style = MaterialTheme.typography.titleLarge)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                MasteryDonutChart(
                                    easy = uiState.easyCards,
                                    difficult = uiState.difficultCards,
                                    unmarked = uiState.unmarkedCards,
                                    masteryPercent = uiState.masteryPercent,
                                    modifier = Modifier.size(130.dp)
                                )

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    LegendStatRow("Easy / Mastered", uiState.easyCards, ext.easy)
                                    LegendStatRow("Difficult", uiState.difficultCards, ext.difficult)
                                    LegendStatRow("New / Unreviewed", uiState.unmarkedCards, MaterialTheme.colorScheme.outline)
                                }
                            }

                            if (uiState.difficultCards > 0) {
                                Button(
                                    onClick = onReviewDifficult,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ext.difficultContainer,
                                        contentColor = ext.difficult
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("progress_review_difficult_button")
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Review ${uiState.difficultCards} Difficult Flashcard(s)")
                                }
                            }
                        }
                    }
                }

                // 2x2 Metrics Grid
                item(key = "metrics_grid") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricTile(
                                title = "Total Decks",
                                value = "${uiState.totalDecks}",
                                subtitle = "${uiState.totalCards} cards",
                                modifier = Modifier.weight(1f)
                            )
                            MetricTile(
                                title = "Quizzes Taken",
                                value = "${uiState.quizzesTaken}",
                                subtitle = "Completed sessions",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricTile(
                                title = "Average Score",
                                value = if (uiState.quizzesTaken > 0) "${uiState.averageQuizPercent}%" else "—",
                                subtitle = "Across all quizzes",
                                modifier = Modifier.weight(1f)
                            )
                            MetricTile(
                                title = "Best Score",
                                value = if (uiState.quizzesTaken > 0) "${uiState.bestQuizPercent}%" else "—",
                                subtitle = "Personal best",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Subject Breakdown
                if (uiState.subjectBreakdown.isNotEmpty()) {
                    item(key = "subject_breakdown") {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Progress by Subject", style = MaterialTheme.typography.titleLarge)
                                uiState.subjectBreakdown.forEach { subject ->
                                    SubjectProgressRow(subject)
                                }
                            }
                        }
                    }
                }

                // Quiz Score History
                item(key = "quiz_history_header") {
                    Text("Quiz Score History", style = MaterialTheme.typography.titleLarge)
                }

                if (uiState.recentSessions.isEmpty()) {
                    item(key = "no_quiz_history") {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Complete a Multiple Choice or True/False quiz to track your scores here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(18.dp)
                            )
                        }
                    }
                } else {
                    items(uiState.recentSessions.take(10), key = { "history_${it.id}" }) { session ->
                        QuizScoreRow(session = session)
                    }
                }

                // Appearance & Data Management (Dark Mode + Import/Export)
                item(key = "appearance_and_data") {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text("Appearance & Data", style = MaterialTheme.typography.titleLarge)

                            Text("Theme Mode", style = MaterialTheme.typography.labelLarge)
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("theme_mode_selector")
                            ) {
                                val options = listOf(
                                    ThemeMode.SYSTEM to "System",
                                    ThemeMode.LIGHT to "Light",
                                    ThemeMode.DARK to "Dark"
                                )
                                options.forEachIndexed { idx, (mode, label) ->
                                    SegmentedButton(
                                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = options.size),
                                        onClick = { onSetThemeMode(mode) },
                                        selected = themeMode == mode
                                    ) {
                                        Text(label)
                                    }
                                }
                            }

                            Text("Import & Export Library", style = MaterialTheme.typography.labelLarge)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onImportClick,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Import")
                                }
                                OutlinedButton(
                                    onClick = { onExportClick(ExportFormat.JSON) },
                                    enabled = uiState.totalCards > 0,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("JSON")
                                }
                                OutlinedButton(
                                    onClick = { onExportClick(ExportFormat.CSV) },
                                    enabled = uiState.totalCards > 0,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("CSV")
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun MasteryDonutChart(
    easy: Int,
    difficult: Int,
    unmarked: Int,
    masteryPercent: Int,
    modifier: Modifier = Modifier
) {
    val ext = LocalSmartCardsColors.current
    val total = (easy + difficult + unmarked).coerceAtLeast(1)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val unmarkedColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            if (easy + difficult + unmarked == 0) {
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            } else {
                var start = -90f
                val segments = listOf(
                    easy to ext.easy,
                    difficult to ext.difficult,
                    unmarked to unmarkedColor
                )
                segments.forEach { (count, color) ->
                    if (count > 0) {
                        val sweep = (count.toFloat() / total.toFloat()) * 360f
                        drawArc(
                            color = color,
                            startAngle = start,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Butt)
                        )
                        start += sweep
                    }
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$masteryPercent%",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Mastered",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LegendStatRow(label: String, count: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = color,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.size(10.dp)
            ) {}
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MetricTile(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SubjectProgressRow(subject: SubjectProgress) {
    val ext = LocalSmartCardsColors.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(subject.subject, style = MaterialTheme.typography.titleSmall)
            Text(
                text = "${subject.easyCards}/${subject.totalCards} Easy" +
                    if (subject.difficultCards > 0) " · ${subject.difficultCards} Difficult" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = { subject.masteryFraction },
            color = ext.easy,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}
