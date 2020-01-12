package com.example.ws;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.TextView;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothDevice;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ws.adapter.ListAdapter;
import com.example.ws.bean.User;
import com.example.ws.util.UserConst;
import com.qingniu.qnble.utils.QNLogUtils;
import com.yolanda.health.qnblesdk.constant.QNBleConst;
import com.yolanda.health.qnblesdk.constant.QNScaleStatus;
import com.yolanda.health.qnblesdk.constant.UserGoal;
import com.yolanda.health.qnblesdk.constant.UserShape;
import com.yolanda.health.qnblesdk.constant.QNIndicator;
import com.yolanda.health.qnblesdk.listener.QNBleProtocolDelegate;
import com.yolanda.health.qnblesdk.listener.QNLogListener;
import com.yolanda.health.qnblesdk.listener.QNResultCallback;
import com.yolanda.health.qnblesdk.listener.QNScaleDataListener;
import com.yolanda.health.qnblesdk.out.QNBleApi;
import com.yolanda.health.qnblesdk.out.QNBleDevice;
import com.yolanda.health.qnblesdk.out.QNBleProtocolHandler;
import com.yolanda.health.qnblesdk.out.QNScaleData;
import com.yolanda.health.qnblesdk.out.QNScaleItemData;
import com.yolanda.health.qnblesdk.out.QNScaleStoreData;
import com.yolanda.health.qnblesdk.out.QNUser;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MeasureDataActivity extends AppCompatActivity {
    public static Intent getCallIntent(Context context, User user, QNBleDevice device) {
        return new Intent(context, MeasureDataActivity.class)
                .putExtra(UserConst.USER, user)
                .putExtra(UserConst.DEVICE, device);
    }

    @BindView(R.id.status_tv)
    TextView statusTv;

    @BindView(R.id.weight_tv)
    TextView weightTv;

    @BindView(R.id.data_view_grid)
    GridView dataViewGrid;

    private Handler handler = new Handler(Looper.myLooper());
    private BluetoothGattCharacteristic qnReadBgc, qnWriteBgc, qnBleReadBgc, qnBleWriteBgc;

    private QNBleDevice qnBleDevice;
    private List<QNScaleItemData> datas = new ArrayList<>();
    private QNBleApi qnBleApi;

    private User user;

    private boolean isConnected;
    private BluetoothGatt bluetoothGatt;
    private boolean isFirstService;

    private QNBleProtocolHandler protocolHandler;
    private QNScaleData currentQNScaleData;

    private List<QNScaleData> historyQNScaleData = new ArrayList<>();
    private ListAdapter listAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_measure_data);

        qnBleApi = QNBleApi.getInstance(this);
        qnBleApi.setLogListener(new QNLogListener() {
            @Override
            public void onLog(String s) {
                Log.e("MeasureDataActivity", s);
            }
        });
        ButterKnife.bind(this);


        initIntent(); // checked OK
        initView();
        initData();
    }


    private void initIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            qnBleDevice = intent.getParcelableExtra(UserConst.DEVICE);
            user = intent.getParcelableExtra(UserConst.USER);

            Log.d("MeasureDataActivity", "initIntent" + user.toString());
        }
    }

    private void initView() {
        listAdapter = new ListAdapter(datas, qnBleApi, createUser());
        dataViewGrid.setAdapter(listAdapter);
        listAdapter.notifyDataSetChanged();
    }

    private void initData() {
        initUserData();
        if (isConnected) {
            doDisconnect();
        }
        else {
            connectQnDevice(qnBleDevice);
        }

    }

    // Checked OK
    private void connectQnDevice(QNBleDevice qnBleDevice) {
        setBleStatus(QNScaleStatus.STATE_CONNECTING);
        buildHandler();
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(qnBleDevice.getMac());

        if (device != null) {
            Log.d("MeasureDataActivity", "Proceed to connect " + device.getName());
            bluetoothGatt = device.connectGatt(MeasureDataActivity.this, false, gattCallback);
        }
    }

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);


            if (status != BluetoothGatt.GATT_SUCCESS) {
                String err = "Cannot connect device with error status: " + status;
                gatt.close();
                if (bluetoothGatt != null) {
                    bluetoothGatt.disconnect();
                    bluetoothGatt.close();
                    bluetoothGatt = null;
                }
                setBleStatus(QNScaleStatus.STATE_DISCONNECTED);
                return;
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                isConnected = true;

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        setBleStatus(QNScaleStatus.STATE_CONNECTED);
                        Toast.makeText(MeasureDataActivity.this, getResources().getString(R.string.connect_successfully), Toast.LENGTH_SHORT).show();
                    }
                });

                // TODO: 2019/9/7  某些手机可能存在无法发现服务问题,此处可做延时操作
                if (bluetoothGatt != null) {
                    bluetoothGatt.discoverServices();
                }

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                isConnected = false;

                if (bluetoothGatt != null) {
                    bluetoothGatt.disconnect();
                    bluetoothGatt.close();
                    bluetoothGatt = null;
                }

                qnReadBgc = null;
                qnWriteBgc = null;
                qnBleReadBgc = null;
                qnBleWriteBgc = null;

                gatt.close();
                //TODO 实际运用中可发起重新连接
