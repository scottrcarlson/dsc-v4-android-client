package io.bbqresearch.roomwordsample;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import java.util.List;

import io.bbqresearch.roomwordsample.entity.Message;
import io.bbqresearch.roomwordsample.viewmodel.MessageViewModel;

public class MainActivity extends AppCompatActivity {
    //public static final int NEW_MESSAGE_ACTIVITY_REQUEST_CODE = 1;
    private final static String TAG = MainActivity.class.getSimpleName();
    private MessageViewModel mMessageViewModel;
    private boolean isNewSentMessage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
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