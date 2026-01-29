package com.xiangqi.assistant.vision

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

/**
 * 棋盘识别器
 * 使用 OpenCV 识别屏幕上的象棋棋盘
 */
class BoardRecognizer {
    
    fun recognize(bitmap: Bitmap): String? {
        try {
            // 转换为 OpenCV Mat
            val mat = Mat()
            Utils.bitmapToMat(bitmap, mat)
            
            // 转换为灰度图
            val gray = Mat()
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGB2GRAY)
            
            // 边缘检测
            val edges = Mat()
            Imgproc.Canny(gray, edges, 50.0, 150.0)
            
            // 检测直线（棋盘网格）
            val lines = Mat()
            Imgproc.HoughLinesP(edges, lines, 1.0, Math.PI / 180, 100, 100.0, 10.0)
            
            // 分析直线，找出棋盘区域
            val board = detectBoardGrid(lines)
            
            if (board != null) {
                // 识别棋子
                val position = detectPieces(mat, board)
                return positionToFen(position)
            }
            
            return null
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    private fun detectBoardGrid(lines: Mat): Board? {
        // 简化版：返回固定区域
        // 实际应该分析直线找出棋盘
        return Board(
            x = 100,
            y = 200,
            cellSize = 50
        )
    }
    
    private fun detectPieces(mat: Mat, board: Board): Array<Array<String?>> {
        // 创建空棋盘
        val position = Array(10) { Array<String?>(9) { null } }
        
        // 遍历每个交叉点
        for (row in 0 until 10) {
            for (col in 0 until 9) {
                val x = board.x + col * board.cellSize
                val y = board.y + row * board.cellSize
                
                // 提取该位置的小区域
                val roi = extractROI(mat, x, y, board.cellSize / 2)
                
                // 识别棋子
                val piece = recognizePiece(roi)
                position[row][col] = piece
            }
        }
        
        return position
    }
    
    private fun extractROI(mat: Mat, x: Int, y: Int, size: Int): Mat {
        val rect = Rect(
            maxOf(0, x - size),
            maxOf(0, y - size),
            minOf(size * 2, mat.cols() - x + size),
            minOf(size * 2, mat.rows() - y + size)
        )
        return mat.submat(rect)
    }
    
    private fun recognizePiece(roi: Mat): String? {
        // 简化版：基于颜色判断
        // 实际应该使用 OCR 或 CNN 模型识别棋子
        
        val avgColor = Core.mean(roi)
        
        // 判断是否有棋子（基于亮度）
        if (avgColor.`val`[0] < 100) {
            return null // 没有棋子
        }
        
        // 简单判断红黑方
        // 实际需要更复杂的识别逻辑
        return if (avgColor.`val`[0] > 150) "R" else "r"
    }
    
    private fun positionToFen(position: Array<Array<String?>>): String {
        val fenRows = mutableListOf<String>()
        
        for (row in position) {
            var fenRow = ""
            var empty = 0
            
            for (piece in row) {
                if (piece == null) {
                    empty++
                } else {
                    if (empty > 0) {
                        fenRow += empty.toString()
                        empty = 0
                    }
                    fenRow += piece
                }
            }
            
            if (empty > 0) {
                fenRow += empty.toString()
            }
            
            fenRows.add(fenRow)
        }
        
        return fenRows.joinToString("/") + " w - - 0 1"
    }
    
    data class Board(
        val x: Int,
        val y: Int,
        val cellSize: Int
    )
}
