package com.wavecat.inline;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class InlineService extends AccessibilityService {

    private static InlineService instance;

    private Globals environment;

    private SharedPreferences preferences;

    private Timer timer;

    private final HashMap<String, Command> commands = new HashMap<>();
    private final HashMap<LuaValue, Integer> watchers = new HashMap<>();

    private final Set<LuaValue> commandFinders = new HashSet<>();

    private HashSet<String> defaultPath = new HashSet<>();

    public final static String PATH = "path";
    public final static String PATTERN = "pattern";
    public final static String UNLOADED = "unloaded";

    public final static String DEFAULT_ASSETS_PATH = "modules/";

    private final static String CHANNEL_ID = "error";

    public static final int TYPE_TEXT_CHANGED = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED;
    public static final int TYPE_SELECTION_CHANGED = AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED;
    public static final int TYPE_ALL_MASK = TYPE_SELECTION_CHANGED | TYPE_TEXT_CHANGED;

    private Pattern pattern;

    private String previousText;

    @Override
    protected void onServiceConnected() {
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        defaultPath = new HashSet<>(
                Arrays.asList(Environment.getExternalStorageDirectory().getPath() + "/inline",
                        getExternalFilesDirs(null)[0].getAbsolutePath() + "/modules"));

        instance = this;

        createEnvironment();

        super.onServiceConnected();

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();

        info.flags = AccessibilityServiceInfo.DEFAULT;

        info.eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED | AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
        setServiceInfo(info);

        Thread.setDefaultUncaughtExceptionHandler(
                (thread, e) -> notifyException(2, e));
    }

    public static InlineService getInstance() {
        return instance;
    }

    public void createEnvironment() {
        environment = JsePlatform.standardGlobals();

        environment.set("inline", CoerceJavaToLua.coerce(this));

        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }

        commands.clear();
        watchers.clear();
        commandFinders.clear();

        try {
            loadModules();
        } catch (IOException | LuaError e) {
            notifyException(1, e);
        }
    }

    public void notifyException(int id, Throwable throwable) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);

            int importance = NotificationManager.IMPORTANCE_LOW;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(throwable.getMessage())
                .setStyle(new NotificationCompat.BigTextStyle())
                .setSmallIcon(R.drawable.ic_baseline_error_24)
                .build();

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            return;

        notificationManager.notify(1, notification);

        throwable.printStackTrace();
    }

    private class Module {

        private final String filepath;
        private final boolean isInternal;

        private String category;

        public Module(File file) {
            this.filepath = file.getPath();
            isInternal = false;
        }

        public Module(String filepath) {
            this.filepath = filepath;
            isInternal = true;
        }

        public boolean isInternal() {
            return isInternal;
        }

        public String getFilepath() {
            return filepath;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public void registerCommand(String name, LuaValue callable, String description) {
            commands.put(name, new Command(category, callable.checkfunction(), description));
        }

        public void registerCommand(String name, LuaValue function) {
            registerCommand(name, function, "");
        }

        public void unregisterCommand(String name) {
            commands.remove(name);
        }

        public void registerWatcher(LuaValue callable, int mask) {
            watchers.put(callable.checkfunction(), mask);
        }

        public void registerWatcher(LuaValue callable) {
            watchers.put(callable.checkfunction(), TYPE_TEXT_CHANGED);
        }

        public void unregisterWatcher(LuaValue callable) {
            watchers.remove(callable);
        }

        public void registerCommandFinder(LuaValue callable) {
            commandFinders.add(callable.checkfunction());
        }

        public void unregisterCommandFinder(LuaValue callable) {
            commandFinders.remove(callable);
        }
    }

    public void loadModules() throws IOException {
        Set<String> unloaded = preferences.getStringSet(UNLOADED, new HashSet<>());

        AssetManager assets = getResources().getAssets();

        for (String fileName : assets.list(DEFAULT_ASSETS_PATH)) {
            if (unloaded.contains(fileName)) continue;

            String path = DEFAULT_ASSETS_PATH + fileName;

            InputStream stream = assets.open(path);
            byte[] buffer = new byte[stream.available()];
            stream.read(buffer);

            environment.load(new String(buffer), path).call().call(CoerceJavaToLua.coerce(new Module(path)));
        }

        Set<String> paths = preferences.getStringSet(PATH, defaultPath);

        for (String path : paths) {
            if (unloaded.contains(path)) continue;

            File[] files = new File(path).listFiles();

            if (files == null)
                continue;

            for (File file : files) {
                if (!file.isFile()) continue;

                BufferedReader reader = new BufferedReader(new FileReader(file));
                reader.mark(1);

                int ch = reader.read();
                if (ch != 65279) reader.reset();

                LuaValue result = environment.load(reader, file.getAbsolutePath()).call();

                if (result.isfunction()) result.call(CoerceJavaToLua.coerce(new Module(file)));
            }
        }
    }

    private void notifyWatchers(AccessibilityNodeInfo accessibilityNodeInfo, int eventType) {
        for (Map.Entry<LuaValue, Integer> entry : watchers.entrySet())
            try {
                if ((entry.getValue() & eventType) == eventType)
                    entry.getKey().call(CoerceJavaToLua.coerce(accessibilityNodeInfo), LuaValue.valueOf(eventType));
            } catch (LuaError e) {
                notifyException(entry.getKey().hashCode(), e);
            }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        AccessibilityNodeInfo accessibilityNodeInfo = accessibilityEvent.getSource();

        if (accessibilityNodeInfo == null)
            return;

        if (accessibilityEvent.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
            notifyWatchers(accessibilityNodeInfo, accessibilityEvent.getEventType());
            return;
        }

        String text = accessibilityNodeInfo.getText() == null ? "" : accessibilityNodeInfo.getText().toString();

        if (text.equals(previousText))
            return;

        notifyWatchers(accessibilityNodeInfo, accessibilityEvent.getEventType());

        previousText = accessibilityNodeInfo.getText() == null ? "" : accessibilityNodeInfo.getText().toString();

        if (pattern == null)
            pattern = Pattern.compile(preferences.getString(PATTERN, "(\\{([\\S]+)(?:\\s([\\S\\s]+?)\\}*)?\\}\\$)+"), Pattern.DOTALL);

        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            LuaValue callable = LuaValue.NIL;

            Command command = commands.get(matcher.group(2));

            if (command != null)
                callable = command.getCallable();

            LuaValue args = LuaValue.valueOf(
                    matcher.group(3) == null
                            ? ""
                            : matcher.group(3));

            for (LuaValue finder : commandFinders) {
                try {
                    Varargs values = finder.invoke(LuaValue.valueOf(matcher.group(2)), args, callable);

                    if (values.arg1().isfunction())
                        callable = values.arg1();

                    if (values.arg(2) instanceof LuaString)
                        args = values.arg(2);

                } catch (LuaError e) {
                    notifyException(finder.hashCode(), e);
                }
            }

            if (callable.isnil()) {
                continue;
            }

            Query query = new Query(accessibilityNodeInfo, text, matcher.group(), args.tojstring());

            try {
                callable.call(
                        CoerceJavaToLua.coerce(accessibilityNodeInfo),
                        CoerceJavaToLua.coerce(query));
            } catch (LuaError e) {
                notifyException(callable.hashCode(), e);
            }

            text = query.getText();
        }
    }

    public HashMap<String, Command> getAllCommands() {
        return commands;
    }

    public HashMap<LuaValue, Integer> getAllWatchers() {
        return watchers;
    }

    public Set<LuaValue> getAllCommandFinders() {
        return commandFinders;
    }

    public SharedPreferences getDefaultSharedPreferences() {
        return preferences;
    }

    public SharedPreferences getSharedPreferences(String name) {
        return getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    public Timer getTimer() {
        return timer == null ? timer = new Timer() : timer;
    }

    public TimerTask timerTask(LuaValue function) {
        function.checkfunction();
        return new TimerTask() {
            @Override
            public void run() {
                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        function.call();
                    } catch (Exception e) {
                        notifyException(function.hashCode(), e);
                    }
                });
            }
        };
    }

    public static void setText(AccessibilityNodeInfo accessibilityNodeInfo, String text) {
        Bundle arguments = new Bundle();
        arguments.putCharSequence(AccessibilityNodeInfo
                .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
    }

    public static void setSelection(AccessibilityNodeInfo accessibilityNodeInfo, int start, int end) {
        Bundle arguments = new Bundle();
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, start);
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, end);
        accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, arguments);
    }

    public static boolean cut(AccessibilityNodeInfo accessibilityNodeInfo) {
        return accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CUT);
    }

    public static boolean copy(AccessibilityNodeInfo accessibilityNodeInfo) {
        return accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_COPY);
    }

    public static void paste(AccessibilityNodeInfo accessibilityNodeInfo) {
        accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE);
    }

    public void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onInterrupt() {
        instance = null;
    }
}
