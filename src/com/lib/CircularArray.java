
package com.lib;


public abstract class CircularArray {
    int pointer = -1;
    int capacity = 0;
    boolean capacityReached = false;

    public void setPointer(int point) {
        pointer = point;
    }

    public void setCapacity(int cap) {
        capacity = cap;
    }

    int getCapacity() {
        return capacity;
    }

    int getPointer() {
        return pointer;
    }

    public void clear() {
        pointer = -1;
        capacityReached = false;
    }

    public double getStdErr() {
        return getStdDev() / Math.sqrt(size());
    }

    protected void increment(boolean adding) {
        pointer++;
    }

    protected void decrement(boolean adding) {
        pointer--;
    }

    public abstract double getAverage();

    public abstract double getStdDev();

    public abstract int size();

    public abstract String getLRU();

    public abstract String getMRU();

}
