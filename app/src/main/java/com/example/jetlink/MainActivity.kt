package com.example.jetlink

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.jetlink.data.AppDatabase
import com.example.jetlink.data.repository.ChatRepository
import com.example.jetlink.socket.SocketClientManager
import com.example.jetlink.socket.SocketServer
import com.example.jetlink.ui.screens.ChatScreen
import com.example.jetlink.ui.screens.ConversationListScreen
import com.example.jetlink.ui.theme.JetLinkTheme
import com.example.jetlink.ui.viewmodel.ChatViewModel
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 模拟当前登录用户
        // 使用 SharedPreferences 持久化 UserID，确保 App 重启或配置变更（如旋转屏幕）后
        // 当前用户的身份保持一致，从而保证"发送者"气泡逻辑正确。
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val currentUserId = sharedPref.getString("user_id", null) ?: run {
            // 第一次启动生成随机 ID (4位随机数)
            val newId = "user_${Random.nextInt(1000, 9999)}"
            sharedPref.edit { putString("user_id", newId) }
            newId
        }
        Log.d("MainActivity", "Current User ID: $currentUserId")

        // 1. 启动服务端 (可选，如果是 P2P 或作为主机)
        // 这里为了演示方便，我们在 App 启动时尝试启动 Server
        // 如果端口被占用 (例如开了两个模拟器)，可能会抛出异常，SocketServer 内部会处理
        SocketServer.startServer()

        // 2. 初始化数据层
        val database = AppDatabase.getDatabase(this)
        val chatRepository = ChatRepository(database.chatDao())
        
        // 3. 初始化并连接 Socket 客户端
        val socketManager = SocketClientManager(chatRepository, currentUserId)
        chatRepository.setSocketManager(socketManager)
        socketManager.connect()

        setContent {
            JetLinkTheme {
                val navController = rememberNavController()
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = when {
                                        currentRoute?.startsWith("chat") == true -> "Chat ($currentUserId)"
                                        else -> "JetLink ($currentUserId)"
                                    }
                                )
                            },
                            navigationIcon = {
                                if (currentRoute?.startsWith("chat") == true) {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back"
                                        )
                                    }
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "conversations",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        // 1. 会话列表页
                        composable("conversations") {
                            val recentSessions by chatRepository.getRecentSessionsStream()
                                .collectAsState(initial = emptyList())

                            ConversationListScreen(
                                recentMessages = recentSessions,
                                onConversationClick = { sessionId ->
                                    navController.navigate("chat/$sessionId")
                                }
                            )
                        }

                        // 2. 聊天详情页
                        composable(
                            route = "chat/{sessionId}",
                            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
                            
                            val viewModel: ChatViewModel = viewModel(
                                key = sessionId, // 关键修复：使用 sessionId 作为 Key，确保不同会话创建不同的 ViewModel 实例
                                factory = ChatViewModel.Factory(
                                    repository = chatRepository,
                                    sessionId = sessionId,
                                    userId = currentUserId
                                )
                            )

                            ChatScreen(
                                viewModel = viewModel,
                                currentUserId = currentUserId
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 资源释放
        SocketServer.stopServer()
    }
}
