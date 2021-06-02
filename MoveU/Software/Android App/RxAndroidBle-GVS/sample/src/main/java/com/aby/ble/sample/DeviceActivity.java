package com.aby.ble.sample;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;




import butterknife.ButterKnife;
import butterknife.OnClick;

public class DeviceActivity extends AppCompatActivity {

    public static final String EXTRA_MAC_ADDRESS = "extra_mac_address";
    private String macAddress;

    @OnClick(R.id.connect)
    public void onConnectClick() {

    }

    @OnClick(R.id.discovery)
    public void onDiscoveryClick() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        ButterKnife.bind(this);
        macAddress = getIntent().getStringExtra(EXTRA_MAC_ADDRESS);
        //noinspection ConstantConditions
        getSupportActionBar().setSubtitle(getString(R.string.mac_address, macAddress));

    }


}
