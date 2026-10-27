package com.mercedes.minimalclock

import android.content.Context
import android.content.SharedPreferences

enum class ClockStyle {
    ANALOG,
    DIGITAL
}

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
