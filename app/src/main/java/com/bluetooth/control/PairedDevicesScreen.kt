package com.bluetooth.control

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairedDevicesScreen(
    viewModel: BluetoothViewModel,
    connectionState: ConnectionState,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit
) {
    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val targetDevice by viewModel.targetDevice.collectAsState()

    var showUnpairDialog by remember { mutableStateOf<BluetoothDeviceInfo?>(null) }
    var previousState by remember { mutableStateOf(connectionState) }

    LaunchedEffect(Unit) {
        viewModel.loadPairedDevices()
    }

    // 监听连接状态：只有当状态变为 CONNECTED 时才回到首页
    LaunchedEffect(connectionState) {
        if (previousState != ConnectionState.CONNECTED && connectionState == ConnectionState.CONNECTED) {
            onNavigateHome()
        }
        previousState = connectionState
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("已配对设备") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            },
            actions = {
                IconButton(onClick = { viewModel.loadPairedDevices() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新")
                }
            }
        )

        if (pairedDevices.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.BluetoothDisabled,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "没有已配对的设备",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "请先扫描并配对设备",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pairedDevices) { device ->
                    PairedDeviceItem(
                        device = device,
                        isTarget = targetDevice?.address == device.address,
                        onSetTarget = { viewModel.setTargetDevice(device) },
                        onConnect = { viewModel.connectToDevice(device) },
                        onUnpair = { showUnpairDialog = device }
                    )
                }
            }
        }
    }

    // 解绑确认对话框
    showUnpairDialog?.let { device ->
        AlertDialog(
            onDismissRequest = { showUnpairDialog = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("确认解绑") },
            text = { Text("确定要解绑设备 \"${device.name}\" 吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.unpairDevice(device)
                        showUnpairDialog = null
                    }
                ) {
                    Text("解绑", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnpairDialog = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun PairedDeviceItem(
    device: BluetoothDeviceInfo,
    isTarget: Boolean,
    onSetTarget: () -> Unit,
    onConnect: () -> Unit,
    onUnpair: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isTarget) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) else CardDefaults.cardColors()
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
                Icon(
                    imageVector = if (isTarget) Icons.Default.Star else Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = if (isTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (isTarget) {
                            SuggestionChip(
                                onClick = { },
                                label = { Text("目标", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                    Text(
                        text = device.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多选项")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (!isTarget) {
                        DropdownMenuItem(
                            text = { Text("设为目标设备") },
                            onClick = {
                                onSetTarget()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Star, contentDescription = null)
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("取消目标设备") },
                            onClick = {
                                // 清除目标设备的逻辑
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.StarOutline, contentDescription = null)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("解绑设备") },
                        onClick = {
                            onUnpair()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete, 
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}
