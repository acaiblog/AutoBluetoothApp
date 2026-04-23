package com.bluetooth.control

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "bluetooth_control_settings"
        private const val KEY_SCAN_TIME = "scan_time"
        private const val KEY_CONNECT_TIMEOUT = "connect_timeout"
        private const val KEY_RETRY_COUNT = "retry_count"
        private const val KEY_RETRY_DELAY = "retry_delay"
        private const val KEY_AUTO_CONNECT = "auto_connect"
        private const val KEY_AUTO_CONNECT_ON_PAIR = "auto_connect_on_pair"
        private const val KEY_TARGET_DEVICE = "target_device"
        private const val KEY_TARGET_DEVICE_ADDRESS = "target_device_address"
    }

    // 扫描时间（秒）
    var scanTime: Int
        get() = prefs.getInt(KEY_SCAN_TIME, 10)
        set(value) = prefs.edit().putInt(KEY_SCAN_TIME, value).apply()

    // 连接超时（秒）
    var connectTimeout: Int
        get() = prefs.getInt(KEY_CONNECT_TIMEOUT, 10)
        set(value) = prefs.edit().putInt(KEY_CONNECT_TIMEOUT, value).apply()

    // 重试次数
    var retryCount: Int
        get() = prefs.getInt(KEY_RETRY_COUNT, 3)
        set(value) = prefs.edit().putInt(KEY_RETRY_COUNT, value).apply()

    // 重试间隔（秒）
    var retryDelay: Int
        get() = prefs.getInt(KEY_RETRY_DELAY, 5)
        set(value) = prefs.edit().putInt(KEY_RETRY_DELAY, value).apply()

    // 自动连接开关
    var autoConnect: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CONNECT, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CONNECT, value).apply()

    // 配对成功后自动连接
    var autoConnectOnPair: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CONNECT_ON_PAIR, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CONNECT_ON_PAIR, value).apply()

    // 目标设备名称
    var targetDeviceName: String?
        get() = prefs.getString(KEY_TARGET_DEVICE, null)
        set(value) = prefs.edit().putString(KEY_TARGET_DEVICE, value).apply()

    // 目标设备地址
    var targetDeviceAddress: String?
        get() = prefs.getString(KEY_TARGET_DEVICE_ADDRESS, null)
        set(value) = prefs.edit().putString(KEY_TARGET_DEVICE_ADDRESS, value).apply()

    fun saveTargetDevice(name: String, address: String) {
        targetDeviceName = name
        targetDeviceAddress = address
    }

    fun clearTargetDevice() {
        prefs.edit()
            .remove(KEY_TARGET_DEVICE)
            .remove(KEY_TARGET_DEVICE_ADDRESS)
            .apply()
    }

    fun getTargetDevice(): Pair<String, String>? {
        val name = targetDeviceName
        val address = targetDeviceAddress
        return if (name != null && address != null) {
            Pair(name, address)
        } else {
            null
        }
    }
}

// 设置选项数据类
data class SettingOption(
    val label: String,
    val value: Int
)

// 设置选项列表
object SettingOptions {
    val scanTimeOptions = listOf(
        SettingOption("5秒", 5),
        SettingOption("10秒", 10),
        SettingOption("15秒", 15),
        SettingOption("20秒", 20),
        SettingOption("30秒", 30),
        SettingOption("60秒", 60)
    )

    val connectTimeoutOptions = listOf(
        SettingOption("5秒", 5),
        SettingOption("10秒", 10),
        SettingOption("15秒", 15),
        SettingOption("20秒", 20),
        SettingOption("30秒", 30)
    )

    val retryCountOptions = listOf(
        SettingOption("1次", 1),
        SettingOption("2次", 2),
        SettingOption("3次", 3),
        SettingOption("5次", 5),
        SettingOption("10次", 10)
    )

    val retryDelayOptions = listOf(
        SettingOption("3秒", 3),
        SettingOption("5秒", 5),
        SettingOption("10秒", 10),
        SettingOption("15秒", 15),
        SettingOption("30秒", 30)
    )
}
