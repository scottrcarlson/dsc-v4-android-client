package io.bbqresearch.dsc;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.bbqresearch.roomwordsample.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class StatusDeviceFragment extends Fragment {

    TextView deviceName;
    TextView bleName;
    TextView bleMacAddr;
    TextView bleRssi;
    TextView deviceDatetime;
    TextView radioMode;

    public StatusDeviceFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_status_device, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        deviceName = getView().findViewById(R.id.device_name);
        bleName = getView().findViewById(R.id.ble_name);
        bleMacAddr = getView().findViewById(R.id.ble_mac_addr);
        bleRssi = getView().findViewById(R.id.ble_rssi);
        deviceDatetime = getView().findViewById(R.id.device_datetime);
        radioMode = getView().findViewById(R.id.device_radio_mode);
    }

    public void setDeviceName(String name) {
        deviceName.setText(name);

    }

    public void setBleName(String name) {
        bleName.setText(name);
    }

    public void setBleMacAddr(String addr) {
        bleMacAddr.setText(addr);
    }

    public void setDeviceDatetime(String datetime) {
        deviceDatetime.setText(datetime);
    }

    public void setDeviceRadioMode(String mode) {
        if (mode.contentEquals("false")) {
            radioMode.setText("RX");
        } else {
            radioMode.setText("TX");
        }

    }

    public void setBleRssi(String rssi) {
        bleRssi.setText(rssi);
    }

}
