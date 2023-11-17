package com.szchoiceway.autotasker.preferences;

public class LaunchApplicationPreferences {
    public static final String PREFERENCE_NAME = "START_APPLICATON_PREFERENCES";
    private String applicationName;
    private String packageName;
    private boolean isActive;
    private String logo;

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public Boolean isLaunchApplicationActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
}
