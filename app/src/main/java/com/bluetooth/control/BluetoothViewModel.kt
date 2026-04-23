package com.bluetooth.control

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothController = BluetoothController(application)
    private val settingsManager = SettingsManager(application)
    private val handler = Handler(Looper.getMainLooper())

    // 配对设备列表
    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDeviceInfo>> = _pairedDevices.asStateFlow()

    // 扫描到的设备列表
    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val scannedDevices: StateFlow<List<BluetoothDeviceInfo>> = _scannedDevices.asStateFlow()

    // 连接状态
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // 蓝牙是否启用
    private val _isBluetoothEnabled = MutableStateFlow(false)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    // 是否正在扫描
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // 状态提示消息
    val statusMessage: StateFlow<String> = bluetoothController.statusMessage

    // 日志列表
    val logs = LogManager.logs

    fun clearLogs() = LogManager.clear()

    fun exportLogs(context: android.content.Context): String? = LogManager.saveToFile(context)

    // 目标设备
    private val _targetDevice = MutableStateFlow<BluetoothDeviceInfo?>(null)
    val targetDevice: StateFlow<BluetoothDeviceInfo?> = _targetDevice.asStateFlow()

    // 设置相关
    var scanTime: Int
        get() = settingsManager.scanTime
        set(value) {
            settingsManager.scanTime = value
            _settingsUpdated.value = true
        }

    var connectTimeout: Int
        get() = settingsManager.connectTimeout
        set(value) {
            settingsManager.connectTimeout = value
            _settingsUpdated.value = true
        }

    var retryCount: Int
        get() = settingsManager.retryCount
        set(value) {
            settingsManager.retryCount = value
            _settingsUpdated.value = true
        }

    var retryDelay: Int
        get() = settingsManager.retryDelay
        set(value) {
            settingsManager.retryDelay = value
            _settingsUpdated.value = true
        }

    var autoConnect: Boolean
        get() = settingsManager.autoConnect
        set(value) {
            settingsManager.autoConnect = value
            _settingsUpdated.value = true
        }

    // 配对成功后自动连接
    private val _autoConnectOnPair = MutableStateFlow(settingsManager.autoConnectOnPair)
    val autoConnectOnPair: StateFlow<Boolean> = _autoConnectOnPair.asStateFlow()
    
    fun setAutoConnectOnPair(value: Boolean) {
        _autoConnectOnPair.value = value
        settingsManager.autoConnectOnPair = value
        bluetoothController.autoConnectOnPair = value
    }

    private val _settingsUpdated = MutableStateFlow(false)

    init {
        loadSavedTargetDevice()
        // 同步自动连接设置到控制器
        bluetoothController.autoConnectOnPair = settingsManager.autoConnectOnPair
        observeBluetoothState()
    }

    private fun loadSavedTargetDevice() {
        val target = settingsManager.getTargetDevice()
        if (target != null) {
            _targetDevice.value = BluetoothDeviceInfo(
                name = target.first,
                address = target.second,
                isPaired = true
            )
        }
    }

    private fun observeBluetoothState() {
        viewModelScope.launch {
            bluetoothController.isBluetoothEnabled.collect { enabled ->
                _isBluetoothEnabled.value = enabled
                if (enabled) {
                    loadPairedDevices()
                    checkAutoConnect()
                }
            }
        }

        viewModelScope.launch {
            bluetoothController.connectionState.collect { state ->
                _connectionState.value = state
            }
        }

        viewModelScope.launch {
            bluetoothController.pairedDevices.collect { devices ->
                _pairedDevices.value = devices
            }
        }

        viewModelScope.launch {
            bluetoothController.scannedDevices.collect { devices ->
                _scannedDevices.value = devices
            }
        }
    }

    fun checkBluetoothState() {
        bluetoothController.checkBluetoothState()
    }

    fun enableBluetooth() {
        bluetoothController.enableBluetooth()
    }

    fun loadPairedDevices() {
        bluetoothController.loadPairedDevices()
    }

    fun startScan() {
        _isScanning.value = true
        _scannedDevices.value = emptyList()
        bluetoothController.startScan()

        // 在指定时间后停止扫描
        handler.postDelayed({
            stopScan()
        }, scanTime * 1000L)
    }

    fun stopScan() {
        _isScanning.value = false
        bluetoothController.stopScan()
    }

    fun pairDevice(device: BluetoothDeviceInfo) {
        device.device?.let { bluetoothController.pairDevice(it) }
    }

    fun unpairDevice(device: BluetoothDeviceInfo) {
        viewModelScope.launch {
            device.device?.let {
                bluetoothController.unpairDevice(it)
                // 延迟后重新加载设备列表
                handler.postDelayed({
                    loadPairedDevices()
                }, 1000)
            }
        }
    }

    fun setTargetDevice(device: BluetoothDeviceInfo) {
        _targetDevice.value = device
        settingsManager.saveTargetDevice(device.name, device.address)
    }

    fun clearTargetDevice() {
        _targetDevice.value = null
        settingsManager.clearTargetDevice()
    }

    fun connectToDevice(device: BluetoothDeviceInfo) {
        device.device?.let { bluetoothController.connectToDevice(it) }
    }

    fun disconnect() {
        bluetoothController.disconnect()
    }

    private fun checkAutoConnect() {
        if (autoConnect && _connectionState.value != ConnectionState.CONNECTED) {
            val target = _targetDevice.value
            if (target != null) {
                startAutoConnect()
            }
        }
    }

    fun startAutoConnect() {
        val target = _targetDevice.value
        if (target != null) {
            bluetoothController.autoConnect(
                address = target.address,
                maxRetries = retryCount,
                connectTimeout = connectTimeout * 1000L,
                delayBetweenRetries = retryDelay * 1000L
            )
        }
    }

    fun retryConnect() {
        checkAutoConnect()
    }

    /**
     * 从后台切回前台时调用：检查 socket 是否真实存活。
     * 如果已断开，重置状态并自动重连（若开启了自动连接）。
     */
    fun checkConnectionAlive() {
        val isAlive = bluetoothController.checkAndSyncConnectionState()
        if (!isAlive && autoConnect) {
            val target = _targetDevice.value
            if (target != null) {
                LogManager.i("VM", "从后台恢复，连接已断开，触发自动重连")
                startAutoConnect()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothController.release()
    }
}
