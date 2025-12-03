# JetLink V2.0
![JetLink Logo](https://img.shields.io/badge/JetLink-v2.0-blue.svg) 
![Platform](https://img.shields.io/badge/Platform-Android-green.svg) 
![Compose](https://img.shields.io/badge/Compose-1.6.5-blue.svg)

**JetLink** 是一个基于 Jetpack Compose 和原生 Java Socket 构建的现代化、高性能的局域网（LAN）即时聊天应用。它旨在提供一个流畅、美观且功能丰富的聊天体验，展示了在 Android 平台上构建响应式、单向数据流应用的最新实践。

---

## ✨ 核心功能 (Features)

JetLink V2.0 带来了全面的功能升级，提供了一个完整的 IM 体验：

-   **富媒体消息**:
    -   **文本与图片**：支持发送纯文本消息和图片。图片在发送前会自动进行压缩并使用 Base64 编码，确保在局域网内的传输效率。
-   **高级交互**:
    -   **引用回复**：长按消息即可引用，被引用的消息会在新消息气泡中以摘要形式展示。
    -   **长按菜单**：提供“复制”、“回复”、“删除”等上下文操作。
-   **实时状态同步**:
    -   **正在输入提示**：当对方正在输入时，聊天界面顶部会实时显示“对方正在输入...”的动画提示，增强了聊天的临场感。
-   **现代化 UI/UX**:
    -   **Material 3 设计**：完全遵循 Material Design 3 规范，提供干净、现代的视觉体验。
    -   **深色模式 & 动态主题**：无缝适配系统深色模式，并在支持的设备上启用动态取色，与壁纸融为一体。
-   **系统级集成**:
    -   **后台通知**：当应用在后台运行时，新消息会以系统通知的形式弹出，确保用户不会错过任何信息。
    -   **错误反馈**：在图片发送失败或网络连接中断时，界面底部会弹出 Snackbar 给予明确的错误提示。

---

## 🛠️ 技术栈 (Tech Stack)

项目采用了 Android 官方推荐的现代化技术栈：

-   **核心语言**: [Kotlin](https://kotlinlang.org/)
-   **UI 框架**: [Jetpack Compose](https://developer.android.com/jetpack/compose) - 用于构建声明式、响应式的用户界面。
-   **架构模式**: **MVVM** + **Repository** Pattern。
-   **数据库**: [Room Database](https://developer.android.com/training/data-storage/room) - 用于本地消息和用户数据的持久化存储。
-   **异步处理**: [Kotlin Coroutines & Flow](https://kotlinlang.org/docs/coroutines-guide.html) - 全面用于处理网络、数据库和 UI 状态更新。
-   **网络通信**: **Java Socket** (TCP) - 实现底层的点对点实时通信。
-   **图片加载**: [Coil (Compose)](https://coil-kt.github.io/coil/) - 高性能的图片加载库，用于在聊天界面流畅地显示和缓存图片。
-   **生命周期管理**: `AndroidViewModel`, `lifecycle-process`。

---

## 🚀 如何运行 (Getting Started)

#### 1. 前置条件

JetLink 是一个局域网应用，其核心依赖于一个 Java Socket 服务端。

-   **服务端**: 应用内置了一个简易的 `SocketServer`。当第一个用户启动 App 时，它会自动在 `8888` 端口开启一个 TCP 服务端。
-   **网络环境**: 为了让设备间能够互相通信，请确保**所有测试设备（模拟器或真机）连接到同一个局域网（Wi-Fi）**。

#### 2. 客户端配置

-   **模拟器之间**: 通常无需配置，它们默认在同一内部网络中 (`10.0.2.2` 指向宿主机)。
-   **真机之间**:
    1.  找到作为服务端的设备（第一个启动 App 的设备）的局域网 IP 地址（例如 `192.168.1.10`）。
    2.  在其他客户端设备的代码中，修改 `app/src/main/java/com/example/jetlink/socket/SocketClientManager.kt` 文件：
        ```kotlin
        private const val SERVER_IP = "192.168.1.10" // 替换为你的服务端 IP
        ```
    3.  重新编译并安装应用。

#### 3. Android 权限

为了完整体验所有功能，请确保授予应用以下权限：

-   **通知 (POST_NOTIFICATIONS)**: 在 Android 13+ 设备上，首次启动时会请求此权限，用于在后台接收消息通知。
-   **相册读取**: 发送图片时，系统会自动弹出相册选择器，通常无需手动请求权限。
