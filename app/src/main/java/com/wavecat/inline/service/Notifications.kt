package com.wavecat.inline.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.wavecat.inline.R

private const val CHANNEL_ID = "error"

fun Context.notifyException(id: Int, throwable: Throwable) {
    val notificationManager = NotificationManagerCompat.from(this)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name: CharSequence = getString(R.string.channel_name)
        val description = getString(R.string.channel_description)

        val importance = NotificationManager.IMPORTANCE_LOW

        val channel = NotificationChannel(CHANNEL_ID, name, importance)
        channel.description = description

        notificationManager.createNotificationChannel(channel)
    }

    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(getString(R.string.app_name))
        .setContentText(throwable.message)
        .setStyle(NotificationCompat.BigTextStyle())
        .setSmallIcon(R.drawable.ic_baseline_error_24)
        .build()

    if (ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        notificationManager.notify(id, notification)
    }

    throwable.printStackTrace()
}