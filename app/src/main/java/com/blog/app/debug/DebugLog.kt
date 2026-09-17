package com.blog.app.debug

import android.os.Handler
import android.os.Looper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 保存 Debug 构建中的开发者调试日志。
 */
object DebugLog {
    private const val MAX_ENTRIES = 300
    private val entries = CopyOnWriteArrayList<String>()
    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    /**
     * 写入一条调试日志。
     */
    fun add(message: String) {
        if (!com.blog.app.BuildConfig.DEBUG) {
            return
        }
        val entry = "[${formatter.format(Date())}] $message"
        entries.add(entry)
        while (entries.size > MAX_ENTRIES) {
            entries.removeAt(0)
        }
        mainHandler.post {
            listeners.forEach { it() }
        }
    }

    /**
     * 返回当前全部调试日志。
     */
    fun snapshot(): List<String> = entries.toList()

    /**
     * 清空调试日志。
     */
    fun clear() {
        entries.clear()
        mainHandler.post {
            listeners.forEach { it() }
        }
    }

    /**
     * 注册日志刷新监听器。
     */
    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    /**
     * 移除日志刷新监听器。
     */
    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }
}
