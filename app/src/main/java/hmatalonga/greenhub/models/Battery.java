/*
 * Copyright (c) 2016 Hugo Matalonga & João Paulo Fernandes
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

package hmatalonga.greenhub.models;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;

import hmatalonga.greenhub.Config;
import hmatalonga.greenhub.util.SettingsUtils;

import static hmatalonga.greenhub.util.LogUtils.LOGI;
import static hmatalonga.greenhub.util.LogUtils.makeLogTag;

/**
 * Battery.
 */
public class Battery {

    private static final String TAG = makeLogTag(Battery.class);

    public static void setupBatteryConfig(final Context context) {
        int now;
        boolean isSupported = false;

        // Try default
        for (int i = 0; i < 3; i++) {
            SettingsUtils.saveServerUrl(context, Config.BATTERY_SOURCE_DEFAULT);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                BatteryManager manager = (BatteryManager)
                        context.getSystemService(Context.BATTERY_SERVICE);
                now = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
                if (now == 0) {
                    now = getBatteryCurrentNowLegacy();
                }
            } else {
                now = getBatteryCurrentNowLegacy();
            }

            isSupported = isSupported || (now != 0);
        }

        if (isSupported) {
            SettingsUtils.markBatteryNowSupported(context, true);
            return;
        }

        // Try alternative
        for (int i = 0; i < 3; i++) {
            SettingsUtils.saveServerUrl(context, Config.BATTERY_SOURCE_ALTERNATIVE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                BatteryManager manager = (BatteryManager)
                        context.getSystemService(Context.BATTERY_SERVICE);
                now = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
                if (now == 0) {
                    now = getBatteryCurrentNowLegacy();
                }
            } else {
                now = getBatteryCurrentNowLegacy();
            }

            isSupported = isSupported || (now != 0);
        }

        SettingsUtils.markBatteryNowSupported(context, isSupported);
    }

    public static double getBatteryVoltage(final Context context) {
        Intent receiver =
                context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        if(receiver == null) return -1;

        double voltage = receiver.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);

        return (voltage == -1) ? voltage : voltage / 1000;
    }

    public static int getBatteryCapacity(final Context context) {
        if (Build.VERSION.SDK_INT >= 21) {
            BatteryManager manager = (BatteryManager)
                    context.getSystemService(Context.BATTERY_SERVICE);
            int value = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

            return (value != Integer.MIN_VALUE) ? value : -1;
        }

        try {
            // Please note: Uses reflection, API not available on all devices
            Class<?> powerProfile = Class.forName("com.android.internal.os.PowerProfile");
            Object mPowerProfile = powerProfile.getConstructor(Context.class).newInstance(context);
            Method getAveragePower = powerProfile.getMethod("getAveragePower", String.class);
            getAveragePower.setAccessible(true);
            // TODO: java.lang.ClassCastException: java.lang.Double cannot be cast to java.lang.Integer
            String value = getAveragePower.invoke(mPowerProfile, "battery.capacity").toString();
            return ((int) Double.parseDouble(value));
        } catch (Throwable th) {
            th.printStackTrace();
        }

        return -1;
    }

    public static int getBatteryChargeCounter(final Context context) {
        if (Build.VERSION.SDK_INT >= 21) {
            BatteryManager manager = (BatteryManager)
                    context.getSystemService(Context.BATTERY_SERVICE);
            return manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
        }
        return -1;
    }

    public static int getBatteryCurrentAverage(final Context context) {
        int value = -1;

        if (Build.VERSION.SDK_INT >= 21) {
            BatteryManager manager = (BatteryManager)
                    context.getSystemService(Context.BATTERY_SERVICE);
            value = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE);
        }

        return (value != Integer.MIN_VALUE) ? value : -1;
    }

    public static int getBatteryCurrentNow(final Context context) {
        int value;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            BatteryManager manager = (BatteryManager)
                    context.getSystemService(Context.BATTERY_SERVICE);
            value = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        } else {
           value = getBatteryCurrentNowLegacy();
        }

        return (value != Integer.MIN_VALUE) ? value / 1000 : -1;
    }

    public static long getBatteryEnergyCounter(final Context context) {
        long value = -1;

        if (Build.VERSION.SDK_INT >= 21) {
            BatteryManager manager = (BatteryManager)
                    context.getSystemService(Context.BATTERY_SERVICE);
            value = manager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER);
        }

        return (value != Long.MIN_VALUE) ? value : -1;
    }

    private static int getBatteryCurrentNowLegacy() {
        int value = -1;

        try {
            RandomAccessFile reader = new RandomAccessFile(Config.BATTERY_SOURCE_DEFAULT, "r");
            String average = reader.readLine();
            value = Integer.parseInt(average);
            reader.close();
        } catch (IOException e) {
            // Device has no current_avg file available
            LOGI(TAG, "Device has no current_avg file available");
        }
        return -value;
    }
}
