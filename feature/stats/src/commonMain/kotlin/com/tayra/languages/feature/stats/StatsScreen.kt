package com.tayra.languages.feature.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.service.ReadingStatsSummary
import com.tayra.languages.core.domain.service.StatsService
import com.tayra.languages.core.domain.stats.ChartPoint
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.EmptyMessage
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.navigation.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

class StatsViewModel(private val statsService: StatsService) : ViewModel() {
    private val _summary = MutableStateFlow<ReadingStatsSummary?>(null)
    val summary: StateFlow<ReadingStatsSummary?> = _summary.asStateFlow()

    init {
        viewModelScope.launch { _summary.value = statsService.summary() }
    }
}

private val seriesColors = listOf(Color(0xFF1F77B4), Color(0xFFFF7F0E), Color(0xFF2CA02C), Color(0xFFD62728), Color(0xFF9467BD), Color(0xFF8C564B), Color(0xFFE377C2), Color(0xFF17BECF))

@Composable
fun StatsScreen(onNavigate: (Route) -> Unit, viewModel: StatsViewModel = koinViewModel()) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    Scaffold(topBar = { AppTopBar(title = "Statistics", onNavigate = onNavigate) }) { padding ->
        val data = summary
        if (data == null) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        if (data.table.isEmpty()) {
            EmptyMessage("No reading recorded yet. Mark pages as read to build statistics.", Modifier.padding(padding))
            return@Scaffold
        }
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).widthIn(max = 900.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Reading streak: ${data.streak} day${if (data.streak == 1) "" else "s"}", style = MaterialTheme.typography.titleMedium)
            Text("Words read", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth()) {
                listOf("Language", "Today", "Week", "Month", "Year", "Total").forEachIndexed { i, h ->
                    Text(h, Modifier.weight(if (i == 0) 2f else 1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
            }
            HorizontalDivider()
            data.table.forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(row.languageName, Modifier.weight(2f), style = MaterialTheme.typography.bodyMedium)
                    listOf(row.counts.day, row.counts.week, row.counts.month, row.counts.year, row.counts.total).forEach {
                        Text(it.toString(), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Text("Cumulative words read", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
            Chart(data.chart)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                data.chart.keys.forEachIndexed { i, language ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.size(12.dp).background(seriesColors[i % seriesColors.size])) {}
                        Text("  $language", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun Chart(series: Map<String, List<ChartPoint>>) {
    val points = series.values.flatten()
    if (points.isEmpty()) return
    val minDate = points.minOf { it.date.toEpochDays() }
    val maxDate = points.maxOf { it.date.toEpochDays() }.coerceAtLeast(minDate + 1)
    val maxTotal = points.maxOf { it.runningTotal }.coerceAtLeast(1)
    val axisColor = MaterialTheme.colorScheme.outline
    Canvas(Modifier.fillMaxWidth().height(220.dp)) {
        val left = 8f
        val bottom = size.height - 8f
        drawLine(axisColor, Offset(left, 0f), Offset(left, bottom))
        drawLine(axisColor, Offset(left, bottom), Offset(size.width, bottom))
        series.values.forEachIndexed { i, list ->
            val path = Path()
            list.forEachIndexed { index, point ->
                val x = left + (point.date.toEpochDays() - minDate).toFloat() / (maxDate - minDate) * (size.width - left)
                val y = bottom - point.runningTotal.toFloat() / maxTotal * bottom
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, seriesColors[i % seriesColors.size], style = Stroke(width = 3f))
        }
    }
}
