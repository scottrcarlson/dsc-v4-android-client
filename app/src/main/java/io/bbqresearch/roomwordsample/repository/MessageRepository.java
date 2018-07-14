package io.bbqresearch.roomwordsample.repository;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.List;

import io.bbqresearch.roomwordsample.dao.MessageDao;
import io.bbqresearch.roomwordsample.entity.Message;
import io.bbqresearch.roomwordsample.room.MessageRoomDatabase;
import io.bbqresearch.roomwordsample.service.DscService;

public class MessageRepository extends BroadcastReceiver {
    private final static String TAG = MessageRepository.class.getSimpleName();
    private DscService dscService;
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            dscService = ((DscService.LocalBinder) service).getService();
            if (!dscService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
            }
            // Automatically connects to the device upon successful start-up initialization.
            if (!dscService.isConnected()) {
                dscService.setmBluetoothDeviceName("DSC");
                dscService.setmBluetoothDeviceAddress("B8:27:EB:F2:1E:01");
                dscService.connect();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            dscService = null;
        }
    };
    private final BroadcastReceiver dscUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (DscService.ACTION_READY.equals(action)) {
                // Connected and Proper DSC GATT Services Available
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        dscService.readParams_ble();
                    }
                }, 3000);

            }
            /*else if (DscService.ACTION_DATA_AVAILABLE.equals(action)) {
                if (intent.getStringExtra(DscService.EXTRA_DATA) != null) {

                }
            }*/
        }
    };
    private MessageDao mMessageDao;
    private LiveData<List<Message>> mAllMessages;

    public MessageRepository(Application application) {
        MessageRoomDatabase db = MessageRoomDatabase.getDatabase(application);
        mMessageDao = db.messageDao();
        mAllMessages = mMessageDao.getAllMessages();

        Intent gattServiceIntent = new Intent(application, DscService.class);
        application.getApplicationContext().bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        application.registerReceiver(dscUpdateReceiver, makeGattUpdateIntentFilter());
        if (dscService != null) {
            if (!dscService.isConnected()) {
                final boolean result = dscService.connect();
                Log.d(TAG, "Connect request result=" + result);
            }
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DscService.ACTION_READY);
        //intentFilter.addAction(DscService.ACTION_DATA_AVAILABLE);

        return intentFilter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, "onReceive Called: " + intent.getAction());
        /***** For start Service  ****/
        Intent myIntent = new Intent(context, DscService.class);
        context.startService(myIntent);
    }

    public LiveData<List<Message>> getAllMessages() {
        return mAllMessages;
    }

    public void insert(Message message) {
        new insertAsyncTask(mMessageDao).execute(message);
    }

    private static class insertAsyncTask extends AsyncTask<Message, Void, Void> {

        private MessageDao mAsyncTaskDao;

        insertAsyncTask(MessageDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Message... params) {
            mAsyncTaskDao.insert(params[0]);
            return null;
        }
    }
}
