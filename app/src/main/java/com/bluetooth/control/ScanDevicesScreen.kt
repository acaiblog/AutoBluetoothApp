package com.bluetooth.control

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
fun ScanDevicesScreen(
    viewModel: BluetoothViewModel,
    connectionState: ConnectionState,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit
) {
    val scannedDevices by viewModel.scannedDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val targetDevice by viewModel.targetDevice.collectAsState()

    var showPairDialog by remember { mutableStateOf<BluetoothDeviceInfo?>(null) }
    var previousState by remember { mutableStateOf(connectionState) }

    // 监听连接状态：只有当状态变为 CONNECTED 时才回到首页
    LaunchedEffect(connectionState) {
        if (previousState != ConnectionState.CONNECTED && connectionState == ConnectionState.CONNECTED) {
            onNavigateHome()
        }
        previousState = connectionState
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("扫描设备") },
            navigationIcon = {
                IconButton(onClick = {
                    if (isScanning) {
                        viewModel.stopScan()
                    }
                    onNavigateBack()
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            },
            actions = {
                if (isScanning) {
                    IconButton(onClick = { viewModel.stopScan() }) {
                        Icon(Icons.Default.Stop, contentDescription = "停止扫描")
                    }
                } else {
                    IconButton(onClick = { viewModel.startScan() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "开始扫描")
                    }
                }
            }
        )

        // 扫描状态卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isScanning) 
                    MaterialTheme.colorScheme.secondaryContainer 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            )
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
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                    Column {
                        Text(
                            text = if (isScanning) "正在扫描..." else "扫描已停止",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "已发现 ${scannedDevices.size} 个设备",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Button(
                    onClick = { 
                        if (isScanning) viewModel.stopScan() 
                        else viewModel.startScan() 
                    }
                ) {
                    Text(if (isScanning) "停止" else "开始扫描")
                }
            }
        }

        if (scannedDevices.isEmpty() && !isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.BluetoothSearching,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "点击「开始扫描」查找附近设备",
                        style = MaterialTheme.typography.bodyLarge,
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
                items(scannedDevices) { device ->
                    ScannedDeviceItem(
                        device = device,
                        isTarget = targetDevice?.address == device.address,
                        onPair = { showPairDialog = device }
                    )
                }
            }
        }
    }

    // 配对确认对话框
    showPairDialog?.let { device ->
        AlertDialog(
            onDismissRequest = { showPairDialog = null },
            icon = { Icon(Icons.Default.Bluetooth, contentDescription = null) },
            title = { Text("配对请求") },
            text = { 
                Column {
                    Text("确定要与以下设备配对吗？")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = device.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.pairDevice(device)
                        showPairDialog = null
                    }
                ) {
                    Text("配对")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPairDialog = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun ScannedDeviceItem(
    device: BluetoothDeviceInfo,
    isTarget: Boolean,
    onPair: () -> Unit
) {
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
                    imageVector = if (device.isPaired) Icons.Default.CheckCircle else Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = if (device.isPaired) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
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
                        if (device.isPaired) {
                            SuggestionChip(
                                onClick = { },
                                label = { Text("已配对", style = MaterialTheme.typography.labelSmall) },
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

            if (!device.isPaired) {
                Button(onClick = onPair) {
                    Text("配对")
                }
            }
        }
    }
}
