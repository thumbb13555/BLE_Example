package com.jetec.ble_example;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.List;
import java.util.UUID;

public class BluetoothLeService extends Service {
    private static final String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;//藍芽管理器
    private BluetoothAdapter mBluetoothAdapter;//藍芽適配器
    private String mBluetoothDeviceAddress;//藍芽設備位址
    private BluetoothGatt mBluetoothGatt;

    private static final int STATE_DISCONNECTED = 0;//設備無法連接
    private static final int STATE_CONNECTING = 1;//設備正在連接
    private static final int STATE_CONNECTED = 2;//設備連接完畢

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";//已連接到GATT服務器
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";//未連接GATT服務器
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";//未發現GATT服務
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";//接收到來自設備的數據，可通過讀取或操作獲得

    public final static String EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA"; //其他數據
    private int mConnectionState = STATE_DISCONNECTED;

    private final IBinder mBinder = new LocalBinder();

    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    /**
     * 通過BLE API的不同類型的連接方法
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        /**當連接狀態發生改變*/
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {//當設備已連接
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.d(TAG, "已連接至GATT服務");
                Log.d(TAG, "開始嘗試搜尋服務: "+mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {//當設備無法連接
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.d(TAG, "已斷開服務");
                broadcastUpdate(intentAction);
            }
        }

        /**當發現新的服務器*/
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {

                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.d(TAG, "onServicesDiscovered: "+status);
            }
        }

        /**讀資料(只有從藍芽回傳的資料才會被顯示)*/
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        /**廣播更新(只有從藍芽回傳的資料才會被顯示)*/
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            readCharacteristic(characteristic);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d(TAG, "成功發送訊息");
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            writeCustomCharacteristic();

        }
    };


    private void broadcastUpdate(final String action) {

        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    /**
     * 當一個特定的回調函數被觸發時，會調用相應的broadcastUpdate()輔助方法
     */
    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        /**對於所有其他配置文件，以十六進制寫數據*/
        final byte[] data = characteristic.getValue();
        Log.d(TAG, "broadcastUpdate: "+byteArrayToHexStr(data));
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for (byte byteChar : data)
                stringBuilder.append(String.format("%02X ", byteChar));
            intent.putExtra(EXTRA_DATA,characteristic.getValue());
        }

        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */

    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    /**
     * 在這裏讀取藍芽中數據
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {


        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
        byte [] a = characteristic.getValue();
        Log.d(TAG, "回傳String: "+characteristic.getStringValue(0) );
        Log.d(TAG, "readCharacteristic: 回傳byte[]: "+byteArrayToHexStr(a));


    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {

        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        UUID ServiceUUID = UUID.fromString(SampleGattAttributes.myGatt.get(5));
        UUID TXUUID = UUID.fromString(SampleGattAttributes.myGatt.get(6));
        if (mBluetoothGatt != null) {
            BluetoothGattService service = mBluetoothGatt.getService(ServiceUUID);
            if (service != null) {
                BluetoothGattCharacteristic chara = service.getCharacteristic(TXUUID);
                if (chara != null) {

                    BluetoothGattDescriptor descriptor = chara.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
                    if (descriptor != null) {
                    }
                    if (enabled) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                    } else {
                        descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                    }
                    for (BluetoothGattDescriptor dp : characteristic.getDescriptors()) {
                        dp.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        mBluetoothGatt.writeDescriptor(dp);
                    }
                    mBluetoothGatt.writeDescriptor(descriptor);
                    mBluetoothGatt.setCharacteristicNotification(characteristic, true);

                }
            }
        }//All if
    }
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }
    /**寫資料*/
    public void writeCustomCharacteristic() {
        BluetoothGattService RxService = mBluetoothGatt
                .getService(UUID.fromString(SampleGattAttributes.myGatt.get(5)));
        if (RxService == null) {
            Log.d(TAG, "Rx service not found on RxService!");
            return;
        }
        BluetoothGattCharacteristic RxChar = RxService
                .getCharacteristic(UUID.fromString(SampleGattAttributes.myGatt.get(7)));
        if (RxChar == null) {
            Log.d(TAG, "Rx char not found on RxService!");
            return;
        }
        byte[] strBytes = "嘗試送出訊息".getBytes();
        RxChar.setValue(strBytes);
        mBluetoothGatt.writeCharacteristic(RxChar);

    }

    /**
     * Byte轉16進字串工具
     */
    public static String byteArrayToHexStr(byte[] byteArray) {
        if (byteArray == null) {
            return null;
        }

        StringBuilder hex = new StringBuilder(byteArray.length * 2);
        for (byte aData : byteArray) {
            hex.append(String.format("%02X", aData));
        }
        String gethex = hex.toString();
        return gethex;

    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;
        return mBluetoothGatt.getServices();
    }

}
