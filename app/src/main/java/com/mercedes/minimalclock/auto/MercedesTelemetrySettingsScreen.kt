package com.mercedes.minimalclock.auto

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.ParkedOnlyOnClickListener
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.model.Toggle
import com.mercedes.minimalclock.ClockPreferences

/** Per-field visibility controls for the driving information panel. */
class MercedesTelemetrySettingsScreen(
    private val carContext: CarContext,
    private val prefs: ClockPreferences,
    private val onSettingsChanged: () -> Unit,
    private val onRequestLocationPermission: () -> Unit
) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val list = ItemList.Builder()
            .addItem(toggleRow("Current speed", prefs.showTelemetrySpeed) { prefs.showTelemetrySpeed = it })
            .addItem(toggleRow("Acceleration", prefs.showTelemetryAcceleration) { prefs.showTelemetryAcceleration = it })
            .addItem(toggleRow("0–100 km/h time", prefs.showTelemetryZeroToHundred) { prefs.showTelemetryZeroToHundred = it })
            .addItem(toggleRow("Maximum speed", prefs.showTelemetryMaxSpeed) { prefs.showTelemetryMaxSpeed = it })
            .addItem(toggleRow("Altitude", prefs.showTelemetryAltitude) { prefs.showTelemetryAltitude = it })
            .addItem(toggleRow("Heading", prefs.showTelemetryHeading) { prefs.showTelemetryHeading = it })
            .addItem(toggleRow("G-force", prefs.showTelemetryGForce) { prefs.showTelemetryGForce = it })

        if (ContextCompat.checkSelfPermission(carContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            list.addItem(
                Row.Builder()
                    .setTitle("Allow speed data while parked")
                    .addText("Uses the phone GPS for speed, altitude and heading")
                    .setOnClickListener(ParkedOnlyOnClickListener.create { onRequestLocationPermission() })
                    .build()
            )
        }

        return ListTemplate.Builder()
            .setTitle("Telemetry fields")
            .setHeaderAction(Action.BACK)
            .setSingleList(list.build())
            .build()
    }

    private fun toggleRow(title: String, checked: Boolean, update: (Boolean) -> Unit) =
        Row.Builder()
            .setTitle(title)
            .setToggle(Toggle.Builder { value -> update(value); changed() }.setChecked(checked).build())
            .build()

    private fun changed() {
        onSettingsChanged()
        invalidate()
    }
}
