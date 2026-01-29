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
        // 悬浮窗不需要前台服务通知
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "FloatingWindowService onStartCommand")
        Toast.makeText(this, "开始创建悬浮窗...", Toast.LENGTH_SHORT).show()
        if (floatingView == null) {
            createFloatingWindow()
        }
        return START_STICKY
    }
    
    private fun createFloatingWindow() {
        try {
            Log.d(TAG, "开始创建悬浮窗布局")
            Toast.makeText(this, "正在创建悬浮窗布局...", Toast.LENGTH_SHORT).show()
            
            // 创建悬浮窗布局
            floatingView = LayoutInflater.from(this).inflate(R.layout.floating_window, null)
            Log.d(TAG, "布局加载成功")
            
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
            
            Log.d(TAG, "准备添加视图到窗口管理器")
            // 添加到窗口
            windowManager.addView(floatingView, params)
            Log.d(TAG, "悬浮窗已添加到窗口管理器")
            Toast.makeText(this, "悬浮窗已显示！", Toast.LENGTH_LONG).show()
            
            // 设置拖动
            setupDragging(params)
            
            // 设置点击事件
            setupClickListeners()
            
            // 显示初始状态
            updateStatus("悬浮窗已启动 (模拟模式)")
            chessBoard?.setInitialPosition()
            updateBestMove("车二平五")
            
        } catch (e: Exception) {
            Log.e(TAG, "创建悬浮窗失败", e)
            Toast.makeText(this, "创建悬浮窗失败: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            // 如果创建失败，停止服务
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
        floatingView?.let { windowManager.removeView(it) }
        serviceScope.cancel()
        engine.quit()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    companion object {
        private const val TAG = "FloatingWindowService"
    }
}
