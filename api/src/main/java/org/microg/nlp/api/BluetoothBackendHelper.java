/*
 * SPDX-FileCopyrightText: 2013, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.api;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.HashSet;
import java.util.Set;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.BLUETOOTH;
import static android.Manifest.permission.BLUETOOTH_ADMIN;

/**
 * Utility class to support backend using Device for geolocation.
 */
@SuppressWarnings({"MissingPermission", "WeakerAccess", "unused"})
public class BluetoothBackendHelper extends AbstractBackendHelper {
    private final static IntentFilter bluetoothBroadcastFilter =
            new IntentFilter();

    private final Listener listener;
    private final BluetoothAdapter bluetoothAdapter;
    private final Set<Device> devices = new HashSet<>();
    private final BroadcastReceiver bluetoothBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                devices.clear();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                onBluetoothChanged();
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                    Device deviceDiscovered = new Device(device.getAddress(), device.getName(), rssi);
                    devices.add(deviceDiscovered);
                }
            }
        }
    };

    public BluetoothBackendHelper(Context context, Listener listener){
        super(context);
        if (listener == null)
            throw new IllegalArgumentException("listener must not be null");
        this.listener = listener;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothBroadcastFilter.addAction(BluetoothDevice.ACTION_FOUND);
        bluetoothBroadcastFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        bluetoothBroadcastFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
    }

    public synchronized void onOpen() {
        super.onOpen();
        bluetoothBroadcastFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        bluetoothBroadcastFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        bluetoothBroadcastFilter.addAction(BluetoothDevice.ACTION_FOUND);
        context.registerReceiver(bluetoothBroadcastReceiver, bluetoothBroadcastFilter);
    }

    public synchronized void onClose() {
        super.onClose();
        context.unregisterReceiver(bluetoothBroadcastReceiver);
    }

    public synchronized void onUpdate() {
        if (!currentDataUsed) {
            listener.onDevicesChanged(getDevices());
        } else {
            scanBluetooth();
        }
    }

    @Override
    public String[] getRequiredPermissions() {
        return new String[]{BLUETOOTH, BLUETOOTH_ADMIN, ACCESS_COARSE_LOCATION};
    }

    private void onBluetoothChanged() {
        if (loadBluetooths()) {
            listener.onDevicesChanged(getDevices());
        }
    }

    private synchronized boolean scanBluetooth() {
        if (state == State.DISABLED)
            return false;
        if (bluetoothAdapter.isEnabled()) {
            state = State.SCANNING;
            bluetoothAdapter.startDiscovery();
            return true;
        }
        return false;
    }

    private synchronized boolean loadBluetooths() {
        currentDataUsed = false;
        if (state == State.DISABLING)
            state = State.DISABLED;
        switch (state) {
            default:
            case DISABLED:
                return false;
            case SCANNING:
                state = State.WAITING;
                return true;
        }
    }

    public synchronized Set<Device> getDevices() {
        currentDataUsed = true;
        return new HashSet<>(devices);
    }

    public interface Listener {
        void onDevicesChanged(Set<Device> device);
    }

    public static class Device {
        private final String bssid;
        private final String name;
        private final int rssi;

        public String getBssid() { return bssid; }

        public String getName() {return name; }

        public int getRssi() { return rssi; }

        public Device(String bssid, String name, int rssi) {
            this.bssid = Utils.wellFormedMac(bssid);
            this.name = name;
            this.rssi = rssi;
        }

        @Override
        public String toString() {
            return "Device{" +
                    "name=" + name +
                    ", bssid=" + bssid +
                    ", rssi=" + rssi +
                    "}";
        }
    }
}
