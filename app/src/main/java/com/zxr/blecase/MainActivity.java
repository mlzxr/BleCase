package com.zxr.blecase;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.zxr.util.HexDump;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements BluetoothAdapter.LeScanCallback, AdapterView.OnItemClickListener, Runnable {

    private static final String TAG = "MainActivity";

    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private ArrayList<BluetoothDevice> bluetoothDeviceArrayList;

    private BluetoothGatt mBluetoothGatt;


    // 服务标识
    private final UUID SERVICE_UUID = UUID.fromString("fefe5245-5652-4553-5f45-49585f4e5558");
    // 特征标识（读取数据）
    private final UUID CHARACTERISTIC_READ_UUID = UUID.fromString("fefe534d-5652-4553-5f45-49585f4e5558");
    // 描述标识
    private final UUID DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


    private ListView listView;

    private ArrayList<String> arrayList = new ArrayList<>();

    private ArrayAdapter<String> adapter;
    private boolean bleFlag = false;
    private Thread thread;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        thread = new Thread(this);
        thread.start();

        listView = findViewById(R.id.list_view);
        adapter = new ArrayAdapter<String>(
                MainActivity.this,   // Context上下文
                android.R.layout.simple_list_item_1, arrayList);                                // 数据
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);

        //
        bluetoothDeviceArrayList = new ArrayList<>();
        // 询问打开蓝牙
        if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
        //
        mBluetoothAdapter.stopLeScan(this);
        mBluetoothAdapter.startLeScan(this);
        //
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBluetoothAdapter.stopLeScan(MainActivity.this);
            }
        }, 10000);

    }

    // 申请打开蓝牙请求的回调
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "蓝牙已经开启", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "没有蓝牙权限", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
        //
        if (!bluetoothDeviceArrayList.contains(bluetoothDevice)) {
            bluetoothDeviceArrayList.add(bluetoothDevice);

            Log.e(TAG, " bluetoothDevice.getName():" + bluetoothDevice.getName());
            Log.e(TAG, "bluetoothDevice.getAddress():" + bluetoothDevice.getAddress());
            Log.e(TAG, "------------------------------------------------------------");
            //
            arrayList.add(bluetoothDevice.getAddress() + "," + bluetoothDevice.getName());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyDataSetChanged();
                }
            });
        }


    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

        BluetoothDevice bluetoothDevice = bluetoothDeviceArrayList.get(i);
        if (bluetoothDevice != null) {
            //获取所需地址
            Log.e(TAG, "根据MAC地址进行连接");
            mBluetoothGatt = bluetoothDevice.connectGatt(MainActivity.this, false, mGattCallback);
        }


    }


    // BLE连接回调
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // 连接成功
                Log.e(TAG, "连接成功");
                mBluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // 连接断开
                Log.e(TAG, "连接断开");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //发现设备，遍历服务，初始化特征
                Log.e(TAG, "发现设备，遍历服务，初始化特征");
                //initBLE(gatt);
                bleFlag = true;
                //setBleNotification();

            } else {
                Log.e(TAG, "onServicesDiscovered fail-->" + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // 收到的数据
                Log.e(TAG, "onCharacteristicRead");
                Log.e(TAG, Arrays.toString(characteristic.getValue()));
                Log.e(TAG, bytesToHexString(characteristic.getValue()));
                //
                Log.e(TAG, HexDump.byteArrToBinStr(characteristic.getValue()));

            } else {
                Log.e(TAG, "onCharacteristicRead fail-->" + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            //当特征中value值发生改变,发送通知
            Log.e(TAG, "onCharacteristicChanged");
            // 收到的数据
            Log.e(TAG, Arrays.toString(characteristic.getValue()));
            Log.e(TAG, bytesToHexString(characteristic.getValue()));

        }

        /**
         * 收到BLE终端写入数据回调
         * @param gatt
         * @param characteristic
         * @param status
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // 发送成功
                Log.e(TAG, "收到BLE终端写入数据回调--发送成功");
            } else {
                // 发送失败
                Log.e(TAG, "收到BLE终端写入数据回调--发送失败");
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.e(TAG, "onDescriptorWrite");

        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            Log.e(TAG, "onReadRemoteRssi");
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            Log.e(TAG, "onDescriptorRead");
        }
    };


    /**
     * 设置蓝牙设备在数据改变时，通知App
     */
    public void setBleNotification() {

        // 获取蓝牙设备的服务
        BluetoothGattService gattService = mBluetoothGatt.getService(SERVICE_UUID);
        if (gattService == null) {
            return;
        }

        // 获取蓝牙设备的特征
        BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(CHARACTERISTIC_READ_UUID);
        if (gattCharacteristic == null) {
            return;
        }

        // 获取蓝牙设备特征的描述符
        BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(DESCRIPTOR_UUID);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        if (mBluetoothGatt.writeDescriptor(descriptor)) {
            // 蓝牙设备在数据改变时，通知App，App在收到数据后回调onCharacteristicChanged方法
            mBluetoothGatt.setCharacteristicNotification(gattCharacteristic, true);


        }
    }

    //初始化特征
    public void initBLE(BluetoothGatt gatt) {
        if (gatt == null) {
            return;
        }
        Log.e(TAG, "initBLE");
        //循环遍历服务以及每个服务下面的各个特征，判断读写，通知属性
        for (BluetoothGattService bluetoothGattService : gatt.getServices()) {
            Log.e(TAG, "-----start");
            Log.e(TAG, bluetoothGattService.getUuid().toString() + "¥");
            //遍历所有特征
            for (BluetoothGattCharacteristic bluetoothGattCharacteristic : bluetoothGattService.getCharacteristics()) {
                String str = bluetoothGattCharacteristic.getUuid().toString();
                Log.e(TAG, str);
                //
                for (BluetoothGattDescriptor bluetoothGattDescriptor : bluetoothGattCharacteristic.getDescriptors()) {
                    Log.e(TAG, "bluetoothGattDescriptor:" + bluetoothGattDescriptor.getUuid().toString());
                }
            }
            Log.e(TAG, "-----end");
        }
    }

    public boolean disConnect() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
            return true;
        }
        return false;
    }


    @Override
    public void run() {
        Log.e(TAG, "run");
        if (bleFlag) {
            BluetoothGattService service = mBluetoothGatt.getService(SERVICE_UUID);
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_READ_UUID);
            mBluetoothGatt.readCharacteristic(characteristic);
            //

        }
        handler.postDelayed(this, 2000);

    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e(TAG, "onStop");
        bleFlag = false;
        handler.removeCallbacks(this);
        disConnect();
    }

    public static String bytesToHexString(byte[] bytes) {
        String result = "";
        for (int i = 0; i < bytes.length; i++) {
            String hexString = Integer.toHexString(bytes[i] & 0xFF);
            if (hexString.length() == 1) {
                hexString = '0' + hexString;
            }
            result += hexString.toUpperCase();
        }
        return result;
    }

}
