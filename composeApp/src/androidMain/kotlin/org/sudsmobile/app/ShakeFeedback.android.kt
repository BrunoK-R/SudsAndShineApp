package org.sudsmobile.app

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sudsmobile.data.feedback.FEEDBACK_SCREENSHOT_MAX_BYTES
import com.sudsmobile.data.feedback.FeedbackScreenshot
import java.io.ByteArrayOutputStream

internal actual fun platformName() = "android"

@Composable
internal actual fun PlatformShakeEffect(enabled: Boolean, onShake: () -> Unit) {
    val context = LocalContext.current
    val callback = rememberUpdatedState(onShake)
    // Compose lint cannot resolve this constructor from commonMain; the compiler verifies its type.
    @SuppressLint("RememberReturnType")
    val classifier: ShakeClassifier = remember { ShakeClassifier() }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(context, enabled, lifecycleOwner) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (!enabled || manager == null || sensor == null) return@DisposableEffect onDispose { }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.values.size >= 3 && classifier.onSample(
                        event.values[0].toDouble() / SensorManager.GRAVITY_EARTH,
                        event.values[1].toDouble() / SensorManager.GRAVITY_EARTH,
                        event.values[2].toDouble() / SensorManager.GRAVITY_EARTH,
                        event.timestamp / 1_000_000,
                    )
                ) callback.value()
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
                Lifecycle.Event.ON_PAUSE -> manager.unregisterListener(listener)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            manager.unregisterListener(listener)
        }
    }
}

@Composable
internal actual fun rememberFeedbackScreenshotCapture(): suspend () -> FeedbackScreenshot? {
    val view = LocalView.current
    return remember(view) {
        {
            if (view.width <= 0 || view.height <= 0) null else {
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                try {
                    view.draw(Canvas(bitmap))
                    val bytes = ByteArrayOutputStream().use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
                        stream.toByteArray()
                    }
                    if (bytes.isEmpty() || bytes.size > FEEDBACK_SCREENSHOT_MAX_BYTES) null
                    else FeedbackScreenshot(bytes, "image/jpeg", view.width, view.height)
                } finally {
                    bitmap.recycle()
                }
            }
        }
    }
}
