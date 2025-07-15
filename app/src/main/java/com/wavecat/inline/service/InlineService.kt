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
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaString
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import org.luaj.vm2.lib.jse.JsePlatform
import java.util.Timer
import java.util.regex.Pattern
import kotlin.concurrent.timerTask
import kotlin.system.measureTimeMillis


/**
 * An [AccessibilityService] that provides inline functionality for text manipulation.
 *
 * This service allows users to define custom commands and watchers using Lua scripts.
 * It monitors text changes and selection changes in other applications and executes
 * corresponding commands or notifies watchers based on user-defined patterns.
 *
 * The service manages a Lua environment (`globals`) where user scripts are loaded.
 * It maintains collections of:
 *  - `allCommands`: Registered commands and their corresponding Lua functions.
 *  - `allWatchers`: Lua functions to be notified on text/selection changes, along with the event types they are interested in.
 *  - `allPreferences`: Preferences items defined by Lua scripts.
 *  - `allCommandFinders`: Lua functions that can dynamically find or modify commands.
 *
 * The service also provides utility functions accessible from Lua for interacting with
 * Android UI elements (e.g., showing toasts, setting text, managing selections).
 *
 * @see AccessibilityService
 * @see Globals
 * @see Command
 * @see PreferencesItem
 */
class InlineService : AccessibilityService() {
    /**
     * Provides access to the default shared preferences for the application.
     * This is lazily initialized using [PreferenceManager.getDefaultSharedPreferences].
     */
    val defaultSharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    val lazyLoadSharedPreferences by lazy { getSharedPreferences(LAZYLOAD) }

    var timer = Timer()

    val allCommands: MutableMap<String, Command> = mutableMapOf()
    val allWatchers: MutableMap<LuaValue, Int> = mutableMapOf()
    val allPreferences: MutableMap<String?, HashSet<PreferencesItem>> = mutableMapOf()
    val allCommandFinders: MutableSet<LuaValue> = mutableSetOf()

    private var globals: Globals? = null

    /**
     * A lazily initialized set of default paths where Inline modules can be found.
     * This set includes:
     * 1. A directory named "inline" in the primary external storage.
     *    Example: `/storage/emulated/0/inline`
     * 2. A directory named "modules" within the application's specific external files directory.
     *    Example: `/storage/emulated/0/Android/data/com.wavecat.inline/files/modules`
     */
    private val defaultPath by lazy {
        hashSetOf(
            Environment.getExternalStorageDirectory().path + "/inline",
            getExternalFilesDirs(null)[0].absolutePath + "/modules"
        )
    }

    /**
     * Regular expression pattern used to identify and parse commands within text.
     * The pattern is compiled from a string retrieved from defaultSharedPreferences,
     * or a default pattern if not found.
     * The `Pattern.DOTALL` flag is used to allow the dot (`.`) to match any character, including line terminators.
     *
     * The default pattern `(\\{([\\S]+)(?:\\s([\\S\\s]+?)\\}*)?\\}[\\$₽₴])+` is designed to match structures like:
     * - `{command}` followed by a currency symbol (`$`, `₽`, or `₴`)
     * - `{command argument}` followed by a currency symbol
     *
     * Groupings in the default pattern:
     * 1. The entire matched command structure (e.g., `{cmd arg}$`)
     * 2. The command name itself (e.g., `cmd`)
     * 3. The arguments to the command (e.g., `arg`), if present.
     */
    val pattern: Pattern by lazy {
        Pattern.compile(
            defaultSharedPreferences.getString(PATTERN, "(\\{([\\S]+)(?:\\s([\\S\\s]+?)\\}*)?\\}[\\$₽₴])+")!!,
            Pattern.DOTALL
        )
    }

    private var previousText: String? = null

