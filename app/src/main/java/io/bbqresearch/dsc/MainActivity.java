package io.bbqresearch.dsc;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;

import java.util.List;
import java.util.Random;

import io.bbqresearch.dsc.entity.Message;
import io.bbqresearch.dsc.service.DscServiceUpgrade;
import io.bbqresearch.dsc.viewmodel.MessageViewModel;
import io.bbqresearch.roomwordsample.R;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDR = "DEVICE_ADDR";
    public static final String NOTIFY_CHANNEL_DSC = "DSC_NOTIFY_CHANNEL";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int RESULT_BT_SCAN = 10;
    private DscServiceUpgrade dscService;
    private MessageViewModel mMessageViewModel;
    private boolean isNewSentMessage = false;
    private SharedPreferences prefs;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            dscService = ((DscServiceUpgrade.LocalBinder) service).getService();
            if (dscService.getRxBleClient().getState() != RxBleClient.State.READY) {
                Log.e(TAG, "Bluetooth Not Ready: " + dscService.getRxBleClient().getState());
            }
            // Automatically connects to the device upon successful start-up initialization.
            String btaddr = prefs.getString("btaddr", "");
            if (!dscService.isConnected()) {
                if (!btaddr.contentEquals("")) {
                    dscService.connect(btaddr);
                }
            }

            if (!btaddr.contentEquals("")) {
                dscService.getRxBleClient().getBleDevice(btaddr).observeConnectionStateChanges()
                        .subscribeOn(Schedulers.newThread())
                        //.compose(bindUntilEvent(DESTROY))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onConnectionStateChange);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            dscService = null;
        }

        private void onConnectionStateChange(RxBleConnection.RxBleConnectionState newState) {
            Log.d(TAG, "Connection: " + newState);
            if (newState == RxBleConnection.RxBleConnectionState.CONNECTED) {
                toolbar.setBackgroundResource(R.color.colorPrimary);
            } else if (newState == RxBleConnection.RxBleConnectionState.DISCONNECTED) {
                toolbar.setBackgroundResource(R.color.colorPrimaryDisconnected);
            }
        }
    };


    private CoordinatorLayout coordinatorLayout;
    private Toolbar toolbar;
    /*private final BroadcastReceiver dscUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (DscService.ACTION_CONNECTED.equals(action)) {

            } else if (DscService.ACTION_DISCONNECTED.equals(action)) {
                toolbar.setBackgroundResource(R.color.colorPrimaryDisconnected);
            } else if (DscService.ACTION_READY.equals(action)) {
                toolbar.setBackgroundResource(R.color.colorPrimary);
            }
        }
    };*/

   /* private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DscServiceUpgrade.ACTION_READY);
        intentFilter.addAction(DscServiceUpgrade.ACTION_CONNECTED);
        intentFilter.addAction(DscServiceUpgrade.ACTION_DISCONNECTED);
        return intentFilter;
    }*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSION_REQUEST_COARSE_LOCATION);
            }
        } else {
            // Permission has already been granted
        }

        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setBackgroundResource(R.color.colorPrimaryDisconnected);
        setSupportActionBar(toolbar);

        final RecyclerView recyclerView = findViewById(R.id.recyclerview);
        final MessageListAdapter adapter = new MessageListAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        //Creates on first call, if a configuration change occurs, this returns existing viewmodel
        mMessageViewModel = ViewModelProviders.of(this).get(MessageViewModel.class);

        //Observe LiveData returned by getAllWords in ViewModel. If this activity is in the foreground
        //it will call onChanged
        mMessageViewModel.getAllMessages().observe(this, new Observer<List<Message>>() {
            @Override
            public void onChanged(@Nullable final List<Message> messages) {
                // Update the cached copy of the words in the adapter.
                Log.d(TAG, "1***********Message List Size: " + messages.size());
                adapter.setMessages(messages);

                if (isNewSentMessage) {
                    isNewSentMessage = false;
                    recyclerView.smoothScrollToPosition(messages.size() - 1);
                    adapter.notifyDataSetChanged();
                }
            }
        });

        coordinatorLayout = findViewById(R.id
                .coordinatorLayout);

        Intent gattServiceIntent = new Intent(this, DscServiceUpgrade.class);
        this.getApplicationContext().bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        //this.registerReceiver(dscUpdateReceiver, makeGattUpdateIntentFilter());

        createNotificationChannel();

        prefs = this.getSharedPreferences("settings", 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //registerReceiver(dscUpdateReceiver, makeGattUpdateIntentFilter());
        if (dscService != null) {
            if (!dscService.isConnected()) {
                dscService.connect(prefs.getString("btaddr", ""));
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //unregisterReceiver(dscUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unbindService(mServiceConnection);
        //dscService = null;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    // permission denied
                    // disable functionality that depends on this permission.
                }
                return;
            }
        }
    }


    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(NOTIFY_CHANNEL_DSC, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            final Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_status) {
            final Intent intent = new Intent(this, StatusActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_delete_messages) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    mMessageViewModel.deleteAll();
                }
            });
            return true;
        } else if (id == R.id.action_connect) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    //TODO check to see if configured, connect option shoudl be hidden from menu

                    dscService.connect(prefs.getString("btaddr", ""));
                    Snackbar snackbar = Snackbar
                            .make(coordinatorLayout, "Connecting to DSC peripheral", Snackbar.LENGTH_LONG);

                    snackbar.show();
                }
            });
            return true;
        } else if (id == R.id.action_disconnect) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    dscService.disconnect();
                    Snackbar snackbar = Snackbar
                            .make(coordinatorLayout, "Disconnecting from DSC peripheral", Snackbar.LENGTH_LONG);

                    snackbar.show();
                }
            });
            return true;
        } else if (id == R.id.action_scan) {
            final Intent intent = new Intent(this, ScanningActivityUpgrade.class);
            startActivityForResult(intent, RESULT_BT_SCAN);
            return true;
        } else if (id == R.id.action_usb_ble_pair) {
            /*final Intent intent = new Intent(this, UsbBlePairingActivity.class);
            startActivity(intent);*/
            List<Message> msgs = mMessageViewModel.getAllMessages().getValue();
            for (int i = 0; i < msgs.size(); i++) {
                Log.d(TAG, msgs.get(i).getMsg());
                Log.d(TAG, msgs.get(i).getAuthor());
                Log.d(TAG, "" + msgs.get(i).getOrigTimestamp());
                Log.d(TAG, "" + msgs.get(i).getRecvTimestamp());
            }
            Log.d(TAG, "********** " + mMessageViewModel.getAllMessages().getValue().size());

            return true;
        } else if (id == R.id.action_sync_datetime) {
            dscService.dscSyncTime();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_BT_SCAN) {
            if (resultCode == RESULT_OK) {
                String deviceName = data.getStringExtra(EXTRAS_DEVICE_NAME);
                String deviceAddr = data.getStringExtra(EXTRAS_DEVICE_ADDR);

                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("btaddr", deviceAddr);
                editor.putString("btname", deviceName);
                editor.commit();

                Log.d(TAG, "Name: " + deviceName);
                Log.d(TAG, "Addr: " + deviceAddr);
                if (dscService.getRxBleClient().getState() != RxBleClient.State.READY) {
                    Log.e(TAG, "Bluetooth Not Ready: " + dscService.getRxBleClient().getState());
                }
                // Automatically connects to the device upon successful start-up initialization.
                if (!dscService.isConnected()) {
                    dscService.connect(deviceAddr);

                    Snackbar snackbar = Snackbar
                            .make(coordinatorLayout, "Connecting to DSC peripheral", Snackbar.LENGTH_LONG);

                    snackbar.show();
                }
            }
        }
    }

    public void onClickSend(View v) {
        isNewSentMessage = true;
        TextView sendMessage = findViewById(R.id.editText);

        if (!sendMessage.getText().toString().contentEquals("")) {
            long time = System.currentTimeMillis() / 1000L;

            Random rand = new Random();

            String tempCipher = String.valueOf(rand.nextInt(5000) + 1);

            Message message = new Message(tempCipher, sendMessage.getText().toString(),
                    prefs.getString("alias", "unnamed_1"), time, time, true);
            mMessageViewModel.insert(message);
            dscService.sendMsg(message);
            sendMessage.setText("");
            sendMessage.clearFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

        } else {
            Message message = new Message("", "Hey blah blah blah, what happens when the messages is really long and drawn out.???",
                    "Joe", 0, 100, false);
            mMessageViewModel.insert(message);
        }
    }
}