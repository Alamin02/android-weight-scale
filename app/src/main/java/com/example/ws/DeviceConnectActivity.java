package com.example.ws;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ws.bean.Config;
import com.example.ws.bean.User;
import com.example.ws.picker.WIFISetDialog;
import com.example.ws.util.AndroidPermissionCenter;
import com.example.ws.util.UserConst;
import com.yolanda.health.qnblesdk.constant.CheckStatus;
import com.yolanda.health.qnblesdk.constant.QNDeviceType;
import com.yolanda.health.qnblesdk.listener.QNResultCallback;
import com.yolanda.health.qnblesdk.out.QNBleApi;
import com.yolanda.health.qnblesdk.out.QNBleDevice;
import com.yolanda.health.qnblesdk.out.QNConfig;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DeviceConnectActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    @BindView(R.id.scan_device_btn)
    Button scanBtn;
    @BindView(R.id.stop_scan_btn)
    Button stopBtn;

    @BindView(R.id.device_list)
    ListView deviceListView;

    private QNBleApi qnBleApi;
    private User user;
    private Config config;

    private Boolean isScanning = false;
    private WIFISetDialog wifiSetDialog;

    private List<String> macList = new ArrayList<>();
    private List<QNBleDevice> devices = new ArrayList<>();

    public static Intent getCallIntent(Context context, User user, Config config) {
        return new Intent(context, DeviceConnectActivity.class)
                .putExtra(UserConst.CONFIG, config)
                .putExtra(UserConst.USER, user);
    }


    private BaseAdapter listAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return devices.size();
        }

        @Override
        public Object getItem(int position) {
            return devices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return devices.get(position).hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_items, null);
            }

            TextView nameTv = (TextView) convertView.findViewById(R.id.nameTv);
            TextView modelTv = (TextView) convertView.findViewById(R.id.modelTv);
            TextView macTv = (TextView) convertView.findViewById(R.id.macTv);
            TextView rssiTv = (TextView) convertView.findViewById(R.id.rssiTv);

            QNBleDevice scanResult = devices.get(position);

            nameTv.setText(scanResult.getName());
            modelTv.setText(scanResult.getModeId());
            macTv.setText(scanResult.getMac());
            rssiTv.setText(String.valueOf(scanResult.getRssi()));

            return convertView;
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_device);
        ButterKnife.bind(this);

        qnBleApi = QNBleApi.getInstance(this);

        user = getIntent().getParcelableExtra(UserConst.USER);
        config = getIntent().getParcelableExtra(UserConst.CONFIG);

        initData();

        AndroidPermissionCenter.verifyPermissions(this);
        deviceListView.setAdapter(this.listAdapter);
        deviceListView.setOnItemClickListener(this);
    }

    private void initData() {
        QNConfig qnConfig = qnBleApi.getConfig(); //Get the last set object, if not set, get the default object
        qnConfig.setAllowDuplicates(config.isAllowDuplicates());
        qnConfig.setDuration(config.getDuration());
        qnConfig.setConnectOutTime(config.getConnectOutTime());
        qnConfig.setUnit(config.getUnit());
        qnConfig.setOnlyScreenOn(config.isOnlyScreenOn());

        // Set scan objects
        qnConfig.save(new QNResultCallback() {
            @Override
            public void onResult(int i, String s) {
                Log.d("ScanActivity", "initData:" + s);
            }
        });

        wifiSetDialog = new WIFISetDialog(DeviceConnectActivity.this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    private BluetoothAdapter.LeScanCallback scanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

            if(device == null) {
                return;
            }

            QNBleDevice qnBleDevice = qnBleApi.buildDevice(device, rssi, scanRecord, new QNResultCallback() {
                @Override
                public void onResult(int code, String message) {
                    if (code != CheckStatus.OK.getCode()) {
                        // Something is not right...
                    }
                }
            });

            if (qnBleDevice != null && !macList.contains(qnBleDevice.getMac())) {
                macList.add(qnBleDevice.getMac());
                devices.add(qnBleDevice);
                listAdapter.notifyDataSetChanged();
            }

        }
    };

    private void startScan() {
        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(DeviceConnectActivity.this, "Device Not Supported", Toast.LENGTH_SHORT).show();
            return;
        }

        if (bluetoothAdapter.getState() != BluetoothAdapter.STATE_ON) {
            Toast.makeText(DeviceConnectActivity.this, "Please Turn on Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        isScanning = bluetoothAdapter.startLeScan(scanCallback);
    }

    private BluetoothAdapter getBluetoothAdapter() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            return bluetoothManager == null? null : bluetoothManager.getAdapter();
        } else {
            return BluetoothAdapter.getDefaultAdapter();
        }
    }

    private void stopScan() {
        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(DeviceConnectActivity.this, "Device Not Supported", Toast.LENGTH_SHORT).show();
            return;
        }
        bluetoothAdapter.stopLeScan(scanCallback);
        isScanning = false;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position < 0 || position >= this.devices.size()) {
            return;
        }

        stopScan();
        final QNBleDevice device = this.devices.get(position);

        if (device.getDeviceType() == QNDeviceType.SCALE_BLE_DEFAULT) {
            if (device.isSupportWifi()) {
                wifiSetDialog.setDialogClickListener(new WIFISetDialog.DialogClickListener() {
                    @Override
                    public void confirmClick(String ssid, String pwd) {
                        Log.e("DeviceConnectActivity", "ssid : " + ssid);
                        // Some wifi realted wizardy...
                    }

                    @Override
                    public void cancelClick() {

                    }
                });
                wifiSetDialog.show();
            } else {
                startActivity(MeasureDataActivity.getCallIntent(DeviceConnectActivity.this, user, device));
            }
        }

    }

    @OnClick({R.id.scan_device_btn, R.id.stop_scan_btn})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.scan_device_btn:
                if(!isScanning) {
                    this.devices.clear();
                    this.macList.clear();

                    listAdapter.notifyDataSetChanged();
                    startScan();
                }
                break;
            case R.id.stop_scan_btn:
                if (isScanning) {
                    stopScan();
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == AndroidPermissionCenter.REQUEST_EXTERNAL_STORAGE) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Successfully gained permission", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
