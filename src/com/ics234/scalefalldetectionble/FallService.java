package com.ics234.scalefalldetectionble;


import android.app.Activity;
import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.ble.sensors.ti.TiAccelerometer;
import com.ble.sensors.ti.TiSensorAttr;
import com.ble.service.BluetoothLeServiceTask.BluetoothLeTaskState;
import com.ble.service.BluetoothLeServiceTask.TaskType;
import com.ble.service.BluetoothLeTaskManager;
import com.ble.service.IBluetoothServiceCallback;
import com.lib.CircularDoubleArray;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FallService extends Service implements 
    SensorEventListener,     
    LeScanCallback,
    IBluetoothServiceCallback{

	public static final String TAG = FallService.class.getName();
	public static final int SCREEN_OFF_RECEIVER_DELAY = 500;
	public static final int LOW_THRESH = 2;
	public static final int HIGH_THRESH = 25;
	private static long lastTime = 0;
	private static float innerTimer = 0;
	private static float outerTimer = 0;
	private double[] accelVals = new double[5];
	
	private CircularDoubleArray accelValues = new CircularDoubleArray(5);
	private boolean timeFreeFallEvent = false;
	private boolean timeGroundContactEvent = false;
	private boolean fallPhaseOne = false;
	private boolean fallPhaseTwo = false;
	private boolean fallOccurred = false;
	private double maxAccelExperienced = 0.0;

	private FallServiceBinder mBinder = new FallServiceBinder();
	private SensorManager mSensorManager = null;
//	private WakeLock mWakeLock = null;

	private SharedPreferences prefs;

    private BluetoothDevice mBluetoothDevice;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattStatus mBluetoothGattCallback;
    private BluetoothLeTaskManager bleManager;
    private Set<BluetoothDevice> discoveredDevices;
    private Set<BluetoothDevice> pairedDevices;

	/*
	 * Register this as a sensor event listener.
	 */
	private void registerListener() {
		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_FASTEST);
	}

	/*
	 * Un-register this as a sensor event listener.
	 */
	private void unregisterListener() {
		mSensorManager.unregisterListener(this);
	}

	public BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			if (!intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				return;
			}

			Runnable runnable = new Runnable() {
				public void run() {
					unregisterListener();
					registerListener();
				}
			};

			new Handler().postDelayed(runnable, SCREEN_OFF_RECEIVER_DELAY);
		}
	};

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
		    sensorDataChanged(event.values, event.timestamp);
		}
	}

	public void sensorDataChanged(float[] values, long timeStampNanoSec) {
        long temp;
        temp = timeStampNanoSec;

        // Calculate the acceleration and store in the buffer
        double x = values[0];
        double y = values[1];
        double z = values[2];
        double accelAvg = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2)
                + Math.pow(z, 2));
        shiftBuffer(accelAvg);
        accelValues.add(accelAvg);


        if (fallPhaseOne) {
            if (accelAvg > maxAccelExperienced)
                maxAccelExperienced = accelAvg;
        }

        // Check what the current fall event is and take appropriate action
        int fallEvent = checkBufferEvent(accelAvg);
        switch (fallEvent) {
        case -1:
            break;
        case 0:
            if (timeFreeFallEvent == false) {
                timeFreeFallEvent = true;
                innerTimer = 0;
                outerTimer = 0;
            }
            break;
        case 1:
            if (timeGroundContactEvent == false) {
                timeGroundContactEvent = true;
            }
            break;
        }

        // Time the free fall and make sure it's long enough for a real fall
        if (timeFreeFallEvent == true) {
            if (innerTimer >= (prefs.getFloat("free_fall_slider", (float)0.2)*4) 
                    && accelValues.getAverage() < (prefs.getFloat("low_threshold_slider", 2))*3) {
                fallPhaseOne = true;
                timeFreeFallEvent = false;
            } else if (innerTimer < 0.15 && getBufferMean() > (prefs.getFloat("low_threshold_slider", 2))*3)
                timeFreeFallEvent = false;
        }

        // Time the ground contact and make sure it's long enough for a real fall
        if (timeGroundContactEvent == true) {
            fallPhaseTwo = true;
            timeGroundContactEvent = false;
        }

        // Add time elapsed
        innerTimer += ((float) (temp - lastTime) / 1000000000.0);
        outerTimer += ((float) (temp - lastTime) / 1000000000.0);


        double fallDur = (prefs.getFloat("fall_duration_slider", 4)*8);
        if (outerTimer <= fallDur && fallPhaseOne == true && fallPhaseTwo == true)
            fallOccurred = true;
        else if (outerTimer > fallDur) {
            fallPhaseOne = false;
            timeFreeFallEvent = false;
            timeGroundContactEvent = false;
            for (int i = 0; i < accelVals.length; i++) {
                accelVals[i] = 9.81;
            }
            accelValues.reset(9.81);
            outerTimer = 0;
        }

        if (fallOccurred == true) {
            fallOccurred = false;
            timeFreeFallEvent = false;
            timeGroundContactEvent = false;
            fallPhaseOne = false;
            fallPhaseTwo = false;

            for (int i = 0; i < accelVals.length; i++)
                accelVals[i] = 9.81;

            // Launch the activity that will create the timer and handle
            // emergency response
            Intent intent = new Intent(getBaseContext(), FallResponse.class);
            intent.putExtra("maxAccel", maxAccelExperienced);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getApplication().startActivity(intent);

            maxAccelExperienced = 0.0;
        }

        lastTime = timeStampNanoSec;

	}
	
	public void shiftBuffer(double accelData) {
		for (int i = accelVals.length - 1; i > 0; i--)
			accelVals[i] = accelVals[i - 1];

		accelVals[0] = accelData;
	}

	public int checkBufferEvent(double accelData) {

		if (accelData < (prefs.getFloat("low_threshold_slider", 2))*3) {
			return 0;
		} else if (fallPhaseOne == true && accelData > (prefs.getFloat("high_threshold_slider", 20))*30) {
			return 1;
		} else {
			return -1;
		}
	}

	public double getBufferMean() {
		double sum = 0.0;
		for (double d : accelVals)
			sum += d;
		double mean = sum / accelVals.length;
		return mean;
	}    
 
    
	@Override
	public void onCreate() {
		super.onCreate();

		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

//		PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
//		mWakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
//		registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        
		outerTimer = 0;
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		initBLE(this);
	}

	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        startForeground(Process.myPid(), new Notification());
