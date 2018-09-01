package io.bbqresearch.dsc;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.FrameLayout;

import io.bbqresearch.dsc.viewmodel.StatusViewModel;
import io.bbqresearch.roomwordsample.R;

public class StatusActivity extends AppCompatActivity {
    private final static String TAG = StatusActivity.class.getSimpleName();
    private FrameLayout mFrameLayout;
    private StatusDeviceFragment deviceFragment = new StatusDeviceFragment();
    private StatusPeersFragment peersFragment = new StatusPeersFragment();
    private StatusLogsFragment logsFragment = new StatusLogsFragment();

    private StatusViewModel mStatusViewModel;
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_status_device:
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.status_content, deviceFragment).commit();
                    // Turn on Status Notifications
                    mStatusViewModel.enableDeviceStatus(true);

                    return true;
                case R.id.navigation_status_peers:
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.status_content, peersFragment).commit();
                    mStatusViewModel.enableDeviceStatus(false);
                    return true;
                case R.id.navigation_status_logs:
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.status_content, logsFragment).commit();
                    mStatusViewModel.enableDeviceStatus(false);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
        setContentView(R.layout.activity_status);

        mFrameLayout = findViewById(R.id.status_content);
        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);


        getSupportFragmentManager().beginTransaction()
                .add(R.id.status_content, deviceFragment).commit();

        //Creates on first call, if a configuration change occurs, this returns existing viewmodel
        mStatusViewModel = ViewModelProviders.of(this).get(StatusViewModel.class);

        //Observe LiveData returned by getAllWords in ViewModel. If this activity is in the foreground
        //it will call onChanged
        mStatusViewModel.getDeviceStatus().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable final String status) {
                String[] values = status.split(",");
                deviceFragment.setDeviceName(values[3]);
                deviceFragment.setBleName(values[4]);
                deviceFragment.setBleMacAddr(values[5]);
                deviceFragment.setBleRssi(values[6]);
                deviceFragment.setDeviceDatetime(values[2]);
                deviceFragment.setDeviceRadioMode(values[1]);
            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();

        mStatusViewModel.enableDeviceStatus(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mStatusViewModel.enableDeviceStatus(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mStatusViewModel.enableDeviceStatus(false);
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: // This is the home/back button
                onBackPressed(); // Handle what to do on home/back press
                break;
        }

        return false;
    }

}
