package com.xiangqi.assistant.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/**
 * 象棋棋盘视图
 * 用于悬浮窗显示小棋盘
 */
class ChessBoardView : View {
    
    constructor(context: Context) : super(context) {
        init()
    }
    
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }
    
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private val boardPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val piecePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    private var cellSize = 0f
    private var boardLeft = 0f
    private var boardTop = 0f
    
    // 棋盘状态：9列10行
    private var board = Array(10) { Array<String?>(9) { null } }
    
    // 最佳走法（起点和终点）
    private var bestMoveFrom: Pair<Int, Int>? = null
    private var bestMoveTo: Pair<Int, Int>? = null
    
    private fun init() {
        // 棋盘背景
        boardPaint.color = Color.parseColor("#F5DEB3")
        boardPaint.style = Paint.Style.FILL
        
        // 线条
        linePaint.color = Color.BLACK
        linePaint.strokeWidth = 2f
        linePaint.style = Paint.Style.STROKE
        
        // 文字
        textPaint.color = Color.BLACK
        textPaint.textAlign = Paint.Align.CENTER
        
        // 棋子
        piecePaint.textAlign = Paint.Align.CENTER
        piecePaint.style = Paint.Style.FILL
        
        // 高亮
        highlightPaint.color = Color.parseColor("#8000FF00")
        highlightPaint.style = Paint.Style.FILL
        
        // 设置初始局面
        setInitialPosition()
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = (width * 1.1f).toInt() // 棋盘高度略大于宽度
        setMeasuredDimension(width, height)
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        val padding = w * 0.05f
        val boardWidth = w - padding * 2
        cellSize = boardWidth / 8 // 8个格子宽度
        
        boardLeft = padding
        boardTop = padding
        
        textPaint.textSize = cellSize * 0.6f
        piecePaint.textSize = cellSize * 0.7f
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // 绘制棋盘背景
        canvas.drawRect(
            boardLeft,
            boardTop,
            boardLeft + cellSize * 8,
            boardTop + cellSize * 9,
            boardPaint
        )
        
        // 绘制网格线
        drawGrid(canvas)
        
        // 绘制河界
        drawRiver(canvas)
        
        // 绘制九宫格
        drawPalace(canvas)
        
        // 绘制最佳走法高亮
        drawBestMoveHighlight(canvas)
        
        // 绘制棋子
        drawPieces(canvas)
    }
    
    private fun drawGrid(canvas: Canvas) {
        // 横线
        for (i in 0..9) {
            val y = boardTop + i * cellSize
            canvas.drawLine(
                boardLeft,
                y,
                boardLeft + cellSize * 8,
                y,
                linePaint
            )
        }
        
        // 竖线
        for (i in 0..8) {
            val x = boardLeft + i * cellSize
            // 上半部分
            canvas.drawLine(x, boardTop, x, boardTop + cellSize * 4, linePaint)
            // 下半部分
            canvas.drawLine(x, boardTop + cellSize * 5, x, boardTop + cellSize * 9, linePaint)
        }
    }
    
    private fun drawRiver(canvas: Canvas) {
        val riverY = boardTop + cellSize * 4.5f
        textPaint.textSize = cellSize * 0.4f
        canvas.drawText("楚河", boardLeft + cellSize * 2, riverY, textPaint)
        canvas.drawText("汉界", boardLeft + cellSize * 6, riverY, textPaint)
        textPaint.textSize = cellSize * 0.6f
    }
    
    private fun drawPalace(canvas: Canvas) {
        // 上方九宫格
        canvas.drawLine(
            boardLeft + cellSize * 3,
            boardTop,
            boardLeft + cellSize * 5,
            boardTop + cellSize * 2,
            linePaint
        )
        canvas.drawLine(
            boardLeft + cellSize * 5,
            boardTop,
            boardLeft + cellSize * 3,
            boardTop + cellSize * 2,
            linePaint
        )
        
        // 下方九宫格
        canvas.drawLine(
            boardLeft + cellSize * 3,
            boardTop + cellSize * 7,
            boardLeft + cellSize * 5,
            boardTop + cellSize * 9,
            linePaint
        )
        canvas.drawLine(
            boardLeft + cellSize * 5,
            boardTop + cellSize * 7,
            boardLeft + cellSize * 3,
            boardTop + cellSize * 9,
            linePaint
        )
    }
    
    private fun drawBestMoveHighlight(canvas: Canvas) {
        bestMoveFrom?.let { (row, col) ->
            val x = boardLeft + col * cellSize
            val y = boardTop + row * cellSize
            canvas.drawCircle(x, y, cellSize * 0.4f, highlightPaint)
        }
        
        bestMoveTo?.let { (row, col) ->
            val x = boardLeft + col * cellSize
            val y = boardTop + row * cellSize
            canvas.drawCircle(x, y, cellSize * 0.4f, highlightPaint)
        }
    }
    
    private fun drawPieces(canvas: Canvas) {
        for (row in 0..9) {
            for (col in 0..8) {
                board[row][col]?.let { piece ->
                    drawPiece(canvas, row, col, piece)
                }
            }
        }
    }
    
    private fun drawPiece(canvas: Canvas, row: Int, col: Int, piece: String) {
        val x = boardLeft + col * cellSize
        val y = boardTop + row * cellSize
        
        // 判断红黑方
        val isRed = piece[0].isUpperCase()
        
        // 绘制棋子圆圈
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        circlePaint.color = if (isRed) Color.parseColor("#FFE4E1") else Color.parseColor("#F0F0F0")
        circlePaint.style = Paint.Style.FILL
        canvas.drawCircle(x, y, cellSize * 0.35f, circlePaint)
        
        // 绘制边框
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        borderPaint.color = if (isRed) Color.RED else Color.BLACK
        borderPaint.strokeWidth = 3f
        borderPaint.style = Paint.Style.STROKE
        canvas.drawCircle(x, y, cellSize * 0.35f, borderPaint)
        
        // 绘制棋子文字
        piecePaint.color = if (isRed) Color.RED else Color.BLACK
        val text = getPieceText(piece)
        val textBounds = Rect()
        piecePaint.getTextBounds(text, 0, text.length, textBounds)
        canvas.drawText(text, x, y - textBounds.exactCenterY(), piecePaint)
    }
    
    private fun getPieceText(piece: String): String {
        return when (piece.uppercase()) {
            "K" -> "将"
            "A" -> "士"
            "B" -> "象"
            "N" -> "马"
            "R" -> "车"
            "C" -> "炮"
            "P" -> "兵"
            else -> piece
        }
    }
    
    /**
     * 设置初始局面
     */
    fun setInitialPosition() {
        board = arrayOf(
            arrayOf("r", "n", "b", "a", "k", "a", "b", "n", "r"),
            arrayOf(null, null, null, null, null, null, null, null, null),
            arrayOf(null, "c", null, null, null, null, null, "c", null),
            arrayOf("p", null, "p", null, "p", null, "p", null, "p"),
            arrayOf(null, null, null, null, null, null, null, null, null),
            arrayOf(null, null, null, null, null, null, null, null, null),
            arrayOf("P", null, "P", null, "P", null, "P", null, "P"),
            arrayOf(null, "C", null, null, null, null, null, "C", null),
            arrayOf(null, null, null, null, null, null, null, null, null),
            arrayOf("R", "N", "B", "A", "K", "A", "B", "N", "R")
        )
        invalidate()
    }
    
    /**
     * 从FEN字符串设置局面
     */
    fun setPositionFromFen(fen: String) {
        try {
            val parts = fen.split(" ")
            val rows = parts[0].split("/")
            
            board = Array(10) { Array<String?>(9) { null } }
            
            for (row in 0..9) {
                if (row >= rows.size) break
                var col = 0
                for (char in rows[row]) {
                    if (char.isDigit()) {
                        col += char.toString().toInt()
                    } else {
                        if (col < 9) {
                            board[row][col] = char.toString()
                            col++
                        }
                    }
                }
            }
            invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 设置最佳走法
     */
    fun setBestMove(from: Pair<Int, Int>?, to: Pair<Int, Int>?) {
        bestMoveFrom = from
        bestMoveTo = to
        invalidate()
    }
}
