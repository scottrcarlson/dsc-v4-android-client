package io.bbqresearch.dsc.service;

import android.app.Service;
import android.bluetooth.BluetoothGatt;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;

import com.jakewharton.rx.ReplayingShare;
import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.RxBleRadioOperationCustom;
import com.polidea.rxandroidble2.internal.RxBleLog;
import com.polidea.rxandroidble2.internal.connection.RxBleGattCallback;

import org.json.JSONObject;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.bbqresearch.dsc.entity.Message;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class DscServiceUpgrade extends Service {
    public final static String ACTION_NEW_MESSAGE =
            "io.bbqresearch.android.dirtsimplecomms.NEW_MESSAGE";
    public final static String ACTION_NEW_BEACON =
            "io.bbqresearch.android.dirtsimplecomms.ACTION_NEW_BEACON";
    public final static String EXTRA_DATA =
            "io.bbqresearch.android.dirtsimplecomms.EXTRA_DATA";
    public final static String ACTION_NEW_DEVICE_STATUS =
            "io.bbqresearch.android.dirtsimplecomms.ACTION_NEW_DEVICE_STATUS";

    private final static String TAG = DscServiceUpgrade.class.getSimpleName();
    private final IBinder mBinder = new DscServiceUpgrade.LocalBinder();
    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    private Disposable disposableConnection;
    private RxBleClient rxBleClient;
    private RxBleDevice rxBleDevice;
    private volatile int bleRssi = 0;

    private PublishSubject<Boolean> disconnectTriggerSubject = PublishSubject.create();
    private PublishSubject<Boolean> disableStatusNotification = PublishSubject.create();
    private Observable<RxBleConnection> connectionObservable;


    public RxBleClient getRxBleClient() {
        return this.rxBleClient;
    }

    public RxBleDevice getRxBleDevice() {
        return this.rxBleDevice;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences prefs = this.getSharedPreferences("settings", 0);
        /*SharedPreferences.OnSharedPreferenceChangeListener listener = (preferences, key) -> {
                Log.d(TAG, "Pref Changed: " + key);
                dscUpdateSettings();

        };*/


        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {

            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                Log.d(TAG, "Pref Changed: " + key);
                dscUpdateSettings();
            }
        };

        prefs.registerOnSharedPreferenceChangeListener(listener);

        rxBleClient = RxBleClient.create(this);
        Log.i(TAG, "rxBleClient State: " + rxBleClient.getState().toString());
        Log.i(TAG, "Instantiated RxBleClient");
        RxBleLog.setLogLevel(RxBleLog.VERBOSE);
    }

    public void dscUpdateSettings() {
        SharedPreferences prefs = this.getSharedPreferences("settings", 0);
        try {
            JSONObject obj = new JSONObject();

            obj.put("topic", "setparms");
            obj.put("airplane_mode", prefs.getBoolean("airplane_mode", true));
            obj.put("alias", prefs.getString("alias", ""));
            obj.put("freq", Integer.parseInt(prefs.getString("freq", "915000000")));
            obj.put("netkey", prefs.getString("netkey", ""));
            obj.put("groupkey", prefs.getString("groupkey", ""));
            obj.put("bw", Integer.parseInt(prefs.getString("bandwidth", "3")));
            obj.put("sp_factor", Integer.parseInt(prefs.getString("spread_factor", "11")));
            obj.put("coding_rate", Integer.parseInt(prefs.getString("coding_rate", "2")));
            obj.put("tx_power", Integer.parseInt(prefs.getString("tx_power", "27")));
            obj.put("sync_word", Integer.parseInt(prefs.getString("sync_word", "255")));
            obj.put("deadband", Integer.parseInt(prefs.getString("tx_deadband", "1")));
            obj.put("total_nodes", Integer.parseInt(prefs.getString("total_nodes", "2")));
            obj.put("tdma_slot", Integer.parseInt(prefs.getString("tdma_slot", "0")));
            obj.put("tx_time", Integer.parseInt(prefs.getString("tx_time", "4")));
            obj.put("registered", true);
            connectionObservable
                    .subscribeOn(Schedulers.newThread())
                    .firstOrError()
                    .flatMap(rxBleConnection -> rxBleConnection.writeCharacteristic(UUID.fromString(DscGattAttributes.DSC_SETTINGS_UUID), obj.toString().getBytes()))
                    //.observeOn(AndroidSchedulers.mainThread())
                    .observeOn(Schedulers.newThread())
                    .subscribe(
                            bytes -> onWriteSuccess(),
                            this::onWriteFailure
                    );
            Log.d(TAG, obj.toString());
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public void dscSyncTime() {
        String epoch = String.valueOf(System.currentTimeMillis() / 100L);

        connectionObservable
                .subscribeOn(Schedulers.newThread())
                .firstOrError()
                .flatMap(rxBleConnection -> rxBleConnection.writeCharacteristic(UUID.fromString(DscGattAttributes.DSC_DATETIME_UUID), epoch.getBytes()))
                .observeOn(Schedulers.newThread())
                .subscribe(
                        bytes -> onWriteSuccess(),
                        this::onWriteFailure
                );
    }

    public void sendMsg(Message message) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("topic", "sendmsg");
            obj.put("msg", message.getMsg());
            obj.put("author", message.getAuthor());
            obj.put("recv_time", message.getOrigTimestamp());
            obj.put("lat", -73);
            obj.put("long", 45);
            obj.put("msg_cipher", "");
            connectionObservable
                    .subscribeOn(Schedulers.newThread())
                    .firstOrError()
                    .flatMap(rxBleConnection -> rxBleConnection.writeCharacteristic(UUID.fromString(DscGattAttributes.DSC_MSG_OUTBOUND), obj.toString().getBytes()))
                    .observeOn(Schedulers.newThread())
                    .subscribe(
                            bytes -> onWriteSuccess(),
                            this::onWriteFailure
                    );

        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }

    }

    private Observable<RxBleConnection> prepareConnectionObservable() {
        return rxBleDevice
                .establishConnection(false)
                .takeUntil(disconnectTriggerSubject)
                //.compose(bindUntilEvent(PAUSE))
                .compose(ReplayingShare.instance());
    }

    public void connect(final String macAddress) {
        rxBleDevice = rxBleClient.getBleDevice(macAddress);

        if (!isConnected()) {
            rxBleDevice.getBluetoothDevice().createBond();
            connectionObservable = prepareConnectionObservable();
            connectionObservable
                    .flatMap(rxBleConnection -> rxBleConnection.queue(new CustomGattRefreshOperation()))
                    .subscribe(result -> {
                                Log.d(TAG, "done refreshing gatt cache");
                            },
                            throwable -> {
                                Log.d(TAG, "error refreshing gatt cache ");
                            });

            /*connectionObservable
                    .flatMap(rxBleConnection -> // Set desired interval.
                            Observable.interval(2, TimeUnit.SECONDS).flatMapSingle(sequence -> rxBleConnection.readRssi()))
                    .observeOn(Schedulers.newThread())
                    .subscribe(this::updateRssi, this::onConnectionFailure);*/

            enableNotification(UUID.fromString(DscGattAttributes.DSC_MSG_INBOUND_UUID), 0);
            enableNotification(UUID.fromString(DscGattAttributes.DSC_SETTINGS_UUID), 500);
        }
    }

    public void enableNotification(UUID characteristic_uuid, int delay) {
        connectionObservable
                .flatMap(rxBleConnection -> rxBleConnection.setupNotification(characteristic_uuid))
                .subscribeOn(Schedulers.newThread())
                //.doOnNext(notificationObservable -> Log.d(TAG, "Notification Setup"))
                .flatMap(notificationObservable -> notificationObservable)
                .subscribe(this::onNotificationReceived, this::onNotificationSetupFailure);

        //.delay(delay, TimeUnit.MILLISECONDS, Schedulers.computation())
    }

    public void updateRssi(int rssi) {
        bleRssi = rssi;
    }

    public void enableStatusNotification(boolean enabled) {
        if (enabled) {
            connectionObservable
                    .subscribeOn(Schedulers.newThread())
                    .flatMap(rxBleConnection -> rxBleConnection.setupNotification(UUID.fromString(DscGattAttributes.DSC_STATUS_UUID)))
                    .doOnNext(notificationObservable -> Log.d(TAG, "Notification Setup"))
                    .flatMap(notificationObservable -> notificationObservable)
                    .takeUntil(disableStatusNotification)
                    .subscribe(this::onNotificationReceived, this::onNotificationSetupFailure);


        } else {
            disableStatusNotification.onNext(true);
        }
    }

    private void processInboundFromDsc(String jsonmsg) {
        //Log.d(TAG, "Got Json String: " + jsonmsg);

        if (getTopic(jsonmsg).contentEquals("getparms")) {
            //compareSettings(jsonmsg);
            Log.d(TAG, "device settings received");
        } else if (getTopic(jsonmsg).contentEquals("newmsg")) {
            final Intent intent = new Intent(ACTION_NEW_MESSAGE);
            intent.putExtra(EXTRA_DATA, jsonmsg);
            sendBroadcast(intent);
            Log.d(TAG, "newmsg: " + jsonmsg);
        } else if (getTopic(jsonmsg).contentEquals("newbeacon")) {
            final Intent intent = new Intent(ACTION_NEW_BEACON);
            intent.putExtra(EXTRA_DATA, jsonmsg);
            sendBroadcast(intent);
            Log.d(TAG, "newbeacon: " + jsonmsg);
        } else if (getTopic(jsonmsg).contentEquals("status")) {
            final Intent intent = new Intent(ACTION_NEW_DEVICE_STATUS);
            intent.putExtra(EXTRA_DATA, jsonmsg);
            sendBroadcast(intent);
            Log.d(TAG, "status: " + jsonmsg);
        } else {
            Log.d(TAG, "Unknown Incoming: " + jsonmsg.toString());
        }
    }
    public boolean isConnected() {
        if (rxBleDevice != null) {
            return rxBleDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED;
        } else {
            return false;
        }
    }

    private void onNotificationReceived(byte[] bytes) {
        if (bytes != null && bytes.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(bytes.length);

            for (byte byteChar : bytes)
                stringBuilder.append(String.format("%02X", byteChar));
            processInboundFromDsc(hexStringToByteArray(stringBuilder.toString()));
        }
        Log.d(TAG, Arrays.toString(bytes));
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

    // Read BLE signal strength
    public int readBleRssi() {
        return bleRssi;
    }

    private void onNotificationSetupFailure(Throwable throwable) {
        //noinspection ConstantConditions
        //Snackbar.make(findViewById(R.id.main), "Notifications error: " + throwable, Snackbar.LENGTH_SHORT).show();
        Log.e(TAG, "Notification Setup Error: " + throwable);


    }

    private static class CustomGattRefreshOperation implements RxBleRadioOperationCustom {
        CustomGattRefreshOperation() {
        }

        @Override
        @NonNull
        public Observable<Void> asObservable(BluetoothGatt bluetoothGatt,
                                             RxBleGattCallback rxBleGattCallback,
                                             Scheduler scheduler) {

            return Observable.fromCallable(() -> refreshDeviceCache(bluetoothGatt))
                    .delay(500, TimeUnit.MILLISECONDS, Schedulers.computation())
                    .subscribeOn(scheduler);
        }

        private Void refreshDeviceCache(BluetoothGatt gatt) {
            // from http://stackoverflow.com/questions/22596951/how-to-programmatically-force-bluetooth-low-energy-service-discovery-on-android
            try {
                BluetoothGatt localBluetoothGatt = gatt;
                Method localMethod = localBluetoothGatt.getClass().getMethod("refresh");
                if (localMethod != null) {
                    boolean bool = ((Boolean) localMethod.invoke(localBluetoothGatt, new Object[0])).booleanValue();
                    //Timber.i("Gatt cache refresh successful: [%b]", bool);
                }
            } catch (Exception localException) {
                //Timber.e("An exception occured while refreshing device");
            }
            return null;
        }
    }


    private void onConnectionFailure(Throwable throwable) {
        //noinspection ConstantConditions
        //Snackbar.make(findViewById(R.id.main), "Connection error: " + throwable, Snackbar.LENGTH_SHORT).show();
        Log.e(TAG, "Connection Error: " + throwable);
    }


    private void onWriteSuccess() {
        //noinspection ConstantConditions
        //Snackbar.make(findViewById(R.id.main), "Write success", Snackbar.LENGTH_SHORT).show();
        Log.d(TAG, "GATT Write Success");
    }

    private void onWriteFailure(Throwable throwable) {
        //noinspection ConstantConditions
        //Snackbar.make(findViewById(R.id.main), "Write error: " + throwable, Snackbar.LENGTH_SHORT).show();
        Log.e(TAG, "GATT Write Failed: " + throwable);
    }

    public void disconnect() {
        disconnectTriggerSubject.onNext(true);

    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // Handle unbinding here, should probably dispose of things here.

        return super.onUnbind(intent);
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

    public class LocalBinder extends Binder {
        public DscServiceUpgrade getService() {
            return DscServiceUpgrade.this;
        }
    }
}
