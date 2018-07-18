package io.bbqresearch.dsc.service;

/*
 * Copyright (C) 2013 The Android Open Source Project
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


import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class DscService extends Service {

    public final static String ACTION_CONNECTED =
            "io.bbqresearch.android.dirtsimplecomms.ACTION_CONNECTED";
    public final static String ACTION_DISCONNECTED =
            "io.bbqresearch.android.dirtsimplecomms.ACTION_DISCONNECTED";
    public final static String ACTION_READY =
            "io.bbqresearch.android.dirtsimplecomms.ACTION_READY";
    public final static String ACTION_NEW_MESSAGE =
            "io.bbqresearch.android.dirtsimplecomms.NEW_MESSAGE";
    public final static String EXTRA_DATA =
            "io.bbqresearch.android.dirtsimplecomms.EXTRA_DATA";

    /* defines (in milliseconds) how often RSSI should be updated */
    private static final int RSSI_UPDATE_TIME_INTERVAL = 1500; // 1.5 seconds
    private final static String TAG = DscService.class.getSimpleName();
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private final IBinder mBinder = new LocalBinder();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private String mBluetoothDeviceAddress;
    private String mBluetoothDeviceName;
    private boolean isServicesDiscovered = false;
    private boolean mConnected = false;
    private int mConnectionState = STATE_DISCONNECTED;

    private boolean mSettingsChanged = false;
    private boolean mSettingsNotifyActive = false;


    private Handler mTimerHandler = new Handler();
    private boolean mTimerEnabled = false;
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnectionState = STATE_CONNECTED;
                Log.i(TAG, "Connected to DSC GATT server.");
                broadcastUpdate(ACTION_CONNECTED);

                mBluetoothGatt.discoverServices();

                mConnected = true;
                //startMonitoringRssiValue();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState = STATE_DISCONNECTED;
                String statusDesc = "";
                if (status == 19) statusDesc = "Remote Host Forced Disconnect";
                else if (status == 21) statusDesc = "Remote Host Terminated Connection (Power Off)";
                else if (status == 22) statusDesc = "Local Host Terminated Connection (Power Off)";

                Log.i(TAG, "BLE:" + statusDesc + "(" + status + ")");
                mConnected = false;
                broadcastUpdate(ACTION_DISCONNECTED);
                stopMonitoringRssiValue();

            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (checkForReqServices(getSupportedGattServices())) {
                    isServicesDiscovered = true;
                    Log.d(TAG, "DSC Services Discovered.");


                    setCharacteristicNotification(DscGattAttributes.DSC_SETTINGS_UUID, true);
                    /*handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {

                        }
                    }, 3000);*/

                   /* handler2.postDelayed(new Runnable() {
                        @Override
                        public void run() {

                        }
                    }, 6000);*/

                }
            } else {
                isServicesDiscovered = false;
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d(TAG, "onDescriptorWrite Status: " + status);
            if (descriptor.getCharacteristic().getUuid().equals(UUID.fromString(DscGattAttributes.DSC_SETTINGS_UUID))) {
                setCharacteristicNotification(DscGattAttributes.DSC_MSG_INBOUND, true);
            } else if (descriptor.getCharacteristic().getUuid().equals(UUID.fromString(DscGattAttributes.DSC_MSG_INBOUND))) {
                broadcastUpdate(ACTION_READY);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.d(TAG, "onCharacteristicRead called.");
            if (UUID.fromString(DscGattAttributes.DSC_SETTINGS_UUID).equals(characteristic.getUuid())) {
                byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);

                    for (byte byteChar : data)
                        stringBuilder.append(String.format("%02X", byteChar));
                    processInboundFromDsc(hexStringToByteArray(stringBuilder.toString()));
                }
            }
            //broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            mSettingsNotifyActive = true;
            Log.d(TAG, "onCharacteristicChanged called.");
            if (UUID.fromString(DscGattAttributes.DSC_SETTINGS_UUID).equals(characteristic.getUuid())) {
                byte[] data = characteristic.getValue();
                Log.d(TAG, Arrays.toString(data));
                Log.d(TAG, "bytes: " + data.length);
                if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);

                    for (byte byteChar : data)
                        stringBuilder.append(String.format("%02X", byteChar));
                    processInboundFromDsc(hexStringToByteArray(stringBuilder.toString()));
                }
            } else if (UUID.fromString(DscGattAttributes.DSC_MSG_INBOUND).equals(characteristic.getUuid())) {
                byte[] data = characteristic.getValue();
                Log.d(TAG, Arrays.toString(data));
                Log.d(TAG, "bytes: " + data.length);
                if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);

                    for (byte byteChar : data)
                        stringBuilder.append(String.format("%02X", byteChar));
                    processInboundFromDsc(hexStringToByteArray(stringBuilder.toString()));
                }
            }
            //broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e("onCharacteristicWrite", "Failed write, Connection Failure.");
            } else {

                if (mSettingsChanged) {
                    mSettingsChanged = false;
                }
            }
            Log.d(TAG, "onCharacteristicWrite called (Status: " + status + ")");
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.d(TAG, "Remote RSSI: " + rssi);
        }

        public boolean checkGattStatusAndLog(int status, String operation) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

            } else if (status == 1) {
                Log.e(TAG, operation + ": Invalid Handle (Gatt Service Cached? Cycle Bluetooth Power on Device");
            } else if (BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION == status) {
                Log.e(TAG, operation + ": Insufficient authentication (Maybe not bonded?)");
            } else if (BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION == status) {
                Log.e(TAG, operation + ": Insufficient encryption (Maybe not bonded?)");
            } else if (BluetoothGatt.GATT_CONNECTION_CONGESTED == status) {
                Log.e(TAG, operation + ": Connection congested.");
            } else if (BluetoothGatt.GATT_FAILURE == status) {
                Log.e(TAG, operation + ": General Failure.");
            } else if (BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH == status) {
                Log.e(TAG, operation + ": Invalid Attribute length.");
            } else if (BluetoothGatt.GATT_INVALID_OFFSET == status) {
                Log.e(TAG, operation + ": Invalid Offset.");
            } else if (BluetoothGatt.GATT_READ_NOT_PERMITTED == status) {
                Log.e(TAG, operation + ": Read not Permitted.");
            } else if (BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED == status) {
                Log.e(TAG, operation + ": Request not supported");
            } else if (BluetoothGatt.GATT_WRITE_NOT_PERMITTED == status) {
                Log.e(TAG, operation + ": Write not Permitted.");
            } else {
                Log.e(TAG, operation + ": General Failure did not catch specific status flag: " + status);
            }
            return status == 0;
        }
    };

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }
    private SharedPreferences.OnSharedPreferenceChangeListener listener;

    public void setBluetoothDeviceAddress(String mBluetoothDeviceAddress) {
        this.mBluetoothDeviceAddress = mBluetoothDeviceAddress;
    }

    public void setBluetoothDeviceName(String mBluetoothDeviceName) {
        this.mBluetoothDeviceName = mBluetoothDeviceName;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences prefs = this.getSharedPreferences("settings", 0);
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {

            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                Log.d(TAG, "Pref Changed: " + key);


                writeParams_ble("");

                //readParams_ble();
                switch (key) {
                    case "airplane_mode":
                        Log.d(TAG, "Pref Changed: " + prefs.getBoolean(key, true));
                      /*  BluetoothGattService mCustomService = mBluetoothGatt.getService(UUID.fromString(DscGattAttributes.DSC_SERVICE_UUID));
                        if (mCustomService == null) {
                            Log.w(TAG, "DSC Parameters BLE Service not found");
                            return;
                        }
                        BluetoothGattCharacteristic gattCharacteristic = mCustomService.getCharacteristic(UUID.fromString(DscGattAttributes.DSC_SETTINGS_UUID));
                        if (prefs.getBoolean(key,true)) {
                            setCharacteristicNotification(gattCharacteristic, true);
                        }
                        else {
                            setCharacteristicNotification(gattCharacteristic, false);
                        }*/
                        break;
                }
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(listener);
    }


    /* request new RSSi value for the connection*/
    public void readPeriodicalyRssiValue(final boolean repeat) {
        mTimerEnabled = repeat;
        // check if we should stop checking RSSI value
        if (mConnected == false || mBluetoothGatt == null || mTimerEnabled == false) {
            mTimerEnabled = false;
            return;
        }

        mTimerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mBluetoothGatt == null ||
                        mBluetoothAdapter == null ||
                        mConnected == false) {
                    mTimerEnabled = false;
                    return;
                }

                // request RSSI value
                mBluetoothGatt.readRemoteRssi();
                // add call it once more in the future
                readPeriodicalyRssiValue(mTimerEnabled);
            }
        }, RSSI_UPDATE_TIME_INTERVAL);
    }

    /* starts monitoring RSSI value */
    public void startMonitoringRssiValue() {
        readPeriodicalyRssiValue(true);
    }

    /* stops monitoring of RSSI value */
    public void stopMonitoringRssiValue() {
        readPeriodicalyRssiValue(false);
    }


    public void setCharacteristicNotification(String characteristicUUID,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        BluetoothGattService mCustomService = mBluetoothGatt.getService(UUID.fromString(DscGattAttributes.DSC_SERVICE_UUID));
        if (mCustomService == null) {
            Log.w(TAG, "DSC Parameters BLE Service not found");
            return;
        }
        BluetoothGattCharacteristic characteristic = mCustomService.getCharacteristic(UUID.fromString(characteristicUUID));
        Log.d(TAG, "Activating GATT Notifications");
        for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
            Log.d(TAG, "Descriptors: " + descriptor.getUuid());
        }
        characteristic.getDescriptors();

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                UUID.fromString(DscGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));

        if (enabled) descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        else descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    private boolean checkForReqServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return false;
        String uuid = null;
        List<String> uuids = new ArrayList<String>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();
            uuids.add(uuid);
            for (BluetoothGattCharacteristic gattCharacteristic : gattService.getCharacteristics()) {
                uuid = gattCharacteristic.getUuid().toString();
                uuids.add(uuid);

                int prop = gattCharacteristic.getProperties();
                //Log.d(TAG, "Characteristic Property: " + gattCharacteristic.getProperties());

                if (prop >= BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS) {
                    prop -= BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS;
                    Log.d(TAG, uuid + ": Extended Properties");
                }
                if (prop >= BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) {
                    prop -= BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE;
                    Log.d(TAG, uuid + ": Signed Write");
                }
                if (prop >= BluetoothGattCharacteristic.PROPERTY_INDICATE) {
                    prop -= BluetoothGattCharacteristic.PROPERTY_INDICATE;
                    Log.d(TAG, uuid + ": Indicate");
                }
                if (prop >= BluetoothGattCharacteristic.PROPERTY_NOTIFY) {
                    prop -= BluetoothGattCharacteristic.PROPERTY_NOTIFY;
                    Log.d(TAG, uuid + ": Notify");
                }
                if (prop >= BluetoothGattCharacteristic.PROPERTY_WRITE) {
                    prop -= BluetoothGattCharacteristic.PROPERTY_WRITE;
                    Log.d(TAG, uuid + ": Write");
                }
                if (prop >= BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) {
                    prop -= BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;
                    Log.d(TAG, uuid + ": Write No Response");
                }
                if (prop >= BluetoothGattCharacteristic.PROPERTY_READ) {
                    prop -= BluetoothGattCharacteristic.PROPERTY_READ;
                    Log.d(TAG, uuid + ": Read");
                }
                if (prop >= BluetoothGattCharacteristic.PROPERTY_BROADCAST) {
                    prop -= BluetoothGattCharacteristic.PROPERTY_BROADCAST;
                    Log.d(TAG, uuid + ": Broadcast");
                }


            }
        }

        return DscGattAttributes.checkAllReqAttributesAvail(uuids);

    }

    private String getTopic(String jsonmsg) {
        try {
            JSONObject nodeRoot = new JSONObject(jsonmsg);

            return nodeRoot.getString("topic");
        } catch (Exception e) {
            //Log.e(TAG, "getTopic " + jsonmsg);
        }
        return "";
    }

    private void processInboundFromDsc(String jsonmsg) {
        //Log.d(TAG, "Got Json String: " + jsonmsg);

        if (getTopic(jsonmsg).contentEquals("getparms")) {
            compareSettings(jsonmsg);
            Log.d(TAG, "process inbound msg");
        } else if (getTopic(jsonmsg).contentEquals("newmsg")) {
            final Intent intent = new Intent(ACTION_NEW_MESSAGE);
            intent.putExtra(EXTRA_DATA, jsonmsg);
            sendBroadcast(intent);
            Log.d(TAG, "newmsg: " + jsonmsg);
        } else {
            Log.d(TAG, "Unknown Incoming: " + jsonmsg.toString());
        }
    }

    public void compareSettings(String jsonmsg) {
        SharedPreferences prefs = this.getSharedPreferences("settings", 0);
        try {
            JSONObject nodeRoot = new JSONObject(jsonmsg);
            JSONObject payload = nodeRoot.getJSONObject("payload");

            int equal_cnt = 0;
            int total_cnt = 11; // Parameters that need to match

            if (payload.getBoolean("airplane_mode") == prefs.getBoolean("airplane_mode", true)) {
                equal_cnt += 1;
            } else Log.e(TAG, "airplane" + " not equal");
            if (payload.getString("freq").contentEquals(prefs.getString("freq", "915.000"))) {
                equal_cnt += 1;
            } else Log.e(TAG, "freq" + " not equal");
            if (payload.getInt("bw") == Integer.parseInt(prefs.getString("bandwidth", "0"))) {
                equal_cnt += 1;
            } else Log.e(TAG, "bw" + " not equal");
            if (payload.getInt("coding_rate") == Integer.parseInt(prefs.getString("coding_rate", "0"))) {
                equal_cnt += 1;
            } else Log.e(TAG, "coding_rate" + " not equal");
            if (payload.getInt("deadband") == Integer.parseInt(prefs.getString("tx_deadband", "0"))) {
                equal_cnt += 1;
            } else Log.e(TAG, "deadband" + " not equal");
            if (payload.getInt("sp_factor") == Integer.parseInt(prefs.getString("spread_factor", "0"))) {
                equal_cnt += 1;
            } else Log.e(TAG, "sp_factor" + " not equal");
            if (payload.getInt("sync_word") == Integer.parseInt(prefs.getString("sync_word", "0"))) {
                equal_cnt += 1;
            } else Log.e(TAG, "sync_word" + " not equal");
            if (payload.getInt("tdma_slot") == Integer.parseInt(prefs.getString("tdma_slot", "0"))) {
                equal_cnt += 1;
            } else Log.e(TAG, "tdma_slot" + " not equal");
            if (payload.getInt("tx_power") == Integer.parseInt(prefs.getString("tx_power", "0"))) {
                equal_cnt += 1;
            } else Log.e(TAG, "tx_power" + " not equal");
            if (payload.getInt("tx_time") == Integer.parseInt(prefs.getString("tx_time", "0"))) {
                equal_cnt += 1;
            } else Log.e(TAG, "tx_time" + " not equal");
            if (payload.getInt("total_nodes") == Integer.parseInt(prefs.getString("total_nodes", "0"))) {
                equal_cnt += 1;
            } else Log.e(TAG, "total_nodes" + " not equal");
            if (equal_cnt == total_cnt) {
                Log.d(TAG, "DSC Settings Equal");
            } else {
                Log.e(TAG, "DSC Settings not Equal: " + equal_cnt);
            }
        } catch (Exception e) {
            Log.e(TAG, "updateSettings error: " + jsonmsg);
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public boolean ismConnected() {
        return mConnected;
    }

    public boolean isReady() {
        return mConnectionState == STATE_CONNECTED && isServicesDiscovered;
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }



    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String deviceAddress) {
        mBluetoothDeviceAddress = deviceAddress;
        //String address = settings.getDeviceAddr();
        if (mBluetoothAdapter == null || mBluetoothDeviceAddress == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        /*if (mBluetoothGatt != null && mBluetoothGatt.getDevice().getAddress().equals(mBluetoothDeviceAddress)) {
            // just reconnect
            return mBluetoothGatt.connect();
        } else {*/

            final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mBluetoothDeviceAddress);
            if (device == null) {
                Log.w(TAG, "Device not found.  Unable to connect.");
                return false;
            }
            // We want to directly connect to the device, so we are setting the autoConnect
            // parameter to false.

            mBluetoothGatt = device.connectGatt(this, false, mGattCallback, BluetoothDevice.TRANSPORT_LE);

            Log.d(TAG, "Trying to create a new connection.");
            //mBluetoothDeviceAddress = address;
            mConnectionState = STATE_CONNECTING;
            return true;
        //}
    }


    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }
