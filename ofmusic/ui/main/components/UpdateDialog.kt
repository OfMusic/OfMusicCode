package com.haziel.ofmusic.ui.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.haziel.ofmusic.data.update.UpdateInfo
import com.haziel.ofmusic.ui.theme.liquidGlass

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    isDownloading: Boolean,
    downloadProgress: Int,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    Dialog(
        onDismissRequest = {
            if (!updateInfo.mandatory && !isDownloading) onDismiss()
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xF21A1A1A))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Nueva versión disponible",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Versión ${updateInfo.versionName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                if (updateInfo.changelog.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(14.dp)
                    ) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text(
                                text = "Novedades:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = updateInfo.changelog,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                if (isDownloading) {
                    LinearProgressIndicator(
                        progress = { downloadProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.15f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Descargando... $downloadProgress%",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!updateInfo.mandatory) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .liquidGlass(RoundedCornerShape(50), alpha = 0.08f)
                                    .clickable(onClick = onDismiss),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Después", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .liquidGlass(RoundedCornerShape(50), alpha = 0.25f)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(50))
                                .clickable(onClick = onDownload),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Actualizar",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}