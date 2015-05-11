package com.ble.service;

import java.util.UUID;

public interface IBluetoothServiceCallback {
    public void notifyDataChanged(UUID uuid, double[] data);
}
