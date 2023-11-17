package com.szchoiceway.autotasker.receivers;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.szchoiceway.autotasker.preferences.PreferenceManager;
import com.szchoiceway.autotasker.util.Logger;

import java.util.Timer;
import java.util.TimerTask;

public class WifiConnectionReceiver extends BroadcastReceiver {
    private final static String TAG = WifiConnectionReceiver.class.getName();
    private final WifiManager wifiManager;
    private final Handler handler = new Handler();
    private final Context applicationContext;
    private WifiOnReceiveCallback callback;
    Boolean timerStarted = false;
    Timer timer = null;
    static TimerTask timerTask;
    int numberOfTry = 0;

    public interface WifiOnReceiveCallback {
        void onConnected();
        void onDisconnected();
    }

    public WifiConnectionReceiver(Context context, WifiOnReceiveCallback wifiOnReceiveCallback) {
        applicationContext = context.getApplicationContext();
        wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        callback = wifiOnReceiveCallback;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!PreferenceManager.getInstance().getWifiPreferences().isAutoWifiActive()) {
            return;
        }
        try {
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            NetworkInfo.DetailedState connectionState = networkInfo.getDetailedState();
            if (NetworkInfo.DetailedState.CONNECTED.equals(connectionState)) {
                Logger.log("wifiConnectionRecevier", "connected to wifi.");
                stopTimer();
                callback.onConnected();
                showMessage("Connected to WIFI", Toast.LENGTH_SHORT);
            } else if (NetworkInfo.DetailedState.DISCONNECTED.equals(connectionState)) {
                Logger.log("wifiConnectionRecevier", "disconnected from wifi");
                callback.onDisconnected();
                triggerWifiConnection();
            }
        } catch (Exception e) {
            Logger.log(TAG, e.toString());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    void startTimer() {
        try {
            if (timerStarted) {
                return;
            }
            enableWifi();
            timer = new Timer();
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    connectToWifi();
                    numberOfTry++;
                    if (numberOfTry % 10 == 0) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                showMessage("Tried " + numberOfTry + " times.", Toast.LENGTH_SHORT);
                            }
                        });
                    }
                }
            };
            handler.post(new Runnable() {
                public void run() {
                    showMessage("Connect wifi operation started!", Toast.LENGTH_SHORT);
                }
            });
            timerStarted = true;
            timer.scheduleAtFixedRate(timerTask,
                    0, 1000);
        } catch (Exception e) {
            Logger.log(TAG, e.toString());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    void stopTimer() {
        try {
            if (timer == null) {
                return;
            }
            timer.cancel();
            timer.purge();
            timer = null;
            numberOfTry = 0;
            timerStarted = false;
        } catch (Exception e) {
            Logger.log(TAG, e.toString());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    void connectToWifi() {
        try {
            if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            int networkId = -1;
            if (wifiManager.getConfiguredNetworks() == null) {
                return;
            }
            for (WifiConfiguration wifiConfiguration : wifiManager.getConfiguredNetworks())
                if (wifiConfiguration.SSID.equals("\"" + PreferenceManager.getInstance().getWifiPreferences().getSsid() + "\"")) {
                    networkId = wifiConfiguration.networkId;
                    wifiManager.enableNetwork(networkId, true);
                }

        } catch (Exception e) {
            Logger.log(TAG, e.toString());
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public void triggerWifiConnection() {
        handler.postDelayed(this::startTimer, 100);
    }

    void enableWifi() {
        wifiManager.setWifiEnabled(true);
    }

    void showMessage(String message, int duration) {
        if (PreferenceManager.getInstance().isShowToastMessagesActive()) {
            Toast.makeText(applicationContext, message, duration).show();
        }
    }
}
