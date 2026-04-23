package com.bluetooth.control

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    
    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (!allGranted) {
            Toast.makeText(this, "需要蓝牙权限才能使用此应用", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkAndRequestPermissions()
        
        setContent {
            MaterialTheme {
                BluetoothControlApp()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    override fun onResume() {
        super.onResume()
        // 检查蓝牙状态
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothControlApp(viewModel: BluetoothViewModel = viewModel()) {
    val context = LocalContext.current
    val navController = remember { mutableStateOf("home") }
    
    val isBluetoothEnabled by viewModel.isBluetoothEnabled.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val targetDevice by viewModel.targetDevice.collectAsState()

    // 返回键拦截：非首页返回首页
    BackHandler(enabled = navController.value != "home") {
        navController.value = "home"
    }

    LaunchedEffect(Unit) {
        viewModel.checkBluetoothState()
    }

    // 监听生命周期：从后台切回前台时检查连接是否还活着
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkConnectionAlive()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("蓝牙控制器") },
                actions = {
                    IconButton(onClick = { navController.value = "settings" }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (navController.value) {
                "home" -> HomeScreen(
                    viewModel = viewModel,
                    isBluetoothEnabled = isBluetoothEnabled,
                    connectionState = connectionState,
                    targetDevice = targetDevice,
                    onNavigateToSettings = { navController.value = "settings" },
                    onNavigateToPaired = { navController.value = "paired" },
                    onNavigateToScan = { navController.value = "scan" },
                    onEnableBluetooth = { viewModel.enableBluetooth() }
                )
                "settings" -> SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.value = "home" }
                )
                "paired" -> PairedDevicesScreen(
                    viewModel = viewModel,
                    connectionState = connectionState,
                    onNavigateBack = { navController.value = "home" },
                    onNavigateHome = { navController.value = "home" }
                )
                "scan" -> ScanDevicesScreen(
                    viewModel = viewModel,
                    connectionState = connectionState,
                    onNavigateBack = { navController.value = "home" },
                    onNavigateHome = { navController.value = "home" }
                )
            }
        }
    }
}
