package com.example.util

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

object ShakeLockManager : SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var isListening = false

    private val _isPanicModeActive = MutableStateFlow(false)
    val isPanicModeActive: StateFlow<Boolean> = _isPanicModeActive.asStateFlow()

    private var onPanicTriggeredCallback: (() -> Unit)? = null

    // Continuous shake tracking
    private var firstShakeTimestamp = 0L
    private var lastShakeTimestamp = 0L
    private var shakeCountInWindow = 0
    private val SHAKE_THRESHOLD_ACCEL = 12.0f // m/s^2 delta over gravity
    private val MAX_GAP_BETWEEN_SHAKES_MS = 550L
    private val REQUIRED_CONTINUOUS_DURATION_MS = 1500L // 1.5 seconds

    private val scope = CoroutineScope(Dispatchers.Main)
    private var exitJob: Job? = null

    fun start(context: Context, onPanicTriggered: (() -> Unit)? = null) {
        if (isListening) return
        onPanicTriggeredCallback = onPanicTriggered

        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer != null) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            isListening = true
        }
    }

    fun stop() {
        if (!isListening) return
        try {
            sensorManager?.unregisterListener(this)
        } catch (_: Throwable) {}
        isListening = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || _isPanicModeActive.value) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val totalMagnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val delta = kotlin.math.abs(totalMagnitude - 9.81f)

        if (delta > SHAKE_THRESHOLD_ACCEL) {
            val now = System.currentTimeMillis()

            if (firstShakeTimestamp == 0L || (now - lastShakeTimestamp) > MAX_GAP_BETWEEN_SHAKES_MS) {
                firstShakeTimestamp = now
                shakeCountInWindow = 1
            } else {
                shakeCountInWindow++
            }
            lastShakeTimestamp = now

            val continuousDuration = now - firstShakeTimestamp
            // If user shook continuously for 1.5 - 2.0 seconds with steady vigorous pulses
            if (continuousDuration >= REQUIRED_CONTINUOUS_DURATION_MS && shakeCountInWindow >= 6) {
                // Reset tracking to avoid repeated triggers
                firstShakeTimestamp = 0L
                lastShakeTimestamp = 0L
                shakeCountInWindow = 0

                triggerPanicLock()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    /**
     * Instantly freezes the app, locks vault, shows wallpaper, and exits after 3 seconds.
     */
    fun triggerPanicLock(customActivity: Activity? = null) {
        if (_isPanicModeActive.value) return
        _isPanicModeActive.value = true

        // 1. Instantly freeze and lock the vault
        AppSecurityManager.lockApp()

        // 2. Invoke callback
        onPanicTriggeredCallback?.invoke()

        // 3. Keep wallpaper visible for 3 seconds, then cleanly exit
        exitJob?.cancel()
        exitJob = scope.launch {
            delay(3000L)
            _isPanicModeActive.value = false
            customActivity?.finishAffinity() ?: run {
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }

    fun dismissPanic() {
        exitJob?.cancel()
        _isPanicModeActive.value = false
    }
}
