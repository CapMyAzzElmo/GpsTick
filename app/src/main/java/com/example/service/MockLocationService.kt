package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.model.SpeedPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MockLocationService : Service() {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var stateObserverJob: Job? = null

    private var windowManager: WindowManager? = null
    private var floatingView: FloatingJoyStickView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    companion object {
        private const val TAG = "MockLocationService"
        const val CHANNEL_ID = "gps_joystick_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_MOCK = "com.example.action.START_MOCK"
        const val ACTION_STOP_MOCK = "com.example.action.STOP_MOCK"
        const val ACTION_SHOW_OVERLAY = "com.example.action.SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "com.example.action.HIDE_OVERLAY"
        const val ACTION_TOGGLE_PAUSE = "com.example.action.TOGGLE_PAUSE"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_MOCK

        when (action) {
            ACTION_START_MOCK -> {
                startForegroundWithNotification()
                observeState()
            }
            ACTION_STOP_MOCK -> {
                hideOverlay()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_SHOW_OVERLAY -> {
                startForegroundWithNotification()
                showOverlay()
                observeState()
            }
            ACTION_HIDE_OVERLAY -> {
                hideOverlay()
            }
            ACTION_TOGGLE_PAUSE -> {
                JoyStickController.togglePause()
            }
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GPS JoyStick Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active mock GPS location and joystick overlay"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification("Starting GPS JoyStick...", "Ready to spoof location")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "startForeground error: ${e.message}")
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pOpenApp = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val pauseIntent = Intent(this, MockLocationService::class.java).apply {
            action = ACTION_TOGGLE_PAUSE
        }
        val pPause = PendingIntent.getService(
            this, 1, pauseIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, MockLocationService::class.java).apply {
            action = ACTION_STOP_MOCK
        }
        val pStop = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val overlayIntent = Intent(this, MockLocationService::class.java).apply {
            action = if (floatingView != null) ACTION_HIDE_OVERLAY else ACTION_SHOW_OVERLAY
        }
        val pOverlay = PendingIntent.getService(
            this, 3, overlayIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val isPaused = JoyStickController.locationState.value.isPaused

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pOpenApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, if (isPaused) "Resume" else "Pause", pPause)
            .addAction(0, if (floatingView != null) "Hide Overlay" else "Show Overlay", pOverlay)
            .addAction(0, "Stop", pStop)
            .build()
    }

    private fun updateNotification(title: String, text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(NOTIFICATION_ID, buildNotification(title, text))
    }

    private fun observeState() {
        stateObserverJob?.cancel()
        stateObserverJob = scope.launch {
            JoyStickController.locationState.collectLatest { state ->
                val status = if (state.isPaused) "PAUSED" else "ACTIVE"
                val text = "%.4f, %.4f | %s".format(state.latitude, state.longitude, status)
                val speed = "%.1f km/h".format(state.speedKmh)
                updateNotification("GPS JoyStick ($speed)", text)
            }
        }
    }

    @SuppressLint("RtlHardcoded")
    private fun showOverlay() {
        if (floatingView != null) return // Already showing

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Cannot show overlay: SYSTEM_ALERT_WINDOW permission not granted")
            return
        }

        val wm = windowManager ?: return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            x = 80
            y = 200
        }
        layoutParams = params

        val view = FloatingJoyStickView(
            context = this,
            windowManager = wm,
            layoutParams = params,
            onCloseClicked = {
                hideOverlay()
            }
        )

        try {
            wm.addView(view, params)
            floatingView = view
            JoyStickController.setOverlayActive(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add floating view to window manager: ${e.message}")
        }
    }

    private fun hideOverlay() {
        floatingView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove floating view: ${e.message}")
            }
            floatingView = null
            JoyStickController.setOverlayActive(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
        stateObserverJob?.cancel()
    }
}
