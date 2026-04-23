# AutoBluetoothApp

一个基于 **Kotlin + Jetpack Compose** 的 Android 蓝牙控制应用，支持蓝牙设备扫描、配对、连接及自动重连。

## 功能特性

- 🔍 **设备扫描** — 扫描周围可发现的蓝牙设备
- 📋 **已配对设备** — 列出手机上已配对的所有蓝牙设备
- 🔗 **一键连接** — 点击目标设备即可建立 RFCOMM 连接
- 🔄 **自动重连** — 连接断开后自动尝试重新连接
- ⚡ **配对后自动连** — 开关开启后，配对成功立即自动发起连接
- 📊 **实时状态** — 连接状态实时更新，后台断开后切回前台即时同步

## 截图

> *(可在此处添加应用截图)*

## 环境要求

| 项目 | 要求 |
|------|------|
| Android | 8.0 (API 26) 及以上 |
| 蓝牙 | 经典蓝牙（BT Classic / RFCOMM） |
| 权限 | `BLUETOOTH_CONNECT`、`BLUETOOTH_SCAN`、位置权限 |

## 构建

### 环境
- Android Studio Hedgehog 及以上
- JDK 17
- Kotlin 1.9.22
- Compose Compiler 1.5.10
- Target SDK 34

### 编译 Debug APK

```bash
./gradlew assembleDebug
```

输出路径：`app/build/outputs/apk/debug/app-debug.apk`

### 安装到设备（ADB）

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 项目结构

```
app/src/main/java/com/bluetooth/control/
├── BluetoothController.kt   # 蓝牙核心逻辑（扫描/连接/读写）
├── BluetoothViewModel.kt    # UI 状态管理（StateFlow）
├── MainActivity.kt          # 主界面，生命周期监听
└── ui/
    └── BluetoothScreen.kt   # Compose UI 界面
```

## 技术实现亮点

### 精准连接状态检测
`BluetoothSocket.isConnected` 在 Android 上不可靠（断开后仍返回 `true`）。本应用通过**常驻读取线程**（BT-ReadLoop）持续监听 `inputStream.read()`，连接一旦断开即捕获异常并立刻更新状态，无需轮询探测。

### 响应式 UI 状态
所有连接状态、设备列表均通过 `StateFlow` 暴露给 Compose UI，确保状态变化实时触发界面重组。

### 后台切回自动同步
通过 `LifecycleEventObserver` 监听 `ON_RESUME` 事件，从后台切回前台时检查并同步真实连接状态，若已断开则自动触发重连。

## 权限说明

| 权限 | 用途 |
|------|------|
| `BLUETOOTH_CONNECT` | 连接/断开蓝牙设备（Android 12+） |
| `BLUETOOTH_SCAN` | 扫描附近蓝牙设备（Android 12+） |
| `ACCESS_FINE_LOCATION` | 蓝牙扫描需要位置权限（Android 10 以下） |

## Download

前往 [Releases](https://github.com/acaiblog/AutoBluetoothApp/releases) 页面下载最新 APK。

## License

MIT License
