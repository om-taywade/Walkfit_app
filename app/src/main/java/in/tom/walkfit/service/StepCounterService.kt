package `in`.tom.walkfit.service

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
import `in`.tom.walkfit.MainActivity
import `in`.tom.walkfit.R
import `in`.tom.walkfit.util.StepCounter

class StepCounterService : Service() {
    private var stepCounter: StepCounter? = null
    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "StepCounterChannel"

    override fun onCreate() {
        super.onCreate()
        try {
            createNotificationChannel()
            stepCounter = StepCounter(this)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            // Create and show the notification first
            val notification = createNotification()
            startForeground(NOTIFICATION_ID, notification)
            
            // Then start the step counter
            stepCounter?.start()
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
        
        // If service is killed, restart it
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        try {
            stepCounter?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val name = "Step Counter"
                val descriptionText = "Tracks your steps in the background"
                val importance = NotificationManager.IMPORTANCE_LOW
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            } catch (e: Exception) {
                e.printStackTrace()
                throw e // Re-throw as this is critical for the service
            }
        }
    }

    private fun createNotification(): Notification {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE
            )

            return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("WalkFit Active")
                .setContentText("Tracking your steps")
                .setSmallIcon(R.drawable.ic_footsteps)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e // Re-throw as this is critical for the service
        }
    }
} 