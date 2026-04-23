package com.bluetooth.control

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: BluetoothViewModel,
    onNavigateBack: () -> Unit
) {
    var showScanTimeDialog by remember { mutableStateOf(false) }
    var showConnectTimeoutDialog by remember { mutableStateOf(false) }
    var showRetryCountDialog by remember { mutableStateOf(false) }
    var showRetryDelayDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 返回按钮
        TopAppBar(
            title = { Text("设置") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "扫描设置",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // 扫描时间选择
            item {
                SettingsItem(
                    title = "扫描时间",
                    subtitle = "设置蓝牙扫描持续时间",
                    value = "${viewModel.scanTime}秒",
                    icon = Icons.Default.Timer,
                    onClick = { showScanTimeDialog = true }
                )
            }

            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "连接设置",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // 连接超时选择
            item {
                SettingsItem(
                    title = "连接超时",
                    subtitle = "单次连接尝试的超时时间",
                    value = "${viewModel.connectTimeout}秒",
                    icon = Icons.Default.Timer,
                    onClick = { showConnectTimeoutDialog = true }
                )
            }

            // 重试次数选择
            item {
                SettingsItem(
                    title = "重试次数",
                    subtitle = "连接失败时的最大重试次数",
                    value = "${viewModel.retryCount}次",
                    icon = Icons.Default.Replay,
                    onClick = { showRetryCountDialog = true }
                )
            }

            // 重试间隔选择
            item {
                SettingsItem(
                    title = "重试间隔",
                    subtitle = "两次重试之间的等待时间",
                    value = "${viewModel.retryDelay}秒",
                    icon = Icons.Default.HourglassEmpty,
                    onClick = { showRetryDelayDialog = true }
                )
            }

            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "自动连接",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // 自动连接开关
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.PowerSettingsNew, contentDescription = null)
                            Column {
                                Text(
                                    text = "启动时自动连接",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "应用启动时自动连接目标设备",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = viewModel.autoConnect,
                            onCheckedChange = { viewModel.autoConnect = it }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "当前设置摘要",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "扫描 ${viewModel.scanTime}秒 → 连接超时 ${viewModel.connectTimeout}秒 → 重试 ${viewModel.retryCount}次（间隔 ${viewModel.retryDelay}秒）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // 扫描时间选择对话框
    if (showScanTimeDialog) {
        SelectionDialog(
            title = "选择扫描时间",
            options = SettingOptions.scanTimeOptions,
            selectedValue = viewModel.scanTime,
            onSelect = { viewModel.scanTime = it },
            onDismiss = { showScanTimeDialog = false }
        )
    }

    // 连接超时选择对话框
    if (showConnectTimeoutDialog) {
        SelectionDialog(
            title = "选择连接超时",
            options = SettingOptions.connectTimeoutOptions,
            selectedValue = viewModel.connectTimeout,
            onSelect = { viewModel.connectTimeout = it },
            onDismiss = { showConnectTimeoutDialog = false }
        )
    }

    // 重试次数选择对话框
    if (showRetryCountDialog) {
        SelectionDialog(
            title = "选择重试次数",
            options = SettingOptions.retryCountOptions,
            selectedValue = viewModel.retryCount,
            onSelect = { viewModel.retryCount = it },
            onDismiss = { showRetryCountDialog = false }
        )
    }

    // 重试间隔选择对话框
    if (showRetryDelayDialog) {
        SelectionDialog(
            title = "选择重试间隔",
            options = SettingOptions.retryDelayOptions,
            selectedValue = viewModel.retryDelay,
            onSelect = { viewModel.retryDelay = it },
            onDismiss = { showRetryDelayDialog = false }
        )
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(icon, contentDescription = null)
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SelectionDialog(
    title: String,
    options: List<SettingOption>,
    selectedValue: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(option.value)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (selectedValue == option.value) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "已选择",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (option != options.last()) {
                        Divider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
