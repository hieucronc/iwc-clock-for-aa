package com.mercedes.minimalclock.auto

import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.os.Bundle
import android.view.Surface
import androidx.core.content.ContextCompat
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import com.mercedes.minimalclock.ClockPreferences
import com.mercedes.minimalclock.ClockStyle
import com.mercedes.minimalclock.ClockTheme
import com.mercedes.minimalclock.DigitalColor
import com.mercedes.minimalclock.DigitalFont
import com.mercedes.minimalclock.DigitalPattern
import com.mercedes.minimalclock.IwcClockPainter
import com.mercedes.minimalclock.TelemetryPainter
import com.mercedes.minimalclock.WeatherService
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.RectF
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
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var telemetryActive = false
    private var locationUpdatesActive = false
    private var speedKmh: Float? = null
    private var acceleration: Float? = null
    private var gForce: Float? = null
    private var maxSpeedKmh: Float? = null
    private var altitudeMeters: Double? = null
    private var headingDegrees: Float? = null
    private var accelerationStartMs: Long? = null
    private var lastSpeedKmh: Float? = null
    private var zeroToHundredSeconds: Float? = null
    private val gpsListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val speed = (if (location.hasSpeed()) location.speed * 3.6f else 0f).coerceAtLeast(0f)
            val old = lastSpeedKmh
            if (speed <= 2f) { accelerationStartMs = null; zeroToHundredSeconds = null }
            else if (old != null && speed < old - 2f && speed < 100f) { accelerationStartMs = null; zeroToHundredSeconds = null }
            if (accelerationStartMs == null && speed > 2f && (old == null || old <= 2f)) accelerationStartMs = location.time
            if (speed >= 100f && zeroToHundredSeconds == null) accelerationStartMs?.let {
                zeroToHundredSeconds = (location.time - it).coerceAtLeast(0L) / 1000f
            }
            lastSpeedKmh = speed
            maxSpeedKmh = maxOf(maxSpeedKmh ?: speed, speed)
            altitudeMeters = if (location.hasAltitude()) location.altitude else altitudeMeters
            headingDegrees = if (location.hasBearing()) location.bearing else headingDegrees
            speedKmh = speed
        }
        @Deprecated("Deprecated by Android") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        override fun onProviderEnabled(provider: String) = Unit
        override fun onProviderDisabled(provider: String) = Unit
    }
    private val accelListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values.getOrNull(0) ?: 0f
            val y = event.values.getOrNull(1) ?: 0f
            val z = event.values.getOrNull(2) ?: 0f
            acceleration = kotlin.math.sqrt(x * x + y * y + z * z)
            gForce = acceleration?.div(9.80665f)
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private val digitalTimePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val digitalDatePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val renderRunnable = object : Runnable {
        override fun run() {
            if (isRendering && surface != null && surface!!.isValid) {
                if (prefs.tachometerEnabled != telemetryActive) setTelemetryActive(prefs.tachometerEnabled)
                if (prefs.tachometerEnabled && !locationUpdatesActive) startLocationUpdates()
                drawFrame()
                val interval = when {
                    prefs.tachometerEnabled -> 250L
                    prefs.showSeconds && prefs.style == ClockStyle.ANALOG -> 125L
                    prefs.showSeconds && prefs.style == ClockStyle.DIGITAL -> 500L
                    else -> 1000L
                }
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
        setTelemetryActive(prefs.tachometerEnabled)
        handler.post(renderRunnable)
    }

    override fun onSurfaceDestroyed(container: SurfaceContainer) {
        isRendering = false
        handler.removeCallbacks(renderRunnable)
        setTelemetryActive(false)
        surface = null
    }

    fun notifyStyleChanged() {
        setTelemetryActive(prefs.tachometerEnabled)
        if (isRendering) {
            handler.removeCallbacks(renderRunnable)
            handler.post(renderRunnable)
        }
    }

    fun onLocationPermissionGranted() {
        handler.post { startLocationUpdates() }
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

                if (prefs.tachometerEnabled) {
                    canvas.drawColor(Color.parseColor("#07080A"))
                    if (w >= h) {
                        TelemetryPainter.draw(canvas, RectF(0f, 0f, w * .49f, h), prefs, speedKmh, acceleration,
                            zeroToHundredSeconds, maxSpeedKmh, altitudeMeters, headingDegrees, gForce)
                        canvas.save()
                        canvas.translate(w * .5f, 0f)
                        canvas.clipRect(0f, 0f, w * .5f, h)
                        drawClock(canvas, w * .5f, h)
                        canvas.restore()
                    } else {
                        TelemetryPainter.draw(canvas, RectF(0f, h * .51f, w, h), prefs, speedKmh, acceleration,
                            zeroToHundredSeconds, maxSpeedKmh, altitudeMeters, headingDegrees, gForce)
                        canvas.save()
                        canvas.clipRect(0f, 0f, w, h * .49f)
                        drawClock(canvas, w, h * .49f)
                        canvas.restore()
                    }
                } else {
                    drawClock(canvas, w, h)
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

    private fun drawClock(canvas: Canvas, w: Float, h: Float) {
        if (prefs.style == ClockStyle.ANALOG) iwcPainter.drawIwcClock(canvas, w, h, prefs, calendar)
        else drawDigitalCluster(canvas, w, h)
    }

    private fun setTelemetryActive(active: Boolean) {
        if (telemetryActive == active) return
        telemetryActive = active
        handler.post {
            if (active) {
                sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)?.let {
                    sensorManager.registerListener(accelListener, it, SensorManager.SENSOR_DELAY_NORMAL, handler)
                }
                startLocationUpdates()
            } else {
                sensorManager.unregisterListener(accelListener)
                runCatching { locationManager.removeUpdates(gpsListener) }
                locationUpdatesActive = false
                speedKmh = null; acceleration = null; gForce = null; maxSpeedKmh = null
                altitudeMeters = null; headingDegrees = null; zeroToHundredSeconds = null
                accelerationStartMs = null; lastSpeedKmh = null
            }
        }
    }

    private fun startLocationUpdates() {
        if (!telemetryActive || locationUpdatesActive ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        locationUpdatesActive = runCatching {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500L, 0f, gpsListener, Looper.getMainLooper())
            true
        }.getOrDefault(false)
    }

    private fun drawDigitalCluster(canvas: Canvas, w: Float, h: Float) {
        val bgColor = Color.parseColor("#07080A")
        val textColor = when (prefs.digitalColor) {
            DigitalColor.WHITE -> Color.WHITE
            DigitalColor.CYAN -> Color.rgb(88, 220, 255)
            DigitalColor.AMBER -> Color.rgb(255, 190, 80)
            DigitalColor.RED -> Color.rgb(255, 90, 85)
            else -> Color.parseColor("#F4EFE6")
        }
        fun componentColor(value: DigitalColor) = when (value) { DigitalColor.WHITE -> Color.WHITE; DigitalColor.CYAN -> Color.rgb(88,220,255); DigitalColor.AMBER -> Color.rgb(255,190,80); DigitalColor.RED -> Color.rgb(255,90,85); DigitalColor.IVORY -> Color.parseColor("#F4EFE6") }

        canvas.drawColor(bgColor)

        drawDigitalPattern(canvas, w, h, prefs.digitalPattern)
        val cx = w / 2f
        var cy = h / 2f

        val hour = if (prefs.is24HourFormat) calendar.get(Calendar.HOUR_OF_DAY) else {
            val h = calendar.get(Calendar.HOUR)
            if (h == 0) 12 else h
        }
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)

        val hourString = String.format(Locale.getDefault(), "%02d", hour)
        val minuteString = String.format(Locale.getDefault(), "%02d", minute)
        val secondsString = String.format(Locale.getDefault(), ":%02d", second)

        val baseDim = min(w * 0.35f, h * 0.55f)
        digitalTimePaint.textSize = baseDim
        digitalTimePaint.color = textColor
        val font = when (prefs.digitalFont) {
            DigitalFont.MONOSPACE -> "sans-serif-monospace"
            DigitalFont.SERIF -> "serif"
            else -> "sans-serif"
        }
        val typefaceStyle = (if (prefs.digitalBold) Typeface.BOLD else Typeface.NORMAL) or
            (if (prefs.digitalItalic) Typeface.ITALIC else Typeface.NORMAL)
        digitalTimePaint.typeface = Typeface.create(font, typefaceStyle)

        if (prefs.showDate || prefs.showWeather) {
            cy -= baseDim * 0.12f
        }

        val bounds = Rect()
        digitalTimePaint.getTextBounds(hourString, 0, hourString.length, bounds)
        val textHeight = bounds.height()

        val drawY = cy + textHeight / 2f
        if (prefs.showSeconds) {
            val secPaint = Paint(digitalTimePaint).apply {
                color = componentColor(prefs.digitalSecondColor)
                textSize = baseDim * 0.42f
                textAlign = Paint.Align.LEFT
            }
            val hourWidth=digitalTimePaint.measureText(hourString); val sepWidth=digitalTimePaint.measureText(":"); val minuteWidth=digitalTimePaint.measureText(minuteString)
            val secondsWidth = secPaint.measureText(secondsString)
            val gap = baseDim * .025f
            val total = hourWidth + sepWidth + minuteWidth + gap + secondsWidth
            val fit = (w * .92f / total).coerceAtMost(1f)
            canvas.save()
            canvas.scale(fit, fit, cx, cy)
            val left = cx - total / 2f
            digitalTimePaint.color=componentColor(prefs.digitalHourColor); canvas.drawText(hourString,left+hourWidth/2f,drawY,digitalTimePaint)
            digitalTimePaint.color=componentColor(prefs.digitalSeparatorColor); canvas.drawText(":",left+hourWidth+sepWidth/2f,drawY,digitalTimePaint)
            digitalTimePaint.color=componentColor(prefs.digitalMinuteColor); canvas.drawText(minuteString,left+hourWidth+sepWidth+minuteWidth/2f,drawY,digitalTimePaint)
            canvas.drawText(secondsString, left + hourWidth + sepWidth + minuteWidth + gap, drawY - textHeight * .28f, secPaint)
            canvas.restore()
        } else {
            val hourWidth=digitalTimePaint.measureText(hourString); val sepWidth=digitalTimePaint.measureText(":"); val minuteWidth=digitalTimePaint.measureText(minuteString); val left=cx-(hourWidth+sepWidth+minuteWidth)/2f
            digitalTimePaint.color=componentColor(prefs.digitalHourColor); canvas.drawText(hourString,left+hourWidth/2f,drawY,digitalTimePaint)
            digitalTimePaint.color=componentColor(prefs.digitalSeparatorColor); canvas.drawText(":",left+hourWidth+sepWidth/2f,drawY,digitalTimePaint)
            digitalTimePaint.color=componentColor(prefs.digitalMinuteColor); canvas.drawText(minuteString,left+hourWidth+sepWidth+minuteWidth/2f,drawY,digitalTimePaint)
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

    private fun drawDigitalPattern(canvas: Canvas, w: Float, h: Float, pattern: DigitalPattern) {
        TelemetryPainter.drawPattern(canvas, RectF(0f, 0f, w, h), pattern)
    }

    private fun intColorWithAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}
