package com.terralink.iotfarm.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terralink.iotfarm.data.debug.DebugLogManager
import com.terralink.iotfarm.data.debug.LogEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugLogScreen(
    debugLogManager: DebugLogManager,
    onBackClick: () -> Unit
) {
    val logs by debugLogManager.logs.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Network Packet & Debug Inspector") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { debugLogManager.clear() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Logs")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF1E1E1E))
        ) {
            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No network packets or logs recorded yet.",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { entry ->
                        LogItemRow(entry)
                    }
                }
            }
        }
    }
}

@Composable
fun LogItemRow(entry: LogEntry) {
    val badgeColor = when (entry.type) {
        LogEntry.LogType.TCP_RX -> Color(0xFF4CAF50)
        LogEntry.LogType.TCP_TX -> Color(0xFF2196F3)
        LogEntry.LogType.UDP -> Color(0xFFFF9800)
        LogEntry.LogType.AUTOMATION -> Color(0xFF9C27B0)
        LogEntry.LogType.SYSTEM -> Color(0xFF9E9E9E)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = entry.timestamp,
                color = Color.Gray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Surface(
                color = badgeColor,
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text(
                    text = entry.type.name,
                    color = Color.White,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = entry.message,
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
