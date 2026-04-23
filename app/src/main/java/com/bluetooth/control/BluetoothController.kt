package com.bluetooth.control

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.util.UUID

class BluetoothController(private val context: Context) {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDeviceInfo>> = _pairedDevices.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val scannedDevices: StateFlow<List<BluetoothDeviceInfo>> = _scannedDevices.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _isBluetoothEnabled = MutableStateFlow(false)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    // 自动连接设置（配对成功后自动连接）
    var autoConnectOnPair: Boolean = false

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private var socket: BluetoothSocket? = null
    private var connectThread: Thread? = null
    private var autoConnectThread: Thread? = null
    private var readLoopThread: Thread? = null

    companion object {
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val TAG = "BT"
        // 反射创建 socket 时的 channel 号（HC-05 常用 1）
        private const val RFCOMM_CHANNEL = 1
    }

    /**
     * 优先用反射创建 RFCOMM socket（兼容 HC-05/HC-06 等模块）。
     * 如果反射失败，再回退到标准 createRfcommSocketToServiceRecord。
     */
    private fun BluetoothDevice.createBluetoothSocket(): BluetoothSocket? {
        // 方式1：反射（最可靠，绕过 Android BluetoothStack 的兼容性问题）
        try {
            val paramTypes = arrayOf(Integer.TYPE)
            val createMethod = this.javaClass.getDeclaredMethod("createRfcommSocket", *paramTypes)
            return createMethod.invoke(this, RFCOMM_CHANNEL) as? BluetoothSocket
        } catch (e: Exception) {
            Log.w(TAG, "反射创建 socket 失败，尝试标准方法: ${e.message}")
        }
        // 方式2：标准 SPP UUID（部分设备支持）
        return try {
            createRfcommSocketToServiceRecord(SPP_UUID)
        } catch (e: Exception) {
            Log.e(TAG, "标准方法也失败: ${e.message}")
            null
        }
    }

