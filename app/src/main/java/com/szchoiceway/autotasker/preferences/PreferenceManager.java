package com.szchoiceway.autotasker.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

public class PreferenceManager {
    SharedPreferences preferences;
    private static PreferenceManager singletonInstance;
    private LaunchApplicationPreferences launchApplicationPreferences = new LaunchApplicationPreferences();
    private WifiPreferences wifiPreferences = new WifiPreferences();
    private ClickPreferences clickPreferences = new ClickPreferences();
    private final String PLAY_MEDIA_ACTIVATION_PREFERENCE = "PLAY_MEDIA_ACTIVATION_PREFERENCE";
    private final String KILL_TV_APP_ACTIVATION_PREFERENCE = "KILL_TV_APP_ACTIVATION_PREFERENCE";
    private final String SHOW_TOAST_MESSAGES_ACTIVATION_PREFERENCE = "SHOW_TOAST_MESSAGES_ACTIVATION_PREFERENCE";
    private final Gson gson = new Gson();

    private PreferenceManager() {};

    public static PreferenceManager getInstance() {
        if (null == singletonInstance) {
            synchronized (PreferenceManager.class) {
                if (null == singletonInstance) {
                    singletonInstance = new PreferenceManager();
                }
            }
        }
        return singletonInstance;
    }

    public void initializePreferences(Context context) {
        String PREFS_NAME = "AutoTaskerPreference";
        preferences = context.getSharedPreferences(PREFS_NAME, 0);
        launchApplicationPreferences = (LaunchApplicationPreferences) fromJson(preferences.getString(LaunchApplicationPreferences.PREFERENCE_NAME, toJson(launchApplicationPreferences)), LaunchApplicationPreferences.class);
        wifiPreferences = (WifiPreferences) fromJson(preferences.getString(WifiPreferences.PREFERENCE_NAME, toJson(wifiPreferences)), WifiPreferences.class);
        clickPreferences = (ClickPreferences) fromJson(preferences.getString(ClickPreferences.PREFERENCE_NAME, toJson(clickPreferences)), ClickPreferences.class);
    }

    private String toJson(Object target) {
        return gson.toJson(target);
    }

    private Object fromJson(String json, Class clazz) {
        return gson.fromJson(json, clazz);
    }

    public LaunchApplicationPreferences getLaunchApplicationPreferences() {
        return launchApplicationPreferences;
    }

    public WifiPreferences getWifiPreferences() {
        return wifiPreferences;
    }

    public ClickPreferences getClickPreferences() {
        return clickPreferences;
    }

    public void saveStartApplicationPreferences() {
        savePreference(LaunchApplicationPreferences.PREFERENCE_NAME, toJson(launchApplicationPreferences));
    }

    public void saveWifiPreferences() {
        savePreference(WifiPreferences.PREFERENCE_NAME, toJson(wifiPreferences));
    }
    public void saveClickPreferences() {
        savePreference(ClickPreferences.PREFERENCE_NAME, toJson(clickPreferences));
    }

    public boolean isPlayMediaActive() {
        return preferences.getBoolean(PLAY_MEDIA_ACTIVATION_PREFERENCE, false);
    }

    public boolean isKillTVAppActive() {
        return preferences.getBoolean(KILL_TV_APP_ACTIVATION_PREFERENCE, false);
    }

    public boolean isShowToastMessagesActive() {
        return preferences.getBoolean(SHOW_TOAST_MESSAGES_ACTIVATION_PREFERENCE, false);
    }

    public void savePlayMediaActivationPreference(boolean value) {
        savePreference(PLAY_MEDIA_ACTIVATION_PREFERENCE, value);
    }

    public void saveKillTVAppActivationPreference(boolean value) {
        savePreference(KILL_TV_APP_ACTIVATION_PREFERENCE, value);
    }

    public void saveShowToastMessageAppActivationPreference(boolean value) {
        savePreference(SHOW_TOAST_MESSAGES_ACTIVATION_PREFERENCE, value);
    }

    private void savePreference(String key, String value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    private void savePreference(String key, Boolean value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

}
