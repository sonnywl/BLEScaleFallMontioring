
package com.ble.service;

import com.ble.service.BluetoothLeServiceTask.TaskType;

import java.util.UUID;

/**
 * Call back interface to BluetoothLeTaskManager to notify when task is
 * completed
 * 
 * @author Sonny
 */
public interface IBluetoothLeTaskManagerCallback {
    /**
     * Notifies the Task Manager that the initial task is completed
     * 
     * @param task - Access to the task itself
     * @param type - Type of task at hand
     */
    public void notifyTaskCompletion(BluetoothLeServiceTask task, TaskType type);

    /**
     * Returns double[] data for the particular task.
     * 
     * @param data
     * @return
     */
    public void notifyDataChanged(UUID uuid, double[] data);
}
