package com.xiangqi.assistant.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 数据管理器单例
 * 用于在服务之间安全地传递数据
 */
object DataManager {
    
    private val _currentPosition = MutableStateFlow<String?>(null)
    val currentPosition: StateFlow<String?> = _currentPosition.asStateFlow()
    
    /**
     * 更新棋盘局面
     */
    fun updatePosition(fen: String?) {
        _currentPosition.value = fen
    }
    
    /**
     * 获取当前局面
     */
    fun getCurrentPosition(): String? {
        return _currentPosition.value
    }
    
    /**
     * 清除当前局面
     */
    fun clearPosition() {
        _currentPosition.value = null
    }
}
