@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.wavecat.inline.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.SharedPreferences
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.os.bundleOf
import androidx.preference.PreferenceManager
import com.wavecat.inline.BuildConfig
import com.wavecat.inline.preferences.PreferencesItem
import com.wavecat.inline.service.commands.Command
import com.wavecat.inline.service.commands.Query
import com.wavecat.inline.service.modules.LAZYLOAD
import com.wavecat.inline.service.modules.LuaSearcher
import com.wavecat.inline.service.modules.loadModules
import com.wavecat.inline.utils.runOnUiThread
import org.luaj.vm2.LuaString
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import org.luaj.vm2.lib.jse.JsePlatform
import java.util.Timer
import java.util.regex.Pattern
import kotlin.concurrent.timerTask
import kotlin.system.measureTimeMillis


class InlineService : AccessibilityService() {
    val defaultSharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    val lazyLoadSharedPreferences by lazy { getSharedPreferences(LAZYLOAD) }

    var timer = Timer()

    val allCommands: MutableMap<String, Command> = mutableMapOf()
    val allWatchers: MutableMap<LuaValue, Int> = mutableMapOf()
    val allPreferences: MutableMap<String?, HashSet<PreferencesItem>> = mutableMapOf()
    val allCommandFinders: MutableSet<LuaValue> = mutableSetOf()

    val defaultPath by lazy {
        hashSetOf(
            Environment.getExternalStorageDirectory().path + "/inline",
            getExternalFilesDirs(null)[0].absolutePath + "/modules"
        )
    }

    val pattern: Pattern by lazy {
        Pattern.compile(
            defaultSharedPreferences.getString(PATTERN, "(\\{([\\S]+)(?:\\s([\\S\\s]+?)\\}*)?\\}[\\$₽₴])+")!!,
            Pattern.DOTALL
        )
    }

    private var previousText: String? = null

    override fun onServiceConnected() {
        instance = this

        clearCaches()

        val elapsed = measureTimeMillis {
            createEnvironment()
        }

        defaultSharedPreferences.edit {
            Log.d(TAG, "createEnvironment() took $elapsed ms")
            putLong(ENVIRONMENT_PERF, elapsed)
        }

        super.onServiceConnected()

        serviceInfo = AccessibilityServiceInfo().apply {
            notificationTimeout = defaultSharedPreferences.getInt(NOTIFICATION_TIMEOUT, 0).toLong()

            flags = AccessibilityServiceInfo.DEFAULT
            eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK

            if (defaultSharedPreferences.getBoolean(RECEIVE_SELECTION_CHANGES, true))
                eventTypes = eventTypes or AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED
        }

        Thread.setDefaultUncaughtExceptionHandler { _: Thread?, e: Throwable ->
            notifyException(e)
        }
    }

    fun clearCaches() {
        val previousVersionCode =
            lazyLoadSharedPreferences.getInt(PREVIOUS_VERSION_CODE, BuildConfig.VERSION_CODE)

        if (BuildConfig.VERSION_CODE > previousVersionCode) {
            lazyLoadSharedPreferences.edit() { clear() }

            defaultSharedPreferences.edit {
                defaultSharedPreferences.all.forEach {
                    if (it.key.startsWith("DESC"))
                        remove(it.key)
                }
            }
        }

        lazyLoadSharedPreferences.edit {
            putInt(PREVIOUS_VERSION_CODE, BuildConfig.VERSION_CODE)
        }
    }

    fun createEnvironment() {
        allCommands.clear()
        allWatchers.clear()
        allPreferences.clear()
        allCommandFinders.clear()

        timer.apply { cancel(); purge() }
        timer = Timer()

        com.wavecat.inline.libs.windows.closeAll()

        JsePlatform.standardGlobals().apply {
            set("inline", CoerceJavaToLua.coerce(this@InlineService))
            get("package").get("searchers").set(3, LuaSearcher(this))

            runCatching {
                loadModules(
                    service = this@InlineService,
                    sharedPreferences = defaultSharedPreferences,
                    defaultPath = defaultPath
                )
            }.onFailure { e ->
                notifyException("createEnvironment(): ${e.message}")
            }
        }
    }

    fun toast(text: String?) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()

    fun getSharedPreferences(name: String?): SharedPreferences = getSharedPreferences(name, MODE_PRIVATE)

    private fun notifyWatchers(accessibilityNodeInfo: AccessibilityNodeInfo, eventType: Int) {
        allWatchers.filter { (_, value) -> (value and eventType) == eventType }
            .forEach { (key, _) ->
                runCatching {
                    key.call(CoerceJavaToLua.coerce(accessibilityNodeInfo), LuaValue.valueOf(eventType))
                }.onFailure { e ->
                    notifyException("notifyWatchers() $key: ${e.message}")
                }
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
                }.onFailure { e ->
                    notifyException("CommandFinders: ${e.message}")
                }
            }

            if (!callable.isnil()) {
                val query = Query(node, text, matcher.group(), args.tojstring())
                runCatching { callable.call(CoerceJavaToLua.coerce(node), CoerceJavaToLua.coerce(query)) }
                    .onFailure { e ->
                        notifyException("Command: ${e.message}")
                    }

                text = query.text
            }
        }
    }

    fun timerTask(function: LuaValue) = timerTask {
        runOnUiThread {
            try {
                function.checkfunction().call()
            } catch (e: Exception) {
                notifyException("TimerTask $function: ${e.message}")
            }
        }
    }

    override fun onInterrupt() {
        instance = null
    }

    companion object {
        var instance: InlineService? = null
            private set

        fun requireService() = instance ?: throw IllegalStateException("Service is not active")

        const val PATH: String = "path"
        const val PATTERN: String = "pattern"

        const val ENVIRONMENT_PERF = "environment_perf"
        const val NOTIFICATION_TIMEOUT = "notification_timeout"
        const val RECEIVE_SELECTION_CHANGES = "receive_selection_changes"
        const val PREVIOUS_VERSION_CODE = "previous_version_code"

        const val TAG: String = "InlineService"

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

        @JvmStatic
        fun getText(accessibilityNodeInfo: AccessibilityNodeInfo): String {
            val rawText = (accessibilityNodeInfo.text ?: "").toString()

            return when {
                rawText.isNotEmpty() && accessibilityNodeInfo.textSelectionStart == -1 -> ""
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                        (accessibilityNodeInfo.isShowingHintText || accessibilityNodeInfo.hintText == rawText) -> ""

                else -> rawText
            }
        }

        @JvmStatic
        fun insertText(accessibilityNodeInfo: AccessibilityNodeInfo, textToInsert: String) {
            accessibilityNodeInfo.refresh()

            val start = accessibilityNodeInfo.textSelectionStart.takeIf { it != -1 } ?: 0
            val end = accessibilityNodeInfo.textSelectionEnd.takeIf { it != -1 } ?: start

            val currentText = getText(accessibilityNodeInfo)
            val newText = currentText.substring(0, start) + textToInsert + currentText.substring(end)

            setText(accessibilityNodeInfo, newText)
            setSelection(accessibilityNodeInfo, start + textToInsert.length, start + textToInsert.length)
        }
    }
}
