package com.mercedes.minimalclock

import android.content.Context
import android.content.SharedPreferences

enum class ClockStyle {
    ANALOG,
    DIGITAL
}
enum class AnalogFaceStyle { MERCEDES, MODERN }
enum class DigitalFont { SANS, MONOSPACE, SERIF }
enum class DigitalColor { IVORY, WHITE, CYAN, AMBER, RED }
enum class DigitalPattern { SOLID, GRID, CARBON, SCANLINES, BLUEPRINT, DOT_MATRIX, HEXAGON }

enum class ClockTheme {
    BLACK,
    MIDNIGHT_BLUE,
    AMG_SILVER
}

enum class ClockLogo { NONE, IWC, MERCEDES }
enum class BezelColor { RED, SILVER, BLUE, GOLD, WHITE }
enum class LumeColor { GREEN, BLUE, SOLAR }

/**
 * Manages persisted user settings for the Mercedes Minimal Clock.
 * Read locally with zero network calls, zero tracking.
 */
class ClockPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun addChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.registerOnSharedPreferenceChangeListener(listener)

    fun removeChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.unregisterOnSharedPreferenceChangeListener(listener)

    var style: ClockStyle
        get() {
            val name = prefs.getString(KEY_STYLE, ClockStyle.ANALOG.name)
            return runCatching { ClockStyle.valueOf(name!!) }.getOrDefault(ClockStyle.ANALOG)
        }
        set(value) = prefs.edit().putString(KEY_STYLE, value.name).apply()

    var is24HourFormat: Boolean
        get() = prefs.getBoolean(KEY_24_HOUR, true)
        set(value) = prefs.edit().putBoolean(KEY_24_HOUR, value).apply()

    var showSeconds: Boolean
        get() = prefs.getBoolean(KEY_SHOW_SECONDS, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_SECONDS, value).apply()

    var tachometerEnabled: Boolean
        get() = prefs.getBoolean("tachometer_enabled", false)
        set(value) = prefs.edit().putBoolean("tachometer_enabled", value).apply()
    var showTelemetrySpeed: Boolean
        get() = prefs.getBoolean("telemetry_speed", true)
        set(value) = prefs.edit().putBoolean("telemetry_speed", value).apply()
    var showTelemetryAcceleration: Boolean
        get() = prefs.getBoolean("telemetry_acceleration", true)
        set(value) = prefs.edit().putBoolean("telemetry_acceleration", value).apply()
    var showTelemetryZeroToHundred: Boolean
        get() = prefs.getBoolean("telemetry_zero_to_hundred", true)
        set(value) = prefs.edit().putBoolean("telemetry_zero_to_hundred", value).apply()
    var showTelemetryMaxSpeed: Boolean
        get() = prefs.getBoolean("telemetry_max_speed", true)
        set(value) = prefs.edit().putBoolean("telemetry_max_speed", value).apply()
    var showTelemetryAltitude: Boolean
        get() = prefs.getBoolean("telemetry_altitude", true)
        set(value) = prefs.edit().putBoolean("telemetry_altitude", value).apply()
    var showTelemetryHeading: Boolean
        get() = prefs.getBoolean("telemetry_heading", true)
        set(value) = prefs.edit().putBoolean("telemetry_heading", value).apply()
    var showTelemetryGForce: Boolean
        get() = prefs.getBoolean("telemetry_g_force", true)
        set(value) = prefs.edit().putBoolean("telemetry_g_force", value).apply()
    var digitalBold: Boolean
        get() = prefs.getBoolean("digital_bold", false)
        set(value) = prefs.edit().putBoolean("digital_bold", value).apply()
    var digitalItalic: Boolean
        get() = prefs.getBoolean("digital_italic", false)
        set(value) = prefs.edit().putBoolean("digital_italic", value).apply()
    var digitalFont: DigitalFont
        get() = runCatching { DigitalFont.valueOf(prefs.getString("digital_font", DigitalFont.SANS.name)!!) }.getOrDefault(DigitalFont.SANS)
        set(value) = prefs.edit().putString("digital_font", value.name).apply()
    var digitalColor: DigitalColor
        get() = runCatching { DigitalColor.valueOf(prefs.getString("digital_color", DigitalColor.IVORY.name)!!) }.getOrDefault(DigitalColor.IVORY)
        set(value) = prefs.edit()
            .putString("digital_color", value.name)
            .putString("digital_hour_color", value.name)
            .putString("digital_minute_color", value.name)
            .putString("digital_second_color", value.name)
            .putString("digital_separator_color", value.name)
            .apply()
    var digitalHourColor: DigitalColor
        get() = colorPreference("digital_hour_color")
        set(value) = prefs.edit().putString("digital_hour_color", value.name).apply()
    var digitalMinuteColor: DigitalColor
        get() = colorPreference("digital_minute_color")
        set(value) = prefs.edit().putString("digital_minute_color", value.name).apply()
    var digitalSecondColor: DigitalColor
        get() = colorPreference("digital_second_color")
        set(value) = prefs.edit().putString("digital_second_color", value.name).apply()
    var digitalSeparatorColor: DigitalColor
        get() = colorPreference("digital_separator_color")
        set(value) = prefs.edit().putString("digital_separator_color", value.name).apply()
    private fun colorPreference(key: String): DigitalColor = runCatching {
        DigitalColor.valueOf(prefs.getString(key, digitalColor.name)!!)
    }.getOrDefault(digitalColor)
    var digitalPattern: DigitalPattern
        get() = runCatching { DigitalPattern.valueOf(prefs.getString("digital_pattern", DigitalPattern.SOLID.name)!!) }.getOrDefault(DigitalPattern.SOLID)
        set(value) = prefs.edit().putString("digital_pattern", value.name).apply()
    var analogFaceStyle: AnalogFaceStyle
        get() = runCatching { AnalogFaceStyle.valueOf(prefs.getString("analog_face_style", AnalogFaceStyle.MERCEDES.name)!!) }.getOrDefault(AnalogFaceStyle.MERCEDES)
        set(value) = prefs.edit().putString("analog_face_style", value.name).apply()
    var showDate: Boolean
        get() = prefs.getBoolean(KEY_SHOW_DATE, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_DATE, value).apply()

    var showLogo: Boolean
        get() = logo != ClockLogo.NONE
        set(value) { logo = if (value) ClockLogo.IWC else ClockLogo.NONE }

    var logo: ClockLogo
        get() {
            val stored = prefs.getString(KEY_LOGO, null)
            if (stored == null && prefs.contains(KEY_SHOW_LOGO) && !prefs.getBoolean(KEY_SHOW_LOGO, true)) return ClockLogo.NONE
            return runCatching { ClockLogo.valueOf(stored ?: ClockLogo.IWC.name) }.getOrDefault(ClockLogo.IWC)
        }
        set(value) = prefs.edit().putString(KEY_LOGO, value.name).apply()

    var bezelColor: BezelColor
        get() = runCatching { BezelColor.valueOf(prefs.getString(KEY_BEZEL_COLOR, BezelColor.RED.name)!!) }.getOrDefault(BezelColor.RED)
        set(value) = prefs.edit().putString(KEY_BEZEL_COLOR, value.name).apply()

    var showMinuteRing: Boolean
        get() = prefs.getBoolean(KEY_SHOW_MINUTE_RING, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_MINUTE_RING, value).apply()

    var showLume: Boolean
        get() = prefs.getBoolean(KEY_SHOW_LUME, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_LUME, value).apply()

    var showWeather: Boolean
        get() = prefs.getBoolean(KEY_SHOW_WEATHER, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_WEATHER, value).apply()

    var showWeatherIcon: Boolean
        get() = prefs.getBoolean(KEY_WEATHER_ICON, true)
        set(value) = prefs.edit().putBoolean(KEY_WEATHER_ICON, value).apply()

    var showWeatherCondition: Boolean
        get() = prefs.getBoolean(KEY_WEATHER_CONDITION, true)
        set(value) = prefs.edit().putBoolean(KEY_WEATHER_CONDITION, value).apply()

    var weatherFahrenheit: Boolean
        get() = prefs.getBoolean(KEY_WEATHER_FAHRENHEIT, false)
        set(value) = prefs.edit().putBoolean(KEY_WEATHER_FAHRENHEIT, value).apply()

    var lumeColor: LumeColor
        get() = runCatching { LumeColor.valueOf(prefs.getString(KEY_LUME_COLOR, LumeColor.GREEN.name)!!) }.getOrDefault(LumeColor.GREEN)
        set(value) = prefs.edit().putString(KEY_LUME_COLOR, value.name).apply()

    var theme: ClockTheme
        get() {
            val name = prefs.getString(KEY_THEME, ClockTheme.BLACK.name)
            return runCatching { ClockTheme.valueOf(name!!) }.getOrDefault(ClockTheme.BLACK)
        }
        set(value) = prefs.edit().putString(KEY_THEME, value.name).apply()

    companion object {
        private const val PREFS_NAME = "mercedes_clock_prefs"
        private const val KEY_STYLE = "key_clock_style"
        private const val KEY_24_HOUR = "key_24_hour"
        private const val KEY_SHOW_SECONDS = "key_show_seconds"
        private const val KEY_SHOW_DATE = "key_show_date"
        private const val KEY_SHOW_LOGO = "key_show_logo"
        private const val KEY_LOGO = "key_logo"
        private const val KEY_BEZEL_COLOR = "key_bezel_color"
        private const val KEY_SHOW_LUME = "key_show_lume"
        private const val KEY_SHOW_WEATHER = "key_show_weather"
        private const val KEY_SHOW_MINUTE_RING = "key_show_minute_ring"
        private const val KEY_WEATHER_ICON = "key_weather_icon"
        private const val KEY_WEATHER_CONDITION = "key_weather_condition"
        private const val KEY_WEATHER_FAHRENHEIT = "key_weather_fahrenheit"
        private const val KEY_LUME_COLOR = "key_lume_color"
        private const val KEY_THEME = "key_theme"
    }
}
