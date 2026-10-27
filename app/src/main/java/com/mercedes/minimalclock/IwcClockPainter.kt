package com.mercedes.minimalclock

import android.graphics.*
import java.util.*
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Shared high-fidelity renderer for the IWC Schaffhausen C-Class W205 Saddle Clock.
 *
 * Renders on both:
 * 1. Phone UI [IwcAnalogClockView]
 * 2. Android Auto Head Unit Surface [auto.ClockSurfaceRenderer]
 */
class IwcClockPainter {

    private var basketweaveShader: BitmapShader? = null

    private val bezelOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bezelInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dialBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val radialHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val railwayTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tickMajorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tickMinorPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val hourHandOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hourHandLumePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val minuteHandOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val minuteHandLumePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val secondHandPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val handShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val centerCapPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val centerCapInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val centerCapPinPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val logoIwcPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val logoSubtextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hourNumeralPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dateBoxBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dateBoxBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dateTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val glassReflectionPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        initPaints()
    }

    private fun initPaints() {
        handShadowPaint.apply {
            color = Color.parseColor("#60000000")
            maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
        }

        railwayTrackPaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        tickMajorPaint.apply {
            strokeWidth = 4f
            strokeCap = Paint.Cap.ROUND
        }

        tickMinorPaint.apply {
            strokeWidth = 1.8f
            strokeCap = Paint.Cap.ROUND
        }

        hourHandOutlinePaint.apply {
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = 2f
        }

        hourHandLumePaint.apply {
            style = Paint.Style.FILL
        }

        minuteHandOutlinePaint.apply {
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = 2f
        }

        minuteHandLumePaint.apply {
            style = Paint.Style.FILL
        }

        secondHandPaint.apply {
            color = Color.parseColor("#E53935") // AMG Red needle
            strokeWidth = 2.5f
            strokeCap = Paint.Cap.ROUND
        }

        centerCapPaint.apply {
            color = Color.parseColor("#D0D4DC")
            style = Paint.Style.FILL
        }

        centerCapInnerPaint.apply {
            color = Color.parseColor("#1C1E24")
            style = Paint.Style.FILL
        }

        centerCapPinPaint.apply {
            color = Color.parseColor("#888C96")
            style = Paint.Style.FILL
        }

        logoIwcPaint.apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("serif", Typeface.BOLD)
        }

        logoSubtextPaint.apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            letterSpacing = 0.35f
        }

        hourNumeralPaint.apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }

        dateBoxBgPaint.apply {
            color = Color.parseColor("#0E1014")
            style = Paint.Style.FILL
        }

        dateBoxBorderPaint.apply {
            color = Color.parseColor("#2A2E38")
            style = Paint.Style.STROKE
            strokeWidth = 1.8f
        }

        dateTextPaint.apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        }

        glassReflectionPaint.apply {
            style = Paint.Style.FILL
        }
    }

    fun drawIwcClock(
        canvas: Canvas,
        w: Float,
        h: Float,
        prefs: ClockPreferences,
        calendar: Calendar,
        centerYFactor: Float = 0.5f
    ) {
        val cx = w / 2f
        val radius = min(w, h) * 0.44f
        val cy = maxOf(h * centerYFactor, radius * 1.15f + 2f)

        drawReferenceClock(canvas, cx, cy, radius, prefs, calendar)
    }

    private fun drawReferenceClock(canvas: Canvas, cx: Float, cy: Float, radius: Float, prefs: ClockPreferences, calendar: Calendar) {
        val isLume = prefs.showLume
        val accent = when (prefs.bezelColor) {
            BezelColor.RED -> Color.parseColor("#E11D48")
            BezelColor.SILVER -> Color.parseColor("#E2E8F0")
            BezelColor.BLUE -> Color.parseColor("#0088FF")
            BezelColor.GOLD -> Color.parseColor("#D4AF37")
            BezelColor.WHITE -> Color.WHITE
        }
        val lumeGlow = when (prefs.lumeColor) {
            LumeColor.BLUE -> Color.argb(217, 56, 189, 248)
            LumeColor.SOLAR -> Color.argb(230, 251, 146, 60)
            LumeColor.GREEN -> Color.argb(230, 52, 211, 153)
        }
        val lumeTint = when (prefs.lumeColor) {
            LumeColor.BLUE -> Color.parseColor("#93C5FD")
            LumeColor.SOLAR -> Color.parseColor("#FED7AA")
            LumeColor.GREEN -> Color.parseColor("#A7F3D0")
        }
        val lumeFill = if (isLume) lumeTint else Color.parseColor("#F4EFE6")
        val chromeLight = Color.parseColor("#E2E8F0")
        val chromeDark = Color.parseColor("#475569")
        // The reference web dial uses an ivory lume fill on a dark face in day mode.
        val isAmgSilver = prefs.theme == ClockTheme.AMG_SILVER
        val themeInk = if (isAmgSilver) Color.parseColor("#171A20") else Color.parseColor("#F4EFE6")

        canvas.drawColor(Color.parseColor("#050607"))
        drawWebCasingAndBezel(canvas, cx, cy, radius)
        drawWebDialFace(canvas, cx, cy, radius, prefs.theme)
        drawWebBasketweave(canvas, cx, cy, radius, isAmgSilver)
        drawWebRehaut(canvas, cx, cy, radius, accent, isLume, isAmgSilver, prefs.showMinuteRing)
        drawWebNumeralsAndBatons(canvas, cx, cy, radius, isLume, lumeGlow, lumeFill, chromeLight, chromeDark, isAmgSilver)
        if (prefs.logo != ClockLogo.NONE) drawBrandLogo(canvas, cx, cy, radius, themeInk, prefs.logo)
        if (prefs.showDate) drawReferenceDate(canvas, cx, cy, radius, calendar, !isAmgSilver)
        drawWebHands(canvas, cx, cy, radius, calendar, prefs, accent, isLume, lumeGlow, lumeFill)
        drawWebCenterCap(canvas, cx, cy, radius)
    }

    private fun drawWebCasingAndBezel(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val housing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, cy, r * 1.15f,
                intArrayOf(Color.parseColor("#101216"), Color.parseColor("#101216"), Color.parseColor("#0B0C0E"), Color.parseColor("#050607")),
                floatArrayOf(0f, 0.78f, 0.86f, 1f), Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(cx, cy, r * 1.15f, housing)

        val bezelPath = Path().apply {
            fillType = Path.FillType.EVEN_ODD
            addCircle(cx, cy, r * 1.055f, Path.Direction.CW)
            addCircle(cx, cy, r * 0.975f, Path.Direction.CCW)
        }
        val unit = r / 240f
        val chrome = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(cx - r, cy - r, cx + r, cy + r,
                intArrayOf(Color.parseColor("#CBD5E1"), Color.WHITE, Color.parseColor("#64748B"), Color.parseColor("#F8FAFC"), Color.parseColor("#475569"), Color.parseColor("#94A3B8")),
                floatArrayOf(0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f), Shader.TileMode.CLAMP)
            setShadowLayer(12f * unit, 0f, 0f, Color.argb(180, 0, 0, 0))
        }
        canvas.drawPath(bezelPath, chrome)
        val recess = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0F1116"); style = Paint.Style.STROKE; strokeWidth = 2f * unit }
        canvas.drawCircle(cx, cy, r * 0.975f, recess)
    }

    private fun drawWebDialFace(canvas: Canvas, cx: Float, cy: Float, r: Float, theme: ClockTheme) {
        val colors = when (theme) {
            ClockTheme.BLACK -> intArrayOf(Color.parseColor("#141820"), Color.parseColor("#0C0E13"), Color.parseColor("#07080B"))
            ClockTheme.MIDNIGHT_BLUE -> intArrayOf(Color.parseColor("#15233B"), Color.parseColor("#091629"), Color.parseColor("#050C1B"))
            ClockTheme.AMG_SILVER -> intArrayOf(Color.parseColor("#F1F3F6"), Color.parseColor("#C8CDD5"), Color.parseColor("#8E96A3"))
        }
        val dial = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, cy, r * 0.96f,
                colors,
                floatArrayOf(0.3f / 0.96f, 0.7f / 0.96f, 1f), Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(cx, cy, r * 0.965f, dial)
    }

    private fun getBasketweaveShader(scale: Float): BitmapShader {
        val shader = basketweaveShader
        if (shader != null) {
            shader.setLocalMatrix(Matrix().apply { setScale(scale, scale) })
            return shader
        }
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        val c = Canvas(bitmap)
        c.drawColor(Color.parseColor("#0B0D12"))
        val threadPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        fun thread(x: Float, y: Float, width: Float, height: Float) {
            threadPaint.shader = LinearGradient(x, y, x, y + height,
                intArrayOf(Color.parseColor("#1C212B"), Color.parseColor("#282F3D"), Color.parseColor("#0F1218")),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
            c.drawRect(x + 1f, y + 1f, x + width - 1f, y + height - 1f, threadPaint)
            threadPaint.shader = null
            threadPaint.color = Color.argb(31, 255, 255, 255)
            c.drawRect(x + 1f, y + 1f, x + width - 1f, y + 2f, threadPaint)
            threadPaint.color = Color.argb(153, 0, 0, 0)
            c.drawRect(x + 1f, y + height - 2f, x + width - 1f, y + height - 1f, threadPaint)
            threadPaint.color = Color.parseColor("#05070A")
            threadPaint.style = Paint.Style.STROKE
            threadPaint.strokeWidth = 1f
            c.drawRect(x + 0.5f, y + 0.5f, x + width - 0.5f, y + height - 0.5f, threadPaint)
            threadPaint.style = Paint.Style.FILL
        }
        thread(0f, 0f, 16f, 8f)
        thread(16f, 8f, 16f, 8f)
        thread(0f, 16f, 16f, 8f)
        thread(16f, 24f, 16f, 8f)
        threadPaint.color = Color.parseColor("#141820")
        c.drawRect(16f, 0f, 32f, 8f, threadPaint)
        c.drawRect(0f, 8f, 16f, 16f, threadPaint)
        c.drawRect(16f, 16f, 32f, 24f, threadPaint)
        c.drawRect(0f, 24f, 16f, 32f, threadPaint)
        return BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT).also {
            it.setLocalMatrix(Matrix().apply { setScale(scale, scale) })
            basketweaveShader = it
        }
    }

    private fun drawWebBasketweave(canvas: Canvas, cx: Float, cy: Float, r: Float, silver: Boolean) {
        val centerR = r * 0.60f
        val save = canvas.save()
        canvas.clipPath(Path().apply { addCircle(cx, cy, centerR, Path.Direction.CW) })
        val pattern = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = getBasketweaveShader(r / 240f) }
        canvas.drawRect(cx - centerR, cy - centerR, cx + centerR, cy + centerR, pattern)
        val vignette = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, cy, centerR,
                intArrayOf(Color.argb(13, 0, 0, 0), Color.argb(89, 0, 0, 0), Color.argb(191, 0, 0, 0)),
                floatArrayOf(0.3f, 0.8f, 1f), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(cx - centerR, cy - centerR, cx + centerR, cy + centerR, vignette)
        canvas.restoreToCount(save)
        if (silver) {
            val tintSave = canvas.save()
            canvas.clipPath(Path().apply { addCircle(cx, cy, centerR, Path.Direction.CW) })
            canvas.drawColor(Color.argb(142, 215, 220, 228))
            canvas.restoreToCount(tintSave)
        }
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(64, 203, 213, 225); style = Paint.Style.STROKE; strokeWidth = r / 240f }
        canvas.drawCircle(cx, cy, centerR, border)
    }

    private fun drawWebRehaut(canvas: Canvas, cx: Float, cy: Float, r: Float, accent: Int, isLume: Boolean, silver: Boolean, showMinuteRing: Boolean) {
        val outer = r * 0.95f
        val ringR = r * 0.88f
        val inner = r * 0.84f
        val unit = r / 240f
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent; style = Paint.Style.STROKE; strokeWidth = 2f * unit
            if (showMinuteRing && isLume) setShadowLayer(8f * unit, 0f, 0f, accent)
        }
        if (showMinuteRing) canvas.drawCircle(cx, cy, ringR, ring)
        val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.BUTT }
        for (i in 0 until 60) {
            val a = Math.toRadians(i * 6.0 - 90.0)
            val major = i % 5 == 0
            val start = if (major) outer else outer - 2f * unit
            val end = if (major) inner + 2f * unit else ringR + 2f * unit
            tick.color = if (major && showMinuteRing) accent else if (silver) {
                if (major) Color.parseColor("#334155") else Color.parseColor("#64748B")
            } else if (major) Color.parseColor("#F8FAFC") else Color.parseColor("#94A3B8")
            tick.strokeWidth = (if (major) 2.4f else 1.1f) * unit
            canvas.drawLine(cx + cos(a).toFloat() * start, cy + sin(a).toFloat() * start, cx + cos(a).toFloat() * end, cy + sin(a).toFloat() * end, tick)
            if (major && showMinuteRing) {
                canvas.save()
                canvas.rotate(Math.toDegrees(a).toFloat() + 90f, cx + cos(a).toFloat() * ringR, cy + sin(a).toFloat() * ringR)
                val bx = cx + cos(a).toFloat() * ringR
                val by = cy + sin(a).toFloat() * ringR
                val block = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
                canvas.drawRect(bx - 2.5f * unit, by - 1.8f * unit, bx + 2.5f * unit, by + 1.8f * unit, block)
                canvas.restore()
            }
        }
    }

    private fun drawWebNumeralsAndBatons(canvas: Canvas, cx: Float, cy: Float, r: Float, isLume: Boolean, glow: Int, lumeFill: Int, chromeLight: Int, chromeDark: Int, silver: Boolean) {
        val unit = r / 240f
        val numeralFill = if (silver && !isLume) Color.parseColor("#17202D") else lumeFill
        val numeralChrome = if (silver) Color.parseColor("#64748B") else chromeLight
        val numeralPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER; typeface = Typeface.create("sans-serif", Typeface.BOLD); textSize = r * 0.20f
        }
        fun numeral(value: String, y: Float) {
            val x = cx
            if (isLume) {
                val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = lumeFill; textAlign = Paint.Align.CENTER; typeface = numeralPaint.typeface; textSize = numeralPaint.textSize; setShadowLayer(18f * unit, 0f, 0f, glow) }
                canvas.drawText(value, x, y - (halo.ascent() + halo.descent()) / 2f, halo)
            }
            val baseline = y - (numeralPaint.ascent() + numeralPaint.descent()) / 2f
            numeralPaint.style = Paint.Style.STROKE
            numeralPaint.color = numeralChrome
            numeralPaint.strokeWidth = 3.5f * unit
            canvas.drawText(value, x, baseline, numeralPaint)
            numeralPaint.color = chromeDark
            numeralPaint.strokeWidth = 1.5f * unit
            canvas.drawText(value, x, baseline, numeralPaint)
            numeralPaint.style = Paint.Style.FILL
            numeralPaint.color = numeralFill
            canvas.drawText(value, x, baseline, numeralPaint)
        }
        numeral("12", cy - r * 0.71f)
        numeral("6", cy + r * 0.735f)

        for (hour in intArrayOf(1, 2, 3, 4, 5, 7, 8, 9, 10, 11)) {
            val horizontal = hour == 3 || hour == 9
            val length = r * if (horizontal) 0.20f else 0.17f
            val width = r * 0.046f
            val outer = r * 0.81f
            val a = hour * 30f
            canvas.save()
            canvas.rotate(a, cx, cy)
            val rect = RectF(cx - width / 2f, cy - outer, cx + width / 2f, cy - outer + length)
            val casing = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (silver) Color.parseColor("#475569") else Color.parseColor("#CBD5E1"); setShadowLayer(4f * unit, 0f, 2f * unit, Color.argb(153, 0, 0, 0)) }
            canvas.drawRect(rect, casing)
            val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = chromeDark; style = Paint.Style.STROKE; strokeWidth = unit }
            canvas.drawRect(rect, edge)
            val padX = 1.8f * unit
            val padY = 2f * unit
            val inner = RectF(rect.left + padX, rect.top + padY, rect.right - padX, rect.bottom - padY)
            val inlay = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (silver && !isLume) Color.parseColor("#D5DAE2") else lumeFill; if (isLume) setShadowLayer(14f * unit, 0f, 0f, glow) }
            canvas.drawRect(inner, inlay)
            canvas.restore()
        }
    }

    private fun drawReferenceIndices(canvas: Canvas, cx: Float, cy: Float, radius: Float, ink: Int, showLume: Boolean) {
        val outer = radius - 16f
        val numeralRadius = radius * 0.72f
        val major = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink; strokeWidth = maxOf(3.5f, radius * 0.009f); strokeCap = Paint.Cap.ROUND }
        val minor = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = intColorWithAlpha(ink, 115); strokeWidth = maxOf(1.5f, radius * 0.004f); strokeCap = Paint.Cap.ROUND }
        val numerals = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink; textAlign = Paint.Align.CENTER; typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL); textSize = radius * 0.20f }
        val lume = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#D9F5D8"); strokeCap = Paint.Cap.ROUND }
        val ambient = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55D9F5D8; strokeCap = Paint.Cap.ROUND; strokeWidth = radius * 0.045f }
        for (i in 0 until 60) {
            val angle = Math.toRadians(i * 6.0 - 90.0)
            val dx = cos(angle).toFloat()
            val dy = sin(angle).toFloat()
            if (i % 5 == 0) {
                val hour = if (i == 0) 12 else i / 5
                if (hour == 12 || hour == 6) {
                    val x = cx + dx * numeralRadius
                    val y = cy + dy * numeralRadius - (numerals.descent() + numerals.ascent()) / 2f
                    canvas.drawText(hour.toString(), x, y, numerals)
                } else {
                    val inner = outer - radius * 0.08f
                    canvas.drawLine(cx + dx * inner, cy + dy * inner, cx + dx * outer, cy + dy * outer, major)
                    if (showLume) {
                        canvas.drawLine(cx + dx * (inner + radius * 0.015f), cy + dy * (inner + radius * 0.015f), cx + dx * (outer - radius * 0.015f), cy + dy * (outer - radius * 0.015f), ambient)
                        lume.strokeWidth = radius * 0.012f
                        canvas.drawLine(cx + dx * (inner + radius * 0.015f), cy + dy * (inner + radius * 0.015f), cx + dx * (outer - radius * 0.015f), cy + dy * (outer - radius * 0.015f), lume)
                    }
                }
            } else {
                val inner = outer - radius * 0.035f
                canvas.drawLine(cx + dx * inner, cy + dy * inner, cx + dx * outer, cy + dy * outer, minor)
            }
        }
    }

    private fun drawReferenceDate(canvas: Canvas, cx: Float, cy: Float, radius: Float, calendar: Calendar, darkDial: Boolean) {
        val x = cx + radius * 0.48f
        val boxH = radius * 0.12f
        val boxW = radius * 0.20f
        val rect = RectF(x - boxW / 2f, cy - boxH / 2f, x + boxW / 2f, cy + boxH / 2f)
        val unit = radius / 240f
        val outer = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(rect.left, rect.top, rect.left, rect.bottom,
                intArrayOf(Color.parseColor("#F8FAFC"), Color.parseColor("#94A3B8"), Color.parseColor("#475569")),
                null, Shader.TileMode.CLAMP)
            setShadowLayer(4f * unit, 0f, 1.5f * unit, Color.argb(170, 0, 0, 0))
        }
        val inner = RectF(rect.left + 2.2f * unit, rect.top + 2.2f * unit, rect.right - 2.2f * unit, rect.bottom - 2.2f * unit)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (darkDial) Color.parseColor("#F4EFE6") else Color.parseColor("#202630") }
        val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (darkDial) Color.parseColor("#64748B") else Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 1f * unit
        }
        canvas.drawRoundRect(rect, boxH * 0.16f, boxH * 0.16f, outer)
        canvas.drawRoundRect(inner, boxH * 0.10f, boxH * 0.10f, fill)
        canvas.drawRoundRect(inner, boxH * 0.10f, boxH * 0.10f, frame)
        dateTextPaint.color = if (darkDial) Color.parseColor("#171A20") else Color.parseColor("#F4EFE6")
        dateTextPaint.typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        dateTextPaint.textSize = boxH * 0.76f
        val baseline = cy - (dateTextPaint.descent() + dateTextPaint.ascent()) / 2f
        canvas.drawText(calendar.get(Calendar.DAY_OF_MONTH).toString(), x, baseline, dateTextPaint)
    }

    private fun drawWebHands(canvas: Canvas, cx: Float, cy: Float, r: Float, calendar: Calendar, prefs: ClockPreferences, accent: Int, isLume: Boolean, glow: Int, lumeFill: Int) {
        val seconds = calendar.get(Calendar.SECOND)
        val millis = calendar.get(Calendar.MILLISECOND)
        val secondsValue = if (prefs.showSeconds) seconds + (millis / 125) * 0.125f else seconds.toFloat()
        val minutesValue = calendar.get(Calendar.MINUTE) + secondsValue / 60f
        val hourValue = (calendar.get(Calendar.HOUR_OF_DAY) % 12) + minutesValue / 60f
        drawWebHand(canvas, cx, cy, hourValue * 30f, r * 0.54f, r * 0.052f, r * 0.38f, r / 240f, isLume, glow, lumeFill)
        drawWebHand(canvas, cx, cy, minutesValue * 6f, r * 0.82f, r * 0.044f, r * 0.60f, r / 240f, isLume, glow, lumeFill)
        if (prefs.showSeconds) {
            val unit = r / 240f
            canvas.save()
            canvas.rotate(secondsValue * 6f, cx, cy)
            val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent; strokeWidth = 2f * unit; strokeCap = Paint.Cap.ROUND; setShadowLayer(6f * unit, 2f * unit, 3f * unit, Color.argb(128, 0, 0, 0)) }
            canvas.drawLine(cx, cy + r * 0.22f, cx, cy - r * 0.88f, shadow)
            val tail = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent; style = Paint.Style.STROKE; strokeWidth = 1.8f * unit }
            canvas.drawCircle(cx, cy + r * 0.14f, r * 0.03f, tail)
            canvas.restore()
        }
    }

    private fun drawWebHand(canvas: Canvas, cx: Float, cy: Float, angle: Float, length: Float, baseWidth: Float, windowLength: Float, unit: Float, isLume: Boolean, glow: Int, lumeFill: Int) {
        canvas.save()
        canvas.rotate(angle, cx, cy)
        canvas.translate(3f * unit, 5f * unit)
        val handPath = createWebHandPath(cx, cy, length, baseWidth)
        val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(150, 0, 0, 0); setShadowLayer(10f * unit, 0f, 0f, Color.argb(170, 0, 0, 0)) }
        canvas.drawPath(handPath, shadow)
        canvas.translate(-3f * unit, -5f * unit)

        val half = baseWidth / 2f
        val metal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(cx - half, cy, cx + half, cy,
                intArrayOf(Color.parseColor("#E2E8F0"), Color.parseColor("#F8FAFC"), Color.parseColor("#94A3B8")), null, Shader.TileMode.CLAMP)
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = unit
        }
        canvas.drawPath(handPath, metal)
        val windowWidth = half * 0.9f
        val window = RectF(cx - windowWidth / 2f, cy - length * 0.82f, cx + windowWidth / 2f, cy - length * 0.82f + windowLength)
        val lume = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = lumeFill; if (isLume) setShadowLayer(16f * unit, 0f, 0f, glow) }
        canvas.drawRect(window, lume)
        canvas.restore()
    }

    private fun createWebHandPath(cx: Float, cy: Float, length: Float, baseWidth: Float): Path {
        val half = baseWidth / 2f
        val tail = baseWidth * 1.5f
        return Path().apply {
            moveTo(cx - half, cy + tail)
            lineTo(cx - half, cy - length * 0.88f)
            quadTo(cx - half, cy - length, cx, cy - length)
            quadTo(cx + half, cy - length, cx + half, cy - length * 0.88f)
            lineTo(cx + half, cy + tail)
            quadTo(cx, cy + tail + half, cx - half, cy + tail)
            close()
        }
    }

    private fun drawWebCenterCap(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val capR = r * 0.048f
        val cap = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, cy, r * 0.05f,
                intArrayOf(Color.WHITE, Color.parseColor("#CBD5E1"), Color.parseColor("#475569")),
                floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
            setShadowLayer(6f * r / 240f, 0f, 0f, Color.argb(179, 0, 0, 0))
        }
        canvas.drawCircle(cx, cy, capR, cap)
        val pin = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0F172A") }
        canvas.drawCircle(cx, cy, r * 0.02f, pin)
    }

    private fun drawReferenceHands(canvas: Canvas, cx: Float, cy: Float, radius: Float, calendar: Calendar, prefs: ClockPreferences, ink: Int) {
        val second = calendar.get(Calendar.SECOND)
        val millis = calendar.get(Calendar.MILLISECOND)
        val secondProgress = if (prefs.showSeconds) second + (millis / 125) * 0.125f else second.toFloat()
        val minuteProgress = calendar.get(Calendar.MINUTE) + secondProgress / 60f
        val hourAngle = (calendar.get(Calendar.HOUR) + minuteProgress / 60f) * 30f
        val minuteAngle = minuteProgress * 6f
        val handPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink; style = Paint.Style.FILL_AND_STROKE; strokeWidth = 2f }
        drawReferenceLancette(canvas, cx, cy, hourAngle, radius * 0.52f, radius * 0.045f, handPaint, prefs.showLume)
        drawReferenceLancette(canvas, cx, cy, minuteAngle, radius * 0.78f, radius * 0.035f, handPaint, prefs.showLume)
        if (prefs.showSeconds) {
            val secondsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#D4AF37"); strokeWidth = maxOf(2.2f, radius * 0.0055f); strokeCap = Paint.Cap.ROUND }
            canvas.save()
            canvas.rotate(secondProgress * 6f, cx, cy)
            canvas.drawLine(cx, cy + radius * 0.18f, cx, cy - radius * 0.84f, secondsPaint)
            val tailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = secondsPaint.color; style = Paint.Style.STROKE; strokeWidth = maxOf(1.5f, radius * 0.0035f) }
            canvas.drawCircle(cx, cy + radius * 0.126f, radius * 0.025f, tailPaint)
            canvas.restore()
        }
        val cap = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E0E0E0") }
        val pin = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#15181E") }
        canvas.drawCircle(cx, cy, radius * 0.035f, cap)
        canvas.drawCircle(cx, cy, radius * 0.015f, pin)
    }

    private fun drawReferenceLancette(canvas: Canvas, cx: Float, cy: Float, angle: Float, length: Float, width: Float, paint: Paint, showLume: Boolean) {
        canvas.save()
        canvas.rotate(angle, cx, cy)
        val path = Path().apply {
            moveTo(cx - width / 2f, cy + width * 1.2f)
            lineTo(cx - width / 2f, cy - length * 0.75f)
            lineTo(cx, cy - length)
            lineTo(cx + width / 2f, cy - length * 0.75f)
            lineTo(cx + width / 2f, cy + width * 1.2f)
            close()
        }
        if (showLume) {
            val ambient = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x44D9F5D8
                style = Paint.Style.STROKE
                strokeWidth = width * 0.72f
                strokeJoin = Paint.Join.ROUND
            }
            canvas.drawPath(path, ambient)
        }
        canvas.drawPath(path, paint)
        canvas.restore()
    }

    private fun drawBrandLogo(canvas: Canvas, cx: Float, cy: Float, radius: Float, primaryColor: Int, logo: ClockLogo) {
        val unit = radius / 240f
        val logoY = cy - radius * 0.40f
        if (logo == ClockLogo.IWC) {
            logoIwcPaint.color = primaryColor
            logoIwcPaint.textSize = radius * 0.115f
            logoIwcPaint.letterSpacing = 3f * unit / logoIwcPaint.textSize
            logoIwcPaint.setShadowLayer(3f * unit, 0f, 0f, Color.argb(204, 0, 0, 0))
            val base = logoY - (logoIwcPaint.ascent() + logoIwcPaint.descent()) / 2f
            canvas.drawText("I W C", cx, base, logoIwcPaint)
            logoIwcPaint.clearShadowLayer()
            logoSubtextPaint.color = if (primaryColor == Color.parseColor("#171A20")) Color.parseColor("#334155") else Color.parseColor("#CBD5E1")
            logoSubtextPaint.textSize = radius * 0.046f
            logoSubtextPaint.letterSpacing = 2.5f * unit / logoSubtextPaint.textSize
            canvas.drawText("SCHAFFHAUSEN", cx, logoY + radius * 0.10f, logoSubtextPaint)
        } else if (logo == ClockLogo.MERCEDES) {
            drawWebMercedesStar(canvas, cx, logoY + radius * 0.11f * 0.35f, radius * 0.11f, unit)
        }
    }

    private fun drawWebMercedesStar(canvas: Canvas, cx: Float, cy: Float, r: Float, unit: Float) {
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E2E8F0"); style = Paint.Style.STROKE; strokeWidth = 2f * unit; setShadowLayer(4f * unit, 0f, 0f, Color.argb(153, 0, 0, 0)) }
        canvas.drawCircle(cx, cy, r, ring)
        for (point in 0 until 3) {
            canvas.save()
            canvas.rotate(point * 120f, cx, cy)
            val left = Path().apply { moveTo(cx, cy); lineTo(cx - r * 0.22f, cy + r * 0.25f); lineTo(cx, cy - r * 0.95f); close() }
            val right = Path().apply { moveTo(cx, cy); lineTo(cx + r * 0.22f, cy + r * 0.25f); lineTo(cx, cy - r * 0.95f); close() }
            val light = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F8FAFC") }
            val shade = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#94A3B8") }
            canvas.drawPath(left, light)
            canvas.drawPath(right, shade)
            canvas.restore()
        }
        val hub = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#CBD5E1") }
        canvas.drawCircle(cx, cy, r * 0.2f, hub)
    }

    private fun drawMercedesStar(canvas: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.STROKE; strokeWidth = r * 0.13f }
        canvas.drawCircle(cx, cy, r, p)
        p.style = Paint.Style.FILL
        val path = Path().apply {
            moveTo(cx, cy - r * 0.88f); lineTo(cx - r * 0.13f, cy - r * 0.08f)
            lineTo(cx - r * 0.08f, cy + r * 0.03f); lineTo(cx + r * 0.08f, cy + r * 0.03f)
            lineTo(cx + r * 0.13f, cy - r * 0.08f); close()
            moveTo(cx - r * 0.76f, cy + r * 0.44f); lineTo(cx - r * 0.08f, cy + r * 0.06f)
            lineTo(cx + r * 0.01f, cy + r * 0.08f); lineTo(cx + r * 0.08f, cy + r * 0.20f)
            lineTo(cx - r * 0.62f, cy + r * 0.58f); close()
            moveTo(cx + r * 0.76f, cy + r * 0.44f); lineTo(cx + r * 0.08f, cy + r * 0.06f)
            lineTo(cx - r * 0.01f, cy + r * 0.08f); lineTo(cx - r * 0.08f, cy + r * 0.20f)
            lineTo(cx + r * 0.62f, cy + r * 0.58f); close()
        }
        canvas.drawPath(path, p)
    }

    private fun intColorWithAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
