package com.xiangqi.assistant

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.xiangqi.assistant.databinding.ActivityMainBinding
import com.xiangqi.assistant.service.FloatingWindowService
import com.xiangqi.assistant.service.ScreenCaptureService

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val REQUEST_CODE_OVERLAY = 1001
    private val REQUEST_CODE_SCREEN_CAPTURE = 1002
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        checkPermissions()
    }
    
    private fun setupUI() {
        // 启动悬浮窗按钮
        binding.btnStartFloating.setOnClickListener {
            if (checkOverlayPermission()) {
                startFloatingWindow()
            } else {
                requestOverlayPermission()
            }
        }
        
        // 停止悬浮窗按钮
        binding.btnStopFloating.setOnClickListener {
            stopFloatingWindow()
        }
        
        // 开始屏幕识别按钮
        binding.btnStartCapture.setOnClickListener {
            requestScreenCapture()
        }
        
        // 停止屏幕识别按钮
        binding.btnStopCapture.setOnClickListener {
            stopScreenCapture()
        }
        
        // 使用说明
        binding.tvInstructions.text = """
            使用说明：
            
            1. 点击"启动悬浮窗"开启悬浮窗功能
            2. 打开你的象棋APP
            3. 点击"开始屏幕识别"授权截屏
            4. 悬浮窗会自动识别棋盘并显示最佳走法
            5. 点击悬浮窗可以手动摆棋分析
            
            注意：
            - 首次使用需要授予悬浮窗权限
            - 需要授予截屏权限
            - 识别效果取决于棋盘清晰度
        """.trimIndent()
    }
    
    private fun checkPermissions() {
        if (!checkOverlayPermission()) {
            Toast.makeText(this, "需要悬浮窗权限", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }
    
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_CODE_OVERLAY)
        }
    }
    
    private fun requestScreenCapture() {
        val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(intent, REQUEST_CODE_SCREEN_CAPTURE)
    }
    
    private fun startFloatingWindow() {
        val intent = Intent(this, FloatingWindowService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "悬浮窗已启动", Toast.LENGTH_SHORT).show()
        
        // 更新UI
        binding.btnStartFloating.isEnabled = false
        binding.btnStopFloating.isEnabled = true
    }
    
    private fun stopFloatingWindow() {
        val intent = Intent(this, FloatingWindowService::class.java)
        stopService(intent)
        Toast.makeText(this, "悬浮窗已停止", Toast.LENGTH_SHORT).show()
        
        // 更新UI
        binding.btnStartFloating.isEnabled = true
        binding.btnStopFloating.isEnabled = false
    }
    
    private fun stopScreenCapture() {
        val intent = Intent(this, ScreenCaptureService::class.java)
        stopService(intent)
        Toast.makeText(this, "屏幕识别已停止", Toast.LENGTH_SHORT).show()
        
        // 更新UI
        binding.btnStartCapture.isEnabled = true
        binding.btnStopCapture.isEnabled = false
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            REQUEST_CODE_OVERLAY -> {
                if (checkOverlayPermission()) {
                    Toast.makeText(this, "悬浮窗权限已授予", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "需要悬浮窗权限才能使用", Toast.LENGTH_LONG).show()
                }
            }
            REQUEST_CODE_SCREEN_CAPTURE -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    // 启动截屏服务
                    val intent = Intent(this, ScreenCaptureService::class.java)
                    intent.putExtra("resultCode", resultCode)
                    intent.putExtra("data", data)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }
                    Toast.makeText(this, "屏幕识别已启动", Toast.LENGTH_SHORT).show()
                    
                    // 更新UI
                    binding.btnStartCapture.isEnabled = false
                    binding.btnStopCapture.isEnabled = true
                } else {
                    Toast.makeText(this, "需要截屏权限才能识别棋盘", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
