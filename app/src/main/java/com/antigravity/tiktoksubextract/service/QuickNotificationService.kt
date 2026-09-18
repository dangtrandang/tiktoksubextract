package com.antigravity.tiktoksubextract.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.antigravity.tiktoksubextract.R
import com.antigravity.tiktoksubextract.data.pref.AppPreferences
import com.antigravity.tiktoksubextract.ui.transcribe.TranscribeDialogActivity

class QuickNotificationService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            val prefs = AppPreferences(this)
            prefs.quickNotificationEnabled = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        val notification = buildQuickNotification()
        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    private fun buildQuickNotification(): Notification {
        // Intent to extract from clipboard
        val extractIntent = Intent(this, TranscribeDialogActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(TranscribeDialogActivity.EXTRA_READ_CLIPBOARD, true)
        }
        val extractPendingIntent = PendingIntent.getActivity(
            this,
            101,
            extractIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to stop the service
        val stopIntent = Intent(this, QuickNotificationService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            102,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_sparkles)
            .setContentTitle("TikTok SubExtract ⚡")
            .setContentText("Chạm để bóc lời thoại từ link vừa Copy trên TikTok")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Sao chép liên kết video trên TikTok rồi chạm vào đây để bóc lời thoại ngay lập tức!"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setContentIntent(extractPendingIntent)
            .addAction(R.drawable.ic_sparkles, "⚡ Bóc ngay", extractPendingIntent)
            .addAction(R.drawable.ic_close, "Tắt", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bóc lời thoại nhanh",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo thao tác nhanh bóc lời thoại khi copy link TikTok"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "quick_transcribe_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.antigravity.tiktoksubextract.action.STOP_QUICK_SERVICE"

        fun start(context: Context) {
            val intent = Intent(context, QuickNotificationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, QuickNotificationService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
