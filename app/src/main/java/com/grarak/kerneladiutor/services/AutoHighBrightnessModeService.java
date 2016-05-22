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
import android.util.Log;

import com.grarak.kerneladiutor.utils.Constants;
import com.grarak.kerneladiutor.utils.Utils;
import com.grarak.kerneladiutor.utils.kernel.Screen;
import com.grarak.kerneladiutor.utils.root.Control;

/**
 * Created by willi on 08.03.15.
 */
public class AutoHighBrightnessModeService extends Service {
    float lux = 0;
    public static int LuxThresh = 50000;
    public static boolean AutoHBMSensorEnabled = false, HBMActive = false;

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
        super.onDestroy();
        unregisterAutoHBMReceiver(getApplicationContext());
    }

    private void init() {
        registerAutoHBMReceiver(getApplicationContext());
    }

    public void activateLightSensorRead() {
        sMgr = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);

        light = sMgr.getDefaultSensor(Sensor.TYPE_LIGHT);

        sMgr.registerListener(_SensorEventListener, light, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void deactivateLightSensorRead() {
        sMgr.unregisterListener(_SensorEventListener);
        AutoHBMSensorEnabled = false;
    }

    SensorEventListener _SensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (AutoHBMSensorEnabled) {
                lux = event.values[0];

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
                    if (!Utils.getBoolean("AutoHBM", false, getApplicationContext())) {
                        AutoHBMSensorEnabled = false;
                        LuxThresh = Utils.getInt("AutoHBM_Threshold", 1500, getApplicationContext());
                    }
                    deactivateLightSensorRead();
                }

                if (strAction.equals(android.content.Intent.ACTION_SCREEN_ON)) {
                    if (Utils.getBoolean("AutoHBM", false, getApplicationContext())) {
                        AutoHBMSensorEnabled = true;
                        LuxThresh = Utils.getInt("AutoHBM_Threshold", 1500, getApplicationContext());
                    }
                    activateLightSensorRead();
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

