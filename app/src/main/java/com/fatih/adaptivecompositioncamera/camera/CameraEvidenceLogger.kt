package com.fatih.adaptivecompositioncamera.camera

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Device-side evidence used by the diagnostics screen and ADB verification.
 *
 * The log lives in the app-specific external files directory so it can be pulled
 * without broad storage permission:
 *   adb pull /sdcard/Android/data/<package>/files/camera-evidence.log
 */
internal object CameraEvidenceLogger {
    const val LOGCAT_TAG = "AdaptiveCameraEvidence"
    private const val FILE_NAME = "camera-evidence.log"
    private const val MAX_FILE_BYTES = 1_500_000L
    private val lock = Any()
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)

    fun file(context: Context): File {
        val directory = context.getExternalFilesDir(null) ?: context.filesDir
        return File(directory, FILE_NAME)
    }

    fun startCapabilityScan(context: Context) {
        synchronized(lock) {
            val destination = file(context)
            destination.parentFile?.mkdirs()
            val header = buildString {
                appendLine("# Adaptive Composition Camera capability scan")
                appendLine("started=${timestampFormat.format(Date())}")
                appendLine("device=${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine("android=${Build.VERSION.RELEASE} api=${Build.VERSION.SDK_INT}")
                appendLine("package=${context.packageName}")
            }
            if (!destination.exists() || destination.length() > MAX_FILE_BYTES) {
                destination.writeText(header)
            } else {
                destination.appendText("\n$header")
            }
        }
        Log.i(LOGCAT_TAG, "CAPABILITY_SCAN_START file=${file(context).absolutePath}")
    }

    fun record(context: Context, category: String, message: String) {
        val sanitized = message.replace('\n', ' ').replace('\r', ' ')
        val line = "${timestampFormat.format(Date())} [$category] $sanitized"
        Log.i(LOGCAT_TAG, line)
        synchronized(lock) {
            val destination = file(context)
            destination.parentFile?.mkdirs()
            if (destination.exists() && destination.length() > MAX_FILE_BYTES) {
                destination.writeText("# Evidence log rotated at ${timestampFormat.format(Date())}\n")
            }
            destination.appendText("$line\n")
        }
    }
}
