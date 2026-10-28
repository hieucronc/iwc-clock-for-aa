package com.mercedes.minimalclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import java.util.*

/**
 * Analog clock view for the phone display, using the shared analog face painter.
 */
class IwcAnalogClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val painter = IwcClockPainter()
    private val calendar = Calendar.getInstance()
    private val fallbackPrefs = ClockPreferences(context)

    var prefs: ClockPreferences? = null
        set(value) {
            field = value
            updateRefreshRate()
            invalidate()
        }

    private val tickRunnable = object : Runnable {
        override fun run() {
            invalidate()
            val delay = if ((prefs ?: fallbackPrefs).showSeconds) 125L else 1000L
            postDelayed(this, delay)
        }
    }

    private var isAttached = false

    private val timeChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            calendar.timeZone = TimeZone.getDefault()
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isAttached = true
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        context.registerReceiver(timeChangeReceiver, filter)
        post(tickRunnable)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isAttached = false
        context.unregisterReceiver(timeChangeReceiver)
        removeCallbacks(tickRunnable)
    }

    private fun updateRefreshRate() {
        removeCallbacks(tickRunnable)
        if (isAttached) {
            post(tickRunnable)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        calendar.timeInMillis = System.currentTimeMillis()
        val currentPrefs = prefs ?: fallbackPrefs

        painter.drawIwcClock(canvas, w, h, currentPrefs, calendar, centerYFactor = 0.455f)
    }
}
