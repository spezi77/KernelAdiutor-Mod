/*
 * Copyright (C) 2015 Willi Ye
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.grarak.kerneladiutor.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.grarak.kerneladiutor.MainActivity;
import com.grarak.kerneladiutor.utils.Constants;
import com.grarak.kerneladiutor.utils.Utils;
import com.grarak.kerneladiutor.utils.kernel.Screen;

/**
 * Created by willi on 08.03.15.
 */
public class AutoHighBrightnessModeService extends Service {
    public static float lux = 0, avglux = 0;
    public static int LuxThresh = 3000;
    public static boolean HBM_Widget_Toggled = false;
    public static float[] luxvalues = new float [3];


    private SensorManager sMgr;
    Sensor light;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    @Override
    public void onDestroy() {
        LuxThresh = Screen.getAutoHBMThresh(getApplicationContext());
        float[] luxvalues = new float [Screen.getAutoHBMSmoothingSamples(getApplicationContext())];
        super.onDestroy();
        unregisterAutoHBMReceiver(getApplicationContext());
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT > 19 && pm.isInteractive()) {
            deactivateLightSensorRead();
        } else if (Build.VERSION.SDK_INT < 20 ) {
            deactivateLightSensorRead();
        }
    }

    private void init() {
        registerAutoHBMReceiver(getApplicationContext());
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT > 19 && pm.isInteractive()) {
            activateLightSensorRead();
        } else if (Build.VERSION.SDK_INT < 20 ) {
            activateLightSensorRead();
        }
    }

    public void activateLightSensorRead() {
        sMgr = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);

        light = sMgr.getDefaultSensor(Sensor.TYPE_LIGHT);

        sMgr.registerListener(_SensorEventListener, light, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void deactivateLightSensorRead() {
        sMgr.unregisterListener(_SensorEventListener);
    }

    SensorEventListener _SensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            // Call Screen.hasScreenHBM() here in the if block to ensure that the appropriate variables are set when calling Screen.activateScreenHBM
            if (Screen.isScreenAutoHBMSmoothingActive(MainActivity.context) && Screen.hasScreenHBM()) {
                // This should average the last 3 lux values
                // This will cause some delay in autohbm actually working as the values initialize at 0
                avglux = 0;
                for(int i = 1; i <= luxvalues.length-1; i++) {
                    luxvalues[i] = luxvalues[i - 1];
                    avglux += luxvalues[i - 1];
                }
                luxvalues[0] = event.values[0];
                avglux += luxvalues[0];
                lux = Math.round(avglux / luxvalues.length);
            } else {
                lux = Math.round(event.values[0]);
            }
            if (!HBM_Widget_Toggled) {
                if (lux >= LuxThresh && !Screen.isScreenHBMActive()) {
                    Log.i("Kernel Adiutor: ", "AutoHBMService Activating HBM: received LUX value: " + lux + " Threshold: " + LuxThresh);
                    Screen.activateScreenHBM(true, getApplicationContext());
                }
                if (lux < LuxThresh && Screen.isScreenHBMActive()) {
                    Log.i("Kernel Adiutor: ", "De-Activation: AutoHBMService: received LUX value: " + lux + " Threshold: " + LuxThresh);
                    Screen.activateScreenHBM(false, getApplicationContext());
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private BroadcastReceiver AutoHBMreceiver = null;

    private void registerAutoHBMReceiver(Context context) {
        final IntentFilter autohbmfilter = new IntentFilter();
        /** System Defined Broadcast */
        autohbmfilter.addAction(android.content.Intent.ACTION_SCREEN_ON);
        autohbmfilter.addAction(android.content.Intent.ACTION_SCREEN_OFF);

        AutoHBMreceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, android.content.Intent intent) {
                String strAction = intent.getAction();
                if (strAction.equals(android.content.Intent.ACTION_SCREEN_OFF)) {
                    HBM_Widget_Toggled = false;
                    if (Screen.isScreenAutoHBMActive(getApplicationContext())) {
                        LuxThresh = Screen.getAutoHBMThresh(getApplicationContext());
                        deactivateLightSensorRead();
                    }
                }

                if (strAction.equals(android.content.Intent.ACTION_SCREEN_ON)) {
                    HBM_Widget_Toggled = false;
                    if (Screen.isScreenAutoHBMActive(getApplicationContext())) {
                        LuxThresh = Screen.getAutoHBMThresh(getApplicationContext());
                        activateLightSensorRead();
                    }
                }
            }
        };

        context.registerReceiver(AutoHBMreceiver, autohbmfilter);
    }

    private void unregisterAutoHBMReceiver(Context context) {
        int apiLevel = Build.VERSION.SDK_INT;

        if (apiLevel >= 7) {
            try {
                context.unregisterReceiver(AutoHBMreceiver);
            } catch (IllegalArgumentException e) {
                AutoHBMreceiver = null;
            }
        } else {
            AutoHBMreceiver = null;
        }
    }
}