    /**
     * Called when the system connects to this accessibility service.
     *
     * This method initializes the service by:
     * 1. Setting the static `instance` of the service.
     * 2. Clearing any caches if the application has been updated using [clearCaches].
     * 3. Creating the Lua environment by calling [createEnvironment] and measuring the time it takes.
     *    The elapsed time is logged and stored in default shared preferences under the key [ENVIRONMENT_PERF].
     * 4. Calling the superclass's `onServiceConnected` method.
     * 5. Configuring the [AccessibilityServiceInfo] for this service:
     *    - Sets `notificationTimeout` based on the value stored in default shared preferences
     *      (or 0 if not set).
     *    - Sets `flags` to `AccessibilityServiceInfo.DEFAULT`.
     *    - Initially sets `eventTypes` to `AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED`.
     *    - Sets `feedbackType` to `AccessibilityServiceInfo.FEEDBACK_ALL_MASK`.
     *    - If the `RECEIVE_SELECTION_CHANGES` preference is true (default), it adds
     *      `AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED` to the `eventTypes`.
     * 6. Sets a default uncaught exception handler for all threads to [notifyException], ensuring
     *    that any unhandled exceptions are reported.
     */
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

    /**
     * Clears caches if the application has been updated.
     *
     * It updates the stored previous version code to the current version code.
     */
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

    /**
     * Creates and initializes the Lua environment.
     *
     * This function sets up the global Lua environment (`globals`) by:
     * 1. Initializing standard globals using `JsePlatform.standardGlobals()`.
     * 2. Exposing the `InlineService` instance to Lua under the name "inline".
     *    This allows Lua scripts to interact with the service.
     * 3. Registering a custom `LuaSearcher` to enable Lua's `require` function
     *    to load modules from custom paths.
     *
     * After setting up the globals, it calls `loadModules()` to load all
     * configured Inline modules.
     */
    fun createEnvironment() {
        globals = JsePlatform.standardGlobals().apply {
            set("inline", CoerceJavaToLua.coerce(this@InlineService))
            get("package").get("searchers").set(3, LuaSearcher(this))
        }

        loadModules()
    }

    /**
     * Forces the loading of modules that were previously marked for lazy loading.
     */
    fun forceLoadLazy() = globals?.apply {
        runCatching {
            loadModules(
                service = this@InlineService,
                sharedPreferences = defaultSharedPreferences,
                defaultPath = defaultPath,
                forceLazy = true
            )
        }.onFailure { e ->
            notifyException("forceLoadLazy(): ${e.message}")
        }
    }

    /**
     * Reloads all Lua modules.
     *
     * This function clears all existing commands, watchers, preferences, and command finders.
     * It also cancels and purges the existing timer and creates a new one.
     * Any open windows created by Lua scripts are closed.
     *
     * Then, it attempts to load all modules by calling the top-level `loadModules` function,
     * passing the current service instance, default shared preferences, and default module paths.
     */
    fun loadModules() = globals?.apply {
        allCommands.clear()
        allWatchers.clear()
        allPreferences.clear()
        allCommandFinders.clear()

        timer.apply { cancel(); purge() }
        timer = Timer()

        com.wavecat.inline.libs.windows.closeAll()

        runCatching {
            loadModules(
                service = this@InlineService,
                sharedPreferences = defaultSharedPreferences,
                defaultPath = defaultPath,
            )
        }.onFailure { e ->
            notifyException("loadModules(): ${e.message}")
        }
    }

    /**
     * Displays a long toast message.
     *
     * This is a utility function that can be called from Lua scripts to show
     * a toast notification to the user.
     *
     * @param text The text to display in the toast. Can be null, in which case
     *             an empty string will be shown.
     */
    fun toast(text: String?) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()

    /**
     * Retrieves a [SharedPreferences] instance for the given name.
     *
     * @param name The name of the preferences file. If `null`, it will use the default file name.
     * @return The [SharedPreferences] instance for the given name.
     * @see android.content.Context.getSharedPreferences
     */
    fun getSharedPreferences(name: String?): SharedPreferences = getSharedPreferences(name, MODE_PRIVATE)

