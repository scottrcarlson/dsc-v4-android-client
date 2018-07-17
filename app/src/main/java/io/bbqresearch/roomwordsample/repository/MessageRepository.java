package io.bbqresearch.roomwordsample.repository;

import android.app.Application;
import android.app.Notification;
import android.app.PendingIntent;
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
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import org.json.JSONObject;

import java.util.List;

import io.bbqresearch.roomwordsample.MainActivity;
import io.bbqresearch.roomwordsample.R;
import io.bbqresearch.roomwordsample.dao.MessageDao;
import io.bbqresearch.roomwordsample.entity.Message;
import io.bbqresearch.roomwordsample.room.MessageRoomDatabase;
import io.bbqresearch.roomwordsample.service.DscService;

public class MessageRepository extends BroadcastReceiver {
    private final static String TAG = MessageRepository.class.getSimpleName();

    public static final String NOTIFY_CHANNEL_DSC = "DSC_NOTIFY_CHANNEL";
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
                dscService.setBluetoothDeviceName("DSC");
                dscService.setBluetoothDeviceAddress("B8:27:EB:F2:1E:01");
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
            } else if (DscService.ACTION_NEW_MESSAGE.equals(action)) {
                try {
                    JSONObject nodeRoot = new JSONObject(intent.getStringExtra(DscService.EXTRA_DATA));
                    JSONObject payload = nodeRoot.getJSONObject("payload");
                    Message message = new Message(payload.getString("msg"),
                            payload.getString("author"),
                            payload.getInt("sent_time"),
                            payload.getInt("recv_time"),
                            false);
                    new insertAsyncTask(mMessageDao).execute(message);

                    Intent intentNotify = new Intent(context, MainActivity.class);
                    intentNotify.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intentNotify, 0);

                    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, NOTIFY_CHANNEL_DSC)
                            .setSmallIcon(R.drawable.ic_smoking_rooms_black_24dp)
                            .setContentTitle("Dirt Simple Comms")
                            .setContentText("New message from " + payload.getString("author"))
                            .setStyle(new NotificationCompat.BigTextStyle()
                                    .bigText(payload.getString("msg")))
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setContentIntent(pendingIntent)
                            .setDefaults(Notification.DEFAULT_SOUND)
                            .setAutoCancel(true);
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

                    // notificationId is a unique int for each notification that you must define
                    notificationManager.notify(1, mBuilder.build());
                } catch (Exception e) {
                    Log.e(TAG, "Inbound NewMsg Error: " + Log.getStackTraceString(e));
                }
            }


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
        intentFilter.addAction(DscService.ACTION_NEW_MESSAGE);
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

    public void deleteAll() {
        mMessageDao.deleteAll();
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