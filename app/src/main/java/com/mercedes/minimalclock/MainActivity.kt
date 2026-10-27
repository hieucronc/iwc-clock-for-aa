package com.mercedes.minimalclock

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.RadioButton
import android.widget.RadioGroup
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = ClockPreferences(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding.analogClockView.prefs = prefs
        binding.digitalClockView.prefs = prefs

        applyPreferences()

        binding.analogClockView.setOnClickListener { showSettingsDialog() }
        binding.digitalClockView.setOnClickListener { showSettingsDialog() }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        applyPreferences()
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
        if (prefs.style == ClockStyle.ANALOG) {
            binding.analogClockView.visibility = View.VISIBLE
            binding.digitalClockView.visibility = View.GONE
            binding.analogClockView.invalidate()
        } else {
            binding.analogClockView.visibility = View.GONE
            binding.digitalClockView.visibility = View.VISIBLE
            binding.digitalClockView.invalidate()
        }
    }

    private fun showSettingsDialog() {
        val dialog = BottomSheetDialog(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
        dialog.setContentView(dialogView)

        val rgClockStyle = dialogView.findViewById<RadioGroup>(R.id.rgClockStyle)
        val rbAnalog = dialogView.findViewById<RadioButton>(R.id.rbAnalog)
        val rbDigital = dialogView.findViewById<RadioButton>(R.id.rbDigital)

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
        val switchWeatherIcon = dialogView.findViewById<SwitchMaterial>(R.id.switchWeatherIcon)
        val switchWeatherCondition = dialogView.findViewById<SwitchMaterial>(R.id.switchWeatherCondition)
        val rgWeatherUnit = dialogView.findViewById<RadioGroup>(R.id.rgWeatherUnit)

        // Initial states
        if (prefs.style == ClockStyle.ANALOG) rbAnalog.isChecked = true else rbDigital.isChecked = true

        when (prefs.theme) {
            ClockTheme.BLACK -> rbThemeBlack.isChecked = true
            ClockTheme.MIDNIGHT_BLUE -> rbThemeMidnight.isChecked = true
            ClockTheme.AMG_SILVER -> rbThemeAmg.isChecked = true
        }

        switch24Hour.isChecked = prefs.is24HourFormat
        switchSeconds.isChecked = prefs.showSeconds
        switchDate.isChecked = prefs.showDate
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
        updateSettingsVisibility(dialogView)

        // Listeners
        rgClockStyle.setOnCheckedChangeListener { _, checkedId ->
            prefs.style = if (checkedId == R.id.rbAnalog) ClockStyle.ANALOG else ClockStyle.DIGITAL
            applyPreferences()
            updateSettingsVisibility(dialogView)
        }

        rgTheme.setOnCheckedChangeListener { _, checkedId ->
            prefs.theme = when (checkedId) {
                R.id.rbThemeMidnight -> ClockTheme.MIDNIGHT_BLUE
                R.id.rbThemeAmg -> ClockTheme.AMG_SILVER
                else -> ClockTheme.BLACK
            }
            applyPreferences()
        }

        switch24Hour.setOnCheckedChangeListener { _, isChecked ->
            prefs.is24HourFormat = isChecked
            applyPreferences()
        }

        switchSeconds.setOnCheckedChangeListener { _, isChecked ->
            prefs.showSeconds = isChecked
            applyPreferences()
        }

        switchDate.setOnCheckedChangeListener { _, isChecked ->
            prefs.showDate = isChecked
            applyPreferences()
        }

        rgLogo.setOnCheckedChangeListener { _, checkedId ->
            prefs.logo = when (checkedId) {
                R.id.rbLogoIwc -> ClockLogo.IWC
                R.id.rbLogoMercedes -> ClockLogo.MERCEDES
                else -> ClockLogo.NONE
            }
            applyPreferences()
        }

        rgBezelColor.setOnCheckedChangeListener { _, checkedId ->
            prefs.bezelColor = when (checkedId) {
                R.id.rbBezelSilver -> BezelColor.SILVER
                R.id.rbBezelBlue -> BezelColor.BLUE
                R.id.rbBezelGold -> BezelColor.GOLD
                R.id.rbBezelWhite -> BezelColor.WHITE
                else -> BezelColor.RED
            }
            applyPreferences()
        }

        switchMinuteRing.setOnCheckedChangeListener { _, checked ->
            prefs.showMinuteRing = checked
            applyPreferences()
        }

        rgLumeColor.setOnCheckedChangeListener { _, checkedId ->
            prefs.lumeColor = when (checkedId) {
                R.id.rbLumeBlue -> LumeColor.BLUE
                R.id.rbLumeSolar -> LumeColor.SOLAR
                else -> LumeColor.GREEN
            }
            applyPreferences()
        }

        switchLume.setOnCheckedChangeListener { _, isChecked ->
            prefs.showLume = isChecked
            applyPreferences()
        }

        switchWeather.setOnCheckedChangeListener { _, isChecked ->
            prefs.showWeather = isChecked
            if (isChecked && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), WEATHER_PERMISSION_REQUEST)
            } else if (isChecked) {
                WeatherService.refresh(this)
            }
            applyPreferences()
            updateSettingsVisibility(dialogView)
        }

        switchWeatherIcon.setOnCheckedChangeListener { _, checked ->
            prefs.showWeatherIcon = checked
            applyPreferences()
        }

        switchWeatherCondition.setOnCheckedChangeListener { _, checked ->
            prefs.showWeatherCondition = checked
            applyPreferences()
        }

        rgWeatherUnit.setOnCheckedChangeListener { _, checkedId ->
            prefs.weatherFahrenheit = checkedId == R.id.rbFahrenheit
            applyPreferences()
        }

        dialog.show()
    }

    private fun updateSettingsVisibility(root: View) {
        val analog = prefs.style == ClockStyle.ANALOG
        listOf(R.id.labelTheme, R.id.rgTheme, R.id.labelLogo, R.id.rgLogo, R.id.labelBezel,
            R.id.rgBezelColor, R.id.switchMinuteRing, R.id.switchLume, R.id.labelLumeColor, R.id.rgLumeColor).forEach { id ->
            root.findViewById<View>(id).visibility = if (analog) View.VISIBLE else View.GONE
        }
        root.findViewById<View>(R.id.switch24Hour).visibility = if (analog) View.GONE else View.VISIBLE
        root.findViewById<View>(R.id.switchWeather).visibility = if (analog) View.GONE else View.VISIBLE
        val weatherOptions = if (!analog && prefs.showWeather) View.VISIBLE else View.GONE
        listOf(R.id.switchWeatherIcon, R.id.switchWeatherCondition, R.id.rgWeatherUnit).forEach { id ->
            root.findViewById<View>(id).visibility = weatherOptions
        }
    }

    @Deprecated("Use the permission result callback")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == WEATHER_PERMISSION_REQUEST && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
            WeatherService.refresh(this)
        }
    }

    companion object {
        private const val WEATHER_PERMISSION_REQUEST = 701
    }
}
