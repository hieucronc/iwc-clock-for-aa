package com.mercedes.minimalclock.auto

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session

/**
 * Manages the lifecycle of the in-vehicle session on the Mercedes W205 COMAND display.
 */
class MercedesClockSession : Session() {

    override fun onCreateScreen(intent: Intent): Screen {
        return MercedesClockScreen(carContext)
    }
}
