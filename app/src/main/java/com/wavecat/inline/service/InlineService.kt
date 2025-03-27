@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.wavecat.inline.service

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.bundleOf
import androidx.preference.PreferenceManager
import com.wavecat.inline.R
import com.wavecat.inline.libs.Searcher
import com.wavecat.inline.preferences.FloatingWindow
import com.wavecat.inline.preferences.PreferencesItem
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaString
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import org.luaj.vm2.lib.jse.JsePlatform
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.util.Timer
import java.util.TimerTask
import java.util.regex.Pattern

class InlineService : AccessibilityService() {
    private var environment: Globals? = null

    val defaultSharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    val timer = Timer()

    val allCommands: HashMap<String, Command> = hashMapOf()
    val allWatchers: HashMap<LuaValue, Int> = hashMapOf()
    val allPreferences: HashMap<String?, HashSet<PreferencesItem>> = hashMapOf()
    val allCommandFinders: MutableSet<LuaValue> = hashSetOf()

    var defaultPath = HashSet<String>()

    val pattern: Pattern by lazy {
        Pattern.compile(
            defaultSharedPreferences.getString(PATTERN, "(\\{([\\S]+)(?:\\s([\\S\\s]+?)\\}*)?\\}\\$)+")!!,
            Pattern.DOTALL
        )
    }

    private var previousText: String? = null

    override fun onServiceConnected() {
        defaultPath = HashSet(
            listOf(
                Environment.getExternalStorageDirectory().path + "/inline",
                getExternalFilesDirs(null)[0].absolutePath + "/modules"
            )
        )

        instance = this
        createEnvironment()
        super.onServiceConnected()

        serviceInfo = AccessibilityServiceInfo().apply {
            flags = AccessibilityServiceInfo.DEFAULT
            eventTypes =
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        }

        Thread.setDefaultUncaughtExceptionHandler { _: Thread?, e: Throwable ->
            notifyException(2, e)
        }
    }

    fun createEnvironment() {
        environment = JsePlatform.standardGlobals().apply {
            set("inline", CoerceJavaToLua.coerce(this@InlineService))
            get("package").get("searchers").set(3, Searcher(this))
        }

        timer.cancel()
        timer.purge()

        allCommands.clear()
        allWatchers.clear()
        allPreferences.clear()
        allCommandFinders.clear()

        try {
            loadModules()
        } catch (e: IOException) {
            notifyException(1, e)
        } catch (e: LuaError) {
            notifyException(1, e)
        }
    }

