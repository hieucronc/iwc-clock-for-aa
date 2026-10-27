package com.mercedes.minimalclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

/**
 * Direction B: Minimal Digital Clock View.
 *
 * Designed specifically for the wide 16:9 Mercedes-Benz C-Class W205 COMAND / Audio 20 display.
 */
class MercedesDigitalClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val fallbackPrefs = ClockPreferences(context)

    var prefs: ClockPreferences? = null
        set(value) {
            field = value
            updateRefreshRate()
            invalidate()
        }

    private val calendar = Calendar.getInstance()

    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val secondsPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val datePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val weatherPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val tickRunnable = object : Runnable {
        override fun run() {
            invalidate()
            val currentPrefs = prefs ?: fallbackPrefs
            val delay = if (currentPrefs.showSeconds) 500L else 1000L
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

    init {
        initPaints()
    }

    private fun initPaints() {
        timePaint.apply {
            color = Color.parseColor("#F4EFE6")
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        secondsPaint.apply {
            color = Color.parseColor("#8E9299")
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
        }

        datePaint.apply {
            color = Color.parseColor("#8E9299")
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.25f
        }
        weatherPaint.apply {
            color = Color.parseColor("#8E9299")
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
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

        val width = width.toFloat()
        val height = height.toFloat()
        if (width <= 0 || height <= 0) return

        calendar.timeInMillis = System.currentTimeMillis()
        val currentPrefs = prefs ?: fallbackPrefs

        // Background
        // Analog dial themes do not tint the digital display.
        val (bgColor, textColor, secColor) = Triple(
            Color.parseColor("#07080A"), Color.parseColor("#F4EFE6"), Color.parseColor("#8E9299")
        )

        canvas.drawColor(bgColor)

        timePaint.color = textColor
        secondsPaint.color = secColor
        datePaint.color = intColorWithAlpha(textColor, 180)
        weatherPaint.color = intColorWithAlpha(textColor, 200)

        // Time calculations
        val hour = if (currentPrefs.is24HourFormat) {
            calendar.get(Calendar.HOUR_OF_DAY)
        } else {
            val h = calendar.get(Calendar.HOUR)
            if (h == 0) 12 else h
        }
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)

        val timeString = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        val secondsString = String.format(Locale.getDefault(), ":%02d", second)

        val baseDim = min(width * 0.35f, height * 0.55f)
        timePaint.textSize = baseDim
        secondsPaint.textSize = baseDim * 0.42f
        datePaint.textSize = baseDim * 0.14f

        val centerX = width / 2f
        var centerY = height * 0.455f

        if (currentPrefs.showDate || currentPrefs.showWeather) {
            centerY -= baseDim * 0.12f
        }

        val bounds = Rect()
        timePaint.getTextBounds(timeString, 0, timeString.length, bounds)
        val textHeight = bounds.height()

        val drawY = centerY + textHeight / 2f
        if (currentPrefs.showSeconds) {
            val secOffset = timePaint.measureText(timeString) / 2f
            canvas.drawText(timeString, centerX - baseDim * 0.15f, drawY, timePaint)
            canvas.drawText(
                secondsString,
                centerX - baseDim * 0.15f + secOffset + 12f,
                drawY - (textHeight * 0.28f),
                secondsPaint
            )
        } else {
            canvas.drawText(timeString, centerX, drawY, timePaint)
        }

        if (currentPrefs.showDate) {
            val dateFmt = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
            val dateText = dateFmt.format(calendar.time).uppercase()
            val dateY = drawY + baseDim * 0.38f
            canvas.drawText(dateText, centerX, dateY, datePaint)
        }
        if (currentPrefs.showWeather) {
            weatherPaint.textSize = baseDim * 0.12f
            val weather = WeatherService.formatWeather(currentPrefs.showWeatherIcon, currentPrefs.showWeatherCondition, currentPrefs.weatherFahrenheit)
            val weatherY = drawY + baseDim * if (currentPrefs.showDate) 0.60f else 0.38f
            canvas.drawText(weather, centerX, weatherY, weatherPaint)
        }
    }

    private fun intColorWithAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}
