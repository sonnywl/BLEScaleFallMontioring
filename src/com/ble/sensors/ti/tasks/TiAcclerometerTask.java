
package com.ble.sensors.ti.tasks;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import com.ble.sensors.IBLEServiceAttr;
import com.ble.sensors.ti.TiAccelerometer;
import com.ble.sensors.ti.TiSensorAttr;
import com.ble.service.BluetoothLeServiceTask;
import com.ble.service.IBluetoothLeTaskManagerCallback;

public class TiAcclerometerTask extends BluetoothLeServiceTask {

    public static final String TAG = TiAcclerometerTask.class.getSimpleName();
    private static final TiAccelerometer sensor = new TiAccelerometer();
    private int periodValue = TiAccelerometer.DEF_PERIOD;
    private byte[] configValue = TiAccelerometer.RANGE_8G_ENABLE;

    public TiAcclerometerTask(IBluetoothLeTaskManagerCallback callback) {
        super(callback);
    }

    @Override
    public TiAcclerometerTask setPeriodicValue(int value) {
        if (value <= TiAccelerometer.MAX_PERIOD && value >= TiAccelerometer.MIN_PERIOD) {
            this.periodValue = value;
        }
        return this;
    }

    @Override
    public BluetoothLeServiceTask setConfigValue(byte[] value) {
        configValue = value;
        return this;
    }

    @Override
    public void notifyStateChangedDescriptor(BluetoothLeTaskState state,
            BluetoothGattDescriptor descriptor,
            BluetoothGatt gattService) {
        super.notifyStateChangedDescriptor(state, descriptor, gattService);
        switch (getTaskType()) {
            case DISABLE:
            case ENABLE:
//                BluetoothGattService mService = gattService.getService(sensor.getServiceUUID());
//                BluetoothGattCharacteristic chara = mService
//                        .getCharacteristic(sensor.getConfigUUID());
//                chara.setValue(DISABLE);
//                setTaskType(TaskType.ENABLE_FINAL);
//                gattService.writeCharacteristic(chara);
//                break;
            case CONFIG:
                setState(BluetoothLeTaskState.COMPLETED);
                getCallback().notifyTaskCompletion(this, getTaskType());
                break;
            default:
        }
    }

    @Override
    public void notifyStateChangedCharacteristic(BluetoothLeTaskState state,
            BluetoothGattCharacteristic characteristics,
            BluetoothGatt gattService) {
        super.notifyStateChangedCharacteristic(state, characteristics, gattService);
        if (state != BluetoothLeTaskState.CHARACTERISTIC_CHANGED) {
            switch (getTaskType()) {
                case ENABLE:
                    setAccelerometerNotification(gattService, true);
                    break;
                case DISABLE:
                    setAccelerometerNotification(gattService, false);
                    break;
                case CONFIG:
                    configureDescriptor(gattService);
                    break;
                case PERIODIC:
                case ENABLE_FINAL:
                default:
                    setState(BluetoothLeTaskState.COMPLETED);
                    getCallback().notifyTaskCompletion(this, getTaskType());
            }
        } else {
            double[] data = sensor.readAcclerometerData(characteristics);
            getCallback().notifyDataChanged(characteristics.getUuid(), data);
        }
    }

    @Override
    public void run(BluetoothGatt gattService) {
        BluetoothGattService mService = gattService.getService(sensor.getServiceUUID());
        BluetoothGattCharacteristic chara = mService
                .getCharacteristic(sensor.getConfigUUID());
        Log.i(TAG, "Running Serivce " + getTaskType());
        switch (getTaskType()) {
            case ENABLE:
                chara.setValue(ENABLE);
                gattService.writeCharacteristic(chara);
                break;
            case DISABLE:
                chara.setValue(DISABLE);
                gattService.writeCharacteristic(chara);
                break;
            case PERIODIC:
                if (periodValue <= TiAccelerometer.MAX_PERIOD
                        && periodValue >= TiAccelerometer.MIN_PERIOD) {
                    chara = mService.getCharacteristic(sensor.getPeriodUUID());
                    chara.setValue(new byte[] {
                            (byte) (periodValue & 0xFF)
                    });
                    gattService.writeCharacteristic(chara);
                } else {
                    Log.w(TAG, "input value " + periodValue + " exceeed Ti Sensor range");
                }
                break;
            case CONFIG:
                chara = mService.getCharacteristic(TiSensorAttr.UUID_ACC_CONF);
                chara.setValue(TiAccelerometer.RANGE_8G_ENABLE);
                gattService.writeCharacteristic(chara);
                break;
            default:
                Log.w(TAG, "Unknown configuration " + getTaskType());
        }
    }

    private void configureDescriptor(BluetoothGatt gattService) {
        BluetoothGattCharacteristic characteristic = gattService
                .getService(TiSensorAttr.UUID_ACC_SERV)
                .getCharacteristic(TiSensorAttr.UUID_ACC_CONF);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(TiSensorAttr.UUID_CUD);
        descriptor.getValue();
        descriptor.setValue(configValue);
        descriptor.setValue(TiAccelerometer.RANGE_8G_ENABLE);
        gattService.writeDescriptor(descriptor);
    }
    
    private void setAccelerometerNotification(
            BluetoothGatt gattService,
            boolean enableStatus) {
        Log.i(TAG, "Reading Accelerometer Data");
        BluetoothGattCharacteristic characteristic = gattService
                .getService(TiSensorAttr.UUID_ACC_SERV)
                .getCharacteristic(TiSensorAttr.UUID_ACC_DATA);
        BluetoothGattDescriptor notification = characteristic
                .getDescriptor(TiSensorAttr.UUID_CCC);
        if (enableStatus) {
            // Enable local notification
            gattService.setCharacteristicNotification(characteristic, true);
            // Enable remote notification
            notification.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            Log.i(TAG, "Disabling Accelerometer Notification");
            // Disable local notification
            gattService.setCharacteristicNotification(characteristic, false);
            // Disable remote notification
            notification.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
        gattService.writeDescriptor(notification);
    }

    @Override
    public IBLEServiceAttr getBleService() {
        return sensor;
    }

}
