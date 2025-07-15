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

/**
 * Displays a notification with the error message from the given [Throwable] and prints its stack trace.
 *
 * This function is an extension function for the [Context] class.
 * It first calls another `notifyException` function with the throwable's message (or an empty string if the message is null).
 * Then, it prints the stack trace of the throwable to the standard error stream.
 *
 * @param throwable The [Throwable] object representing the exception to be notified.
 */
fun Context.notifyException(throwable: Throwable) {
    notifyException(throwable.message.orEmpty())
    throwable.printStackTrace()
}

/**
 * Displays a notification with the given message.
 *
 * This function creates a notification channel (if necessary on Android O and above)
 * and then builds and displays a notification with the app's name as the title
 * and the provided message as the content. The notification will only be shown
 * if the app has the `POST_NOTIFICATIONS` permission.
 *
 * @param message The message to display in the notification.
 */
fun Context.notifyException(message: String) {
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
        .setContentText(message)
        .setStyle(NotificationCompat.BigTextStyle())
        .setSmallIcon(R.drawable.ic_baseline_error_24)
        .build()

    if (ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        notificationManager.notify(message.hashCode(), notification)
    }
}