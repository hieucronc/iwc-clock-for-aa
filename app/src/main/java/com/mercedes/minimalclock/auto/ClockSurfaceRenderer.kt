package com.mercedes.minimalclock.auto

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.view.Surface
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import com.mercedes.minimalclock.ClockPreferences
import com.mercedes.minimalclock.ClockStyle
import com.mercedes.minimalclock.ClockTheme
import com.mercedes.minimalclock.IwcClockPainter
import com.mercedes.minimalclock.WeatherService
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

/**
 * Renders the clock onto the native Surface provided by Android Auto.
 */
class ClockSurfaceRenderer(
    private val context: Context,
    private val prefs: ClockPreferences
) : SurfaceCallback {

    private var surface: Surface? = null
    private var isRendering = false

    private val handler = Handler(Looper.getMainLooper())
    private val calendar = Calendar.getInstance()
    private val iwcPainter = IwcClockPainter()

    private val digitalTimePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val digitalDatePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val renderRunnable = object : Runnable {
        override fun run() {
            if (isRendering && surface != null && surface!!.isValid) {
                drawFrame()
                val interval = if (prefs.showSeconds && prefs.style == ClockStyle.ANALOG) 125L else 1000L
                handler.postDelayed(this, interval)
            }
        }
    }

    init {
        initDigitalPaints()
    }

    private fun initDigitalPaints() {
        digitalTimePaint.apply {
            color = Color.parseColor("#F4EFE6")
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        digitalDatePaint.apply {
            color = Color.parseColor("#8E9299")
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.2f
        }
    }

    override fun onSurfaceAvailable(container: SurfaceContainer) {
        surface = container.surface
        isRendering = true
        if (prefs.showWeather) WeatherService.refresh(context)
        handler.post(renderRunnable)
    }

    override fun onSurfaceDestroyed(container: SurfaceContainer) {
        isRendering = false
        handler.removeCallbacks(renderRunnable)
        surface = null
    }

    fun notifyStyleChanged() {
        if (isRendering) {
            handler.removeCallbacks(renderRunnable)
            handler.post(renderRunnable)
        }
    }

    private fun drawFrame() {
        val s = surface ?: return
        if (!s.isValid) return

        var canvas: Canvas? = null
        try {
            canvas = s.lockCanvas(null)
            if (canvas != null) {
                calendar.timeInMillis = System.currentTimeMillis()
                val w = canvas.width.toFloat()
                val h = canvas.height.toFloat()
                canvas.save()

                if (prefs.style == ClockStyle.ANALOG) {
                    iwcPainter.drawIwcClock(canvas, w, h, prefs, calendar)
                } else {
                    drawDigitalCluster(canvas, w, h)
                }
                canvas.restore()
            }
        } catch (e: Exception) {
            // Surface lock safety
        } finally {
            if (canvas != null) {
                try {
                    s.unlockCanvasAndPost(canvas)
                } catch (e: Exception) {
                    // Ignored
                }
            }
        }
    }

    private fun drawDigitalCluster(canvas: Canvas, w: Float, h: Float) {
        val (bgColor, textColor, secColor) = Triple(
            Color.parseColor("#07080A"), Color.parseColor("#F4EFE6"), Color.parseColor("#8E9299")
        )

        canvas.drawColor(bgColor)

        val cx = w / 2f
        var cy = h / 2f

        val hour = if (prefs.is24HourFormat) calendar.get(Calendar.HOUR_OF_DAY) else {
            val h = calendar.get(Calendar.HOUR)
            if (h == 0) 12 else h
        }
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)

        val timeString = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        val secondsString = String.format(Locale.getDefault(), ":%02d", second)

        val baseDim = min(w * 0.35f, h * 0.55f)
        digitalTimePaint.textSize = baseDim
        digitalTimePaint.color = textColor

        if (prefs.showDate || prefs.showWeather) {
            cy -= baseDim * 0.12f
        }

        val bounds = Rect()
        digitalTimePaint.getTextBounds(timeString, 0, timeString.length, bounds)
        val textHeight = bounds.height()

        val drawY = cy + textHeight / 2f
        if (prefs.showSeconds) {
            val secOffset = digitalTimePaint.measureText(timeString) / 2f
            canvas.drawText(timeString, cx - baseDim * 0.15f, drawY, digitalTimePaint)

            val secPaint = Paint(digitalTimePaint).apply {
                color = secColor
                textSize = baseDim * 0.42f
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText(
                secondsString,
                cx - baseDim * 0.15f + secOffset + 12f,
                drawY - (textHeight * 0.28f),
                secPaint
            )
        } else {
            canvas.drawText(timeString, cx, drawY, digitalTimePaint)
        }

        if (prefs.showDate) {
            digitalDatePaint.textSize = baseDim * 0.14f
            digitalDatePaint.color = intColorWithAlpha(textColor, 180)
            val dateFmt = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
            canvas.drawText(
                dateFmt.format(calendar.time).uppercase(),
                cx,
                drawY + baseDim * 0.38f,
                digitalDatePaint
            )
        }
        if (prefs.showWeather) {
            val weatherPaint = Paint(digitalDatePaint).apply { textSize = baseDim * 0.12f }
            val weather = WeatherService.formatWeather(prefs.showWeatherIcon, prefs.showWeatherCondition, prefs.weatherFahrenheit)
            val weatherY = drawY + baseDim * if (prefs.showDate) 0.60f else 0.38f
            canvas.drawText(weather, cx, weatherY, weatherPaint)
        }
    }

    private fun intColorWithAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}
