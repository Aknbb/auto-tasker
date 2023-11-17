package com.szchoiceway.autotasker;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.szchoiceway.autotasker.preferences.ClickPreferences;
import com.szchoiceway.autotasker.preferences.LaunchApplicationPreferences;
import com.szchoiceway.autotasker.preferences.WifiPreferences;
import com.szchoiceway.autotasker.receivers.WifiConnectionReceiver;
import com.szchoiceway.autotasker.services.AutoTaskerAccessibilityService;
import com.szchoiceway.autotasker.util.Base64Helper;
import com.szchoiceway.autotasker.util.Logger;
import com.szchoiceway.autotasker.preferences.PreferenceManager;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RequiresApi(api = Build.VERSION_CODES.O)
public class MainActivity extends AppCompatActivity {

    final String TAG = MainActivity.class.getName();

    Dialog listViewDialog;
    Dialog logDialog;
    ListView listView;
    ImageView launchApplicationButton;
    TextView launchApplicationName;
    TextView autoWifiSSID;
    TextView loggerArea;
    TextView applicationTitle;
    TextView versionTextView;
    EditText autoClickXInput;
    EditText autoClickYInput;
    Switch launchApplicationSwitch;
    Switch autoWifiSwitch;
    Switch playMediaSwitch;
    Switch killTVAppSwitch;
    Switch showMessagesSwitch;
    Switch autoClickSwitch;
    Button clearLogButton;

    WifiConnectionReceiver wifiConnectionReceiver;
    PreferenceManager preferenceManager;
    LaunchApplicationPreferences launchApplicationPreferences;
    WifiPreferences wifiPreferences;
    ClickPreferences clickPreferences;

    WifiManager wifiManager;
    PackageManager packageManager;
    Handler handler;

    ArrayAdapter<ApplicationInfo> applicationListAdapter;
    ArrayAdapter<String> wifiSSIDAdapter;

    List<String> wifiSSIDList = Collections.singletonList("Regnex-Note 10 Lite");
    List<ApplicationInfo> applicationList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            Logger.log(TAG, "AutoTasker is started.");
            initVariables();
            initRecivers();
            updateUIWithPreferences();
            initDialogAdapters();
            initInputEvents();

            handler.postDelayed(this::launchApplication, 5000);

