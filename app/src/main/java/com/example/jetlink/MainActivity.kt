package com.example.jetlink

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.jetlink.data.AppDatabase
import com.example.jetlink.data.repository.ChatRepository
import com.example.jetlink.socket.SocketClientManager
import com.example.jetlink.socket.SocketServer
import com.example.jetlink.ui.screens.ChatScreen
import com.example.jetlink.ui.screens.ConversationListScreen
import com.example.jetlink.ui.screens.ProfileScreen
import com.example.jetlink.ui.theme.JetLinkTheme
import com.example.jetlink.ui.viewmodel.ChatViewModel
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    
    // 注册权限请求 Launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
        } else {
            Log.w("MainActivity", "Notification permission denied")
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 检查并请求通知权限 (Android 13+)
        checkNotificationPermission()

        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val currentUserId = sharedPref.getString("user_id", null) ?: run {
            val newId = "user_${Random.nextInt(1000, 9999)}"
            sharedPref.edit { putString("user_id", newId) }
            newId
        }
        Log.d("MainActivity", "Current User ID: $currentUserId")

        SocketServer.startServer()

        val database = AppDatabase.getDatabase(this)
        val chatRepository = ChatRepository(database.chatDao(), this)
        val socketManager = SocketClientManager(this, currentUserId)
        chatRepository.setSocketManager(socketManager)
        socketManager.connect()

        setContent {
            JetLinkTheme {
                val navController = rememberNavController()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
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
                                key = sessionId,
                                factory = ChatViewModel.Factory(
                                    repository = chatRepository,
                                    sessionId = sessionId,
                                    userId = currentUserId
                                )
                            )

                            ChatScreen(
                                viewModel = viewModel,
                                currentUserId = currentUserId,
                                onBackClick = { navController.popBackStack() },
                                onHeaderClick = { navController.navigate("profile/$sessionId") }
                            )
                        }

                        // 3. 用户详情页
                        composable(
                            route = "profile/{userId}",
                            arguments = listOf(navArgument("userId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                            
                            ProfileScreen(
                                userId = userId,
                                onBackClick = { navController.popBackStack() },
                                onClearHistoryClick = { 
                                    chatRepository.clearHistory(userId) 
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // 请求权限
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SocketServer.stopServer()
    }
}