/*
    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
 /*   public void setSettingsNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        try {
            //Log.d(TAG, "Is this settings uuid?: " + characteristic.getUuid().toString());
            if (UUID.fromString(DscGattAttributes.DSC_SETTINGS_UUID).equals(characteristic.getUuid())) {
                Log.d(TAG, "Enabling DSC Settings Push Notification");
                mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
            }
            //Log.d(TAG, characteristic.getValue().toString());
        } catch (Exception e) {
            Log.d(TAG, "Error reading value from chara." + e.getStackTrace());
        }
    }
*/
    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    public void readParams_ble() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        BluetoothGattService mCustomService = mBluetoothGatt.getService(UUID.fromString(DscGattAttributes.DSC_SERVICE_UUID));
        if (mCustomService == null) {
            Log.w(TAG, "Custom BLE Service not found");
            return;
        }
        BluetoothGattCharacteristic mReadCharacteristic = mCustomService.getCharacteristic(UUID.fromString(DscGattAttributes.DSC_SETTINGS_UUID));
        if (mBluetoothGatt.readCharacteristic(mReadCharacteristic) == false) {
            Log.w(TAG, "Failed to read characteristic");
        }
    }

    public void writeParams_ble(String value) {

        //For now we ignore value argument. Write all settings.
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        BluetoothGattService mCustomService = mBluetoothGatt.getService(UUID.fromString(DscGattAttributes.DSC_SERVICE_UUID));
        if (mCustomService == null) {
            Log.w(TAG, "DSC Parameters BLE Service not found");
            return;
        }
        BluetoothGattCharacteristic mWriteCharacteristic = mCustomService.getCharacteristic(UUID.fromString(DscGattAttributes.DSC_SETTINGS_UUID));
        //mWriteCharacteristic.setValue(settings.getSettingsJsonStr());

        SharedPreferences prefs = this.getSharedPreferences("settings", 0);
        try {
            JSONObject obj = new JSONObject();

            obj.put("topic", "setparms");
            obj.put("airplane_mode", prefs.getBoolean("airplane_mode", true));
            obj.put("freq", prefs.getString("freq", "915.000"));
            obj.put("bw", Integer.parseInt(prefs.getString("bandwidth", "3")));
            obj.put("sp_factor", Integer.parseInt(prefs.getString("spread_factor", "11")));
            obj.put("coding_rate", Integer.parseInt(prefs.getString("coding_rate", "2")));
            obj.put("tx_power", Integer.parseInt(prefs.getString("tx_power", "27")));
            obj.put("sync_word", Integer.parseInt(prefs.getString("sync_word", "255")));
            obj.put("deadband", Integer.parseInt(prefs.getString("tx_deadband", "1")));
            obj.put("total_nodes", Integer.parseInt(prefs.getString("total_nodes", "2")));
            obj.put("tdma_slot", Integer.parseInt(prefs.getString("tdma_slot", "0")));
            obj.put("tx_time", Integer.parseInt(prefs.getString("tx_time","4")));
            mWriteCharacteristic.setValue(obj.toString());
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        if (mBluetoothGatt.writeCharacteristic(mWriteCharacteristic) == false) {
            Log.w(TAG, "Failed to write DSC Parameters BLE");
        }

        //setCharacteristicNotification(mWriteCharacteristic, true);

    }

    private String hexStringToByteArray(String hex) {
        int l = hex.length();
        byte[] data = new byte[l / 2];
        for (int i = 0; i < l; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return new String(data, StandardCharsets.UTF_8);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    public class LocalBinder extends Binder {
        public DscService getService() {
            return DscService.this;
        }
    }


}
