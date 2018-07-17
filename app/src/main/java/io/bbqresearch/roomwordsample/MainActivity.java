package io.bbqresearch.roomwordsample;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
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

import java.util.List;

import io.bbqresearch.roomwordsample.entity.Message;
import io.bbqresearch.roomwordsample.service.DscService;
import io.bbqresearch.roomwordsample.viewmodel.MessageViewModel;

public class MainActivity extends AppCompatActivity {
    //public static final int NEW_MESSAGE_ACTIVITY_REQUEST_CODE = 1;

    private final static String TAG = MainActivity.class.getSimpleName();
    private MessageViewModel mMessageViewModel;
    private boolean isNewSentMessage = false;
    private DscService dscService;
    public static final String NOTIFY_CHANNEL_DSC = "DSC_NOTIFY_CHANNEL";
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            dscService = ((DscService.LocalBinder) service).getService();
            if (!dscService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
            }
            // Automatically connects to the device upon successful start-up initialization.
            if (!dscService.ismConnected()) {
                dscService.setBluetoothDeviceName("DSC");
                dscService.connect("B8:27:EB:F2:1E:01");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            dscService = null;
        }
    };
    private Toolbar toolbar;
    private final BroadcastReceiver dscUpdateReceiver = new BroadcastReceiver() {
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
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DscService.ACTION_READY);
        intentFilter.addAction(DscService.ACTION_CONNECTED);
        intentFilter.addAction(DscService.ACTION_DISCONNECTED);
        return intentFilter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                adapter.setMessages(messages);

                if (isNewSentMessage) {
                    isNewSentMessage = false;
                    recyclerView.smoothScrollToPosition(messages.size() - 1);
                    adapter.notifyDataSetChanged();
                }
            }
        });

        Intent gattServiceIntent = new Intent(this, DscService.class);
        this.getApplicationContext().bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        this.registerReceiver(dscUpdateReceiver, makeGattUpdateIntentFilter());

        createNotificationChannel();
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            final Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_delete_messages) {
            //mMessageViewModel.deleteAll();
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    mMessageViewModel.deleteAll();
                }
            });
            return true;
        } else if (id == R.id.action_connect) {
            //mMessageViewModel.deleteAll();
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    dscService.connect("B8:27:EB:F2:1E:01");
                }
            });
            return true;
        } else if (id == R.id.action_disconnect) {
            //mMessageViewModel.deleteAll();
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    dscService.disconnect();
                }
            });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //utilize this for a device scanning fragment
  /*  public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == NEW_MESSAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            Message message = new Message(data.getStringExtra(NewWordActivity.EXTRA_REPLY),
                    "Bob",
                    0,
                    100);
            mMessageViewModel.insert(message);
        } else {
            Toast.makeText(
                    getApplicationContext(),
                    R.string.empty_not_saved,
                    Toast.LENGTH_LONG).show();
        }
    }*/

    public void onClickSend(View v) {
        isNewSentMessage = true;
        TextView sendMessage = findViewById(R.id.editText);

        if (!sendMessage.getText().toString().contentEquals("")) {
            Message message = new Message(sendMessage.getText().toString(),
                    "Bob", 0, 100, true);
            mMessageViewModel.insert(message);
            sendMessage.setText("");
            sendMessage.clearFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        } else {
            Message message = new Message("Hey blah blah blah, what happens when the messages is really long and drawn out.???",
                    "Joe", 0, 100, false);
            mMessageViewModel.insert(message);
        }
    }
}