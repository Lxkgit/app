package com.blog.app.debug

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.max
import kotlin.math.min

/**
 * Debug 构建专用的可拖动开发者调试悬浮窗。
 */
object DebugOverlay {
    private const val BUTTON_SIZE_DP = 52
    private const val PANEL_HEIGHT_DP = 360
    private const val MARGIN_DP = 12

    /**
     * 在当前 Activity 上挂载开发者调试悬浮窗。
     */
    fun attach(activity: Activity, parent: ViewGroup) {
        if (!com.blog.app.BuildConfig.DEBUG || parent.findViewWithTag<View>(TAG) != null) {
            return
        }

        val overlay = FrameLayout(activity).apply {
            tag = TAG
            isClickable = false
        }
        val button = TextView(activity).apply {
            text = "调试"
            textSize = 13f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = roundedBackground(Color.rgb(46, 180, 96), dp(activity, 26))
            elevation = dp(activity, 8).toFloat()
        }
        val panel = createPanel(activity)

        overlay.addView(button, FrameLayout.LayoutParams(dp(activity, BUTTON_SIZE_DP), dp(activity, BUTTON_SIZE_DP)).apply {
            gravity = Gravity.TOP or Gravity.END
            topMargin = dp(activity, 180)
            rightMargin = dp(activity, MARGIN_DP)
        })
        overlay.addView(panel, FrameLayout.LayoutParams(-1, dp(activity, PANEL_HEIGHT_DP)).apply {
            gravity = Gravity.BOTTOM
            leftMargin = dp(activity, MARGIN_DP)
            rightMargin = dp(activity, MARGIN_DP)
            bottomMargin = dp(activity, MARGIN_DP)
        })
        panel.visibility = View.GONE
        parent.addView(overlay, FrameLayout.LayoutParams(-1, -1))

        var downRawX = 0f
        var downRawY = 0f
        var downX = 0f
        var downY = 0f
        var moved = false

        button.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    downX = view.x
                    downY = view.y
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (kotlin.math.abs(dx) > dp(activity, 4) || kotlin.math.abs(dy) > dp(activity, 4)) {
                        moved = true
                    }
                    if (moved) {
                        val maxX = max(0f, (overlay.width - view.width).toFloat())
                        val maxY = max(0f, (overlay.height - view.height).toFloat())
                        view.x = min(max(0f, downX + dx), maxX)
                        view.y = min(max(0f, downY + dy), maxY)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        panel.visibility = if (panel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                    }
                    true
                }
                else -> true
            }
        }

        val refresh = panel.getTag(TAG_REFRESH) as () -> Unit
        refresh()
    }

    private fun createPanel(activity: Activity): View {
        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10))
            background = roundedBackground(Color.rgb(25, 28, 31), dp(activity, 12))
            elevation = dp(activity, 10).toFloat()
        }

        val header = LinearLayout(activity).apply {
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(activity).apply {
            text = "开发者调试"
            textSize = 16f
            setTextColor(Color.WHITE)
        }
        val clear = TextView(activity).apply {
            text = "清空"
            textSize = 13f
            setTextColor(Color.rgb(110, 220, 150))
            gravity = Gravity.CENTER
            setPadding(dp(activity, 12), dp(activity, 6), dp(activity, 4), dp(activity, 6))
        }
        header.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(clear, LinearLayout.LayoutParams(-2, -2))
        container.addView(header)

        val scroll = ScrollView(activity)
        val logView = TextView(activity).apply {
            textSize = 11f
            setTextColor(Color.rgb(225, 230, 235))
            setTextIsSelectable(true)
            setPadding(0, dp(activity, 8), 0, 0)
            typeface = android.graphics.Typeface.MONOSPACE
        }
        scroll.addView(logView, ScrollView.LayoutParams(-1, -2))
        container.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val refresh = {
            val text = DebugLog.snapshot().joinToString("\n")
            logView.text = if (text.isBlank()) "等待接口请求..." else text
            scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
        }
        container.setTag(TAG_REFRESH, refresh)
        clear.setOnClickListener { DebugLog.clear() }
        val listener: () -> Unit = refresh
        container.setTag(TAG_LISTENER, listener)
        DebugLog.addListener(listener)
        return container
    }

    private fun roundedBackground(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
        }
    }

    private fun dp(activity: Activity, value: Int): Int {
        return (value * activity.resources.displayMetrics.density).toInt()
    }

    private const val TAG = "BLOG_DEBUG_OVERLAY"
    private const val TAG_REFRESH = 1001
    private const val TAG_LISTENER = 1002
}
