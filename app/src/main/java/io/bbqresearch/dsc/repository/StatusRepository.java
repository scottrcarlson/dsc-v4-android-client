package io.bbqresearch.dsc.repository;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONObject;

import java.util.List;

import io.bbqresearch.dsc.entity.Message;
import io.bbqresearch.dsc.service.DscServiceUpgrade;

public class StatusRepository extends BroadcastReceiver {
    private final static String TAG = StatusRepository.class.getSimpleName();
    private MutableLiveData<String> mDeviceStatus;
    private SharedPreferences prefs;
    private DscServiceUpgrade dscService;
    private final BroadcastReceiver dscUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (DscServiceUpgrade.ACTION_NEW_DEVICE_STATUS.equals(action)) {
                try {
                    JSONObject nodeRoot = new JSONObject(intent.getStringExtra(DscServiceUpgrade.EXTRA_DATA));
                    JSONObject payload = nodeRoot.getJSONObject("payload");

                    mDeviceStatus.setValue(
                            payload.getString("rf_freq") + "," +
                                    payload.getString("rf_state") + "," +
                                    payload.getString("time") + "," +
                                    prefs.getString("alias", "") + "," +
                                    prefs.getString("btname", "") + "," +
                                    prefs.getString("btaddr", "") + "," +
                                    dscService.readBleRssi()
                    );


                } catch (Exception e) {
                    Log.e(TAG, "Inbound DSC Status Error: " + Log.getStackTraceString(e));
                }
            }
        }
    };
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            dscService = ((DscServiceUpgrade.LocalBinder) service).getService();
            enableDeviceStatus(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            dscService = null;
        }
    };

    private LiveData<List<Message>> mAllMessages;

    public StatusRepository(Application application) {
        Intent gattServiceIntent = new Intent(application, DscServiceUpgrade.class);
        application.getApplicationContext().bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        application.registerReceiver(dscUpdateReceiver, makeGattUpdateIntentFilter());

        prefs = application.getSharedPreferences("settings", 0);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DscServiceUpgrade.ACTION_NEW_DEVICE_STATUS);
        return intentFilter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, "onReceive Called: " + intent.getAction());
        /***** For start Service  ****/
        Intent myIntent = new Intent(context, DscServiceUpgrade.class);
        context.startService(myIntent);
    }

    public MutableLiveData<String> getDeviceStatus() {
        if (mDeviceStatus == null) {
            mDeviceStatus = new MutableLiveData<>();
        }
        return mDeviceStatus;
    }

    public void enableDeviceStatus(boolean enable) {
        if (dscService != null) dscService.enableStatusNotification(enable);
    }

    public void enablePeerStatus() {

    }

    public void enableDeviceLogs() {

    }

}