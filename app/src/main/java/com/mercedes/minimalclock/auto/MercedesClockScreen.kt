package com.mercedes.minimalclock.auto

import android.Manifest
import android.content.pm.PackageManager
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.OnRequestPermissionsListener
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.Action
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import com.mercedes.minimalclock.ClockPreferences

/** Screen projected onto the Mercedes-Benz W205 infotainment display. */
class MercedesClockScreen(private val cContext: CarContext) : Screen(cContext) {

    private val prefs = ClockPreferences(cContext)
    private val surfaceRenderer = ClockSurfaceRenderer(cContext, prefs)

    init {
        cContext.getCarService(AppManager::class.java)
            .setSurfaceCallback(surfaceRenderer)
    }

    override fun onGetTemplate(): Template =
        NavigationTemplate.Builder()
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(
                        Action.Builder()
                            .setTitle("Settings")
                            .setOnClickListener { openSettings() }
                            .build()
                    )
                    .build()
            )
            .build()

    private fun openSettings() {
        cContext.getCarService(ScreenManager::class.java).push(
            MercedesClockSettingsScreen(
                cContext,
                prefs,
                onSettingsChanged = { surfaceRenderer.notifyStyleChanged() },
                onRequestTelemetryPermission = { requestTachometerPermission() }
            )
        )
    }

    private fun requestTachometerPermission() {
        if (cContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            surfaceRenderer.onLocationPermissionGranted()
            return
        }
        cContext.requestPermissions(mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION), OnRequestPermissionsListener { granted, _ ->
            if (Manifest.permission.ACCESS_FINE_LOCATION in granted) {
                surfaceRenderer.onLocationPermissionGranted()
                surfaceRenderer.notifyStyleChanged()
            }
        })
    }
}
