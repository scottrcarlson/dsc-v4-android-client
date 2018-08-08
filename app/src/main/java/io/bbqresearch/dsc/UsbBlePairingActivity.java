package io.bbqresearch.dsc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import io.bbqresearch.roomwordsample.R;

public class UsbBlePairingActivity extends AppCompatActivity {
    private final static String TAG = UsbBlePairingActivity.class.getSimpleName();
    private CoordinatorLayout coordinatorLayout;
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //Bus 020 Device 046: ID 0525:a4a7
            final String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                Log.d(TAG, "USB Device Attached.");
                Snackbar snackbar = Snackbar
                        .make(coordinatorLayout, "USB Device Attached", Snackbar.LENGTH_LONG);

                snackbar.show();
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Log.d(TAG, "USB Device Attached.");
                Snackbar snackbar = Snackbar
                        .make(coordinatorLayout, "USB Device Deattached", Snackbar.LENGTH_LONG);

                snackbar.show();
            }
        }
    };

    private static IntentFilter makeUsbIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        return intentFilter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usb_ble_pairing);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        coordinatorLayout = findViewById(R.id
                .coordinatorLayoutUsb);
        this.registerReceiver(usbReceiver, makeUsbIntentFilter());
    }

}
