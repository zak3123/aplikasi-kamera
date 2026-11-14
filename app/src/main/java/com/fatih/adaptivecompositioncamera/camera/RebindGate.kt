package com.fatih.adaptivecompositioncamera.camera

/**
 * Defers CameraX rebinds (which call unbindAll and would end a recording) while a recording is active,
 * and replays at most one rebind after it ends. Main-thread only.
 */
class RebindGate {
    var pending: Boolean = false
        private set

    /** Returns true if the caller may rebind now; false if the rebind was deferred. */
    fun request(recordingActive: Boolean): Boolean {
        pending = recordingActive
        return !recordingActive
    }

    /** Call on every recording end (Finalize or failed start). Returns true if a deferred rebind must run now. */
    fun onRecordingEnded(): Boolean {
        val replay = pending
        pending = false
        return replay
    }
}
