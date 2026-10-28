package com.mercedes.minimalclock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import java.util.Locale
import kotlin.math.min

/** Draws the same compact driving panel on the phone and Android Auto surface. */
object TelemetryPainter {
    private var carbonTile: Bitmap? = null

    fun drawPattern(canvas: Canvas, bounds: RectF, pattern: DigitalPattern) {
        if (pattern == DigitalPattern.SOLID || bounds.width() <= 0f || bounds.height() <= 0f) return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; alpha = 30; strokeWidth = 1f }
        when (pattern) {
            DigitalPattern.GRID, DigitalPattern.BLUEPRINT -> {
                val small = if (pattern == DigitalPattern.BLUEPRINT) 30f else 24f
                val major = small * 4f
                paint.alpha = if (pattern == DigitalPattern.BLUEPRINT) 23 else 30
                var x = bounds.left
                while (x <= bounds.right) {
                    paint.alpha = if (pattern == DigitalPattern.BLUEPRINT && ((x - bounds.left) % major) < small) 70 else 24
                    canvas.drawLine(x, bounds.top, x, bounds.bottom, paint); x += small
                }
                var y = bounds.top
                while (y <= bounds.bottom) {
                    paint.alpha = if (pattern == DigitalPattern.BLUEPRINT && ((y - bounds.top) % major) < small) 70 else 24
                    canvas.drawLine(bounds.left, y, bounds.right, y, paint); y += small
                }
                if (pattern == DigitalPattern.BLUEPRINT) {
                    paint.alpha = 95
                    val step = 120f
                    var cx = bounds.left + step / 2f
                    while (cx < bounds.right) {
                        var cy = bounds.top + step / 2f
                        while (cy < bounds.bottom) {
                            canvas.drawLine(cx - 5f, cy, cx + 5f, cy, paint)
                            canvas.drawLine(cx, cy - 5f, cx, cy + 5f, paint)
                            cy += step
                        }
                        cx += step
                    }
                }
            }
            DigitalPattern.CARBON -> {
                val tile = carbonTile ?: createCarbonTile().also { carbonTile = it }
                paint.alpha = 255
                paint.shader = BitmapShader(tile, android.graphics.Shader.TileMode.REPEAT, android.graphics.Shader.TileMode.REPEAT)
                canvas.drawRect(bounds, paint)
                paint.shader = null
            }
            DigitalPattern.SCANLINES -> {
                var y = bounds.top
                while (y <= bounds.bottom) { canvas.drawLine(bounds.left, y, bounds.right, y, paint); y += 5f }
            }
            DigitalPattern.DOT_MATRIX -> {
                paint.style = Paint.Style.FILL
                val dots = Path()
                var row = 0; var y = bounds.top + 8f
                while (y < bounds.bottom) {
                    var x = bounds.left + 8f + if (row % 2 == 0) 0f else 7f
                    while (x < bounds.right) {
                        dots.addCircle(x, y, 1.65f, Path.Direction.CW)
                        x += 18f
                    }
                    row++; y += 18f
                }
                paint.alpha = 80
                canvas.drawPath(dots, paint)
            }
            DigitalPattern.HEXAGON -> {
                paint.style = Paint.Style.STROKE
                paint.alpha = 40
                val r = 11f
                val dx = r * 1.75f; val dy = r * 1.5f
                val path = Path()
                var row = 0; var cy = bounds.top - r
                while (cy < bounds.bottom + r) {
                    var cx = bounds.left - r + if (row % 2 == 0) 0f else dx / 2f
                    while (cx < bounds.right + r) {
                        for (i in 0..5) {
                            val a = Math.toRadians(60.0 * i - 30.0)
                            val px = cx + r * kotlin.math.cos(a).toFloat()
                            val py = cy + r * kotlin.math.sin(a).toFloat()
                            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                        }
                        path.close()
                        cx += dx
                    }
                    row++; cy += dy
                }
                canvas.drawPath(path, paint)
            }
            else -> Unit
        }
    }

    private fun createCarbonTile(): Bitmap {
        val bitmap = Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888)
        val tileCanvas = Canvas(bitmap)
        tileCanvas.drawColor(Color.rgb(7, 9, 12))
        for (row in 0 until 3) {
            val y = row * 16f
            val startX = if (row % 2 == 0) -16f else 0f
            var col = 0
            var x = startX
            while (x < 64f) {
                val bright = (col + row) % 2 == 0
                val top = if (bright) Color.rgb(62, 69, 78) else Color.rgb(30, 35, 42)
                val bottom = if (bright) Color.rgb(31, 36, 43) else Color.rgb(13, 16, 20)
                val shape = Path().apply {
                    moveTo(x + 3f, y); lineTo(x + 27f, y); lineTo(x + 32f, y + 16f); lineTo(x + 8f, y + 16f); close()
                }
                val blockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(x, y, x + 4f, y + 16f, top, bottom, Shader.TileMode.CLAMP)
                }
                tileCanvas.drawPath(shape, blockPaint)
                val ridgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (bright) Color.argb(210, 125, 133, 144) else Color.argb(110, 77, 84, 94)
                    strokeWidth = 1.4f
                }
                tileCanvas.drawLine(x + 4f, y + 1f, x + 26f, y + 1f, ridgePaint)
                col++; x += 32f
            }
        }
        return bitmap
    }

    fun draw(
        canvas: Canvas,
        bounds: RectF,
        prefs: ClockPreferences,
        speedKmh: Float?,
        acceleration: Float?,
        zeroToHundredSeconds: Float?,
        maxSpeedKmh: Float?,
        altitudeMeters: Double?,
        headingDegrees: Float?,
        gForce: Float?
    ) {
        if (bounds.width() <= 0f || bounds.height() <= 0f) return
        val backgroundPaint = Paint().apply {
            color = if (prefs.style == ClockStyle.ANALOG) Color.parseColor("#050607") else Color.parseColor("#07080A")
        }
        canvas.drawRect(bounds, backgroundPaint)
        if (prefs.style == ClockStyle.DIGITAL) drawPattern(canvas, bounds, prefs.digitalPattern)
        val cardHeight = if (bounds.height() > bounds.width()) min(bounds.height() * .90f, bounds.width() * 1.8f) else bounds.height() * .88f
        val cardTop = bounds.centerY() - cardHeight / 2f
        val card = RectF(bounds.left + bounds.width() * .045f, cardTop,
            bounds.right - bounds.width() * .045f, cardTop + cardHeight)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.argb(if (prefs.style == ClockStyle.DIGITAL) 72 else 238, 10, 13, 18)
        canvas.drawRoundRect(card, 22f, 22f, paint)
        val scale = min(card.width() / 310f, card.height() / 270f).coerceIn(.62f, 1.55f)
        val x = card.left + 25f * scale
        val right = card.right - 24f * scale
        paint.color = Color.rgb(140, 148, 160)
        paint.textSize = 16f * scale
        canvas.drawText("DRIVE", x, card.top + 31f * scale, paint)
        var cursor = card.top + 41f * scale
        if (prefs.showTelemetrySpeed) {
            paint.color = Color.WHITE
            paint.textSize = 48f * scale
            canvas.drawText(speedKmh?.let { String.format(Locale.getDefault(), "%.0f", it) } ?: "--", x, cursor + 42f * scale, paint)
            paint.color = Color.rgb(140, 148, 160)
            paint.textSize = 15f * scale
            canvas.drawText("km/h", x + 2f * scale, cursor + 63f * scale, paint)
            cursor += 75f * scale
        }
        val rows = buildList {
            if (prefs.showTelemetryAcceleration) add("ACCELERATION" to (acceleration?.let { String.format(Locale.getDefault(), "%+.1f m/s²", it) } ?: "-- m/s²"))
            if (prefs.showTelemetryZeroToHundred) add("0–100 KM/H" to (zeroToHundredSeconds?.let { String.format(Locale.getDefault(), "%.2f s", it) } ?: "--"))
            if (prefs.showTelemetryMaxSpeed) add("MAX SPEED" to (maxSpeedKmh?.let { String.format(Locale.getDefault(), "%.0f km/h", it) } ?: "--"))
            if (prefs.showTelemetryAltitude) add("ALTITUDE" to (altitudeMeters?.let { String.format(Locale.getDefault(), "%.0f m", it) } ?: "--"))
            if (prefs.showTelemetryHeading) add("HEADING" to (headingDegrees?.let { String.format(Locale.getDefault(), "%03d°", it.toInt().mod(360)) } ?: "--"))
            if (prefs.showTelemetryGForce) add("G-FORCE" to (gForce?.let { String.format(Locale.getDefault(), "%.2f g", it) } ?: "--"))
        }
        val available = (card.bottom - cursor - 14f * scale).coerceAtLeast(1f)
        val rowHeight = if (rows.isEmpty()) 0f else available / rows.size
        rows.forEachIndexed { index, (label, value) ->
            val baseline = cursor + rowHeight * (index + .68f)
            paint.color = Color.rgb(140, 148, 160)
            paint.textAlign = Paint.Align.LEFT
            paint.textSize = min(13f * scale, rowHeight * .34f)
            canvas.drawText(label, x, baseline, paint)
            paint.color = if (index == 0 && label == "ACCELERATION") Color.rgb(115, 211, 255) else Color.WHITE
            paint.textAlign = Paint.Align.RIGHT
            paint.textSize = min(17f * scale, rowHeight * .48f)
            canvas.drawText(value, right, baseline, paint)
        }
        paint.textAlign = Paint.Align.LEFT
    }

}

class TelemetryView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    var prefs: ClockPreferences? = null
    var speedKmh: Float? = null
    var acceleration: Float? = null
    var zeroToHundredSeconds: Float? = null
    var maxSpeedKmh: Float? = null
    var altitudeMeters: Double? = null
    var headingDegrees: Float? = null
    var gForce: Float? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0 || h <= 0) return
        val landscape = w >= h
        val panel = if (landscape) RectF(w * .52f, 0f, w, h) else RectF(0f, h * .51f, w, h)
        val currentPrefs = prefs ?: return
        canvas.save()
        canvas.clipRect(panel)
        TelemetryPainter.draw(canvas, panel, currentPrefs, speedKmh, acceleration, zeroToHundredSeconds,
            maxSpeedKmh, altitudeMeters, headingDegrees, gForce)
        canvas.restore()
    }
}
