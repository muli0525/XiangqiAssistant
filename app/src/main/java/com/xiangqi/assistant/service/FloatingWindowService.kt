package com.xiangqi.assistant.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.xiangqi.assistant.MainActivity
import com.xiangqi.assistant.R
import com.xiangqi.assistant.engine.PikafishEngine
import com.xiangqi.assistant.view.ChessBoardView
import kotlinx.coroutines.*

class FloatingWindowService : Service() {
    
    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private lateinit var engine: PikafishEngine
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private var chessBoard: ChessBoardView? = null
    private var tvStatus: TextView? = null
    private var tvBestMove: TextView? = null
    
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FloatingWindowService onCreate")
        Toast.makeText(this, "悬浮窗服务已创建", Toast.LENGTH_SHORT).show()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        engine = PikafishEngine(this)
        
        // 创建通知渠道（Android 8.0+需要）
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "FloatingWindowService onStartCommand")
        Toast.makeText(this, "开始创建悬浮窗...", Toast.LENGTH_SHORT).show()
        
        // 启动前台服务，防止被系统杀掉
        startForeground(NOTIFICATION_ID, createNotification())
        
        if (floatingView == null) {
            createFloatingWindow()
        }
        return START_STICKY
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "象棋助手悬浮窗",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持悬浮窗服务运行"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "通知渠道已创建")
        }
    }
    
    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("象棋助手")
            .setContentText("悬浮窗正在运行")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun createFloatingWindow() {
        try {
            Log.d(TAG, "开始创建悬浮窗布局")
            Toast.makeText(this, "正在创建悬浮窗布局...", Toast.LENGTH_LONG).show()
            
            // 检查悬浮窗权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!android.provider.Settings.canDrawOverlays(this)) {
                    Log.e(TAG, "没有悬浮窗权限！")
                    Toast.makeText(this, "错误：没有悬浮窗权限！", Toast.LENGTH_LONG).show()
                    stopSelf()
                    return
                }
            }
            Log.d(TAG, "悬浮窗权限检查通过")
            
            // 创建悬浮窗布局
            floatingView = LayoutInflater.from(this).inflate(R.layout.floating_window, null)
            Log.d(TAG, "布局加载成功")
            
            // 设置悬浮窗参数
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
            
            params.gravity = Gravity.TOP or Gravity.START
            params.x = 100
            params.y = 100
            
            Log.d(TAG, "准备添加视图到窗口管理器，类型: $layoutType")
            
            // 添加到窗口
            windowManager.addView(floatingView, params)
            
            Log.d(TAG, "悬浮窗已添加到窗口管理器！")
            Toast.makeText(this, "✓ 悬浮窗已显示！", Toast.LENGTH_LONG).show()
            
            // 设置拖动
            setupDragging(params)
            
            // 设置点击事件
            setupClickListeners()
            
            // 显示初始状态
            updateStatus("悬浮窗已启动")
            chessBoard?.setInitialPosition()
            updateBestMove("等待分析...")
            
        } catch (e: SecurityException) {
            Log.e(TAG, "安全异常：可能没有悬浮窗权限", e)
            Toast.makeText(this, "错误：需要悬浮窗权限！", Toast.LENGTH_LONG).show()
            stopSelf()
        } catch (e: Exception) {
            Log.e(TAG, "创建悬浮窗失败", e)
            Toast.makeText(this, "创建悬浮窗失败: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            stopSelf()
        }
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
        
        floatingView?.findViewById<Button>(R.id.btn_analyze)?.setOnClickListener {
            analyzePosition()
        }
        
        // 获取View引用
        chessBoard = floatingView?.findViewById(R.id.chess_board)
        tvStatus = floatingView?.findViewById(R.id.tv_status)
        tvBestMove = floatingView?.findViewById(R.id.tv_best_move)
    }
    
    private fun analyzePosition() {
        serviceScope.launch {
            try {
                updateStatus("正在分析...")
                
                // 获取当前棋盘局面（从屏幕识别服务）
                val fen = ScreenCaptureService.currentPosition
                
                if (fen != null) {
                    // 更新棋盘显示
                    chessBoard?.setPositionFromFen(fen)
                    
                    // 使用引擎分析
                    val result = withContext(Dispatchers.IO) {
                        engine.analyze(fen, depth = 20)
                    }
                    
                    // 更新走法显示
                    if (result.isNotEmpty()) {
                        updateBestMove(result[0])
                        updateStatus("分析完成")
                    } else {
                        updateStatus("未找到走法")
                    }
                } else {
                    updateStatus("未识别到棋盘")
                }
            } catch (e: Exception) {
                updateStatus("分析失败: ${e.message}")
            }
        }
    }
    
    private fun updateBestMove(move: String) {
        tvBestMove?.text = "最佳: $move"
    }
    
    private fun updateStatus(status: String) {
        tvStatus?.text = status
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "FloatingWindowService onDestroy")
        Toast.makeText(this, "悬浮窗服务已停止", Toast.LENGTH_SHORT).show()
        floatingView?.let { 
            try {
                windowManager.removeView(it)
                Log.d(TAG, "悬浮窗已移除")
            } catch (e: Exception) {
                Log.e(TAG, "移除悬浮窗失败", e)
            }
        }
        serviceScope.cancel()
        engine.quit()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    companion object {
        private const val TAG = "FloatingWindowService"
        private const val CHANNEL_ID = "xiangqi_assistant_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