            if (clickPreferences.isAutoClickActive() && AutoTaskerAccessibilityService.getInstance() != null) {
                handler.postDelayed(() -> AutoTaskerAccessibilityService.getInstance().triggerClick(clickPreferences.getX(), clickPreferences.getY()), 9000);
            }

        } catch (Exception e) {
            Logger.log(TAG, e.toString());
        }

    }

    void initVariables() {
        launchApplicationButton = findViewById(R.id.launchApplicationButton);
        launchApplicationName = findViewById(R.id.launchApplicationName);
        autoWifiSSID = findViewById(R.id.autoWifiSSID);
        applicationTitle = findViewById(R.id.applicationTitle);
        versionTextView = findViewById(R.id.versionTextView);
        launchApplicationSwitch = findViewById(R.id.launchApplicationSwitch);
        autoWifiSwitch = findViewById(R.id.autoWifiSwitch);
        playMediaSwitch = findViewById(R.id.playMediaSwitch);
        killTVAppSwitch = findViewById(R.id.killTVAppSwitch);
        showMessagesSwitch = findViewById(R.id.showMessagesSwitch);
        autoClickSwitch = findViewById(R.id.autoClickSwitch);
        autoClickXInput = findViewById(R.id.autoClickXInput);
        autoClickYInput = findViewById(R.id.autoClickYInput);

        listViewDialog = new Dialog(MainActivity.this);
        logDialog = new Dialog(MainActivity.this);
        listViewDialog.setContentView(R.layout.listview_dialog);
        logDialog.setContentView(R.layout.log_dialog);
        listView = listViewDialog.findViewById(R.id.viewList);

        preferenceManager = PreferenceManager.getInstance();
        preferenceManager.initializePreferences(getApplicationContext());
        launchApplicationPreferences = preferenceManager.getLaunchApplicationPreferences();
        wifiPreferences = preferenceManager.getWifiPreferences();
        clickPreferences = preferenceManager.getClickPreferences();

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        packageManager = getPackageManager();
        applicationList = packageManager.getInstalledApplications(PackageManager.GET_META_DATA).stream().filter(application -> application.category == 1 || application.category == 2).collect(Collectors.toList());
        versionTextView.setText("V" + BuildConfig.VERSION_NAME);
        handler = new Handler();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            wifiSSIDList = wifiManager.getConfiguredNetworks().stream().map(wifiConfiguration -> wifiConfiguration.SSID.replaceAll("\"", "")).collect(Collectors.toList());
        }

    }

    void initRecivers() {
        wifiConnectionReceiver = new WifiConnectionReceiver(getApplicationContext(), new WifiConnectionReceiver.WifiOnReceiveCallback() {
            @Override
            public void onConnected() {
                handler.post(MainActivity.this::launchApplication);
            }

            @Override
            public void onDisconnected() {
                handler.post(MainActivity.this::openWIFISettings);
            }
        });
        this.registerReceiver(wifiConnectionReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
    }

    void initDialogAdapters() {
        wifiSSIDAdapter = new ArrayAdapter<>(this, R.layout.wifi_dialog_list_item, R.id.text, wifiSSIDList);
        applicationListAdapter = new ArrayAdapter<ApplicationInfo>(this, R.layout.start_application_dialog_list_item, applicationList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null)
                    convertView = LayoutInflater.from(parent.getContext()).
                            inflate(R.layout.start_application_dialog_list_item, parent, false);

                String text = applicationList.get(position).loadLabel(getPackageManager()).toString();
                ((TextView) convertView.findViewById(R.id.text)).setText(text);

                Drawable drawable = applicationList.get(position).loadIcon(packageManager);
                ((ImageView) convertView.findViewById(R.id.image)).setImageDrawable(drawable);

                return convertView;
            }
        };
    }

    void startMedia() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY);
        audioManager.dispatchMediaKeyEvent(keyEvent);
    }

    void updateUIWithPreferences() {
        String applicationName = launchApplicationPreferences.getApplicationName();
        String ssid = wifiPreferences.getSsid();
        if (null != applicationName) {
            launchApplicationName.setText(applicationName);
        }
        String logoAsBase64 = launchApplicationPreferences.getLogo();
        if (null != logoAsBase64) {
            launchApplicationButton.setImageBitmap(Base64Helper.toBitmap(logoAsBase64));
        }
        if (null != ssid) {
            autoWifiSSID.setText(ssid);
        }
        autoClickXInput.setText(String.valueOf(clickPreferences.getX()));
        autoClickYInput.setText(String.valueOf(clickPreferences.getY()));

        updateSwitchButtonsWithPreferences();
    }

    void updateSwitchButtonsWithPreferences() {
        launchApplicationSwitch.setChecked(launchApplicationPreferences.isLaunchApplicationActive());
        autoWifiSwitch.setChecked(wifiPreferences.isAutoWifiActive());
        autoClickSwitch.setChecked(clickPreferences.isAutoClickActive());
        playMediaSwitch.setChecked(preferenceManager.isPlayMediaActive());
        killTVAppSwitch.setChecked(preferenceManager.isKillTVAppActive());
        showMessagesSwitch.setChecked(preferenceManager.isShowToastMessagesActive());
    }

    void initInputEvents() {
        autoWifiSSID.setOnClickListener(view -> {
            listView.setAdapter(wifiSSIDAdapter);
            listView.setOnItemClickListener((parent, itemView, position, id) -> {
                String ssid = wifiSSIDList.get(position);
                autoWifiSSID.setText(ssid);
                wifiPreferences.setSsid(ssid);
                preferenceManager.saveWifiPreferences();
                listViewDialog.hide();
            });
            listViewDialog.show();
        });

        applicationTitle.setOnLongClickListener(view -> {
            loggerArea = logDialog.findViewById(R.id.loggerArea);
            clearLogButton = logDialog.findViewById(R.id.clearLogButton);
            clearLogButton.setOnClickListener(v -> {
                boolean cleared = Logger.clearLogs();
                if (cleared) {
                    loggerArea.setText(Logger.NO_LOG);
                    Toast.makeText(this, "Logs are cleared successfully.", Toast.LENGTH_LONG).show();
                }
            });
            loggerArea.setText(Logger.readLogs());
            logDialog.show();
            return true;
        });


        launchApplicationButton.setOnClickListener(view -> {
            listView.setAdapter(applicationListAdapter);
            listView.setOnItemClickListener((parent, itemView, position, id) -> {
                ApplicationInfo applicationInfo = applicationList.get(position);
                String selectedApplicationName = applicationInfo.loadLabel(getPackageManager()).toString();
                Drawable icon = applicationInfo.loadIcon(packageManager);
                String logoAsBase64 = Base64Helper.fromDrawable(icon);
                launchApplicationButton.setImageDrawable(icon);
                launchApplicationName.setText(selectedApplicationName);
                launchApplicationPreferences.setApplicationName(selectedApplicationName);
                launchApplicationPreferences.setLogo(logoAsBase64);
                launchApplicationPreferences.setPackageName(applicationInfo.packageName);
                preferenceManager.saveStartApplicationPreferences();
                listViewDialog.hide();
            });
            listViewDialog.show();
        });

        View.OnFocusChangeListener textViewOnFocusChangeListener = (view, hasFocus) -> {
            if (!hasFocus) {
                TextView textView = (TextView) view;
                String input = textView.getText().toString();
                if (input.length() > 0) {
                    int result = Integer.parseInt(input);
                    if (textView.getId() == R.id.autoClickXInput) {
                        clickPreferences.setX(result);
                    } else {
                        clickPreferences.setY(result);
                    }
                    preferenceManager.saveClickPreferences();
                }

            }
        };

        autoClickXInput.setOnFocusChangeListener(textViewOnFocusChangeListener);
        autoClickYInput.setOnFocusChangeListener(textViewOnFocusChangeListener);

        launchApplicationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            launchApplicationPreferences.setActive(isChecked);
            preferenceManager.saveStartApplicationPreferences();
        });

        autoWifiSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            wifiPreferences.setActive(isChecked);
            preferenceManager.saveWifiPreferences();
        });

        autoClickSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            clickPreferences.setActive(isChecked);
            preferenceManager.saveClickPreferences();
        });

        playMediaSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> preferenceManager.savePlayMediaActivationPreference(isChecked));
        killTVAppSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> preferenceManager.saveKillTVAppActivationPreference(isChecked));
        showMessagesSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> preferenceManager.saveShowToastMessageAppActivationPreference(isChecked));
    }

    void launchApplication() {
        if (!launchApplicationPreferences.isLaunchApplicationActive() || null == launchApplicationPreferences.getPackageName()) {
                return;
        }
        try {
            Intent i = packageManager.getLaunchIntentForPackage(launchApplicationPreferences.getPackageName());
            Logger.log(TAG, i.toString());
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.setAction(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            Logger.log(TAG, "Launching application " + launchApplicationPreferences.getApplicationName());
            getApplicationContext().startActivity(i);
        } catch (Exception e) {
            Logger.log(TAG, e.toString());
        }
    }

    void openWIFISettings() {
        Intent wifiSettingsIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
        wifiSettingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(wifiSettingsIntent);
    }
}