    /**
     * Notifies registered watchers about an accessibility event.
     *
     * This function iterates through `allWatchers`. For each watcher, it checks if
     * the `eventType` of the current accessibility event matches the event types
     * the watcher is interested in (using a bitwise AND operation).
     *
     * If there's a match, the watcher's Lua function is called with two arguments:
     * 1. The `AccessibilityNodeInfo` associated with the event, coerced to a Lua value.
     * 2. The `eventType` as a Lua number.
     *
     * @param accessibilityNodeInfo The [AccessibilityNodeInfo] from the event.
     * @param eventType The type of the accessibility event (e.g., [AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED]).
     */
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

    /**
     * Handles accessibility events received by the service.
     *
     * This method is the core of the service's functionality. It processes
     * `AccessibilityEvent`s, specifically `TYPE_VIEW_TEXT_SELECTION_CHANGED` and
     * `TYPE_VIEW_TEXT_CHANGED`.
     *
     * - For `TYPE_VIEW_TEXT_SELECTION_CHANGED` events, it calls `notifyWatchers` to inform
     *   any registered Lua watchers about the selection change.
     *
     * - For `TYPE_VIEW_TEXT_CHANGED` events:
     *   1. It retrieves the text content from the event's source node.
     *   2. It checks if the text has actually changed compared to the `previousText`
     *      to avoid redundant processing.
     *   3. If the text has changed, it calls `notifyWatchers`.
     *   4. It updates `previousText` with the new text.
     *   5. It uses the `pattern` (a regular expression) to find command invocations
     *      within the new text.
     *   6. For each matched command:
     *      a. It attempts to find the corresponding Lua function (`callable`) in `allCommands`.
     *      b. It extracts any arguments provided with the command.
     *      c. It iterates through `allCommandFinders` (Lua functions that can dynamically
     *         locate or modify commands). Each finder is invoked with the command name,
     *         arguments, and the initially found callable. Finders can modify the
     *         `callable` or `args`.
     *      d. If a valid `callable` (a Lua function) is found:
     *         i. A `Query` object is created, containing information about the
     *            event source node, the full text, the matched command string, and arguments.
     *         ii. The `callable` is executed with the source node and the `Query` object
     *             as arguments.
     *         iii. The `text` variable is updated with `query.text`, allowing commands to
     *              modify the text content being processed by subsequent commands in the same event.
     *      e. Any exceptions occurring during command finder execution or command execution
     */
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

    /**
     * Creates and schedules a [java.util.TimerTask] that executes the provided Lua function.
     *
     * The Lua function is executed on the main UI thread.
     *
     * @param function The Lua function to be executed by the timer task.
     *                 It is expected to be a function that takes no arguments.
     * @return The created [java.util.TimerTask]. This can be used with a [Timer] to schedule
     *         the execution of the Lua function.
     *
     * @see Timer.schedule
     */
    fun timerTask(function: LuaValue) = timerTask {
        runOnUiThread {
            try {
                function.checkfunction().call()
            } catch (e: Exception) {
                notifyException("TimerTask $function: ${e.message}")
            }
        }
    }

    /**
     * Called by the system when the service is interrupted.
     * This can happen, for example, when the system needs to kill the service due to low memory.
     * When this method is called, the service's static instance is set to `null` to indicate
     * that the service is no longer active.
     */
    override fun onInterrupt() {
        instance = null
    }

