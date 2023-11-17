package com.szchoiceway.autotasker.preferences;

public class ClickPreferences {
    public static final String PREFERENCE_NAME = "CLICK_PREFERENCES";
    private int x;
    private int y;
    private boolean isActive;

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public boolean isAutoClickActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
