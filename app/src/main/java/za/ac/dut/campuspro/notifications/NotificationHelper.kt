package za.ac.dut.campuspro.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import za.ac.dut.campuspro.MainActivity
import za.ac.dut.campuspro.R
import za.ac.dut.campuspro.data.TripNotifier

/**
 * Wraps Android's NotificationCompat API. Uses one high-importance channel so
 * "arriving soon" and "arrived" alerts show as heads-up notifications.
 */
class NotificationHelper(private val context: Context) : TripNotifier {

    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Bus arrival alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Tells you when your shuttle is arriving and when it has arrived."
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // checked in canNotify()
    override fun sendNotification(id: Int, title: String, text: String) {
        if (!canNotify()) return // app keeps working without alerts if permission is denied

        val openApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_bus)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    companion object {
        const val CHANNEL_ID = "bus_arrival_alerts"
    }
}
