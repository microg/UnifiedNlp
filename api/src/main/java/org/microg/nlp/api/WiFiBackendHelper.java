/*
 * SPDX-FileCopyrightText: 2013, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.api;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_WIFI_STATE;
import static android.Manifest.permission.CHANGE_WIFI_STATE;

/**
 * Utility class to support backends that use Wi-Fis for geolocation.
 */
@SuppressWarnings({"MissingPermission", "WeakerAccess", "unused"})
public class WiFiBackendHelper extends AbstractBackendHelper {
    private final static IntentFilter wifiBroadcastFilter =
            new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

    private final Listener listener;
    private final WifiManager wifiManager;
    private final Set<WiFi> wiFis = new HashSet<>();
    private final BroadcastReceiver wifiBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onWiFisChanged();
        }
    };

    private boolean ignoreNomap = true;

    /**
     * Create a new instance of {@link WiFiBackendHelper}. Call this in
     * {@link LocationBackendService#onCreate()}.
     *
     * @throws IllegalArgumentException if either context or listener is null.
     */
    public WiFiBackendHelper(Context context, Listener listener) {
        super(context);
        if (listener == null)
            throw new IllegalArgumentException("listener must not be null");
        this.listener = listener;
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    /**
     * Sets whether to ignore the "_nomap" flag on Wi-Fi SSIDs or not.
     * <p/>
     * Usually, Wi-Fis whose SSID end with "_nomap" are ignored for geolocation. This behaviour can
     * be suppressed by {@code setIgnoreNomap(false)}.
     * <p/>
     * Default is {@code true}.
     */
    public void setIgnoreNomap(boolean ignoreNomap) {
        this.ignoreNomap = ignoreNomap;
    }

    /**
     * Call this in {@link LocationBackendService#onOpen()}.
     */
    public synchronized void onOpen() {
        super.onOpen();
        context.registerReceiver(wifiBroadcastReceiver, wifiBroadcastFilter);
    }

    /**
     * Call this in {@link LocationBackendService#onClose()}.
     */
    public synchronized void onClose() {
        super.onClose();
        context.unregisterReceiver(wifiBroadcastReceiver);
    }

    /**
     * Call this in {@link LocationBackendService#update()}.
     */
    public synchronized void onUpdate() {
        if (!currentDataUsed) {
            listener.onWiFisChanged(getWiFis());
        } else {
            scanWiFis();
        }
    }

    @Override
    public String[] getRequiredPermissions() {
        return new String[]{CHANGE_WIFI_STATE, ACCESS_WIFI_STATE, ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION};
    }

    private void onWiFisChanged() {
        if (loadWiFis()) {
            listener.onWiFisChanged(getWiFis());
        }
    }

    @SuppressWarnings("deprecation")
    private synchronized boolean scanWiFis() {
        if (state == State.DISABLED)
            return false;
        if (wifiManager.isWifiEnabled() || isScanAlwaysAvailable()) {
            state = State.SCANNING;
            wifiManager.startScan();
            return true;
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @SuppressWarnings("deprecation")
    private boolean isScanAlwaysAvailable() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                && wifiManager.isScanAlwaysAvailable();
    }

    private synchronized boolean loadWiFis() {
        int oldHash = wiFis.hashCode();
        wiFis.clear();
        currentDataUsed = false;
        List<ScanResult> scanResults = wifiManager.getScanResults();
        for (ScanResult scanResult : scanResults) {
            if (ignoreNomap && scanResult.SSID.toLowerCase(Locale.US).endsWith("_nomap")) continue;
            wiFis.add(new WiFi(scanResult.BSSID, scanResult.level, frequencyToChannel(scanResult.frequency), scanResult.frequency));
        }
        if (state == State.DISABLING)
            state = State.DISABLED;
        switch (state) {
            default:
            case DISABLED:
                return false;
            case SCANNING:
                state = State.WAITING;
                return wiFis.hashCode() != oldHash;
        }
    }

    @SuppressWarnings("MagicNumber")
    private static int frequencyToChannel(int freq) {
        if (freq >= 2412 && freq <= 2484) {
            return (freq - 2412) / 5 + 1;
        } else if (freq >= 5170 && freq <= 5825) {
            return (freq - 5170) / 5 + 34;
        } else {
            return -1;
        }
    }

    /**
     * @return the latest scan result.
     */
    public synchronized Set<WiFi> getWiFis() {
        currentDataUsed = true;
        return new HashSet<>(wiFis);
    }

    /**
     * Interface to listen for Wi-Fi scan results.
     */
    public interface Listener {
        /**
         * Called when a new set of Wi-Fi's is discovered.
         */
        void onWiFisChanged(Set<WiFi> wiFis);
    }

    /**
     * Represents a generic Wi-Fi scan result.
     * <p/>
     * This does contain the BSSID (mac address) and the RSSI (in dBm) of a Wi-Fi.
     * Additional data is not provided, but also not usable for geolocation.
     */
    public static class WiFi {
        private final String bssid;
        private final int rssi;
        private final int channel;
        private final int frequency;

        public String getBssid() {
            return bssid;
        }

        public int getRssi() {
            return rssi;
        }

        public int getChannel() {
            return channel;
        }

        public int getFrequency() {
            return frequency;
        }

        public WiFi(String bssid, int rssi) {
            this(bssid, rssi, -1, -1);
        }

        public WiFi(String bssid, int rssi, Integer channel, Integer frequency) {
            this.bssid = Utils.wellFormedMac(bssid);
            this.rssi = rssi;
            this.channel = channel;
            this.frequency = frequency;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            WiFi wiFi = (WiFi) o;

            if (rssi != wiFi.rssi) return false;
            if (channel != wiFi.channel) return false;
            if (frequency != wiFi.frequency) return false;
            return bssid != null ? bssid.equals(wiFi.bssid) : wiFi.bssid == null;
        }

        @Override
        public int hashCode() {
            int result = bssid != null ? bssid.hashCode() : 0;
            result = 31 * result + rssi;
            result = 31 * result + channel;
            result = 31 * result + frequency;
            return result;
        }
    }
}