    companion object {
        var instance: InlineService? = null
            private set

        /**
         * Retrieves the active [InlineService] instance.
         *
         * @return The current [InlineService] instance.
         * @throws IllegalStateException if the service is not currently active.
         */
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

        /**
         * Sets the text of an [AccessibilityNodeInfo].
         *
         * This function performs the [AccessibilityNodeInfo.ACTION_SET_TEXT] action on the
         * provided node, replacing its current text with the specified text.
         *
         * @param accessibilityNodeInfo The node whose text is to be set.
         * @param text The new text to set. Can be null to clear the text.
         * @return `true` if the action was successfully performed, `false` otherwise.
         */
        @JvmStatic
        fun setText(accessibilityNodeInfo: AccessibilityNodeInfo, text: String?) =
            accessibilityNodeInfo.performAction(
                AccessibilityNodeInfo.ACTION_SET_TEXT, bundleOf(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE to text
                )
            )

        /**
         * Sets the text selection in the provided [AccessibilityNodeInfo].
         *
         * @param accessibilityNodeInfo The node in which to set the selection.
         * @param start The starting index of the selection (inclusive).
         * @param end The ending index of the selection (exclusive).
         * @return `true` if the action was performed successfully, `false` otherwise.
         */
        @JvmStatic
        fun setSelection(accessibilityNodeInfo: AccessibilityNodeInfo, start: Int, end: Int) =
            accessibilityNodeInfo.performAction(
                AccessibilityNodeInfo.ACTION_SET_SELECTION, bundleOf(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT to start,
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT to end
                )
            )

        /**
         * Performs the "cut" action on the given [AccessibilityNodeInfo].
         *
         * This attempts to cut the currently selected text in the node.
         *
         * @param accessibilityNodeInfo The node on which to perform the cut action.
         * @return `true` if the action was performed successfully, `false` otherwise.
         * @see AccessibilityNodeInfo.ACTION_CUT
         */
        @JvmStatic
        fun cut(accessibilityNodeInfo: AccessibilityNodeInfo): Boolean =
            accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CUT)

        /**
         * Performs a copy action on the given [AccessibilityNodeInfo].
         *
         * @param accessibilityNodeInfo The node on which to perform the copy action.
         * @return `true` if the action was successful, `false` otherwise.
         */
        @JvmStatic
        fun copy(accessibilityNodeInfo: AccessibilityNodeInfo): Boolean =
            accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_COPY)

        /**
         * Performs the paste action on the given [AccessibilityNodeInfo].
         * This action attempts to paste the content from the clipboard into the node.
         *
         * @param accessibilityNodeInfo The [AccessibilityNodeInfo] on which to perform the paste action.
         * @return `true` if the action was successfully performed, `false` otherwise.
         * @see AccessibilityNodeInfo.ACTION_PASTE
         */
        @JvmStatic
        fun paste(accessibilityNodeInfo: AccessibilityNodeInfo): Boolean =
            accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE)

        /**
         * Retrieves the text content from an [AccessibilityNodeInfo], with special handling
         * for hint text and cases where no selection is active.
         *
         * This function aims to provide a more reliable way to get the *actual* user-editable
         * text from a node, filtering out hint text or returning an empty string if the text
         * content appears to be a hint or if there's no active text selection (which can sometimes
         * indicate the node's text is not user-editable content).
         *
         * The logic is as follows:
         * 1. Get the raw text from `accessibilityNodeInfo.text`. If it's null, use an empty string.
         * 2. If the `rawText` is not empty AND `accessibilityNodeInfo.textSelectionStart` is -1
         *    (indicating no selection), return an empty string. This is a heuristic to avoid
         *    returning text that might not be user-editable.
         * 3. If the Android version is Oreo (API 26) or higher:
         *    - If `accessibilityNodeInfo.isShowingHintText` is true, return an empty string.
         *    - If `accessibilityNodeInfo.hintText` is not null and is equal to `rawText`, return an empty string.
         *      This helps to ensure that hint text is not mistaken for actual content.
         * 4. Otherwise, return the `rawText`.
         *
         * @param accessibilityNodeInfo The [AccessibilityNodeInfo] from which to extract the text.
         * @return The filtered text content as a [String]. Returns an empty string if the text is
         *         determined to be a hint or if there's no active selection in a non-empty field.
         */
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

        /**
         * Inserts the given text into the provided [AccessibilityNodeInfo] at the current selection.
         *
         * If there is a selection, the selected text will be replaced by [textToInsert].
         * If there is no selection (cursor position), [textToInsert] will be inserted at the cursor.
         * After insertion, the selection will be placed at the end of the inserted text.
         *
         * This function refreshes the node's information before performing operations to ensure
         * it has the latest state.
         *
         * @param accessibilityNodeInfo The node where the text will be inserted.
         * @param textToInsert The text to insert.
         */
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
