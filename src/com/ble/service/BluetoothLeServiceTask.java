
package com.ble.service;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import com.ble.sensors.IBLEServiceAttr;

/**
 * Abstraction for serially configure each Bluetooth LE service
 * 
 * @author Sonny
 */
public abstract class BluetoothLeServiceTask {

    public enum BluetoothLeTaskState {
        CHARACTERISTIC_WRITE,
        CHARACTERISTIC_READ,
        CHARACTERISTIC_CHANGED,
        DESCRIPTOR_READ,
        DESCRIPTOR_WRITE,
        COMPLETED;
    }

    public enum TaskType {
        ENABLE,
        ENABLE_FINAL,
        DISABLE,
        READ,
        PERIODIC,
        CONFIG,
        UNKNOWN;
    }

    //@formatter:off
    public static final byte[] ENABLE = new byte[] {1};
    public static final byte[] DISABLE = new byte[] {0};
    //@formatter:on

    private final IBluetoothLeTaskManagerCallback callback;
    private BluetoothLeTaskState state = BluetoothLeTaskState.CHARACTERISTIC_WRITE;
    private TaskType type = TaskType.UNKNOWN;
    private boolean isRunning;

    public BluetoothLeServiceTask(IBluetoothLeTaskManagerCallback callback) {
        this.callback = callback;
    }

    public abstract void run(BluetoothGatt gattService);

    public abstract BluetoothLeServiceTask setPeriodicValue(int value);

    public abstract BluetoothLeServiceTask setConfigValue(byte[] value);

    public void notifyStateChangedDescriptor(
            BluetoothLeTaskState state,
            BluetoothGattDescriptor descriptor,
            BluetoothGatt gattService) {
        this.state = state;
    }

    public void notifyStateChangedCharacteristic(
            BluetoothLeTaskState state,
            BluetoothGattCharacteristic characteristics,
            BluetoothGatt gattService) {
        this.state = state;
    }

    public BluetoothLeServiceTask setTaskType(TaskType type) {
        this.type = type;
        return this;
    }

    public TaskType getTaskType() {
        return type;
    }

    public void setState(BluetoothLeTaskState state) {
        this.state = state;
    }

    public BluetoothLeTaskState getState() {
        return state;
    }

    public void setRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public IBluetoothLeTaskManagerCallback getCallback() {
        return callback;
    }

    public abstract IBLEServiceAttr getBleService();

}
