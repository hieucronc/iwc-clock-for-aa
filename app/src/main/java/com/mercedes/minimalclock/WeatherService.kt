package com.mercedes.minimalclock

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** Fetches a compact current-weather reading for the optional digital clock line. */
object WeatherService {
    @Volatile var reading: String? = null
        private set
    @Volatile private var temperatureC: Double? = null
    @Volatile private var currentCode: Int? = null
    @Volatile private var fetching = false

    fun refresh(context: Context) {
        if (fetching) return
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) {
            reading = "Allow location to show weather"
            return
        }
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = if (fine) listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            else listOf(LocationManager.NETWORK_PROVIDER)
        val location = try {
            (providers
                .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
                .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() })
                .maxByOrNull { it.time }
        } catch (_: SecurityException) { null }
        if (location != null) fetch(location) else requestOneLocation(manager, fine)
    }

    fun formatWeather(showIcon: Boolean, showCondition: Boolean, fahrenheit: Boolean): String {
        val temp = temperatureC ?: return reading ?: "Getting local weather..."
        val code = currentCode ?: return reading ?: "Getting local weather..."
        val parts = mutableListOf<String>()
        if (showIcon) parts += weatherIcon(code)
        if (showCondition) parts += weatherDescription(code)
        val displayTemp = if (fahrenheit) (temp * 9.0 / 5.0 + 32.0).toInt() else temp.toInt()
        parts += "$displayTemp${if (fahrenheit) "\u00B0F" else "\u00B0C"}"
        return parts.joinToString("  ")
    }

    @Suppress("DEPRECATION")
    private fun requestOneLocation(manager: LocationManager, fine: Boolean) {
        val providers = if (fine) listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            else listOf(LocationManager.NETWORK_PROVIDER)
        val provider = providers
            .firstOrNull { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        if (provider == null) {
            reading = "Location unavailable"
            return
        }
        try {
            manager.requestSingleUpdate(provider, object : LocationListener {
                override fun onLocationChanged(location: Location) = fetch(location)
                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) { reading = "Location unavailable" }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            }, Looper.getMainLooper())
        } catch (_: SecurityException) {
            reading = "Allow location to show weather"
        }
    }

    private fun fetch(location: Location) {
        if (fetching) return
        fetching = true
        Thread {
            try {
                val url = URL(String.format(Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.5f&longitude=%.5f&current=temperature_2m,weather_code",
                    location.latitude, location.longitude))
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 7000
                    readTimeout = 7000
                    requestMethod = "GET"
                }
                try {
                    val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                    val current = json.getJSONObject("current")
                    val code = current.getInt("weather_code")
                    temperatureC = current.getDouble("temperature_2m")
                    currentCode = code
                    reading = null
                } finally { connection.disconnect() }
            } catch (_: Exception) {
                reading = "Weather unavailable"
            } finally { fetching = false }
        }.start()
    }

    private fun weatherDescription(code: Int): String = when (code) {
        0 -> "Clear"
        1, 2 -> "Partly cloudy"
        3 -> "Cloudy"
        in 45..48 -> "Fog"
        in 51..67, in 80..82 -> "Rain"
        in 71..77, in 85..86 -> "Snow"
        in 95..99 -> "Thunderstorm"
        else -> "Weather"
    }

    private fun weatherIcon(code: Int): String = when (code) {
        0 -> "\u2600"
        1, 2 -> "\u26C5"
        3 -> "\u2601"
        in 45..48 -> "\u224B"
        in 51..67, in 80..82 -> "\u2614"
        in 71..77, in 85..86 -> "\u2744"
        in 95..99 -> "\u26A1"
        else -> "\u2601"
    }
}
