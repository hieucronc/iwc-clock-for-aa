package com.mercedes.minimalclock.auto

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.model.Toggle
import com.mercedes.minimalclock.BezelColor
import com.mercedes.minimalclock.ClockLogo
import com.mercedes.minimalclock.ClockPreferences
import com.mercedes.minimalclock.ClockStyle
import com.mercedes.minimalclock.ClockTheme
import com.mercedes.minimalclock.AnalogFaceStyle
import com.mercedes.minimalclock.DigitalColor
import com.mercedes.minimalclock.DigitalFont
import com.mercedes.minimalclock.DigitalPattern
import com.mercedes.minimalclock.LumeColor
import com.mercedes.minimalclock.R
import com.mercedes.minimalclock.WeatherService

class MercedesClockSettingsScreen(
    carContext: CarContext,
    private val prefs: ClockPreferences,
    private val onSettingsChanged: () -> Unit,
    private val onRequestTelemetryPermission: () -> Unit
) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val list = ItemList.Builder()
            .addItem(
                Row.Builder()
                    .setTitle("Clock Style")
                    .addText(prefs.style.label(carContext))
                    .setToggle(
                        Toggle.Builder { isAnalog ->
                            prefs.style = if (isAnalog) ClockStyle.ANALOG else ClockStyle.DIGITAL
                            settingsChanged()
                        }
                            .setChecked(prefs.style == ClockStyle.ANALOG)
                            .build()
                    )
                    .build()
            )

        if (prefs.style == ClockStyle.ANALOG) {
            addAnalogSettings(list)
        } else {
            addDigitalSettings(list)
        }
        list.addItem(toggleRow("Driving information panel", prefs.tachometerEnabled) {
            prefs.tachometerEnabled = it
        })
        if (prefs.tachometerEnabled) {
            list.addItem(
                Row.Builder()
                    .setTitle("Telemetry fields")
                    .addText("Choose which readings appear")
                    .setOnClickListener {
                        carContext.getCarService(ScreenManager::class.java).push(
                            MercedesTelemetrySettingsScreen(carContext, prefs, onSettingsChanged, onRequestTelemetryPermission)
                        )
                    }
                    .build()
            )
        }

        return ListTemplate.Builder()
            .setTitle("Settings")
            .setHeaderAction(Action.BACK)
            .setSingleList(list.build())
            .build()
    }

    private fun addAnalogSettings(list: ItemList.Builder) {
        list.addItem(choiceRow("Analog face style", prefs.analogFaceStyle.label()) {
            prefs.analogFaceStyle = if (prefs.analogFaceStyle == AnalogFaceStyle.MERCEDES) AnalogFaceStyle.MODERN else AnalogFaceStyle.MERCEDES
        })
        if (prefs.analogFaceStyle == AnalogFaceStyle.MERCEDES) {
            list.addItem(
                choiceRow(carContext.getString(R.string.theme_title), prefs.theme.label(carContext)) {
                    prefs.theme = ClockTheme.values().nextAfter(prefs.theme)
                }
            )
            list.addItem(
                choiceRow("Dial Logo", prefs.logo.label()) {
                    prefs.logo = ClockLogo.values().nextAfter(prefs.logo)
                }
            )
            list.addItem(toggleRow("Show Minute Ring", prefs.showMinuteRing) { prefs.showMinuteRing = it })
            list.addItem(
                choiceRow("Minute Ring Color", prefs.bezelColor.label()) {
                    prefs.bezelColor = BezelColor.values().nextAfter(prefs.bezelColor)
                }
            )
            list.addItem(toggleRow(carContext.getString(R.string.show_lume), prefs.showLume) { prefs.showLume = it })
            list.addItem(
                choiceRow("Lume Color", prefs.lumeColor.label()) {
                    prefs.lumeColor = LumeColor.values().nextAfter(prefs.lumeColor)
                }
            )
        }
        list.addItem(toggleRow(carContext.getString(R.string.show_seconds), prefs.showSeconds) { prefs.showSeconds = it })
        list.addItem(toggleRow(carContext.getString(R.string.show_date), prefs.showDate) { prefs.showDate = it })
    }

    private fun addDigitalSettings(list: ItemList.Builder) {
        list.addItem(toggleRow("Bold digital text", prefs.digitalBold) { prefs.digitalBold = it })
        list.addItem(toggleRow("Italic digital text", prefs.digitalItalic) { prefs.digitalItalic = it })
        list.addItem(choiceRow("Digital font", prefs.digitalFont.label()) {
            prefs.digitalFont = DigitalFont.values().nextAfter(prefs.digitalFont)
        })
        list.addItem(choiceRow("All time parts color", prefs.digitalColor.label()) {
            prefs.digitalColor = DigitalColor.values().nextAfter(prefs.digitalColor)
        })
        list.addItem(choiceRow("Hour color", prefs.digitalHourColor.label()) { prefs.digitalHourColor = DigitalColor.values().nextAfter(prefs.digitalHourColor) })
        list.addItem(choiceRow("Minute color", prefs.digitalMinuteColor.label()) { prefs.digitalMinuteColor = DigitalColor.values().nextAfter(prefs.digitalMinuteColor) })
        list.addItem(choiceRow("Seconds color", prefs.digitalSecondColor.label()) { prefs.digitalSecondColor = DigitalColor.values().nextAfter(prefs.digitalSecondColor) })
        list.addItem(choiceRow("Colon color", prefs.digitalSeparatorColor.label()) { prefs.digitalSeparatorColor = DigitalColor.values().nextAfter(prefs.digitalSeparatorColor) })
        list.addItem(choiceRow("Digital background", prefs.digitalPattern.label()) {
            prefs.digitalPattern = DigitalPattern.values().nextAfter(prefs.digitalPattern)
        })
        list.addItem(
            toggleRow(carContext.getString(R.string.time_format_24), prefs.is24HourFormat) {
                prefs.is24HourFormat = it
            }
        )
        list.addItem(
            toggleRow(carContext.getString(R.string.show_seconds), prefs.showSeconds) {
                prefs.showSeconds = it
            }
        )
        list.addItem(
            toggleRow(carContext.getString(R.string.show_date), prefs.showDate) {
                prefs.showDate = it
            }
        )
        list.addItem(
            toggleRow("Show local weather (uses device location)", prefs.showWeather) {
                prefs.showWeather = it
                if (it) WeatherService.refresh(carContext)
            }
        )
        if (prefs.showWeather) {
            list.addItem(toggleRow("Show Weather Icon", prefs.showWeatherIcon) { prefs.showWeatherIcon = it })
            list.addItem(
                toggleRow("Show Weather Condition", prefs.showWeatherCondition) {
                    prefs.showWeatherCondition = it
                }
            )
            list.addItem(
                choiceRow(
                    "Temperature Unit",
                    if (prefs.weatherFahrenheit) "Fahrenheit" else "Celsius"
                ) {
                    prefs.weatherFahrenheit = !prefs.weatherFahrenheit
                }
            )
        }
    }

    private fun choiceRow(title: String, value: String, onClick: () -> Unit) =
        Row.Builder()
            .setTitle(title)
            .addText(value)
            .setOnClickListener {
                onClick()
                settingsChanged()
            }
            .build()

    private fun toggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) =
        Row.Builder()
            .setTitle(title)
            .setToggle(
                Toggle.Builder { isChecked ->
                    onChange(isChecked)
                    settingsChanged()
                }
                    .setChecked(checked)
                    .build()
            )
            .build()

    private fun settingsChanged() {
        onSettingsChanged()
        invalidate()
    }
}

