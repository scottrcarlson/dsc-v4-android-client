package io.bbqresearch.roomwordsample.service;

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
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.bbqresearch.roomwordsample.R;

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
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    public final static UUID UUID_DSC_NOTIFICATION =
            UUID.fromString(DscGattAttributes.DSC_NOTITFYCHAR_UUID);
    private final static String TAG = DscService.class.getSimpleName();
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    private final IBinder mBinder = new LocalBinder();
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private String mBluetoothDeviceAddress;
    private String mBluetoothDeviceName;
    private boolean isServicesDiscovered = false;
    private boolean isConnected = false;
    private int mConnectionState = STATE_DISCONNECTED;
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnectionState = STATE_CONNECTED;

                Log.i(TAG, "Connected to DSC GATT server.");

                //intentAction = ACTION_GATT_CONNECTED;
                broadcastUpdate(ACTION_CONNECTED);

                mBluetoothGatt.discoverServices();
                isConnected = true;

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from DSC GATT server.");
                isConnected = false;

                //intentAction = ACTION_GATT_DISCONNECTED;
                broadcastUpdate(ACTION_DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

                if (checkForReqServices(getSupportedGattServices())) {
                    isServicesDiscovered = true;
                    Log.d(TAG, "DSC Services Discovered.");
                    broadcastUpdate(ACTION_READY);
                }
            } else {
                isServicesDiscovered = false;
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {

            if (checkGattStatusAndLog(status, "Read")) {
                byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for (byte byteChar : data)
                        stringBuilder.append(String.format("%02X", byteChar));
                    processInboundFromDsc(hexStringToByteArray(stringBuilder.toString()));
                }
            }
        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data)
                    stringBuilder.append(String.format("%02X", byteChar));
                processInboundFromDsc(hexStringToByteArray(stringBuilder.toString()));
            }
            //broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
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
            if (status == 0) return true;
            else return false;
        }
    };
    private SharedPreferences.OnSharedPreferenceChangeListener listener;

    public void setmBluetoothDeviceAddress(String mBluetoothDeviceAddress) {
        this.mBluetoothDeviceAddress = mBluetoothDeviceAddress;
    }

    public void setmBluetoothDeviceName(String mBluetoothDeviceName) {
        this.mBluetoothDeviceName = mBluetoothDeviceName;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences prefs = this.getSharedPreferences("settings", 0);
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {

            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                Log.d(TAG, "Pref Changed: " + key);
                Log.d(TAG, "Pref Changed: " + prefs.getBoolean(key, true));

                writeParams_ble("");
                switch (key) {
                    case "airplane_mode":
                        break;
                }

            }
        };
        prefs.registerOnSharedPreferenceChangeListener(listener);
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private boolean checkForReqServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return false;
        String uuid = null;
        List<String> uuids = new ArrayList<String>();
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);

        /*ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();*/


        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {

            uuid = gattService.getUuid().toString();
            uuids.add(uuid);
            Log.d(TAG, DscGattAttributes.lookup(uuid, unknownServiceString) + ":" + uuid);
            for (BluetoothGattCharacteristic gattCharacteristic : gattService.getCharacteristics()) {
                uuid = gattCharacteristic.getUuid().toString();
                uuids.add(uuid);
                Log.d(TAG, DscGattAttributes.lookup(uuid, unknownCharaString) + ":" + uuid);
            }
        }
        if (DscGattAttributes.checkAllReqAttributesAvail(uuids)) {
            return true;
        } else {
            return false;
        }
            /*HashMap<String, String> currentServiceData = new HashMap<String, String>();

            uuid = gattService.getUuid().toString();

            Log.d(TAG, DscGattAttributes.lookup(uuid, unknownServiceString) + ":" + uuid);

            currentServiceData.put(
                    LIST_NAME, DscGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();

                Log.d(TAG, DscGattAttributes.lookup(uuid, "Unknown") + ":" + uuid);
               /* if (DscGattAttributes.DSC_NOTITFYCHAR_UUID.contentEquals(uuid))
                {
                    dscService.setCharacteristicNotification(
                            gattCharacteristic, false);

                }*/
            /*
                currentCharaData.put(
                        LIST_NAME, DscGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);

            return false;*/
        //}

        /*SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        mGattServicesList.setAdapter(gattServiceAdapter);*/
    }

    private String getTopic(String jsonmsg) {
        try {
            JSONObject nodeRoot = new JSONObject(jsonmsg);

            return nodeRoot.getString("topic");
        } catch (Exception e) {
            Log.e(TAG, "getTopic " + jsonmsg + " -- " + e.toString());
        }
        return "";
    }

    private void processInboundFromDsc(String jsonmsg) {
        Log.d(TAG, "Got Json String: " + jsonmsg);

        if (getTopic(jsonmsg).contentEquals("getparms")) {
            compareSettings(jsonmsg);
            Log.d(TAG, "process inbound msg");
        }
    }

    public void logAllSettings() {
        SharedPreferences prefs = this.getSharedPreferences("settings", 0);

        /*Log.d(TAG, "Freq" + ": " + settings.getFrequency());
        Log.d(TAG, "CR" + ": " + settings.getCodingRate());
        Log.d(TAG, "BW" + ": " + settings.getBandwidth());
        Log.d(TAG, "SPF" + ": " + settings.getSpread_factor());
        Log.d(TAG, "SW" + ": " + settings.getSyncWord());
        Log.d(TAG, "TDMASlot" + ": " + settings.getTdmaSlot());
        Log.d(TAG, "TxPow" + ": " + settings.getTxPower());
        Log.d(TAG, "TxTime" + ": " + settings.getTxTime());
        Log.d(TAG, "DeadB" + ": " + settings.getDeadBand());*/
        Log.d(TAG, "AirplaneMode" + ": " + prefs.getBoolean("airplane_mode", true));
        //Log.d(TAG, "TotalNodes" + ": " + settings.getTotalNodes());

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
            }
            if (payload.getString("freq").contentEquals(prefs.getString("freq", "915.000"))) {
                equal_cnt += 1;
            }
            if (payload.getInt("bw") == prefs.getInt("bw", 0)) {
                equal_cnt += 1;
            }
            if (payload.getInt("coding_rate") == prefs.getInt("coding_rate", 0)) {
                equal_cnt += 1;
            }
            if (payload.getInt("deadband") == prefs.getInt("deadband", 0)) {
                equal_cnt += 1;
            }
            if (payload.getInt("sp_factor") == prefs.getInt("sp_factor", 0)) {
                equal_cnt += 1;
            }
            if (payload.getInt("sync_word") == prefs.getInt("sync_word", 0)) {
                equal_cnt += 1;
            }
            if (payload.getInt("tdma_slot") == prefs.getInt("tdma_slot", 0)) {
                equal_cnt += 1;
            }
            if (payload.getInt("tx_power") == prefs.getInt("tx_power", 0)) {
                equal_cnt += 1;
            }
            if (payload.getInt("tx_time") == prefs.getInt("tx_time", 0)) {
                equal_cnt += 1;
            }
            if (payload.getInt("total_nodes") == prefs.getInt("total_nodes", 0)) {
                equal_cnt += 1;
            }

            if (equal_cnt == total_cnt) {
                Log.d(TAG, "DSC Settings Equal");
            } else {
                Log.e(TAG, "DSC Settings not Equal");
            }
        } catch (Exception e) {
            Log.e(TAG, "updateSettings error: " + jsonmsg + " -- " + e.toString());
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    public boolean isReady() {
        if (mConnectionState == STATE_CONNECTED && isServicesDiscovered)
            return true;
        else {
            return false;
        }
    }


    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }
    // This is special handling for the Heart Rate Measurement profile.  Data parsing is
    // carried out as per profile specifications:
    // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        /*if (UUID_DSC_NOTIFICATION.equals(characteristic.getUuid())) {
            final byte[] data = characteristic.getValue();
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for(byte byteChar : data)
                stringBuilder.append(String.format("%02X ", byteChar));
            Log.d("DSC GATT ?", stringBuilder.toString());
            int flag = characteristic.getProperties();
            int format = -1;

            String msg = "DSC Notification!";

            intent.putExtra(EXTRA_DATA, msg);
        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X", byteChar));
                Log.d("DEBUG", "********************");
                Log.d("DSC VALUE",stringBuilder.toString());
                //String msg = stringBuilder.toString();

                intent.putExtra(EXTRA_DATA, stringBuilder.toString());
            }
        }
        sendBroadcast(intent);
    }*/

    /*private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        Log.d("DSC GATT BROADCAST FROM", characteristic.getUuid().toString());
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for (byte byteChar : data)
                stringBuilder.append(String.format("%02X", byteChar));
            Log.d("DEBUG", "********************");
            Log.d("DSC VALUE", stringBuilder.toString());
            //String msg = stringBuilder.toString();

            intent.putExtra(EXTRA_DATA, stringBuilder.toString());

            sendBroadcast(intent);
        }
    }*/

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

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect() {

        //String address = settings.getDeviceAddr();
        if (mBluetoothAdapter == null || mBluetoothDeviceAddress == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null // && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                Log.d("DSC GATT CONNECT", "Made it to connection.");
                return true;
            } else {
                return false;
            }
        }

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

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        Log.d("DSC GATT setChara", characteristic.getUuid().toString());
        if (UUID_DSC_NOTIFICATION.equals(characteristic.getUuid())) {
            Log.d("DSC GATT setChara", "Found DSC NOTIFICATION.");
            /*BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(DscGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

            mBluetoothGatt.writeDescriptor(descriptor);*/
            mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        } else if (DscGattAttributes.DSC_SETTINGS_UUID.equals(characteristic.getUuid())) {

        }
        try {
            Log.d("DSC MSG", characteristic.getValue().toString());
        } catch (Exception e) {
            Log.d("DSC MSG ERR", "Error reading value from chara." + e.getStackTrace());
        }
    }

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
            obj.put("freq", "905.030");
            obj.put("bw", "125");
            obj.put("sp_factor", 11);
            obj.put("coding_rate", 0);
            obj.put("tx_power", 11);
            obj.put("sync_word", 200);
            obj.put("deadband", 2);
            obj.put("total_nodes", 2);
            obj.put("tdma_slot", 0);
            obj.put("tx_time", 1);
            mWriteCharacteristic.setValue(obj.toString());
        } catch (Exception e) {

        }
        if (mBluetoothGatt.writeCharacteristic(mWriteCharacteristic) == false) {
            Log.w(TAG, "Failed to write DSC Parameters BLE");
        }

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
