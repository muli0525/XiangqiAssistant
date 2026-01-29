package com.xiangqi.assistant.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.xiangqi.assistant.MainActivity
import com.xiangqi.assistant.R
import com.xiangqi.assistant.engine.PikafishEngine
import kotlinx.coroutines.*

class FloatingWindowService : Service() {
    
    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private lateinit var engine: PikafishEngine
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        engine = PikafishEngine(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (floatingView == null) {
            createFloatingWindow()
        }
        return START_STICKY
    }
    
    private fun createFloatingWindow() {
        // 创建悬浮窗布局
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_window, null)
        
        // 设置悬浮窗参数
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 100
        
        // 添加到窗口
        windowManager.addView(floatingView, params)
        
        // 设置拖动
        setupDragging(params)
        
        // 设置点击事件
        setupClickListeners()
    }
    
    private fun setupDragging(params: WindowManager.LayoutParams) {
        floatingView?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(floatingView, params)
                    true
                }
                else -> false
            }
        }
    }
    
    private fun setupClickListeners() {
        floatingView?.findViewById<View>(R.id.btn_close)?.setOnClickListener {
            stopSelf()
        }
        
        floatingView?.findViewById<View>(R.id.btn_analyze)?.setOnClickListener {
            analyzePosition()
        }
    }
    
    private fun analyzePosition() {
        serviceScope.launch {
            try {
                // 获取当前棋盘局面（从屏幕识别服务）
                val fen = ScreenCaptureService.currentPosition
                
                if (fen != null) {
                    // 使用引擎分析
                    val result = withContext(Dispatchers.IO) {
                        engine.analyze(fen, depth = 20)
                    }
                    
                    // 更新悬浮窗显示
                    updateMoves(result)
                } else {
                    updateStatus("未识别到棋盘")
                }
            } catch (e: Exception) {
                updateStatus("分析失败: ${e.message}")
            }
        }
    }
    
    private fun updateMoves(moves: List<String>) {
        floatingView?.findViewById<TextView>(R.id.tv_move1)?.text = 
            if (moves.isNotEmpty()) "1. ${moves[0]}" else "1. ---"
        floatingView?.findViewById<TextView>(R.id.tv_move2)?.text = 
            if (moves.size > 1) "2. ${moves[1]}" else "2. ---"
        floatingView?.findViewById<TextView>(R.id.tv_move3)?.text = 
            if (moves.size > 2) "3. ${moves[2]}" else "3. ---"
    }
    
    private fun updateStatus(status: String) {
        floatingView?.findViewById<TextView>(R.id.tv_status)?.text = status
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "悬浮窗服务",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("象棋辅助工具")
            .setContentText("悬浮窗正在运行")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let { windowManager.removeView(it) }
        serviceScope.cancel()
        engine.quit()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    companion object {
        private const val CHANNEL_ID = "floating_window_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