private fun ClockStyle.label(carContext: CarContext) = when (this) {
    ClockStyle.ANALOG -> carContext.getString(R.string.clock_style_analog)
    ClockStyle.DIGITAL -> carContext.getString(R.string.clock_style_digital)
}

private fun ClockTheme.label(carContext: CarContext) = when (this) {
    ClockTheme.BLACK -> carContext.getString(R.string.theme_black)
    ClockTheme.MIDNIGHT_BLUE -> carContext.getString(R.string.theme_midnight)
    ClockTheme.AMG_SILVER -> carContext.getString(R.string.theme_amg_silver)
}

private fun ClockLogo.label() = when (this) {
    ClockLogo.NONE -> "Off"
    ClockLogo.IWC -> "IWC"
    ClockLogo.MERCEDES -> "Mercedes"
}

private fun BezelColor.label() = when (this) {
    BezelColor.RED -> "Red"
    BezelColor.SILVER -> "Silver"
    BezelColor.BLUE -> "Blue"
    BezelColor.GOLD -> "Gold"
    BezelColor.WHITE -> "White"
}

private fun LumeColor.label() = when (this) {
    LumeColor.GREEN -> "Emerald green"
    LumeColor.BLUE -> "Ice blue"
    LumeColor.SOLAR -> "Solar amber"
}

private fun DigitalFont.label() = when (this) {
    DigitalFont.SANS -> "Sans"
    DigitalFont.MONOSPACE -> "Monospace"
    DigitalFont.SERIF -> "Serif"
}

private fun DigitalColor.label() = when (this) {
    DigitalColor.IVORY -> "Ivory"
    DigitalColor.WHITE -> "White"
    DigitalColor.CYAN -> "Cyan"
    DigitalColor.AMBER -> "Amber"
    DigitalColor.RED -> "Red"
}

private fun AnalogFaceStyle.label() = when (this) {
    AnalogFaceStyle.MERCEDES -> "Mercedes"
    AnalogFaceStyle.MODERN -> "Modern"
}


private fun DigitalPattern.label() = when (this) {
    DigitalPattern.SOLID -> "Solid"
    DigitalPattern.GRID -> "Grid"
    DigitalPattern.CARBON -> "Carbon"
    DigitalPattern.SCANLINES -> "Scanlines"
    DigitalPattern.BLUEPRINT -> "Blueprint"
    DigitalPattern.DOT_MATRIX -> "Dot matrix"
    DigitalPattern.HEXAGON -> "Hexagon"
}

private fun <T : Enum<T>> Array<T>.nextAfter(value: T): T {
    val currentIndex = indexOf(value)
    return this[(currentIndex + 1) % size]
}