//                if (mBleDevice != null) {
//                    connectQnDevice(mBleDevice);
//                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        setBleStatus(QNScaleStatus.STATE_LINK_LOSS);
                    }
                });

            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                //发现服务,并遍历服务,找到公司对于的设备服务
                List<BluetoothGattService> services = gatt.getServices();

                for (BluetoothGattService service : services) {
                    //第一套
                    if (service.getUuid().equals(UUID.fromString(QNBleConst.UUID_IBT_SERVICES))) {
                        if (protocolHandler != null) {
                            //使能所有特征值
                            initCharacteristic(gatt, true);
                            protocolHandler.prepare(QNBleConst.UUID_IBT_SERVICES);
                        }
                        break;
                    }
                    //第二套
                    if (service.getUuid().equals(UUID.fromString(QNBleConst.UUID_IBT_SERVICES_1))) {
                        if (protocolHandler != null) {
                            //使能所有特征值
                            initCharacteristic(gatt, false);
                            protocolHandler.prepare(QNBleConst.UUID_IBT_SERVICES_1);
                        }
                        break;
                    }

                }

            }
        }


        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //获取到数据
                if (protocolHandler != null) {
                    protocolHandler.onGetBleData(getService(), characteristic.getUuid().toString(), characteristic.getValue());
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            //获取到数据
            if (protocolHandler != null) {
                protocolHandler.onGetBleData(getService(), characteristic.getUuid().toString(), characteristic.getValue());
            }

        }

    };

    private String getService() {
        if (isFirstService) {
            return QNBleConst.UUID_IBT_SERVICES;
        } else {
            return QNBleConst.UUID_IBT_SERVICES_1;
        }
    }

    private void initCharacteristic(BluetoothGatt gatt, boolean isFirstService) {

        this.isFirstService = isFirstService;

        //第一套服务
        if (isFirstService) {
            qnReadBgc = getCharacteristic(gatt, QNBleConst.UUID_IBT_SERVICES, QNBleConst.UUID_IBT_READ);
            qnWriteBgc = getCharacteristic(gatt, QNBleConst.UUID_IBT_SERVICES, QNBleConst.UUID_IBT_WRITE);
            qnBleReadBgc = getCharacteristic(gatt, QNBleConst.UUID_IBT_SERVICES, QNBleConst.UUID_IBT_BLE_READER);
            qnBleWriteBgc = getCharacteristic(gatt, QNBleConst.UUID_IBT_SERVICES, QNBleConst.UUID_IBT_BLE_WRITER);
        } else {
            qnReadBgc = getCharacteristic(gatt, QNBleConst.UUID_IBT_SERVICES_1, QNBleConst.UUID_IBT_READ_1);
            qnWriteBgc = getCharacteristic(gatt, QNBleConst.UUID_IBT_SERVICES_1, QNBleConst.UUID_IBT_WRITE_1);
        }

        enableNotifications(qnReadBgc);
        enableIndications(qnBleReadBgc);
    }

    private BluetoothGattCharacteristic getCharacteristic(final BluetoothGatt gatt, String serviceUuid, String characteristicUuid) {
        BluetoothGattService service = gatt.getService(UUID.fromString(serviceUuid));
        if (service == null) {
            return null;
        }
        return service.getCharacteristic(UUID.fromString(characteristicUuid));
    }

    private boolean enableNotifications(BluetoothGattCharacteristic characteristic) {

        final BluetoothGatt gatt = bluetoothGatt;

        if (gatt == null || characteristic == null)
            return false;


        int properties = characteristic.getProperties();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0)
            return false;

        boolean isSuccess = gatt.setCharacteristicNotification(characteristic, true);

        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(QNBleConst.CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            return gatt.writeDescriptor(descriptor);
        }

        return false;
    }

    private boolean enableIndications(BluetoothGattCharacteristic characteristic) {

        final BluetoothGatt gatt = bluetoothGatt;

        if (gatt == null || characteristic == null)
            return false;

        int properties = characteristic.getProperties();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) == 0)
            return false;

        boolean isSuccess = gatt.setCharacteristicNotification(characteristic, true);

        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(QNBleConst.CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            return gatt.writeDescriptor(descriptor);
        }
        return false;
    }


    private void buildHandler() {
        protocolHandler = qnBleApi.buildProtocolHandler(qnBleDevice, createUser(), new QNBleProtocolDelegate() {
            @Override
            public void writeCharacteristicValue(String service_uuid, String characteristic_uuid, byte[] data) {
                writeCharacteristicData(service_uuid, characteristic_uuid, data);
            }

            @Override
            public void readCharacteristic(String service_uuid, String characteristic_uuid) {
                readCharacteristicData(service_uuid, characteristic_uuid);
            }
        }, new QNResultCallback() {
            @Override
            public void onResult(int i, String s) {
                // Something...
                Log.d("MeasureDataActivity", "BuildHandler: value -> "+ s);
            }
        });
    }

    private void readCharacteristicData(String service_uuid, String characteristic_uuid) {

        switch (characteristic_uuid) {
            case QNBleConst.UUID_IBT_READ:

                if (bluetoothGatt != null && qnReadBgc != null) {
                    bluetoothGatt.readCharacteristic(qnReadBgc);
                }

                break;
            case QNBleConst.UUID_IBT_BLE_READER:

                if (bluetoothGatt != null && qnBleReadBgc != null) {
                    bluetoothGatt.readCharacteristic(qnBleReadBgc);
                }

                break;
            case QNBleConst.UUID_IBT_READ_1:

                if (bluetoothGatt != null && qnReadBgc != null) {
                    bluetoothGatt.readCharacteristic(qnReadBgc);
                }

                break;

        }

    }

    private void writeCharacteristicData(String service_uuid, String characteristic_uuid, byte[] data) {
        switch (characteristic_uuid) {
            case QNBleConst.UUID_IBT_WRITE:

                if (bluetoothGatt != null && qnWriteBgc != null) {
                    qnWriteBgc.setValue(data);
                    bluetoothGatt.writeCharacteristic(qnWriteBgc);
                }

                break;
            case QNBleConst.UUID_IBT_BLE_WRITER:

                if (bluetoothGatt != null && qnBleWriteBgc != null) {
                    qnBleWriteBgc.setValue(data);
                    bluetoothGatt.writeCharacteristic(qnBleWriteBgc);
                }

                break;
            case QNBleConst.UUID_IBT_WRITE_1:

                if (bluetoothGatt != null && qnWriteBgc != null) {
                    qnWriteBgc.setValue(data);
                    bluetoothGatt.writeCharacteristic(qnWriteBgc);
                }

                break;
        }


    }

    private void initUserData() {
        qnBleApi.setDataListener(new QNScaleDataListener() {
            @Override
            public void onGetUnsteadyWeight(QNBleDevice qnBleDevice, double weight) {
                Log.d("MeasureDataActivity", "Weight : " + weight);
                weightTv.setText(initWeight(weight));
            }

            @Override
            public void onGetScaleData(QNBleDevice qnBleDevice, QNScaleData qnScaleData) {
                Log.d("MeasureDataActivity", "Data Received");

                onReceiveScaleData(qnScaleData);

                QNScaleItemData fatValue = qnScaleData.getItem(QNIndicator.TYPE_SUBFAT);
                if (fatValue != null) {
                    String value = fatValue.getValue() + "";
                }

                currentQNScaleData = qnScaleData;
                historyQNScaleData.add(qnScaleData);

                doDisconnect();
            }

            @Override
            public void onGetStoredScale(QNBleDevice device, List<QNScaleStoreData> storedDataList) {
                if (storedDataList != null && storedDataList.size() > 0) {
                    QNScaleStoreData data = storedDataList.get(0);
                    QNUser qnUser = createUser();
                    data.setUser(qnUser);
                    QNScaleData qnScaleData = data.generateScaleData();
                    onReceiveScaleData(qnScaleData);
                    currentQNScaleData = qnScaleData;
                    historyQNScaleData.add(qnScaleData);
                }
            }

            @Override
            public void onGetElectric(QNBleDevice device, int electric) {
                // TODO

            }

            @Override
            public void onScaleStateChange(QNBleDevice device, int status) {
                setBleStatus(status);
            }
        });
    }


    // Checked OK
    private void setBleStatus(int bleStatus) {
        String stateString;
        String btnString;
        switch (bleStatus) {
            case QNScaleStatus.STATE_CONNECTING: {
                stateString = getResources().getString(R.string.connecting);
                btnString = getResources().getString(R.string.disconnected);
                isConnected = true;
                break;
            }
            case QNScaleStatus.STATE_CONNECTED: {
                stateString = getResources().getString(R.string.connected);
                btnString = getResources().getString(R.string.disconnected);
                isConnected = true;
                break;
            }
            case QNScaleStatus.STATE_DISCONNECTING: {
                stateString = getResources().getString(R.string.disconnect_in_progress);
                btnString = getResources().getString(R.string.connect);
                isConnected = false;
                break;
            }
            case QNScaleStatus.STATE_LINK_LOSS: {
                stateString = getResources().getString(R.string.connection_disconnected);
                btnString = getResources().getString(R.string.connect);
                isConnected = false;
                break;
            }
            case QNScaleStatus.STATE_START_MEASURE: {
                stateString = getResources().getString(R.string.measuring);
                btnString =getResources().getString(R.string.disconnected);
                break;
            }
            case QNScaleStatus.STATE_REAL_TIME: {
                stateString =getResources().getString(R.string.real_time_weight_measurement);
                btnString = getResources().getString(R.string.disconnected);
                break;
            }
            case QNScaleStatus.STATE_BODYFAT: {
                stateString = getResources().getString(R.string.impedance_measured);
                btnString = getResources().getString(R.string.disconnected);
                break;
            }
            case QNScaleStatus.STATE_HEART_RATE: {
                stateString = getResources().getString(R.string.measuring_heart_rate);
                btnString = getResources().getString(R.string.disconnected);
                break;
            }
            case QNScaleStatus.STATE_MEASURE_COMPLETED: {
                stateString = getResources().getString(R.string.measure_complete);
                btnString = getResources().getString(R.string.disconnected);
                break;
            }
            case QNScaleStatus.STATE_WIFI_BLE_START_NETWORK:
                stateString =getResources().getString(R.string.start_set_wifi);
                btnString = getResources().getString(R.string.disconnected);
                break;
            case QNScaleStatus.STATE_WIFI_BLE_NETWORK_FAIL:
                stateString = getResources().getString(R.string.failed_to_set_wifi);
                btnString = getResources().getString(R.string.disconnected);
                break;
            case QNScaleStatus.STATE_WIFI_BLE_NETWORK_SUCCESS:
                stateString =getResources().getString(R.string.success_to_set_wifi);
                btnString =getResources().getString(R.string.disconnected);
                break;
            default: {
                stateString = getResources().getString(R.string.connection_disconnected);
                btnString = getResources().getString(R.string.connect);
                isConnected = false;
                break;
            }
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusTv.setText(stateString);
            }
        });
        // mConnectBtn.setText(btnString);
    }

    private void onReceiveScaleData (QNScaleData d) {
        datas.clear();
        datas.addAll(d.getAllItem());
        listAdapter.notifyDataSetChanged();
    }

    private String initWeight (double weight) {
        int unit = qnBleApi.getConfig().getUnit();
        return qnBleApi.convertWeightWithTargetUnit(weight, unit);
    }


    private QNUser createUser () {

        // Shape Choice
        // TODO: Get data from interface
        UserShape userShape = UserShape.SHAPE_SLIM;

        switch (user.getChoseShape()) {
            case 0:
                userShape = UserShape.SHAPE_NONE;
                break;
            case 1:
                userShape = UserShape.SHAPE_SLIM;
                break;
            case 2:
                userShape = UserShape.SHAPE_NORMAL;
                break;
            case 3:
                userShape = UserShape.SHAPE_STRONG;
                break;
            case 4:
                userShape = UserShape.SHAPE_PLIM;
                break;
            default:
                userShape = UserShape.SHAPE_NONE;
                break;
        }

        UserGoal userGoal = UserGoal.GOAL_LOSE_FAT;

        // Tweaky Tweaky!
        return qnBleApi.buildUser(
                user.getUserId(), user.getHeight(), user.getGender(), user.getBirthDay(),
                user.getAthleteType(), new QNResultCallback() {
                    @Override
                    public void onResult(int i, String s) {
                        Log.d("MeasureDataActivity", "Scale return result -> " + s);
                    }
                });
    }

    private void doDisconnect() {
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
        }

        if (protocolHandler != null) {
            protocolHandler = null;
        }
    }




}
