package com.haziel.ofmusic.ui.main.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haziel.ofmusic.data.lyrics.LyricLine
import kotlin.math.abs

@Composable
fun LyricsView(
    lyrics: List<LyricLine>,
    currentPositionMs: Long,
    isLoading: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    when {
        isLoading -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = accentColor
                )
            }
        }
        lyrics.isEmpty() -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                Text(
                    text = "Letra no disponible",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        }
        else -> {
            val currentIndex = remember(currentPositionMs, lyrics) {
                lyrics.indexOfLast { it.timeMs <= currentPositionMs }.coerceAtLeast(0)
            }
            val listState = rememberLazyListState()

            LaunchedEffect(currentIndex) {
                if (currentIndex in lyrics.indices) {
                    runCatching {
                        listState.animateScrollToItem(
                            index = currentIndex,
                            scrollOffset = -60
                        )
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = modifier,
                contentPadding = PaddingValues(vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(lyrics) { index, line ->
                    val distance = abs(index - currentIndex)
                    val isCurrent = index == currentIndex
                    val alpha = when {
                        isCurrent -> 1f
                        distance == 1 -> 0.55f
                        distance == 2 -> 0.3f
                        else -> 0.15f
                    }
                    Text(
                        text = line.text,
                        fontSize = if (isCurrent) 19.sp else 15.sp,
                        lineHeight = if (isCurrent) 26.sp else 22.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrent) Color.White else Color.White.copy(alpha = alpha),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}