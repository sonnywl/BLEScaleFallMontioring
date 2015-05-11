
package com.ble.service;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Log;

import com.ble.sensors.ti.TiSensorAttr;
import com.ble.sensors.ti.tasks.TiAcclerometerTask;
import com.ble.service.BluetoothLeServiceTask.BluetoothLeTaskState;
import com.ble.service.BluetoothLeServiceTask.TaskType;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

public class BluetoothLeTaskManager implements IBluetoothLeTaskManagerCallback {
    public static final String TAG = BluetoothLeTaskManager.class.getSimpleName();
    private static BluetoothLeTaskManager manager;
    private IBluetoothServiceCallback callback;

    private LinkedList<BluetoothLeServiceTask> activeTasks;
    private BluetoothGatt gattService;

    private LinkedList<UUID> tasksQueue = new LinkedList<UUID>();
    private HashMap<UUID, LinkedList<EnqueueTask>> enqueuedTaskTypes =
            new HashMap<UUID, LinkedList<EnqueueTask>>();
    private HashMap<UUID, BluetoothLeServiceTask> serviceMap =
            new HashMap<UUID, BluetoothLeServiceTask>();

    public static BluetoothLeTaskManager getInstance(IBluetoothServiceCallback serviceCallback) {
        if (manager == null) {
            manager = new BluetoothLeTaskManager(serviceCallback);
        }
        return manager;
    }

    private BluetoothLeTaskManager(IBluetoothServiceCallback serviceCallback) {
        activeTasks = new LinkedList<BluetoothLeServiceTask>();
        serviceMap.put(TiSensorAttr.UUID_ACC_SERV, new TiAcclerometerTask(this));
        serviceMap.put(TiSensorAttr.UUID_HUM_SERV, null);
        serviceMap.put(TiSensorAttr.UUID_BAR_SERV, null);
        serviceMap.put(TiSensorAttr.UUID_MAG_SERV, null);
        serviceMap.put(TiSensorAttr.UUID_GYR_SERV, null);
        callback = serviceCallback;
    }

    public void notifyStateChanged(
            BluetoothLeTaskState state
            , BluetoothGattCharacteristic characteristic) {
        for (BluetoothLeServiceTask task : activeTasks) {
            if (task.getBleService().getTaskUUIDs().contains(characteristic.getUuid())) {
                task.notifyStateChangedCharacteristic(state, characteristic, gattService);
            }
        }
    }

    public void notifyStateChanged(
            BluetoothLeTaskState state,
            BluetoothGattDescriptor descriptor) {
        for (BluetoothLeServiceTask task : activeTasks) {
            if (task.getBleService().getTaskUUIDs().contains(descriptor.getUuid())) {
                task.notifyStateChangedDescriptor(state, descriptor, gattService);
            }
        }
    }

    @Override
    public void notifyTaskCompletion(BluetoothLeServiceTask task, TaskType type) {
        Log.i(TAG, "task completed " + type + " queued " + tasksQueue.size());
        if (tasksQueue.size() > 0) {
            UUID uuid = tasksQueue.poll();
            BluetoothLeServiceTask nextTask = serviceMap.get(uuid);
            EnqueueTask queuedTask = enqueuedTaskTypes.get(uuid).poll();
            if (queuedTask != null) {
                switch(queuedTask.type) {
                    case PERIODIC:
                        Log.i(TAG, "running next task " + queuedTask.type);
                        nextTask.setTaskType(queuedTask.type);
                        nextTask.setPeriodicValue(queuedTask.periodValue);
                        nextTask.run(gattService);
                        break;
                    case CONFIG:
                        Log.i(TAG, "running next task " + queuedTask.type);
                        nextTask.setTaskType(queuedTask.type);
                        nextTask.setConfigValue(queuedTask.configValue);
                        nextTask.run(gattService);
                        break;
                    default:
                        Log.i(TAG, "task " + queuedTask.type + " not supported ");
                        break;
                }
            }
        }

    }

    @Override
    public void notifyDataChanged(UUID uuid, double[] data) {
        callback.notifyDataChanged(uuid, data);
    }

    public void enableService(UUID uuidServ) {
        BluetoothLeServiceTask task;
        if (activeTasks.size() < 3) {
            if ((task = serviceMap.get(uuidServ)) != null) {
                task.setTaskType(TaskType.ENABLE);
                task.run(gattService);
                activeTasks.add(task);
            }
        }
    }

    public void disableService(UUID uuidServ) {
        BluetoothLeServiceTask task;
        if ((task = serviceMap.get(uuidServ)) != null) {
            task.setTaskType(TaskType.DISABLE);
            task.run(gattService);
            activeTasks.remove(task);
        }
    }

    public void addServicePeriod(UUID uuidServ, int period) {
        BluetoothLeServiceTask task;
        if ((task = serviceMap.get(uuidServ)) != null) {
            if (task.getState() != BluetoothLeTaskState.COMPLETED) {
                if (enqueuedTaskTypes.containsKey(uuidServ)) {
                    enqueuedTaskTypes.get(uuidServ).add(
                            new EnqueueTask(TaskType.PERIODIC, period));
                } else {
                    LinkedList<EnqueueTask> list = new LinkedList<EnqueueTask>();
                    list.add(new EnqueueTask(TaskType.PERIODIC, period));
                    enqueuedTaskTypes.put(uuidServ, list);
                }
                tasksQueue.add(uuidServ);
            } else {
                task.setTaskType(TaskType.PERIODIC);
                task.setPeriodicValue(period);
                task.run(gattService);
            }
        }
    }
    
    public void addServiceConfigValue(
            TaskType taskType, UUID uuidServ, byte[] configValue) {
        BluetoothLeServiceTask task;
        if ((task = serviceMap.get(uuidServ)) != null && taskType == TaskType.CONFIG) {
            if (task.getState() != BluetoothLeTaskState.COMPLETED) {
                if (enqueuedTaskTypes.containsKey(uuidServ)) {
                    enqueuedTaskTypes.get(uuidServ).add(
                            new EnqueueTask(taskType, configValue));
                } else {
                    LinkedList<EnqueueTask> list = new LinkedList<EnqueueTask>();
                    list.add(new EnqueueTask(taskType, configValue));
                    enqueuedTaskTypes.put(uuidServ, list);
                }
                tasksQueue.add(uuidServ);
            } else {
                task.setTaskType(taskType);
                task.setConfigValue(configValue);
                task.run(gattService);
            }
        }        
    }

    public void resumeServices() {
        for (BluetoothLeServiceTask task : activeTasks) {
            task.setTaskType(TaskType.ENABLE);
            task.run(gattService);
        }
    }

    public void disableServices() {
        for (BluetoothLeServiceTask task : activeTasks) {
            task.setTaskType(TaskType.DISABLE);
            task.run(gattService);
        }
    }

    public void setGattCallbackDevice(BluetoothGatt mBluetoothGatt) {
        gattService = mBluetoothGatt;
    }

    static class EnqueueTask {
        final TaskType type;
        final int periodValue;
        final byte[] configValue;

        public EnqueueTask(TaskType task, int period) {
            type = task;
            periodValue = period;
            configValue = BluetoothLeServiceTask.DISABLE;
        }
        
        public EnqueueTask(TaskType task, byte[] config) {
            type = task;
            configValue = config;
            periodValue = 0;
        }

    }

}
