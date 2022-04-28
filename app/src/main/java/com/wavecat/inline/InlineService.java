package com.wavecat.inline;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Environment;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InlineService extends AccessibilityService {
    private static InlineService instance;

    private Globals environment;

    private SharedPreferences preferences;
    private SharedPreferences aliases;

    private final HashMap<String, Command> commands = new HashMap<>();
    private final HashMap<Module, LuaValue> watchers = new HashMap<>();

    private final static String PATH = "path";
    private final static String PATTERN = "pattern";

    private final static String DEFAULT_PATH = "/inline";
    private final static String DEFAULT_ASSETS_PATH = "modules/";

    private final static String CHANNEL_ID = "error";

    private Pattern pattern;

    private String previousText;

    @Override
    protected void onServiceConnected() {
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        aliases = getSharedPreferences("aliases", MODE_PRIVATE);

        createEnvironment();

        super.onServiceConnected();

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();

        info.flags = AccessibilityServiceInfo.DEFAULT;

        info.eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
        setServiceInfo(info);

        instance = this;
    }

    public static InlineService getInstance() {
        return instance;
    }

    public void createEnvironment() {
        environment = JsePlatform.standardGlobals();

        environment.set("inline", CoerceJavaToLua.coerce(new BaseLib(this)));

        commands.clear();
        watchers.clear();

        try {
            loadModules();
        } catch (IOException | LuaError e) {
            notifyError(e);
        }
    }

    private void notifyError(Exception e) {
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
                .setContentText(e.getMessage())
                .setStyle(new NotificationCompat.BigTextStyle())
                .setSmallIcon(R.drawable.ic_baseline_error_24)
                .build();

        notificationManager.notify(1, notification);

        e.printStackTrace();
    }

    @SuppressWarnings("unused")
    private class Module {

        private final String filepath;
        private String category;

        public Module(String filepath) {
            this.filepath = filepath;
        }

        public String getFilepath() {
            return filepath;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public void registerCommand(String name, LuaValue function, String description) {
            commands.put(name, new Command(category, function.checkfunction(), description));
        }

        public void registerCommand(String name, LuaValue function) {
            registerCommand(name, function, "");
        }

        public void unregisterCommand(String name) {
            commands.remove(name);
        }

        public HashMap<String, Command> getAllCommands() {
            return commands;
        }

        public void setWatcher(LuaValue value) {
            if (value == null) {
                watchers.remove(this);
                return;
            }

            watchers.put(this, value.checkfunction());
        }
    }

    @SuppressWarnings("unused")
    private static class Command {

        private final String category;
        private final LuaValue callable;
        private final String description;

        public Command(String category, LuaValue callable, String description) {
            this.category = category;
            this.callable = callable;
            this.description = description;
        }

        public String getCategory() {
            return category;
        }

        public String getDescription() {
            return description;
        }

        public LuaValue getCallable() {
            return callable;
        }
    }

    private void applyModule(LuaValue value, String filePath) {
        LuaValue result = value.call();

        if (result.isfunction())
            result.call(CoerceJavaToLua.coerce(new Module(filePath)));
    }

    private void loadModules() throws IOException {
        AssetManager assets = getResources().getAssets();

        for (String fileName : assets.list(DEFAULT_ASSETS_PATH)) {
            String path = DEFAULT_ASSETS_PATH + fileName;

            InputStream stream = assets.open(path);
            byte[] buffer = new byte[stream.available()];
            stream.read(buffer);

            applyModule(environment.load(new String(buffer), path), path);
        }

        Set<String> paths = preferences.getStringSet(PATH, new HashSet<>(
                Collections.singletonList(Environment.getExternalStorageDirectory().getPath() + DEFAULT_PATH)));

        for (String path : paths) {
            File[] files = new File(path).listFiles();

            if (files == null)
                continue;

            for (File file : files) {

                if (!file.isFile())
                    continue;

                BufferedReader reader = new BufferedReader(new FileReader(file));

                reader.mark(1);

                int ch = reader.read();
                if (ch != 65279) reader.reset();

                applyModule(environment.load(reader, file.getAbsolutePath()), file.getPath());
            }
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        AccessibilityNodeInfo accessibilityNodeInfo = accessibilityEvent.getSource();

        String text = accessibilityNodeInfo.getText() == null ? "" : accessibilityNodeInfo.getText().toString();

        if (text.equals(previousText))
            return;

        previousText = text;

        for (LuaValue watcher : watchers.values()) {
            try {
                watcher.call(CoerceJavaToLua.coerce(accessibilityNodeInfo));
            } catch (LuaError e) {
                notifyError(e);
            }
        }

        if (pattern == null)
            pattern = Pattern.compile(preferences.getString(PATTERN, "(\\{([a-zA-Z]+)(?:\\s([\\S\\s]+?)\\}*)?\\}\\$)+"), Pattern.DOTALL);

        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            Command command = commands.get(aliases.getString(matcher.group(2), matcher.group(2)));

            if (command != null) {
                Query query = new Query(accessibilityNodeInfo, text, matcher.group(), matcher.group(3));

                try {
                    command.getCallable().call(
                            CoerceJavaToLua.coerce(accessibilityNodeInfo),
                            CoerceJavaToLua.coerce(query));
                } catch (LuaError e) {
                    notifyError(e);
                }

                text = query.getText();
            }
        }
    }

    @Override
    public void onInterrupt() {

    }
}
