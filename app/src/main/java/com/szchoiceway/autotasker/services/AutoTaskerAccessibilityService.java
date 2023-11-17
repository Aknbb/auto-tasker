package com.szchoiceway.autotasker.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Path;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.szchoiceway.autotasker.MainActivity;
import com.szchoiceway.autotasker.preferences.ClickPreferences;
import com.szchoiceway.autotasker.preferences.PreferenceManager;
import com.szchoiceway.autotasker.preferences.LaunchApplicationPreferences;
import com.szchoiceway.autotasker.util.Logger;


public class AutoTaskerAccessibilityService extends AccessibilityService {
    private static boolean isAppStarted = false;
    private static AutoTaskerAccessibilityService autoTaskerAccessibilityServiceInstance;
    private final static String TAG = AutoTaskerAccessibilityService.class.getName();
    private final static String tvPackageName = "com.szchoiceway.navigation";
    private final static String txzNotificationName = "{com.txznet.smartadapter/android.app.Notification}";
    private final Handler handler = new Handler();

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Listen TXZ Notification to start AutoTasker after the car start or device reboot.
        if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            if (event.getPackageName() != null && event.getClassName() != null) {
                ComponentName componentName = new ComponentName(
                        event.getPackageName().toString(),
                        event.getClassName().toString()
                );
                if (componentName.toShortString().equals(txzNotificationName)) {
                    Logger.log(TAG, "Txz notification detected. " + event.getPackageName());
                    AutoTaskerAccessibilityService.setAppStarted(false);
                    startAutoTasker();
                }
            }
        }

        // Listen window state changes to detect current application.
        // So media can be continue when launch app is on the foreground or TV App can be killed.
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && isAppStarted()) {
            if (event.getPackageName() != null && event.getClassName() != null) {
                ComponentName componentName = new ComponentName(
                        event.getPackageName().toString(),
                        event.getClassName().toString()
                );
                ActivityInfo activityInfo = tryGetActivity(componentName);
                boolean isActivity = activityInfo != null;
                String currentAppPackageName = componentName.getPackageName();
                PreferenceManager preferenceManager = PreferenceManager.getInstance();
                LaunchApplicationPreferences launchApplicationPreferences = preferenceManager.getLaunchApplicationPreferences();
                Logger.log(TAG, "Current app package name: " + currentAppPackageName);

                if (isActivity && currentAppPackageName.equals(launchApplicationPreferences.getPackageName())) {
                    Logger.log(TAG, launchApplicationPreferences.getApplicationName() + " is ON");
                    AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    if (preferenceManager.isPlayMediaActive() && !audioManager.isMusicActive()) {
                        sendPlayEvent(audioManager);
                    }
                    ClickPreferences clickPreferences = preferenceManager.getClickPreferences();
                    handler.postDelayed(() -> {
                        if (preferenceManager.isPlayMediaActive() && clickPreferences.isAutoClickActive() && !audioManager.isMusicActive()) {
                            triggerClick(clickPreferences.getX(), clickPreferences.getY());
                        }
                    }, 1000);
                }
                if (PreferenceManager.getInstance().isKillTVAppActive() && currentAppPackageName.contains("szchoiceway") && currentAppPackageName.contains("navig")) {
                    killTVApp();
                }
            }
        }
    }

    @Override
    protected boolean onGesture(int gestureId) {
        return super.onGesture(gestureId);
    }

    @Override
    public void onInterrupt() {
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        autoTaskerAccessibilityServiceInstance = this;
        AutoTaskerAccessibilityService.setAppStarted(false);
        startAutoTasker();
        Logger.log(TAG, "AutoTasker Accessibility service is created");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        autoTaskerAccessibilityServiceInstance = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Nullable
    public static AutoTaskerAccessibilityService getInstance() {
        return autoTaskerAccessibilityServiceInstance;
    }

    private ActivityInfo tryGetActivity(ComponentName componentName) {
        try {
            return getPackageManager().getActivityInfo(componentName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startAutoTasker() {
        handler.postDelayed(() -> {
            Intent activityIntent = new Intent(getApplicationContext(), MainActivity.class);
            activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            setAppStarted(true);
            startActivity(activityIntent);
            Logger.log(TAG, "Starting Auto Tasker.");
        }, 250);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static boolean isAppStarted() {
        return isAppStarted;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void setAppStarted(boolean initialized) {
        if (AutoTaskerAccessibilityService.isAppStarted == initialized) {
            return;
        }
        Logger.log(TAG, "isAppStarted setting to: " + initialized);
        AutoTaskerAccessibilityService.isAppStarted = initialized;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendPlayEvent(AudioManager audioManager) {
        KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT);
        audioManager.dispatchMediaKeyEvent(keyEvent);
        Logger.log(TAG, "Send Play Media Event");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void killTVApp() {
        Logger.log(TAG, "Kill TV App");
        autoTaskerAccessibilityServiceInstance.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
        ActivityManager aActivityManager = (ActivityManager)getSystemService(Activity.ACTIVITY_SERVICE);
        aActivityManager.killBackgroundProcesses(tvPackageName);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    GestureDescription createClick(float x, float y) {
        Logger.log("activitiy", "click started");
        final int DURATION = 1;
        Path clickPath = new Path();
        clickPath.moveTo(x, y);
        GestureDescription.StrokeDescription clickStroke =
                new GestureDescription.StrokeDescription(clickPath, 0, DURATION);
        GestureDescription.Builder clickBuilder = new GestureDescription.Builder();
        clickBuilder.addStroke(clickStroke);
        return clickBuilder.build();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void triggerClick(float x, float y) {
        AccessibilityService.GestureResultCallback callback = new AccessibilityService.GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                if (PreferenceManager.getInstance().isShowToastMessagesActive()) {
                    handler.post(() -> Toast.makeText(getApplicationContext(), "Clicked to x: " + x + " y: " + y, Toast.LENGTH_SHORT).show());

                }
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                Logger.log("callback", "cancalled");
            }
        };

        AutoTaskerAccessibilityService accessibilityService = AutoTaskerAccessibilityService.getInstance();
        handler.postDelayed(() -> {
            try {
                Logger.log("Accessibility", "sending click");
                accessibilityService.dispatchGesture(createClick(x, y), callback, null);
            } catch (Exception e) {
                Logger.log(TAG, e.toString());
            }
        }, 500);

    }

}