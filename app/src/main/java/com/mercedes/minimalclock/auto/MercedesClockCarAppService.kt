package com.mercedes.minimalclock.auto

import android.content.pm.ApplicationInfo
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

/**
 * Android for Cars App Service.
 *
 * When an Android phone connects to the Mercedes-Benz C-Class W205 (via USB CarPlay/Android Auto port),
 * the Android Auto daemon establishes a IPC binder connection to this service.
 */
class MercedesClockCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        return if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            // Allows testing with the Desktop Head Unit (DHU) and Android Auto emulator
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        } else {
            // Enforces verified Android Auto OEM head units
            HostValidator.Builder(applicationContext)
                .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
                .build()
        }
    }

    override fun onCreateSession(): Session {
        return MercedesClockSession()
    }
}
