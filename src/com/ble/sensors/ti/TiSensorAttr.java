
package com.ble.sensors.ti;

import java.util.HashMap;
import java.util.UUID;

/**
 * CC 2541 Ti Sensor Tag UUIDs
 * http://processors.wiki.ti.com/images/a/a8/BLE_SensorTag_GATT_Server.pdf
 * 
 * @author Sonny
 */
public class TiSensorAttr {
    private static HashMap<String, String> attributes = new HashMap<String, String>();

    //@formatter:off
    public final static UUID
    UUID_IRT_SERV = UUID.fromString("f000aa00-0451-4000-b000-000000000000"), // IR Temperature Service
    UUID_IRT_DATA = UUID.fromString("f000aa01-0451-4000-b000-000000000000"), // IR Temperature Data
    UUID_IRT_CONF = UUID.fromString("f000aa02-0451-4000-b000-000000000000"), // IR Temperature Config 0: disable, 1: enable
    UUID_IRT_PERI = UUID.fromString("f000aa03-0451-4000-b000-000000000000"), // IR Temperature Period in tens of milliseconds // Reconnection Address

    UUID_ACC_SERV = UUID.fromString("f000aa10-0451-4000-b000-000000000000"), // Accelerometer Service
    UUID_ACC_DATA = UUID.fromString("f000aa11-0451-4000-b000-000000000000"), // Accelerometer Data 
    UUID_ACC_CONF = UUID.fromString("f000aa12-0451-4000-b000-000000000000"), // Accelerometer Config 0: disable, 1: enable
    UUID_ACC_PERI = UUID.fromString("f000aa13-0451-4000-b000-000000000000"), // Accelerometer Period in tens of milliseconds

    UUID_HUM_SERV = UUID.fromString("f000aa20-0451-4000-b000-000000000000"), // Humidity Service
    UUID_HUM_DATA = UUID.fromString("f000aa21-0451-4000-b000-000000000000"), // Humidity Data 
    UUID_HUM_CONF = UUID.fromString("f000aa22-0451-4000-b000-000000000000"), // Humidity Config 0: disable, 1: enable
    UUID_HUM_PERI = UUID.fromString("f000aa23-0451-4000-b000-000000000000"), // Humidity Period Period in tens of milliseconds

    UUID_MAG_SERV = UUID.fromString("f000aa30-0451-4000-b000-000000000000"), // Magnetometer Service
    UUID_MAG_DATA = UUID.fromString("f000aa31-0451-4000-b000-000000000000"), // Magnetometer Data
    UUID_MAG_CONF = UUID.fromString("f000aa32-0451-4000-b000-000000000000"), // Magnetometer Config 0: disable, 1: enable
    UUID_MAG_PERI = UUID.fromString("f000aa33-0451-4000-b000-000000000000"), // Magnetometer Period in tens of milliseconds

    UUID_BAR_SERV = UUID.fromString("f000aa40-0451-4000-b000-000000000000"), // Barometer Service
    UUID_BAR_DATA = UUID.fromString("f000aa41-0451-4000-b000-000000000000"), // Barometer Data
    UUID_BAR_CONF = UUID.fromString("f000aa42-0451-4000-b000-000000000000"), // Barometer Config 0: disable, 1: enable
    UUID_BAR_CALI = UUID.fromString("f000aa43-0451-4000-b000-000000000000"), // Barometer Calibration characteristic
    UUID_BAR_PERI = UUID.fromString("f000aa44-0451-4000-b000-000000000000"), // Barometer Period in tens of milliseconds

    UUID_GYR_SERV = UUID.fromString("f000aa50-0451-4000-b000-000000000000"), // Gyroscope Service
    UUID_GYR_DATA = UUID.fromString("f000aa51-0451-4000-b000-000000000000"), // Gyroscope Data
    UUID_GYR_CONF = UUID.fromString("f000aa52-0451-4000-b000-000000000000"), // Gyroscope Config 0: disable, bit 0: enable x, bit 1: enable y, bit 2: enable z
    UUID_GYR_PERI = UUID.fromString("f000aa53-0451-4000-b000-000000000000"), // Gyroscope Period Period in tens of milliseconds

    UUID_CON_PARA = UUID.fromString("f000ccc1-0451-4000-b000-000000000000"), // Connection Parameter
    UUID_REC_CONN = UUID.fromString("f000ccc2-0451-4000-b000-000000000000"), // Connection Request
    UUID_DIS_CONN = UUID.fromString("f000ccc3-0451-4000-b000-000000000000"), // Connection Disconnect
    
    UUID_CUD = UUID.fromString("00002901-0000-1000-8000-00805f9b34fb"), // Characteristic User Description
    UUID_CCC = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"), // Characteristic Client
    UUID_CCD = UUID.fromString("00002903-0000-1000-8000-00805f9b34fb"); // Characteristic User Declaration
    

    //@formatter:on

    static {
        attributes.put(UUID_ACC_SERV.toString(), "Accelerometer Service");
        attributes.put(UUID_HUM_SERV.toString(), "Humidity Service");
        attributes.put(UUID_MAG_SERV.toString(), "Magnometer Service");
        attributes.put(UUID_BAR_SERV.toString(), "Barometer Service");
        attributes.put(UUID_GYR_SERV.toString(), "Gyroscope Service");
    }

    public static String lookupAttribute(String uuid) {
        return attributes.get(uuid);
    }
}
