
package com.ble.sensors.ti;

import android.bluetooth.BluetoothGattCharacteristic;

import com.ble.sensors.IBLEServiceAttr;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class TiAccelerometer implements IBLEServiceAttr {
    private static Set<UUID> UUIDS = new HashSet<UUID>(3);
    static {
        UUIDS.add(TiSensorAttr.UUID_ACC_SERV);
        UUIDS.add(TiSensorAttr.UUID_ACC_CONF);
        UUIDS.add(TiSensorAttr.UUID_ACC_DATA);
        UUIDS.add(TiSensorAttr.UUID_ACC_PERI);
        UUIDS.add(TiSensorAttr.UUID_CCC);
    }

    // @formatter:off
    public static final byte[] RANGE_2G_ENABLE = new byte[] {(byte) (0x01 & 0xFF)};
    public static final byte[] RANGE_4G_ENABLE = new byte[] {(byte) (0x02 & 0xFF)};
    public static final byte[] RANGE_8G_ENABLE = new byte[] {(byte) (0x03 & 0xFF)};
    // @formatter:on
    public static final int MAX_PERIOD = 255;
    public static final int MIN_PERIOD = 10;
    public static final int DEF_PERIOD = 100;

    @Override
    public UUID getServiceUUID() {
        return TiSensorAttr.UUID_ACC_SERV;
    }

    @Override
    public String getServicUUIDName() {
        return "Accelerometer";
    }

    @Override
    public UUID getDataUUID() {
        return TiSensorAttr.UUID_ACC_DATA;
    }

    @Override
    public UUID getConfigUUID() {
        return TiSensorAttr.UUID_ACC_CONF;
    }

    @Override
    public UUID getPeriodUUID() {
        return TiSensorAttr.UUID_ACC_PERI;
    }

    @Override
    public Set<UUID> getTaskUUIDs() {
        return UUIDS;
    }

    public double[] readAcclerometerData(final BluetoothGattCharacteristic c) {
        if (c.getUuid().toString().equals(TiSensorAttr.UUID_ACC_DATA.toString())) {
            Integer x = c.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 0);
            Integer y = c.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 1);
            Integer z = c.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 2) * -1;

            double scaledX = x / 64.0;
            double scaledY = y / 64.0;
            double scaledZ = z / 64.0;

            return new double[] {
                    scaledX, scaledY, scaledZ
            };
        }
        return null;
    }

}