    fun notifyException(id: Int, throwable: Throwable) {
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

    fun isFloatingWindowSupported() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1

    fun showFloatingWindow(config: LuaValue, init: LuaValue): FloatingWindow? {
        if (isFloatingWindowSupported()) {
            return FloatingWindow(ContextThemeWrapper(this, R.style.Theme_Inline)).apply {
                configure(config)
                create(init)
            }
        }

        return null
    }

    fun loadModules() {
        val unloaded = defaultSharedPreferences.getStringSet(UNLOADED, HashSet())!!

        assets.list(DEFAULT_ASSETS_PATH)?.forEach { fileName ->
            if (fileName !in unloaded) {
                val path = "$DEFAULT_ASSETS_PATH/$fileName"

                val buffer = assets.open(path).use { it.readBytes() }
                val result = environment!!.load(String(buffer), path).call()

                Log.d(TAG, "Loading internal module: $path")

                if (result.isfunction())
                    result.call(
                        CoerceJavaToLua.coerce(
                            Module(
                                service = this,
                                filepath = fileName,
                                isInternal = true
                            )
                        )
                    )
            }
        }

        defaultSharedPreferences.getStringSet(PATH, defaultPath)?.forEach { path ->
            if (path !in unloaded) {
                File(path).listFiles()?.filter { it.isFile }?.forEach { file ->
                    val reader = BufferedReader(FileReader(file)).apply { mark(1) }

                    if (reader.read() != 65279)
                        reader.reset()

                    val result = environment!!.load(reader, file.absolutePath).call()

                    Log.d(TAG, "Loading module: ${file.path}")

                    if (result.isfunction())
                        result.call(
                            CoerceJavaToLua.coerce(
                                Module(
                                    service = this,
                                    filepath = file.name,
                                    isInternal = false
                                )
                            )
                        )
                }
            }
        }
    }

    private fun notifyWatchers(accessibilityNodeInfo: AccessibilityNodeInfo, eventType: Int) {
        allWatchers.filter { (_, value) -> (value and eventType) == eventType }
            .forEach { (key, _) ->
                runCatching {
                    key.call(CoerceJavaToLua.coerce(accessibilityNodeInfo), LuaValue.valueOf(eventType))
                }.onFailure { e -> notifyException(key.hashCode(), e) }
            }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val node = event.source ?: return

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED)
            return notifyWatchers(node, event.eventType)

        var text = (node.text ?: "").toString()
        if (text == previousText) return

        notifyWatchers(node, event.eventType)
        previousText = text

        val matcher = pattern.matcher(text)

        while (matcher.find()) {
            var callable = allCommands[matcher.group(2)!!]?.callable ?: LuaValue.NIL
            var args = LuaValue.valueOf(matcher.group(3) ?: "") as LuaValue

            allCommandFinders.forEach { finder ->
                runCatching {
                    val values = finder.invoke(LuaValue.valueOf(matcher.group(2)), args, callable)
                    if (values.arg1().isfunction()) callable = values.arg1()
                    if (values.arg(2) is LuaString) args = values.arg(2)
                }.onFailure { notifyException(callable.hashCode(), it) }
            }

            if (!callable.isnil()) {
                val query = Query(node, text, matcher.group(), args.tojstring())
                runCatching { callable.call(CoerceJavaToLua.coerce(node), CoerceJavaToLua.coerce(query)) }
                    .onFailure { notifyException(callable.hashCode(), it) }

                text = query.text
            }
        }
    }

    fun getSharedPreferences(name: String?): SharedPreferences =
        getSharedPreferences(name, MODE_PRIVATE)

    fun timerTask(function: LuaValue): TimerTask {
        function.checkfunction()
        return object : TimerTask() {
            override fun run() {
                Handler(Looper.getMainLooper()).post {
                    try {
                        function.call()
                    } catch (e: Exception) {
                        notifyException(function.hashCode(), e)
                    }
                }
            }
        }
    }

    fun toast(text: String?) =
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()

    override fun onInterrupt() {
        instance = null
    }

    companion object {
        @JvmStatic
        var instance: InlineService? = null
            private set

        fun requireService() = instance ?: throw IllegalStateException("Service is not active")

        const val PATH: String = "path"
        const val PATTERN: String = "pattern"
        const val UNLOADED: String = "unloaded"

        const val TAG: String = "InlineService"

        const val DEFAULT_ASSETS_PATH: String = "modules"

        private const val CHANNEL_ID = "error"

        @JvmStatic
        val TYPE_TEXT_CHANGED: Int = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED

        @JvmStatic
        val TYPE_SELECTION_CHANGED: Int = AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED

        @JvmStatic
        val TYPE_ALL_MASK: Int = TYPE_SELECTION_CHANGED or TYPE_TEXT_CHANGED

        @JvmStatic
        fun setText(accessibilityNodeInfo: AccessibilityNodeInfo, text: String?) =
            accessibilityNodeInfo.performAction(
                AccessibilityNodeInfo.ACTION_SET_TEXT, bundleOf(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE to text
                )
            )

        @JvmStatic
        fun setSelection(accessibilityNodeInfo: AccessibilityNodeInfo, start: Int, end: Int) =
            accessibilityNodeInfo.performAction(
                AccessibilityNodeInfo.ACTION_SET_SELECTION, bundleOf(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT to start,
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT to end
                )
            )

        @JvmStatic
        fun cut(accessibilityNodeInfo: AccessibilityNodeInfo): Boolean =
            accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CUT)

        @JvmStatic
        fun copy(accessibilityNodeInfo: AccessibilityNodeInfo): Boolean =
            accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_COPY)

        @JvmStatic
        fun paste(accessibilityNodeInfo: AccessibilityNodeInfo): Boolean =
            accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE)
    }
}
