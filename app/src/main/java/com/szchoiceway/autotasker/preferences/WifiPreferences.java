package com.szchoiceway.autotasker.preferences;

public class WifiPreferences {
    public static final String PREFERENCE_NAME = "WIFI_PREFERENCES";
    private String ssid;
    private boolean isActive;

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public Boolean isAutoWifiActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }
}
