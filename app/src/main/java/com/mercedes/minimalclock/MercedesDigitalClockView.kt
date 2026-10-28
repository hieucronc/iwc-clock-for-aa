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

        val textColor = colorFor(currentPrefs.digitalColor)
        val hourColor = colorFor(currentPrefs.digitalHourColor)
        val minuteColor = colorFor(currentPrefs.digitalMinuteColor)
        val separatorColor = colorFor(currentPrefs.digitalSeparatorColor)
        val secondColor = colorFor(currentPrefs.digitalSecondColor)
        val bgColor = Color.parseColor("#07080A")
        canvas.drawColor(bgColor)
        drawPattern(canvas, width, height, currentPrefs.digitalPattern)

        val font = when (currentPrefs.digitalFont) {
            DigitalFont.MONOSPACE -> "sans-serif-monospace"
            DigitalFont.SERIF -> "serif"
            else -> "sans-serif"
        }
        val fontStyle = (if (currentPrefs.digitalBold) Typeface.BOLD else Typeface.NORMAL) or
            (if (currentPrefs.digitalItalic) Typeface.ITALIC else Typeface.NORMAL)
        timePaint.typeface = Typeface.create(font, fontStyle)
        secondsPaint.typeface = Typeface.create(font, fontStyle)
        timePaint.color = textColor
        datePaint.color = intColorWithAlpha(textColor, 180)
        weatherPaint.color = intColorWithAlpha(textColor, 200)
        val secColor = intColorWithAlpha(secondColor, 200)
        secondsPaint.color = secColor

        // Time calculations
        val hour = if (currentPrefs.is24HourFormat) {
            calendar.get(Calendar.HOUR_OF_DAY)
        } else {
            val h = calendar.get(Calendar.HOUR)
            if (h == 0) 12 else h
        }
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)

        val hourString = String.format(Locale.getDefault(), "%02d", hour)
        val minuteString = String.format(Locale.getDefault(), "%02d", minute)
        val separatorString = ":"
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
        timePaint.getTextBounds(hourString, 0, hourString.length, bounds)
        val textHeight = bounds.height()

        val drawY = centerY + textHeight / 2f
        if (currentPrefs.showSeconds) {
            // Center the combined time/seconds block and shrink it to fit narrow screens and preview.
            val hourWidth = timePaint.measureText(hourString)
            val minuteWidth = timePaint.measureText(minuteString)
            val sepWidth = timePaint.measureText(separatorString)
            val secondsWidth = secondsPaint.measureText(secondsString)
            val gap = baseDim * 0.025f
            val totalWidth = hourWidth + sepWidth + minuteWidth + gap + secondsWidth
            val fit = (width * 0.92f / totalWidth).coerceAtMost(1f)
            canvas.save()
            canvas.scale(fit, fit, centerX, centerY)
            val left = centerX - totalWidth / 2f
            timePaint.color = hourColor; canvas.drawText(hourString, left + hourWidth/2f, drawY, timePaint)
            timePaint.color = separatorColor; canvas.drawText(separatorString, left + hourWidth + sepWidth/2f, drawY, timePaint)
            timePaint.color = minuteColor; canvas.drawText(minuteString, left + hourWidth + sepWidth + minuteWidth/2f, drawY, timePaint)
            secondsPaint.color = secColor
            canvas.drawText(secondsString, left + hourWidth + sepWidth + minuteWidth + gap, drawY - (textHeight * 0.28f), secondsPaint)
            canvas.restore()
        } else {
            val hourWidth = timePaint.measureText(hourString); val sepWidth=timePaint.measureText(separatorString); val minuteWidth=timePaint.measureText(minuteString)
            val left=centerX-(hourWidth+sepWidth+minuteWidth)/2f
            timePaint.color=hourColor; canvas.drawText(hourString,left+hourWidth/2f,drawY,timePaint)
            timePaint.color=separatorColor; canvas.drawText(separatorString,left+hourWidth+sepWidth/2f,drawY,timePaint)
            timePaint.color=minuteColor; canvas.drawText(minuteString,left+hourWidth+sepWidth+minuteWidth/2f,drawY,timePaint)
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

    private fun colorFor(color: DigitalColor): Int = when (color) {
        DigitalColor.WHITE -> Color.WHITE
        DigitalColor.CYAN -> Color.rgb(88, 220, 255)
        DigitalColor.AMBER -> Color.rgb(255, 190, 80)
        DigitalColor.RED -> Color.rgb(255, 90, 85)
        DigitalColor.IVORY -> Color.parseColor("#F4EFE6")
    }

    private fun drawPattern(canvas: Canvas, width: Float, height: Float, pattern: DigitalPattern) {
        TelemetryPainter.drawPattern(canvas, RectF(0f, 0f, width, height), pattern)
    }
}
