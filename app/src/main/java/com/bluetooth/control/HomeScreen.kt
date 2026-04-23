package com.bluetooth.control

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// LogEntry and LogLevel are imported from LogManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: BluetoothViewModel,
    isBluetoothEnabled: Boolean,
    connectionState: ConnectionState,
    targetDevice: BluetoothDeviceInfo?,
    onNavigateToSettings: () -> Unit,
    onNavigateToPaired: () -> Unit,
    onNavigateToScan: () -> Unit,
    onEnableBluetooth: () -> Unit
) {
    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val logs by viewModel.logs.collectAsState()
    var logExpanded by remember { mutableStateOf(true) } // 默认展开

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ===== 上半部分：状态卡片区域（固定权重）=====
        Column(
            modifier = Modifier
                .weight(10f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 蓝牙状态 + 连接状态 合并一行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 蓝牙状态卡片
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isBluetoothEnabled)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isBluetoothEnabled) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isBluetoothEnabled) "蓝牙已开启" else "蓝牙已关闭",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (!isBluetoothEnabled) {
                            TextButton(onClick = onEnableBluetooth, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                                Text("开启", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                // 连接状态卡片
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = when (connectionState) {
                            ConnectionState.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
                            ConnectionState.CONNECTING -> MaterialTheme.colorScheme.secondaryContainer
                            ConnectionState.ERROR -> MaterialTheme.colorScheme.errorContainer
                            ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = when (connectionState) {
                                    ConnectionState.CONNECTED -> Icons.Default.CheckCircle
                                    ConnectionState.CONNECTING -> Icons.Default.Sync
                                    ConnectionState.ERROR -> Icons.Default.Error
                                    ConnectionState.DISCONNECTED -> Icons.Default.LinkOff
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = when (connectionState) {
                                    ConnectionState.CONNECTED -> "已连接"
                                    ConnectionState.CONNECTING -> "连接中..."
                                    ConnectionState.ERROR -> "连接失败"
                                    ConnectionState.DISCONNECTED -> "未连接"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // 目标设备卡片
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        if (targetDevice != null) {
                            Column {
                                Text(
                                    text = targetDevice.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = targetDevice.address,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(
                                text = "未设置目标设备",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 清除、重试、自动连接（单独一行）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (targetDevice != null) {
                    OutlinedButton(
                        onClick = { viewModel.clearTargetDevice() },
                        modifier = Modifier.weight(0.73f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text("清除", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Button(
                    onClick = { viewModel.retryConnect() },
                    enabled = targetDevice != null && connectionState != ConnectionState.CONNECTING,
                    modifier = Modifier.weight(0.73f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("重试", style = MaterialTheme.typography.labelMedium)
                }
                // 配对成功后自动连接开关
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("配对后自动连", style = MaterialTheme.typography.labelMedium)
                        Switch(
                            checked = viewModel.autoConnectOnPair.collectAsState().value,
                            onCheckedChange = { viewModel.setAutoConnectOnPair(it) },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }

            // 已配对 + 扫描 按钮（始终在底部可见）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToPaired,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("已配对", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = onNavigateToScan,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("扫描", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // ===== 下半部分：日志面板 = 12 份 =====
        LogPanel(
            logs = logs,
            expanded = logExpanded,
            onToggle = { logExpanded = !logExpanded },
            onClear = { viewModel.clearLogs() },
            modifier = Modifier.weight(12f)
        )
    }
}

@Composable
private fun LogPanel(
    logs: List<LogEntry>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // 新日志到达时自动滚动
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 面板头部（点击展开/收起）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Article,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "日志${if (logs.isNotEmpty()) " (${logs.size})" else ""}",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (expanded && logs.isNotEmpty()) {
                        IconButton(
                            onClick = onClear,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "清除",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = if (expanded) "收起" else "展开",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 日志列表区域（展开时占满剩余空间）
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Divider()

                    if (logs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无日志",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            items(logs) { entry ->
                                LogEntryItem(entry = entry)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogEntryItem(entry: LogEntry) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val levelColor = when (entry.level) {
        LogLevel.INFO -> Color(0xFF1976D2)   // 蓝色
        LogLevel.SUCCESS -> Color(0xFF388E3C) // 绿色
        LogLevel.WARNING -> Color(0xFFF57C00)  // 橙色
        LogLevel.ERROR -> Color(0xFFD32F2F)    // 红色
    }

    val levelTag = when (entry.level) {
        LogLevel.INFO -> "I"
        LogLevel.SUCCESS -> "✓"
        LogLevel.WARNING -> "⚠"
        LogLevel.ERROR -> "✗"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = dateFormat.format(Date(entry.timestamp)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(52.dp)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(2.dp))
                .background(levelColor.copy(alpha = 0.15f))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                text = levelTag,
                style = MaterialTheme.typography.labelSmall,
                color = levelColor
            )
        }
        Text(
            text = entry.message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