//      registerListener();
//      mWakeLock.acquire();

        return START_STICKY;
    }

	@Override
	public void onDestroy() {
//		unregisterReceiver(mReceiver);
//		unregisterListener();
//		mWakeLock.release();
		stopForeground(true);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public class FallServiceBinder extends Binder {
	    public FallService getService() {
	        return FallService.this;
        }
    }
	
    public void initBLE(Context context) {
        pairedDevices = new HashSet<BluetoothDevice>();
        discoveredDevices = new HashSet<BluetoothDevice>();

        mBluetoothAdapter = ((BluetoothManager) context
                .getSystemService(Activity.BLUETOOTH_SERVICE))
                .getAdapter();
        mBluetoothGattCallback = new BluetoothGattStatus();
        bleManager = BluetoothLeTaskManager.getInstance(this);
        if (mBluetoothAdapter == null) {
            Toast.makeText(this,
                    "Bluetooth is not available",
                    Toast.LENGTH_LONG).show();
        }
    }

    public void setSensorStatus(boolean status) {
        if (status && mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        } else if (!status) {
            mBluetoothAdapter.disable();
        }
    }

    public BluetoothDevice[] getPairedDevices() {
        // Get a set of currently paired devices
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : devices) {
            pairedDevices.add(device);
        }
        return pairedDevices.toArray(new BluetoothDevice[] {});
    }

    public BluetoothDevice[] getDiscoveredDevices() {
        return discoveredDevices.toArray(new BluetoothDevice[] {});
    }

    public boolean startLeDiscovery() {
        if (mBluetoothAdapter.isEnabled()) {
            discoveredDevices.clear();
            mBluetoothAdapter.cancelDiscovery();
            return mBluetoothAdapter.startLeScan(this);
        }
        return false;
    }

    public void stopLeDiscovery() {
        mBluetoothAdapter.cancelDiscovery();
        mBluetoothAdapter.stopLeScan(this);
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (!discoveredDevices.contains(device)) {
            discoveredDevices.add(device);
            Log.i(TAG, "Found LE Device " + device.getName() + ":" + device.getAddress());
        }
    }

    public void connectLeDevice(BluetoothDevice bluetoothDevice) {
        Log.i(TAG, "Clicked on Device " + bluetoothDevice.getName());
        mBluetoothDevice = bluetoothDevice;
        mBluetoothGatt = mBluetoothDevice.connectGatt(this, false, mBluetoothGattCallback);
        bleManager.setGattCallbackDevice(mBluetoothGatt);
    }

    public void destroy() {
        if (mBluetoothAdapter != null) {
            Log.i(TAG, "Destorying Bluetooth Service");
            if (mBluetoothGatt != null) {
                Log.i(TAG, "Disconnection Bluetooth Gatt");
                clear();
                mBluetoothGatt.disconnect();
            }
            stopSelf();
        }
    }

    @Override
    public void notifyDataChanged(UUID uuid, double[] data) {
        Log.i(TAG, "Notified Data Changed " + data[0] + " " + data[1] + " " + data[2]);
        float[] floatData = new float[data.length];
        for(int i = 0; i < data.length; i++) {
            floatData[i] = (float) data[i];
        }
        sensorDataChanged(floatData, System.nanoTime());
    }

    public void reset() {
        bleManager.resumeServices();
    }

    public void clear() {
        bleManager.disableServices();
    }


    class BluetoothGattStatus extends BluetoothGattCallback {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                int newState) {

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                gatt.close();
                gatt = null;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gatt.getServices();
                bleManager.enableService(TiSensorAttr.UUID_ACC_SERV);
                bleManager.addServicePeriod(TiSensorAttr.UUID_ACC_SERV, 15);
                
                bleManager.addServiceConfigValue(TaskType.CONFIG,
                        TiSensorAttr.UUID_ACC_SERV, TiAccelerometer.RANGE_8G_ENABLE);
            } else {
                Log.w(TAG, "onServiceDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                bleManager.notifyStateChanged(
                        BluetoothLeTaskState.CHARACTERISTIC_WRITE, characteristic);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                bleManager.notifyStateChanged(
                        BluetoothLeTaskState.CHARACTERISTIC_READ, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic) {
            bleManager.notifyStateChanged(BluetoothLeTaskState.CHARACTERISTIC_CHANGED,
                    characteristic);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                bleManager.notifyStateChanged(BluetoothLeTaskState.DESCRIPTOR_WRITE, descriptor);
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Remote RSSI: " + rssi);
            }
        }
    }

}
