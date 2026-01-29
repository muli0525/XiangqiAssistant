package com.xiangqi.assistant.engine

import android.content.Context
import android.os.Build
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * Pikafish引擎接口
 * 自动从 assets 复制引擎并启动
 */
class PikafishEngine(private val context: Context) {
    
    private var process: Process? = null
    private var writer: OutputStreamWriter? = null
    private var reader: BufferedReader? = null
    private var engineFile: File? = null
    
    init {
        try {
            copyEngineFromAssets()
            startEngine()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 从 assets 复制引擎到可执行目录
     */
    private fun copyEngineFromAssets() {
        // 根据 CPU 架构选择引擎文件
        val assetName = when {
            Build.SUPPORTED_64_BIT_ABIS.isNotEmpty() -> "pikafish-arm64"
            else -> "pikafish-armv7"
        }
        
        engineFile = File(context.filesDir, "pikafish")
        
        // 如果已存在且可执行，跳过复制
        if (engineFile!!.exists() && engineFile!!.canExecute()) {
            return
        }
        
        // 从 assets 复制
        try {
            context.assets.open(assetName).use { input ->
                engineFile!!.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            // 设置可执行权限
            engineFile!!.setExecutable(true, false)
            engineFile!!.setReadable(true, false)
            
        } catch (e: Exception) {
            // 如果 assets 中没有引擎文件，使用模拟模式
            e.printStackTrace()
            engineFile = null
        }
    }
    
    private fun startEngine() {
        if (engineFile == null || !engineFile!!.exists()) {
            return
        }
        
        try {
            // 启动引擎进程
            process = Runtime.getRuntime().exec(engineFile!!.absolutePath)
            writer = OutputStreamWriter(process!!.outputStream)
            reader = BufferedReader(InputStreamReader(process!!.inputStream))
            
            // 初始化 UCI
            sendCommand("uci")
            waitFor("uciok", 5000)
            sendCommand("isready")
            waitFor("readyok", 5000)
            
        } catch (e: Exception) {
            e.printStackTrace()
            process = null
        }
    }
    
    fun analyze(fen: String, depth: Int = 20): List<String> {
        // 如果引擎未启动，返回模拟数据
        if (process == null) {
            return listOf(
                "车二平五 (模拟)",
                "马八进七 (模拟)",
                "炮二平五 (模拟)"
            )
        }
        
        // 实际引擎分析
        try {
            sendCommand("position fen $fen")
            sendCommand("setoption name MultiPV value 5")
            sendCommand("go depth $depth")
            
            val moves = mutableListOf<String>()
            var line: String?
            
            while (true) {
                line = reader?.readLine() ?: break
                
                if (line.startsWith("bestmove")) {
                    break
                }
                
                if (line.contains("info") && line.contains("pv")) {
                    // 解析走法
                    val moveMatch = Regex("pv\\s+(\\S+)").find(line)
                    val scoreMatch = Regex("score\\s+cp\\s+(-?\\d+)").find(line)
                    
                    if (moveMatch != null) {
                        val move = moveMatch.groupValues[1]
                        val score = scoreMatch?.groupValues?.get(1) ?: "0"
                        val moveCn = uciToChinese(move)
                        moves.add("$moveCn ($score)")
                    }
                }
                
                if (moves.size >= 5) break
            }
            
            return moves.ifEmpty {
                listOf("无法分析")
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            return listOf("分析出错: ${e.message}")
        }
    }
    
    /**
     * 将 UCI 走法转换为中文
     */
    private fun uciToChinese(uci: String): String {
        if (uci.length < 4) return uci
        
        val files = arrayOf("一", "二", "三", "四", "五", "六", "七", "八", "九")
        
        val fromFile = uci[0] - 'a'
        val fromRank = uci[1].toString()
        val toFile = uci[2] - 'a'
        val toRank = uci[3].toString()
        
        return "${files.getOrNull(fromFile)}$fromRank→${files.getOrNull(toFile)}$toRank"
    }
    
    private fun sendCommand(command: String) {
        writer?.write("$command\n")
        writer?.flush()
    }
    
    private fun waitFor(keyword: String, timeout: Long = 5000): Boolean {
        val startTime = System.currentTimeMillis()
        try {
            var line: String?
            while (System.currentTimeMillis() - startTime < timeout) {
                line = reader?.readLine()
                if (line?.contains(keyword) == true) {
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
    
    fun quit() {
        try {
            sendCommand("quit")
            process?.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            process?.destroy()
        }
    }
}
