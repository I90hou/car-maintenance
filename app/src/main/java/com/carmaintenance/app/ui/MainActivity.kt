package com.carmaintenance.app.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.carmaintenance.app.ui.theme.CarMaintenanceTheme

/**
 * 主Activity
 * 启动阶段全链路 try-catch，避免任何异常导致无提示闪退
 */
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 由 Compose 自行处理系统栏内边距，避免软键盘弹出时整个窗口被顶起（标题压到状态栏）
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        try {
            setContent {
                CarMaintenanceTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation()
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e("MainActivity", "setContent 崩溃", e)
            // 如果 Compose 初始化崩了，退回到错误提示界面
            try {
                setFallbackContent(e)
            } catch (_: Throwable) {
                // 真的没招了，让系统处理
                throw e
            }
        }
    }
    
    private fun setFallbackContent(error: Throwable) {
        runOnUiThread {
            setContent {
                MaterialTheme {
                    Surface {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("启动失败：${error.message}\n请升级应用版本")
                        }
                    }
                }
            }
        }
    }
}
