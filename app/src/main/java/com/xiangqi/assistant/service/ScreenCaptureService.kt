package com.xiangqi.assistant.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.xiangqi.assistant.MainActivity
import com.xiangqi.assistant.R
import com.xiangqi.assistant.data.DataManager
import com.xiangqi.assistant.vision.BoardRecognizer
import kotlinx.coroutines.*

class ScreenCaptureService : Service() {
    
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val boardRecognizer = BoardRecognizer()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var captureJob: Job? = null
    private var isCapturing = false
    
    companion object {
        private const val TAG = "ScreenCaptureService"
        private const val CHANNEL_ID = "screen_capture_channel"
        private const val NOTIFICATION_ID = 1002
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val data = intent?.getParcelableExtra<Intent>("data")
        
        if (resultCode == Activity.RESULT_OK && data != null) {
            startScreenCapture(resultCode, data)
        } else {
            // 停止截屏
            stopScreenCapture()
        }
        
        return START_STICKY
    }
    
    private fun startScreenCapture(resultCode: Int, data: Intent) {
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
        
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi
        
        imageReader = ImageReader.newInstance(width, height, android.graphics.PixelFormat.RGBA_8888, 2)
        
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )
        
        // 开始定时截图识别
        startPeriodicCapture()
    }
    
    private fun stopScreenCapture() {
        isCapturing = false
        captureJob?.cancel()
        captureJob = null
    }
    
    private fun startPeriodicCapture() {
        isCapturing = true
        captureJob = serviceScope.launch {
            while (isActive && isCapturing) {
                try {
                    captureAndRecognize()
                    delay(1000) // 每秒识别一次
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    private fun captureAndRecognize() {
        imageReader?.acquireLatestImage()?.use { image ->
            val bitmap = imageToBitmap(image)
            
            // 识别棋盘
            serviceScope.launch(Dispatchers.IO) {
                val result = boardRecognizer.recognize(bitmap)
                result.onSuccess { fen ->
                    DataManager.updatePosition(fen)
                }.onFailure { e ->
                    Log.e(TAG, "识别棋盘失败", e)
                }
            }
        }
    }
    
    private fun imageToBitmap(image: Image): Bitmap {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width
        
        // 如果没有行填充，直接创建位图
        if (rowPadding == 0) {
            val bitmap = Bitmap.createBitmap(
                image.width,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            buffer.rewind()
            bitmap.copyPixelsFromBuffer(buffer)
            return bitmap
        }
        
        // 如果有行填充，需要手动处理每一行
        val bitmap = Bitmap.createBitmap(
            image.width,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        
        buffer.rewind()
        val pixels = IntArray(image.width * image.height)
        
        for (row in 0 until image.height) {
            for (col in 0 until image.width) {
                val index = row * image.width + col
                val bufferIndex = row * rowStride + col * pixelStride
                pixels[index] = buffer.getInt(bufferIndex)
            }
        }
        
        bitmap.setPixels(pixels, 0, image.width, 0, 0, image.width, image.height)
        return bitmap
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            
            // 检查渠道是否已存在
            if (notificationManager.getNotificationChannel(CHANNEL_ID) != null) {
                return
            }
            
            val channel = NotificationChannel(
                CHANNEL_ID,
                "屏幕识别服务",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
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
            .setContentText("正在识别屏幕...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopScreenCapture()
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        mediaProjection?.stop()
        mediaProjection = null
        DataManager.clearPosition()
        serviceScope.cancel()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
