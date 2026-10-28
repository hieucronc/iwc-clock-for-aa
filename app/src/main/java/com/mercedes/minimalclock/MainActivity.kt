package com.mercedes.minimalclock

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.switchmaterial.SwitchMaterial
import com.mercedes.minimalclock.databinding.ActivityMainBinding

/**
 * Main Activity for the standalone phone experience.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: ClockPreferences
    private lateinit var locationManager: LocationManager
    private lateinit var sensorManager: SensorManager
    private var accelerationValue: Float? = null
    private var accelerationStartMs: Long? = null
    private var lastSpeedKmh: Float? = null
    private var zeroToHundred: Float? = null
    private var maxSpeedKmh: Float? = null
    private var altitudeMeters: Double? = null
    private var headingDegrees: Float? = null
    private val gpsListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val speed = (if (location.hasSpeed()) location.speed * 3.6f else 0f).coerceAtLeast(0f)
            val old = lastSpeedKmh
            if (speed <= 2f) { accelerationStartMs = null; zeroToHundred = null }
            else if (old != null && speed < old - 2f && speed < 100f) { accelerationStartMs = null; zeroToHundred = null }
            if (accelerationStartMs == null && speed > 2f && (old == null || old <= 2f)) accelerationStartMs = location.time
            if (speed >= 100f && zeroToHundred == null) accelerationStartMs?.let { zeroToHundred = ((location.time - it).coerceAtLeast(0L) / 1000f) }
            lastSpeedKmh = speed
            maxSpeedKmh = maxOf(maxSpeedKmh ?: 0f, speed)
            altitudeMeters = if (location.hasAltitude()) location.altitude else altitudeMeters
            headingDegrees = if (location.hasBearing()) location.bearing else headingDegrees
            binding.telemetryView.speedKmh = speed
            binding.telemetryView.zeroToHundredSeconds = zeroToHundred
            binding.telemetryView.maxSpeedKmh = maxSpeedKmh
            binding.telemetryView.altitudeMeters = altitudeMeters
            binding.telemetryView.headingDegrees = headingDegrees
            binding.telemetryView.invalidate()
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
            accelerationValue = kotlin.math.sqrt(x * x + y * y + z * z)
            binding.telemetryView.acceleration = accelerationValue
            binding.telemetryView.gForce = accelerationValue?.div(9.80665f)
            binding.telemetryView.invalidate()
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private val handler = Handler(Looper.getMainLooper())
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        if (::binding.isInitialized) runOnUiThread { applyPreferences() }
    }
    private val hideGearRunnable = Runnable {
        binding.btnSettingsGear.animate().alpha(0.0f).setDuration(600).start()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = ClockPreferences(this)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding.analogClockView.prefs = prefs
        binding.digitalClockView.prefs = prefs

        applyPreferences()

        // Settings gear button
        binding.btnSettingsGear.setOnClickListener {
            showSettingsDialog()
        }

        // Screen touch reveals top-right settings gear and schedules 4-second auto-hide
        binding.rootContainer.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                showGearAndScheduleAutoHide()
            }
            false
        }

        showGearAndScheduleAutoHide()
    }

    override fun onResume() {
        super.onResume()
        prefs.addChangeListener(preferenceListener)
        hideSystemUI()
        applyPreferences()
        showGearAndScheduleAutoHide()
        startTelemetrySensors()
    }

    override fun onPause() {
        prefs.removeChangeListener(preferenceListener)
        sensorManager.unregisterListener(accelListener)
        runCatching { locationManager.removeUpdates(gpsListener) }
        super.onPause()
    }

    private fun showGearAndScheduleAutoHide() {
        handler.removeCallbacks(hideGearRunnable)
        binding.btnSettingsGear.animate().cancel()
        binding.btnSettingsGear.alpha = 1.0f
        handler.postDelayed(hideGearRunnable, 4000L)
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        }
    }

    private fun applyPreferences() {
        binding.telemetryView.prefs = prefs
        binding.telemetryView.invalidate()
        binding.telemetryView.visibility = if (prefs.tachometerEnabled) View.VISIBLE else View.GONE
        if (prefs.style == ClockStyle.ANALOG) {
            binding.analogClockView.visibility = View.VISIBLE
            binding.digitalClockView.visibility = View.GONE
            binding.analogClockView.invalidate()
        } else {
            binding.analogClockView.visibility = View.GONE
            binding.digitalClockView.visibility = View.VISIBLE
            binding.digitalClockView.invalidate()
        }
        val clock = if (prefs.style == ClockStyle.ANALOG) binding.analogClockView else binding.digitalClockView
        val params = clock.layoutParams as android.widget.FrameLayout.LayoutParams
        if (prefs.tachometerEnabled) {
            val landscape = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            if (landscape) {
                params.width = (resources.displayMetrics.widthPixels * .49f).toInt()
                params.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT
                params.gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
                params.topMargin = 0
                params.leftMargin = 0
            } else {
                params.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT
                params.height = (resources.displayMetrics.heightPixels * .50f).toInt()
                params.gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
                params.topMargin = 0
                params.leftMargin = 0
            }
        } else {
            params.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT
            params.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT
            params.gravity = android.view.Gravity.CENTER
            params.topMargin = 0; params.leftMargin = 0
        }
        clock.layoutParams = params
    }

    private fun startTelemetrySensors() {
        if (!prefs.tachometerEnabled) return
        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)?.let {
            sensorManager.registerListener(accelListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            runCatching { locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500L, 0f, gpsListener) }
        } else requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), TACHOMETER_PERMISSION_REQUEST)
    }

    private fun showSettingsDialog() {
        val dialog = BottomSheetDialog(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
        dialog.setContentView(dialogView)

        // Live Preview Clock Views
        val previewAnalog = dialogView.findViewById<IwcAnalogClockView>(R.id.previewAnalogClock)
        val previewDigital = dialogView.findViewById<MercedesDigitalClockView>(R.id.previewDigitalClock)

        previewAnalog.prefs = prefs
        previewDigital.prefs = prefs

        val updatePreview = {
            applyPreferences()
            if (prefs.style == ClockStyle.ANALOG) {
                previewAnalog.visibility = View.VISIBLE
                previewDigital.visibility = View.GONE
                previewAnalog.invalidate()
            } else {
                previewAnalog.visibility = View.GONE
                previewDigital.visibility = View.VISIBLE
                previewDigital.invalidate()
            }
            updateSettingsVisibility(dialogView)
        }

        val rgClockStyle = dialogView.findViewById<RadioGroup>(R.id.rgClockStyle)
        val rbAnalog = dialogView.findViewById<RadioButton>(R.id.rbAnalog)
        val rbDigital = dialogView.findViewById<RadioButton>(R.id.rbDigital)
        val rgAnalogFaceStyle = dialogView.findViewById<RadioGroup>(R.id.rgAnalogFaceStyle)

        val rgTheme = dialogView.findViewById<RadioGroup>(R.id.rgTheme)
        val rbThemeBlack = dialogView.findViewById<RadioButton>(R.id.rbThemeBlack)
        val rbThemeMidnight = dialogView.findViewById<RadioButton>(R.id.rbThemeMidnight)
        val rbThemeAmg = dialogView.findViewById<RadioButton>(R.id.rbThemeAmg)

        val switch24Hour = dialogView.findViewById<SwitchMaterial>(R.id.switch24Hour)
        val switchSeconds = dialogView.findViewById<SwitchMaterial>(R.id.switchSeconds)
        val switchDate = dialogView.findViewById<SwitchMaterial>(R.id.switchDate)
        val rgLogo = dialogView.findViewById<RadioGroup>(R.id.rgLogo)
        val rgBezelColor = dialogView.findViewById<RadioGroup>(R.id.rgBezelColor)
        val switchMinuteRing = dialogView.findViewById<SwitchMaterial>(R.id.switchMinuteRing)
        val rgLumeColor = dialogView.findViewById<RadioGroup>(R.id.rgLumeColor)
        val switchLume = dialogView.findViewById<SwitchMaterial>(R.id.switchLume)
        val switchWeather = dialogView.findViewById<SwitchMaterial>(R.id.switchWeather)
        val switchTachometer = dialogView.findViewById<SwitchMaterial>(R.id.switchTachometer)
        val telemetrySettingsButton = dialogView.findViewById<View>(R.id.btnTelemetrySettings)
        val switchDigitalBold = dialogView.findViewById<SwitchMaterial>(R.id.switchDigitalBold)
        val switchDigitalItalic = dialogView.findViewById<SwitchMaterial>(R.id.switchDigitalItalic)
        val rgDigitalFont = dialogView.findViewById<RadioGroup>(R.id.rgDigitalFont)
        val rgDigitalColor = dialogView.findViewById<RadioGroup>(R.id.rgDigitalColor)
        val rgDigitalPattern = dialogView.findViewById<RadioGroup>(R.id.rgDigitalPattern)
        val spinnerHourColor = dialogView.findViewById<Spinner>(R.id.spinnerHourColor)
        val spinnerMinuteColor = dialogView.findViewById<Spinner>(R.id.spinnerMinuteColor)
        val spinnerSecondColor = dialogView.findViewById<Spinner>(R.id.spinnerSecondColor)
        val spinnerSeparatorColor = dialogView.findViewById<Spinner>(R.id.spinnerSeparatorColor)
        val switchWeatherIcon = dialogView.findViewById<SwitchMaterial>(R.id.switchWeatherIcon)
        val switchWeatherCondition = dialogView.findViewById<SwitchMaterial>(R.id.switchWeatherCondition)
        val rgWeatherUnit = dialogView.findViewById<RadioGroup>(R.id.rgWeatherUnit)

        // Initial states
        if (prefs.style == ClockStyle.ANALOG) rbAnalog.isChecked = true else rbDigital.isChecked = true
        rgAnalogFaceStyle.check(if (prefs.analogFaceStyle == AnalogFaceStyle.MERCEDES) R.id.rbAnalogFaceMercedes else R.id.rbAnalogFaceModern)

        when (prefs.theme) {
            ClockTheme.BLACK -> rbThemeBlack.isChecked = true
            ClockTheme.MIDNIGHT_BLUE -> rbThemeMidnight.isChecked = true
            ClockTheme.AMG_SILVER -> rbThemeAmg.isChecked = true
        }

        switch24Hour.isChecked = prefs.is24HourFormat
        switchSeconds.isChecked = prefs.showSeconds
        switchDate.isChecked = prefs.showDate
        switchTachometer.isChecked = prefs.tachometerEnabled
        telemetrySettingsButton.setOnClickListener { showTelemetrySettingsDialog() }
        switchDigitalBold.isChecked = prefs.digitalBold
        switchDigitalItalic.isChecked = prefs.digitalItalic
        rgDigitalFont.check(when (prefs.digitalFont) { DigitalFont.MONOSPACE -> R.id.rbFontMono; DigitalFont.SERIF -> R.id.rbFontSerif; else -> R.id.rbFontSans })
        rgDigitalColor.check(when (prefs.digitalColor) { DigitalColor.WHITE -> R.id.rbColorWhite; DigitalColor.CYAN -> R.id.rbColorCyan; DigitalColor.AMBER -> R.id.rbColorAmber; DigitalColor.RED -> R.id.rbColorRed; else -> R.id.rbColorIvory })
        listOf(spinnerHourColor to prefs.digitalHourColor, spinnerMinuteColor to prefs.digitalMinuteColor,
            spinnerSecondColor to prefs.digitalSecondColor, spinnerSeparatorColor to prefs.digitalSeparatorColor).forEach { (spinner, color) -> spinner.setSelection(color.ordinal) }
        rgDigitalPattern.check(when (prefs.digitalPattern) {
            DigitalPattern.GRID -> R.id.rbPatternGrid
            DigitalPattern.CARBON -> R.id.rbPatternCarbon
            DigitalPattern.SCANLINES -> R.id.rbPatternScanlines
            DigitalPattern.BLUEPRINT -> R.id.rbPatternBlueprint
            DigitalPattern.DOT_MATRIX -> R.id.rbPatternDots
            DigitalPattern.HEXAGON -> R.id.rbPatternHexagon
            else -> R.id.rbPatternSolid
        })
        when (prefs.logo) {
            ClockLogo.NONE -> rgLogo.check(R.id.rbLogoNone)
            ClockLogo.IWC -> rgLogo.check(R.id.rbLogoIwc)
            ClockLogo.MERCEDES -> rgLogo.check(R.id.rbLogoMercedes)
        }
        rgBezelColor.check(when (prefs.bezelColor) {
            BezelColor.RED -> R.id.rbBezelRed
            BezelColor.SILVER -> R.id.rbBezelSilver
            BezelColor.BLUE -> R.id.rbBezelBlue
            BezelColor.GOLD -> R.id.rbBezelGold
            BezelColor.WHITE -> R.id.rbBezelWhite
        })
        rgLumeColor.check(when (prefs.lumeColor) {
            LumeColor.GREEN -> R.id.rbLumeGreen
            LumeColor.BLUE -> R.id.rbLumeBlue
            LumeColor.SOLAR -> R.id.rbLumeSolar
        })
        switchLume.isChecked = prefs.showLume
        switchMinuteRing.isChecked = prefs.showMinuteRing
        switchWeather.isChecked = prefs.showWeather
        switchWeatherIcon.isChecked = prefs.showWeatherIcon
        switchWeatherCondition.isChecked = prefs.showWeatherCondition
        rgWeatherUnit.check(if (prefs.weatherFahrenheit) R.id.rbFahrenheit else R.id.rbCelsius)

        updatePreview()

        // Listeners
        rgClockStyle.setOnCheckedChangeListener { _, checkedId ->
            prefs.style = if (checkedId == R.id.rbAnalog) ClockStyle.ANALOG else ClockStyle.DIGITAL
            updatePreview()
        }
        rgAnalogFaceStyle.setOnCheckedChangeListener { _, checkedId ->
            prefs.analogFaceStyle = if (checkedId == R.id.rbAnalogFaceModern) AnalogFaceStyle.MODERN else AnalogFaceStyle.MERCEDES
            updatePreview()
        }
        rgTheme.setOnCheckedChangeListener { _, checkedId ->
            prefs.theme = when (checkedId) {
                R.id.rbThemeMidnight -> ClockTheme.MIDNIGHT_BLUE
                R.id.rbThemeAmg -> ClockTheme.AMG_SILVER
                else -> ClockTheme.BLACK
            }
            updatePreview()
        }

        switch24Hour.setOnCheckedChangeListener { _, isChecked ->
            prefs.is24HourFormat = isChecked
            updatePreview()
        }

        switchSeconds.setOnCheckedChangeListener { _, isChecked ->
            prefs.showSeconds = isChecked
            updatePreview()
        }

        switchDate.setOnCheckedChangeListener { _, isChecked ->
            prefs.showDate = isChecked
            updatePreview()
        }
        switchTachometer.setOnCheckedChangeListener { _, checked ->
            prefs.tachometerEnabled = checked
            updatePreview()
            if (checked) startTelemetrySensors() else {
                sensorManager.unregisterListener(accelListener)
                runCatching { locationManager.removeUpdates(gpsListener) }
                binding.telemetryView.speedKmh = null
                binding.telemetryView.acceleration = null
                binding.telemetryView.zeroToHundredSeconds = null
                binding.telemetryView.maxSpeedKmh = null
                binding.telemetryView.altitudeMeters = null
                binding.telemetryView.headingDegrees = null
                binding.telemetryView.gForce = null
                maxSpeedKmh = null; altitudeMeters = null; headingDegrees = null
                lastSpeedKmh = null; zeroToHundred = null; accelerationStartMs = null
            }
        }
        switchDigitalBold.setOnCheckedChangeListener { _, checked -> prefs.digitalBold = checked; updatePreview() }
        switchDigitalItalic.setOnCheckedChangeListener { _, checked -> prefs.digitalItalic = checked; updatePreview() }
        rgDigitalFont.setOnCheckedChangeListener { _, id -> prefs.digitalFont = when(id) { R.id.rbFontMono -> DigitalFont.MONOSPACE; R.id.rbFontSerif -> DigitalFont.SERIF; else -> DigitalFont.SANS }; updatePreview() }
        rgDigitalColor.setOnCheckedChangeListener { _, id -> prefs.digitalColor = when(id) { R.id.rbColorWhite -> DigitalColor.WHITE; R.id.rbColorCyan -> DigitalColor.CYAN; R.id.rbColorAmber -> DigitalColor.AMBER; R.id.rbColorRed -> DigitalColor.RED; else -> DigitalColor.IVORY }; updatePreview() }
        listOf(spinnerHourColor to { c: DigitalColor -> prefs.digitalHourColor = c }, spinnerMinuteColor to { c: DigitalColor -> prefs.digitalMinuteColor = c },
            spinnerSecondColor to { c: DigitalColor -> prefs.digitalSecondColor = c }, spinnerSeparatorColor to { c: DigitalColor -> prefs.digitalSeparatorColor = c }).forEach { (spinner, setColor) ->
            spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) { setColor(DigitalColor.values()[position]); updatePreview() }
            }
        }
        rgDigitalPattern.setOnCheckedChangeListener { _, id -> prefs.digitalPattern = when(id) {
            R.id.rbPatternGrid -> DigitalPattern.GRID
            R.id.rbPatternCarbon -> DigitalPattern.CARBON
            R.id.rbPatternScanlines -> DigitalPattern.SCANLINES
            R.id.rbPatternBlueprint -> DigitalPattern.BLUEPRINT
            R.id.rbPatternDots -> DigitalPattern.DOT_MATRIX
            R.id.rbPatternHexagon -> DigitalPattern.HEXAGON
            else -> DigitalPattern.SOLID
        }; updatePreview() }

        rgLogo.setOnCheckedChangeListener { _, checkedId ->
            prefs.logo = when (checkedId) {
                R.id.rbLogoIwc -> ClockLogo.IWC
                R.id.rbLogoMercedes -> ClockLogo.MERCEDES
                else -> ClockLogo.NONE
            }
            updatePreview()
        }

        rgBezelColor.setOnCheckedChangeListener { _, checkedId ->
            prefs.bezelColor = when (checkedId) {
                R.id.rbBezelSilver -> BezelColor.SILVER
                R.id.rbBezelBlue -> BezelColor.BLUE
                R.id.rbBezelGold -> BezelColor.GOLD
                R.id.rbBezelWhite -> BezelColor.WHITE
                else -> BezelColor.RED
            }
            updatePreview()
        }

        switchMinuteRing.setOnCheckedChangeListener { _, checked ->
            prefs.showMinuteRing = checked
            updatePreview()
        }

        rgLumeColor.setOnCheckedChangeListener { _, checkedId ->
            prefs.lumeColor = when (checkedId) {
                R.id.rbLumeBlue -> LumeColor.BLUE
                R.id.rbLumeSolar -> LumeColor.SOLAR
                else -> LumeColor.GREEN
            }
            updatePreview()
        }

        switchLume.setOnCheckedChangeListener { _, isChecked ->
            prefs.showLume = isChecked
            updatePreview()
        }

        switchWeather.setOnCheckedChangeListener { _, isChecked ->
            prefs.showWeather = isChecked
            if (isChecked && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), WEATHER_PERMISSION_REQUEST)
            } else if (isChecked) {
                WeatherService.refresh(this)
            }
            updatePreview()
        }

        switchWeatherIcon.setOnCheckedChangeListener { _, checked ->
            prefs.showWeatherIcon = checked
            updatePreview()
        }

        switchWeatherCondition.setOnCheckedChangeListener { _, checked ->
            prefs.showWeatherCondition = checked
            updatePreview()
        }

        rgWeatherUnit.setOnCheckedChangeListener { _, checkedId ->
            prefs.weatherFahrenheit = checkedId == R.id.rbFahrenheit
            updatePreview()
        }

        dialog.show()
    }

    private fun updateSettingsVisibility(root: View) {
        val analog = prefs.style == ClockStyle.ANALOG
        val mercedesFace = analog && prefs.analogFaceStyle == AnalogFaceStyle.MERCEDES
        listOf(R.id.labelAnalogFaceStyle, R.id.rgAnalogFaceStyle).forEach { id ->
            root.findViewById<View>(id).visibility = if (analog) View.VISIBLE else View.GONE
        }
        listOf(R.id.labelMercedesSettings, R.id.labelTheme, R.id.rgTheme, R.id.labelLogo, R.id.rgLogo, R.id.labelBezel,
            R.id.rgBezelColor, R.id.switchMinuteRing, R.id.switchLume, R.id.labelLumeColor, R.id.rgLumeColor).forEach { id ->
            root.findViewById<View>(id).visibility = if (mercedesFace) View.VISIBLE else View.GONE
        }
        root.findViewById<View>(R.id.switch24Hour).visibility = if (analog) View.GONE else View.VISIBLE
        listOf(R.id.labelDigitalCustomize, R.id.switchDigitalBold, R.id.switchDigitalItalic,
            R.id.labelDigitalFont, R.id.rgDigitalFont, R.id.labelDigitalColor, R.id.rgDigitalColor,
            R.id.labelDigitalPattern, R.id.rgDigitalPattern, R.id.labelTimePartColors, R.id.labelHourColor, R.id.spinnerHourColor, R.id.labelMinuteColor, R.id.spinnerMinuteColor,
            R.id.labelSecondColor,
            R.id.spinnerSecondColor, R.id.labelSeparatorColor, R.id.spinnerSeparatorColor).forEach {
            root.findViewById<View>(it).visibility = if (analog) View.GONE else View.VISIBLE
        }
        root.findViewById<View>(R.id.switchWeather).visibility = if (analog) View.GONE else View.VISIBLE
        root.findViewById<View>(R.id.switchTachometer).visibility = View.VISIBLE
        root.findViewById<View>(R.id.btnTelemetrySettings).visibility = if (prefs.tachometerEnabled) View.VISIBLE else View.GONE
        val weatherOptions = if (!analog && prefs.showWeather) View.VISIBLE else View.GONE
        listOf(R.id.switchWeatherIcon, R.id.switchWeatherCondition, R.id.rgWeatherUnit).forEach { id ->
            root.findViewById<View>(id).visibility = weatherOptions
        }
    }

    private fun showTelemetrySettingsDialog() {
        val dialog = BottomSheetDialog(this)
        val content = layoutInflater.inflate(R.layout.dialog_telemetry_settings, null)
        dialog.setContentView(content)
        val switches = listOf(
            R.id.showTelemetrySpeed to prefs.showTelemetrySpeed,
            R.id.showTelemetryAcceleration to prefs.showTelemetryAcceleration,
            R.id.showTelemetryZeroToHundred to prefs.showTelemetryZeroToHundred,
            R.id.showTelemetryMaxSpeed to prefs.showTelemetryMaxSpeed,
            R.id.showTelemetryAltitude to prefs.showTelemetryAltitude,
            R.id.showTelemetryHeading to prefs.showTelemetryHeading,
            R.id.showTelemetryGForce to prefs.showTelemetryGForce
        )
        switches.forEach { (id, checked) -> content.findViewById<SwitchMaterial>(id).isChecked = checked }
        content.findViewById<SwitchMaterial>(R.id.showTelemetrySpeed).setOnCheckedChangeListener { _, value -> prefs.showTelemetrySpeed = value }
        content.findViewById<SwitchMaterial>(R.id.showTelemetryAcceleration).setOnCheckedChangeListener { _, value -> prefs.showTelemetryAcceleration = value }
        content.findViewById<SwitchMaterial>(R.id.showTelemetryZeroToHundred).setOnCheckedChangeListener { _, value -> prefs.showTelemetryZeroToHundred = value }
        content.findViewById<SwitchMaterial>(R.id.showTelemetryMaxSpeed).setOnCheckedChangeListener { _, value -> prefs.showTelemetryMaxSpeed = value }
        content.findViewById<SwitchMaterial>(R.id.showTelemetryAltitude).setOnCheckedChangeListener { _, value -> prefs.showTelemetryAltitude = value }
        content.findViewById<SwitchMaterial>(R.id.showTelemetryHeading).setOnCheckedChangeListener { _, value -> prefs.showTelemetryHeading = value }
        content.findViewById<SwitchMaterial>(R.id.showTelemetryGForce).setOnCheckedChangeListener { _, value -> prefs.showTelemetryGForce = value }
        dialog.show()
    }

    @Deprecated("Use the permission result callback")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == WEATHER_PERMISSION_REQUEST && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
            WeatherService.refresh(this)
        }
        if (requestCode == TACHOMETER_PERMISSION_REQUEST && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
            startTelemetrySensors()
        }
    }

    companion object {
        private const val WEATHER_PERMISSION_REQUEST = 701
        private const val TACHOMETER_PERMISSION_REQUEST = 702
    }
}
