package com.jetec.ble_example.Module.Service;

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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class BluetoothLeService extends Service {
    private final static String TAG = "CommunicationWithBT";
    private BluetoothManager mBluetoothManager;//藍芽管理器
    private BluetoothAdapter mBluetoothAdapter;//藍芽適配器
    private String mBluetoothDeviceAddress;//藍芽設備位址
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;
    private byte[] sendValue;//儲存要送出的資訊
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
    private boolean lockCharacteristicRead = false;//由於送執會觸發onCharacteristicRead並造成干擾，故做一個互鎖
    private final IBinder mBinder = new LocalBinder();

    /**取得特性列表(characteristic的特性)*/
    public ArrayList<String> getPropertiesTagArray(int properties) {
        int addPro = properties;
        ArrayList<String> arrayList = new ArrayList<>();
        int[] bluetoothGattCharacteristicCodes = new int[]{
                BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS,
                BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE,
                BluetoothGattCharacteristic.PROPERTY_INDICATE,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PROPERTY_BROADCAST
        };
        String[] bluetoothGattCharacteristicName = new String[]{
                "EXTENDED_PROPS",
                "SIGNED_WRITE",
                "INDICATE",
                "NOTIFY",
                "WRITE",
                "WRITE_NO_RESPONSE",
                "READ",
                "BROADCAST"
        };
        for (int i = 0; i < bluetoothGattCharacteristicCodes.length; i++) {
            int code = bluetoothGattCharacteristicCodes[i];
            if (addPro >= code) {
                addPro -= code;
                arrayList.add(bluetoothGattCharacteristicName[i]);
            }
        }
        return arrayList;
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
    /**初始化藍芽*/
    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            return false;
        }

        return true;
    }

    /**連線*/
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            return false;
        }

        if (address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            return false;
        }

        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }
    /**斷開連線*/
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.disconnect();
    }

    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**送字串模組*/
    public boolean sendValue(String value, BluetoothGattCharacteristic characteristic) {
        try {

            this.sendValue = value.getBytes();
            setCharacteristicNotification(characteristic, true);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    /**送byte[]模組*/
    public boolean sendValue(byte[] value,BluetoothGattCharacteristic characteristic){
        try{
            this.sendValue = value;
            setCharacteristicNotification(characteristic, true);
            return true;
        }catch (Exception e){
            return false;
        }
    }
    /**送出資訊*/
    private void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        if (characteristic != null) {
            for (BluetoothGattDescriptor dp : characteristic.getDescriptors()) {
                if (enabled) {
                    dp.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                } else {
                    dp.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                }
                /**送出
                 * @see onDescriptorWrite()*/
                mBluetoothGatt.writeDescriptor(dp);
            }

            mBluetoothGatt.setCharacteristicNotification(characteristic, true);
            mBluetoothGatt.readCharacteristic(characteristic);
        }

    }
    /**將搜尋到的服務傳出*/
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;
        return mBluetoothGatt.getServices();
    }

    /**藍芽資訊收發站*/
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        

        /**當連接狀態發生改變*/
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {//當設備已連接
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {//當設備無法連接
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        /**當發現新的服務器*/
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }
        /**Descriptor寫出資訊給藍芽*/
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d(TAG, "送出資訊: Byte: " + byteArrayToHexStr(sendValue)
                    + ", String: " + ascii2String(sendValue));
            BluetoothGattCharacteristic RxChar = descriptor.getCharacteristic();
            RxChar.setValue(sendValue);
            mBluetoothGatt.writeCharacteristic(RxChar);
        }
        /**讀取屬性(像是DeviceName、System ID等等)*/
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (!lockCharacteristicRead){
                    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                }
                lockCharacteristicRead = false;
                Log.d(TAG, "onCharacteristicRead: "+ascii2String(characteristic.getValue()));
            }
        }

        /**如果特性有變更(就是指藍芽有傳值過來)*/
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            if (mBluetoothAdapter == null || mBluetoothGatt == null) {
                Log.w(TAG, "BluetoothAdapter not initialized");
                return;
            }
            lockCharacteristicRead = true;
            mBluetoothGatt.readCharacteristic(characteristic);
            String record = characteristic.getStringValue(0);
            byte[] a = characteristic.getValue();
            Log.d(TAG, "readCharacteristic:回傳 " + record);
            Log.d(TAG, "readCharacteristic: 回傳byte[] " + byteArrayToHexStr(a));
        }
    };

    /**更新廣播*/
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }
    /**更新廣播*/
    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        /**對於所有其他配置文件，以十六進制寫數據*/
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for (byte byteChar : data)
                stringBuilder.append(String.format("%02X ", byteChar));
            intent.putExtra(EXTRA_DATA, characteristic.getValue());
        }
        sendBroadcast(intent);
    }
    

    /**
     * 將byte[] ASCII 轉為字串的方法
     */
    public static String ascii2String(byte[] in) {
        final StringBuilder stringBuilder = new StringBuilder(in.length);
        for (byte byteChar : in)
            stringBuilder.append(String.format("%02X ", byteChar));
        String output = null;
        try {
            output = new String(in, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return output;
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
}
