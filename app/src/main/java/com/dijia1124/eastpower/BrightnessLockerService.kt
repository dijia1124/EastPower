package com.dijia1124.eastpower

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.app.NotificationCompat
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BrightnessLockerService : Service() {
    companion object {
        private const val CHANNEL_ID = "brightness_locker"
        private const val NOTIF_ID = 1002
        const val ACTION_STOP = "com.example.app.ACTION_STOP_BRIGHTNESS"
        const val ACTION_START = "com.example.app.ACTION_START_BRIGHTNESS"
        const val ACTION_APPLY = "com.example.app.ACTION_APPLY_BRIGHTNESS"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val prefs by lazy { PrefsRepository(applicationContext) }
    private var updateJob: Job? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> if (updateJob?.isActive != true) startUpdating()
                Intent.ACTION_SCREEN_OFF -> updateJob?.cancel()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, filter)

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                updateJob?.cancel()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_APPLY -> {
                scope.launch {
                    val value = prefs.brightnessFlow.first()
                    setSysfsBrightness(value)
                }
                return START_NOT_STICKY
            }
            ACTION_START -> {
                if (updateJob?.isActive != true) {
                    startForeground(NOTIF_ID, buildNotification(getString(R.string.brightness_locker_running)))
                    startUpdating()
                }
                return START_STICKY
            }
            else -> return START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        updateJob?.cancel()
        unregisterReceiver(screenReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    private fun startUpdating() {
        updateJob = scope.launch {
            val value = prefs.brightnessFlow.first()
            // to avoid brightness override by system
            delay(500)
            Log.d("BrightnessService", "Setting brightness to $value")
            setSysfsBrightness(value)
        }
    }

    private suspend fun setSysfsBrightness(value: Int) = withContext(Dispatchers.IO) {

        Shell.isAppGrantedRoot()?.let {
            if (!it) {
                Log.e("BrightnessService", "No root access")
                return@withContext
            }
        }

        val result = Shell.cmd("echo $value > /sys/devices/platform/soc/ae00000.qcom,mdss_mdp/backlight/panel0-backlight/brightness").exec()

        if (result.isSuccess) {
            Log.d("BrightnessService", "setSysfsBrightness Brightness set to $value")
        } else {
            Log.e("BrightnessService", "setSysfsBrightness Failed to set brightness")
        }
    }

    private fun createNotificationChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        val chan = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.brightness_locker_service),
            NotificationManager.IMPORTANCE_LOW
        )
        nm.createNotificationChannel(chan)
    }

    private fun buildNotification(content: String): Notification {
        val stopIntent = Intent(this, BrightnessLockerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.light_mode_24dp_1f1f1f_fill1_wght400_grad200_opsz24)
            .setContentTitle(getString(R.string.brightness_locker_service))
            .setContentText(content)
            .addAction(R.drawable.light_mode_24dp_1f1f1f_fill1_wght400_grad200_opsz24,
                getString(R.string.stop), stopPending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }
}