    // 蓝牙状态变化广播
    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    when (state) {
                        BluetoothAdapter.STATE_ON -> {
                            _isBluetoothEnabled.value = true
                            LogManager.s(TAG, "蓝牙已开启")
                            loadPairedDevices()
                        }
                        BluetoothAdapter.STATE_OFF -> {
                            _isBluetoothEnabled.value = false
                            _connectionState.value = ConnectionState.DISCONNECTED
                            _pairedDevices.value = emptyList()
                            LogManager.w(TAG, "蓝牙已关闭，连接断开")
                        }
                        BluetoothAdapter.STATE_TURNING_ON -> LogManager.i(TAG, "蓝牙正在开启...")
                        BluetoothAdapter.STATE_TURNING_OFF -> LogManager.i(TAG, "蓝牙正在关闭...")
                    }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)
                    val name = try { device?.name ?: device?.address ?: "未知设备" } catch (e: SecurityException) { device?.address ?: "未知设备" }
                    when (bondState) {
                        BluetoothDevice.BOND_BONDING  -> LogManager.i(TAG, "正在配对：$name")
                        BluetoothDevice.BOND_BONDED   -> {
                            LogManager.s(TAG, "配对成功：$name")
                            // 配对成功后自动连接
                            if (autoConnectOnPair && device != null) {
                                LogManager.i(TAG, "自动连接配对设备：$name")
                                connectToDevice(device)
                            }
                        }
                        BluetoothDevice.BOND_NONE     -> LogManager.w(TAG, "已解除配对：$name")
                    }
                    loadPairedDevices()
                }
            }
        }
    }

    // 扫描广播
    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.let { btDevice ->
                        try {
                            val devName = btDevice.name
                            // 过滤掉未知设备（名称为空或null）
                            if (devName.isNullOrBlank()) {
                                return@let
                            }
                            val devAddress = btDevice.address ?: return@let
                            val devPaired = btDevice.bondState == BluetoothDevice.BOND_BONDED
                            val deviceInfo = BluetoothDeviceInfo(
                                name = devName,
                                address = devAddress,
                                isPaired = devPaired,
                                device = btDevice
                            )
                            if (_scannedDevices.value.none { d -> d.address == deviceInfo.address }) {
                                _scannedDevices.value = _scannedDevices.value + deviceInfo
                                LogManager.i(TAG, "发现设备：$devName ($devAddress)${if (devPaired) " [已配对]" else ""}")
                            }
                        } catch (e: SecurityException) {
                            Log.e(TAG, "Permission denied when reading device info", e)
                            LogManager.e(TAG, "读取设备信息失败：权限不足")
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    val count = _scannedDevices.value.size
                    LogManager.s(TAG, "扫描完成，共发现 $count 台设备")
                }
            }
        }
    }

    private var scanReceiverRegistered = false

    init {
        LogManager.i(TAG, "蓝牙控制器初始化")
        checkBluetoothState()
        registerBluetoothStateReceiver()
    }

    private fun registerBluetoothStateReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(bluetoothStateReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(bluetoothStateReceiver, filter)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register bluetooth state receiver", e)
            LogManager.e(TAG, "注册蓝牙广播失败：${e.message}")
        }
    }

    fun checkBluetoothState() {
        val enabled = bluetoothAdapter?.isEnabled == true
        _isBluetoothEnabled.value = enabled
        LogManager.i(TAG, "蓝牙状态：${if (enabled) "已开启" else "已关闭"}")
        if (enabled) loadPairedDevices()
    }

    fun enableBluetooth() {
        LogManager.i(TAG, "请求开启蓝牙")
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBtIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(enableBtIntent)
    }

    fun loadPairedDevices() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT) &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            LogManager.e(TAG, "加载配对设备失败：缺少 BLUETOOTH_CONNECT 权限")
            return
        }
        try {
            val devices = bluetoothAdapter?.bondedDevices
                ?.mapNotNull { device ->
                    val name = device.name
                    if (name.isNullOrBlank()) {
                        null
                    } else {
                        BluetoothDeviceInfo(
                            name = name,
                            address = device.address,
                            isPaired = true,
                            device = device
                        )
                    }
                } ?: emptyList()
            _pairedDevices.value = devices
            LogManager.i(TAG, "已加载 ${devices.size} 台配对设备")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied when loading paired devices", e)
            LogManager.e(TAG, "加载配对设备失败：${e.message}")
        }
    }

    fun startScan() {
        _scannedDevices.value = emptyList()
        LogManager.i(TAG, "开始扫描附近蓝牙设备...")
        try {
            if (!scanReceiverRegistered) {
                val filter = IntentFilter().apply {
                    addAction(BluetoothDevice.ACTION_FOUND)
                    addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(scanReceiver, filter, Context.RECEIVER_EXPORTED)
                } else {
                    context.registerReceiver(scanReceiver, filter)
                }
                scanReceiverRegistered = true
            }
            bluetoothAdapter?.cancelDiscovery()
            bluetoothAdapter?.startDiscovery()
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied when starting scan", e)
            LogManager.e(TAG, "扫描失败：权限不足 - ${e.message}")
        }
    }

    fun stopScan() {
        LogManager.i(TAG, "停止扫描")
        try {
            bluetoothAdapter?.cancelDiscovery()
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied when stopping scan", e)
        }
        if (scanReceiverRegistered) {
            try { context.unregisterReceiver(scanReceiver) } catch (e: Exception) { /* ignore */ }
            scanReceiverRegistered = false
        }
    }

    fun pairDevice(device: BluetoothDevice) {
        val name = try { device.name ?: device.address } catch (e: SecurityException) { device.address }
        LogManager.i(TAG, "发起配对请求：$name")
        try {
            device.createBond()
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied when pairing device", e)
            LogManager.e(TAG, "配对失败：权限不足")
        }
    }

    fun unpairDevice(device: BluetoothDevice) {
        val name = try { device.name ?: device.address } catch (e: SecurityException) { device.address }
        LogManager.i(TAG, "发起解绑请求：$name")
        try {
            val method = device.javaClass.getMethod("removeBond")
            method.invoke(device)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unpair device", e)
            LogManager.e(TAG, "解绑失败：${e.message}")
        }
    }

    // 在后台线程连接设备，避免主线程IO阻塞崩溃
    fun connectToDevice(device: BluetoothDevice) {
        val name = try { device.name ?: device.address } catch (e: SecurityException) { device.address }
        connectThread?.interrupt()
        connectThread = Thread {
            _connectionState.value = ConnectionState.CONNECTING
            LogManager.i(TAG, "正在连接：$name (${device.address})")
            try {
                socket?.close()
                socket = null
            } catch (e: IOException) {
                Log.e(TAG, "Error closing old socket", e)
            }
            try {
                // 优先用反射创建 socket（解决 HC-05/HC-06 兼容性问题）
                val newSocket = device.createBluetoothSocket()
                if (newSocket == null) {
                    throw IOException("无法创建蓝牙 socket")
                }
                try { bluetoothAdapter?.cancelDiscovery() } catch (e: SecurityException) { /* ignore */ }
                newSocket.connect()
                socket = newSocket
                _connectionState.value = ConnectionState.CONNECTED
                _statusMessage.value = "已连接到 $name"
                LogManager.s(TAG, "连接成功：$name (${device.address})")
                startReadLoop(newSocket)
            } catch (e: IOException) {
                Log.e(TAG, "Connection failed", e)
                _connectionState.value = ConnectionState.ERROR
                _statusMessage.value = "连接失败：${e.message}"
                LogManager.e(TAG, "连接失败：$name - ${e.message}")
                try { socket?.close() } catch (ex: IOException) { /* ignore */ }
                socket = null
            } catch (e: SecurityException) {
                Log.e(TAG, "Permission denied when connecting", e)
                _connectionState.value = ConnectionState.ERROR
                _statusMessage.value = "权限不足，无法连接"
                LogManager.e(TAG, "连接失败：权限不足")
            }
        }.also { it.start() }
    }


    /**
     * 连接成功后调用：启动后台读取循环，持续监听 socket 是否断开。
     * 一旦 inputStream.read() 返回 -1 或抛出 IOException，说明连接已断，立即更新状态。
     */
    private fun startReadLoop(s: BluetoothSocket) {
        readLoopThread?.interrupt()
        val thread = Thread {
            val buffer = ByteArray(1)
            try {
                while (!Thread.currentThread().isInterrupted) {
                    val bytes = s.inputStream.read(buffer)
                    if (bytes == -1) {
                        // 远端关闭连接
                        break
                    }
                    // 可在此处理收到的数据（当前版本暂不处理）
                }
            } catch (e: IOException) {
                if (!Thread.currentThread().isInterrupted) {
                    Log.w(TAG, "ReadLoop: 连接断开 ${e.message}")
                }
            } finally {
                // 只有在当前状态是 CONNECTED 时才更新（避免覆盖主动 disconnect 设置的状态）
                if (_connectionState.value == ConnectionState.CONNECTED) {
                    Log.w(TAG, "ReadLoop: 连接意外断开，更新状态为 DISCONNECTED")
                    LogManager.w(TAG, "蓝牙连接已断开（远端或网络原因）")
                    try { socket?.close() } catch (e: Exception) { /* ignore */ }
                    socket = null
                    _connectionState.value = ConnectionState.DISCONNECTED
                    _statusMessage.value = "连接已断开"
                }
            }
        }
        thread.isDaemon = true
        thread.name = "BT-ReadLoop"
        thread.start()
        readLoopThread = thread
    }

    /**
     * 检查 socket 是否仍然真实连接（防止后台切回来 socket 已断但状态还是 CONNECTED）
     * 注意：BluetoothSocket.isConnected 在断开后仍返回 true，不可信。
     * 改用向 outputStream 写入 0 字节来探测连接是否真实存活。
     */
    fun isSocketAlive(): Boolean {
        val s = socket ?: return false
        if (!s.isConnected) return false
        return try {
            // 尝试写入空字节，若 socket 已断则会抛出 IOException
            s.outputStream.write(ByteArray(0))
            s.outputStream.flush()
            true
        } catch (e: IOException) {
            Log.w(TAG, "Socket 探测写入失败，连接已断开: ${e.message}")
            false
        } catch (e: Exception) {
            Log.w(TAG, "Socket 探测异常: ${e.message}")
            false
        }
    }

    /**
     * 检查连接状态与实际 socket 是否一致，如果已断开则重置状态
     * 返回 true 表示当前连接正常，false 表示连接已断开（状态已重置）
     */
    fun checkAndSyncConnectionState(): Boolean {
        return if (_connectionState.value == ConnectionState.CONNECTED && !isSocketAlive()) {
            LogManager.w(TAG, "检测到连接已断开（socket 失效），重置连接状态")
            try { socket?.close() } catch (e: Exception) { /* ignore */ }
            socket = null
            _connectionState.value = ConnectionState.DISCONNECTED
            _statusMessage.value = "连接已断开"
            false
        } else {
            _connectionState.value == ConnectionState.CONNECTED
        }
    }

    fun disconnect() {
        connectThread?.interrupt()
        autoConnectThread?.interrupt()
        readLoopThread?.interrupt()
        readLoopThread = null
        LogManager.i(TAG, "主动断开连接")
        try {
            socket?.close()
            socket = null
            _connectionState.value = ConnectionState.DISCONNECTED
            _statusMessage.value = "已断开连接"
            LogManager.w(TAG, "连接已断开")
        } catch (e: IOException) {
            Log.e(TAG, "Error disconnecting", e)
            LogManager.e(TAG, "断开失败：${e.message}")
        }
    }

    fun autoConnect(address: String, maxRetries: Int, connectTimeout: Long, delayBetweenRetries: Long) {
        // 如果已经连接或正在连接中，跳过
        val currentState = _connectionState.value
        if (currentState == ConnectionState.CONNECTED && socket?.isConnected == true) {
            LogManager.i(TAG, "已连接，跳过自动连接 → $address")
            return
        }
        if (currentState == ConnectionState.CONNECTING) {
            LogManager.i(TAG, "正在连接中，跳过本次自动连接 → $address")
            return
        }
        // 如果已有连接线程在运行，先中断
        val prevThread = autoConnectThread
        prevThread?.interrupt()
        LogManager.i(TAG, "启动自动连接 → $address，最多重试 $maxRetries 次，间隔 ${delayBetweenRetries / 1000}s")
        val runnable = Runnable {
            var attempts = 0
            while (attempts < maxRetries && _connectionState.value != ConnectionState.CONNECTED) {
                if (Thread.currentThread().isInterrupted) break
                attempts++
                _statusMessage.value = "自动连接中...（第 $attempts/$maxRetries 次）"
                _connectionState.value = ConnectionState.CONNECTING
                LogManager.i(TAG, "自动连接第 $attempts/$maxRetries 次 → $address")
                try {
                    val device = bluetoothAdapter?.getRemoteDevice(address)
                    if (device != null) {
                        try { socket?.close() } catch (e: IOException) { /* ignore */ }
                        val newSocket = device.createBluetoothSocket()
                        if (newSocket == null) {
                            LogManager.e(TAG, "无法创建蓝牙 socket")
                            break
                        }
                        try { bluetoothAdapter?.cancelDiscovery() } catch (e: SecurityException) { /* ignore */ }
                        newSocket.connect()
                        socket = newSocket
                        val devName = try { device.name ?: address } catch (e: SecurityException) { address }
                        _connectionState.value = ConnectionState.CONNECTED
                        _statusMessage.value = "已连接到 $devName"
                        LogManager.s(TAG, "自动连接成功：$devName ($address)")
                        startReadLoop(newSocket)
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Auto-connect attempt $attempts failed", e)
                    LogManager.e(TAG, "第 $attempts 次连接失败：${e.message}")
                    if (attempts < maxRetries && !Thread.currentThread().isInterrupted) {
                        try { Thread.sleep(delayBetweenRetries) } catch (ie: InterruptedException) { break }
                    }
                } catch (e: SecurityException) {
                    Log.e(TAG, "Permission denied during auto-connect", e)
                    LogManager.e(TAG, "自动连接中断：权限不足")
                    break
                }
            }
            if (_connectionState.value != ConnectionState.CONNECTED) {
                _connectionState.value = ConnectionState.ERROR
                _statusMessage.value = "自动连接失败，已重试 $maxRetries 次"
                LogManager.e(TAG, "自动连接全部失败，共重试 $maxRetries 次")
            }
        }
        autoConnectThread = Thread(runnable).also { it.start() }
    }

    fun release() {
        LogManager.i(TAG, "蓝牙控制器释放资源")
        try { context.unregisterReceiver(bluetoothStateReceiver) } catch (e: Exception) { /* ignore */ }
        stopScan()
        disconnect()
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}

data class BluetoothDeviceInfo(
    val name: String,
    val address: String,
    val isPaired: Boolean,
    val device: BluetoothDevice? = null
)

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}
