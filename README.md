# JetLink - 局域网聊天应用 (Android Engineer Camp Assignment)

JetLink 是一个基于 Modern Android Development (MAD) 标准开发的 Android 局域网聊天应用。它展示了如何使用最新的 Jetpack 组件构建一个具备本地存储和网络通信功能的即时通讯软件。

## 🛠 技术栈 (Tech Stack)

本项目严格遵守 **MVVM** 架构，并使用以下技术：

*   **语言**: Kotlin
*   **UI 框架**: Jetpack Compose (Material3)
*   **架构模式**: MVVM (Model-View-ViewModel)
*   **本地数据库**: Android Jetpack Room (配合 KSP)
*   **异步处理**: Kotlin Coroutines & Flow
*   **网络通信**: Java Socket (TCP)
*   **导航**: Navigation Compose
*   **序列化**: Gson
*   **依赖注入**: 手动注入 (Manual DI)

## 🚀 如何运行 (How to Run)

1.  **环境要求**:
    *   Android Studio Ladybug 或更新版本
    *   JDK 11+
    *   CompileSdk: 36
    *   MinSdk: 24

2.  **运行步骤**:
    *   将项目克隆到本地并使用 Android Studio 打开。
    *   等待 Gradle Sync 完成。
    *   连接 Android 模拟器或真机。
    *   点击 **Run** 按钮。

3.  **通信测试说明**:
    *   **单机/模拟器测试**: App 启动时会在本地开启一个端口为 `8888` 的 Socket Server。默认的 Client 逻辑会尝试连接 `10.0.2.2` (模拟器访问宿主机的 Localhost)。您可以在模拟器上自发自收，或者开启多个模拟器实例（需配置端口转发）。
    *   **局域网真机测试**:
        1.  确保两台手机连接到同一个 WiFi。
        2.  修改 `SocketClientManager.kt` 中的 `SERVER_IP` 为其中一台手机（作为 Server）的局域网 IP 地址。
        3.  在该手机上启动 App（它会作为 Server 监听）。
        4.  在另一台手机上启动 App（它作为 Client 连接）。

## 📐 设计思路文档

### 1. MVVM 架构应用

本项目采用了经典的 MVVM 分层架构，实现了 UI 与数据逻辑的解耦：

*   **Model (Data Layer)**:
    *   **Entity**: 定义了 `UserEntity` 和 `MessageEntity`，映射到 Room 数据库表。
    *   **DAO**: `ChatDao` 提供了对 SQLite 数据库的读写接口，返回 `Flow` 以支持响应式数据流。
    *   **Repository**: `ChatRepository` 是单一数据源（SSOT）。它封装了本地数据库 (`ChatDao`) 和网络层 (`SocketClientManager`)。
        *   *读取*: UI 观察 Repository 提供的 Flow，数据来源是本地数据库（网络消息收到后先存库，库的变动自动通知 UI）。
        *   *写入*: 发送消息时，Repository 先将消息存入本地，同时通过 Socket 发送给服务端。
*   **ViewModel**:
    *   `ChatViewModel`: 持有 UI 状态 (`StateFlow<List<MessageEntity>>`)。它负责处理业务逻辑（如发送消息），并将 UI 事件转换为数据层操作。它不持有任何 View 的引用，保证了生命周期的安全性。
*   **View (UI Layer)**:
    *   使用 **Jetpack Compose** 构建声明式 UI。
    *   `MainActivity`: 负责导航图 (`NavHost`) 的配置和依赖注入。
    *   `ChatScreen` & `ConversationListScreen`: 纯粹的 UI 组件，根据 ViewModel 暴露的状态进行渲染。

### 2. Socket 通信协议

为了简化开发，项目自定义了一个轻量级的 JSON 通信协议。

**消息格式**:
```json
{
  "type": "MSG",
  "from": "user_1234",
  "content": "Hello World"
}
```

*   `type`: 消息类型，目前固定为 `MSG`，可扩展 `IMAGE` 或 `HEARTBEAT`。
*   `from`: 发送者的唯一标识 ID (由 App 首次启动时随机生成并持久化)。
*   `content`: 文本消息内容。

**通信流程**:
1.  **连接**: Client 连接 Server 的 8888 端口。
2.  **发送**: Client 将对象序列化为 JSON 字符串发送。
3.  **转发**: Server 收到消息后，将其广播给所有当前连接的 Client。
4.  **接收**: Client 收到 JSON，反序列化并存入本地 Room 数据库，UI 通过 Flow 自动更新。

## 📂 代码结构树

```text
com.example.jetlink
├── data                 // 数据层
│   ├── dao              // Room DAO 接口
│   │   └── ChatDao.kt
│   ├── entity           // 数据库实体类
│   │   ├── MessageEntity.kt
│   │   └── UserEntity.kt
│   ├── repository       // 数据仓库
│   │   └── ChatRepository.kt
│   └── AppDatabase.kt   // Room 数据库入口
├── model                // 业务/网络模型
│   └── SocketMessage.kt
├── socket               // 网络层
│   ├── SocketClientManager.kt // 客户端逻辑 (连接/收发)
│   └── SocketServer.kt        // 服务端逻辑 (监听/广播)
├── ui                   // UI 层
│   ├── components       // 通用 UI 组件
│   │   ├── ChatBubble.kt      // 聊天气泡
│   │   └── ChatInputBar.kt    // 输入栏(含表情)
│   ├── screens          // 页面
│   │   ├── ChatScreen.kt             // 聊天详情页
│   │   └── ConversationListScreen.kt // 会话列表页
│   ├── theme            // Compose 主题
│   │   └── ...
│   └── viewmodel        // ViewModel
│       └── ChatViewModel.kt
└── MainActivity.kt      // 程序入口 / 导航配置
```

## 🔮 后期优化思路 (Future Improvements)

1.  **网络自动发现 (Service Discovery)**:
    *   目前 IP 是硬编码的。可以使用 UDP 广播 (NSD) 来自动发现局域网内的 Server IP，实现真正的零配置连接。
2.  **多媒体消息**:
    *   扩展 `MessageEntity` 和 Socket 协议，支持图片、语音消息的发送（需处理 Base64 编码或文件流传输）。
3.  **消息状态回执**:
    *   引入 `ACK` 机制，实现消息的“发送中”、“已发送”、“已读”状态管理。
4.  **依赖注入框架**:
    *   引入 **Hilt** 来管理依赖，替换目前的 `Manual DI`，使代码更易于测试和维护。
5.  **UI 美化**:
    *   添加头像选择功能。
    *   优化转场动画和夜间模式适配。
