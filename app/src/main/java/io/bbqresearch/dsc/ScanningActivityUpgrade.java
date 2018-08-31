package io.bbqresearch.dsc;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.exceptions.BleScanException;
import com.polidea.rxandroidble2.scan.ScanResult;
import com.polidea.rxandroidble2.scan.ScanSettings;

import java.util.ArrayList;
import java.util.Locale;

import io.bbqresearch.dsc.service.DscServiceUpgrade;
import io.bbqresearch.roomwordsample.R;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;


public class ScanningActivityUpgrade extends ListActivity {
    private final static String TAG = ScanningActivityUpgrade.class.getSimpleName();
    DscServiceUpgrade dscService;
    Disposable scanDisposable;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            dscService = ((DscServiceUpgrade.LocalBinder) service).getService();
            Log.e(TAG, "Made it START SCANNING");
            startScanning();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            dscService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "HELLO ARE WE HERE?");
        //setContentView(R.layout.activity_scanning_upgrade);
        Intent gattServiceIntent = new Intent(this, DscServiceUpgrade.class);
        this.getApplicationContext().bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (scanDisposable != null) scanDisposable.dispose();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);
    }

    private void dispose() {
        scanDisposable = null;

    }

    private void onScanFailure(Throwable throwable) {

        if (throwable instanceof BleScanException) {
            handleBleScanException((BleScanException) throwable);
        }
    }

    private void startScanning() {
        //Log.i(TAG,"RxBleClient State: " + dscService.getRxBleClient().getState().toString());

        scanDisposable = dscService.getRxBleClient().scanBleDevices(
                new ScanSettings.Builder()
                        //.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        //.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                        .build()
        )
                .observeOn(AndroidSchedulers.mainThread())
                //.doFinally(this::dispose)
                .subscribe(
                        mLeDeviceListAdapter::addDevice,
                        this::onScanFailure
                );
    }


    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final RxBleDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;

        Intent intent = new Intent();
        intent.putExtra(MainActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(MainActivity.EXTRAS_DEVICE_ADDR, device.getMacAddress());
        setResult(RESULT_OK, intent);

        finish();
    }

    private void handleBleScanException(BleScanException bleScanException) {
        final String text;

        switch (bleScanException.getReason()) {
            case BleScanException.BLUETOOTH_NOT_AVAILABLE:
                text = "Bluetooth is not available";
                break;
            case BleScanException.BLUETOOTH_DISABLED:
                text = "Enable bluetooth and try again";
                break;
            case BleScanException.LOCATION_PERMISSION_MISSING:
                text = "On Android 6.0 location permission is required. Implement Runtime Permissions";
                break;
            case BleScanException.LOCATION_SERVICES_DISABLED:
                text = "Location services needs to be enabled on Android 6.0";
                break;
            case BleScanException.SCAN_FAILED_ALREADY_STARTED:
                text = "Scan with the same filters is already started";
                break;
            case BleScanException.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                text = "Failed to register application for bluetooth scan";
                break;
            case BleScanException.SCAN_FAILED_FEATURE_UNSUPPORTED:
                text = "Scan with specified parameters is not supported";
                break;
            case BleScanException.SCAN_FAILED_INTERNAL_ERROR:
                text = "Scan failed due to internal error";
                break;
            case BleScanException.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES:
                text = "Scan cannot start due to limited hardware resources";
                break;
            case BleScanException.UNDOCUMENTED_SCAN_THROTTLE:
                text = String.format(
                        Locale.getDefault(),
                        "Android 7+ does not allow more scans. Try in %d seconds",

                        "30 seconds or so..."
                );
                break;
            case BleScanException.UNKNOWN_ERROR_CODE:
            case BleScanException.BLUETOOTH_CANNOT_START:
            default:
                text = "Unable to start scanning";
                break;
        }
        Log.w("EXCEPTION", text, bleScanException);
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<RxBleDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<RxBleDevice>();
            mInflator = ScanningActivityUpgrade.this.getLayoutInflater();
        }

        public void addDevice(ScanResult bleScanResult) {
            if (!mLeDevices.contains(bleScanResult.getBleDevice())) {
                mLeDevices.add(bleScanResult.getBleDevice());
                this.notifyDataSetChanged();
            }
        }

        public RxBleDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ScanningActivity.ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.list_item, null);
                viewHolder = new ScanningActivity.ViewHolder();
                viewHolder.deviceAddress = view.findViewById(R.id.device_address);
                viewHolder.deviceName = view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ScanningActivity.ViewHolder) view.getTag();
            }

            RxBleDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getMacAddress());

            return view;
        }
    }
}
