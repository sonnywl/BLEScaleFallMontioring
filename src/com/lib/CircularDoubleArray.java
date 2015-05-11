
package com.lib;

public class CircularDoubleArray extends CircularArray {
    final double[] dataSet;

    public CircularDoubleArray(int size) {
        dataSet = new double[size];
        capacity = size;
    }

    public void add(double data) {
        increment(true);
        dataSet[pointer] = data;
    }

    public double getStdDev() {
        int size = size();
        if (size == 0) {
            return 0;
        }
        return getStdDev(getAverage());
    }

    public double getStdDev(double avg) {
        int size = size();
        if (size == 0) {
            return 0;
        }

        double total = 0;
        int loc = pointer;
        for (int i = 0; i < size; i++) {
            if (loc < 0) {
                if (capacityReached) {
                    loc = dataSet.length - 1;
                } else {
                    break;
                }
            }
            total += Math.pow(dataSet[i] - avg, 2);
            loc--;
        }
        if (size() - 1 == 0) {
            return 0;
        }
        return Math.sqrt(total / (size() - 1));
    }

    public double getAverage() {
        double avg = 0;
        long total;
        if (capacityReached) {
            for (int i = 0; i < dataSet.length; i++) {
                avg += dataSet[i];
            }
        } else {
            for (int i = 0; i <= pointer; i++) {
                avg += dataSet[i];
            }
        }
        total = size();
        if (total == 0) {
            total = 1;
        }
        return avg / total;
    }

    @Override
    public String getLRU() {
        StringBuilder sb = new StringBuilder();
        if (pointer >= 0) {
            int initpointer = pointer;
            do {
                increment(false);
                sb.append(dataSet[pointer]);
                sb.append(",");
            } while (initpointer != pointer);
        }

        return sb.toString();
    }

    @Override
    public String getMRU() {
        StringBuilder sb = new StringBuilder();
        if (pointer >= 0) {
            int initpointer = pointer;
            do {
                sb.append(dataSet[pointer]);
                sb.append(",");
                decrement(false);
            } while (initpointer != pointer);
        }
        return sb.toString();
    }

    @Override
    protected void increment(boolean adding) {
        super.increment(adding);
        if (adding) {
            if (pointer >= capacity) {
                pointer = 0;
                capacityReached = true;
            }
        } else {
            if (pointer >= dataSet.length) {
                pointer = 0;
            }
        }
    }

    @Override
    protected void decrement(boolean adding) {
        super.decrement(adding);
        if (pointer < 0) {
            if (adding) {
                pointer = capacity - 1;
            } else {
                pointer = dataSet.length - 1;
            }
        }
    }

    public double getHead() {
        if (pointer >= 0) {
            return dataSet[pointer];
        }
        return -1;
    }

    public int size() {
        if (capacityReached) {
            return dataSet.length;
        }
        return pointer + 1;
    }

    public void reset(double resetValue) {
        for(int i = 0; i < dataSet.length; i++) {
            dataSet[i] = resetValue;
        }
    }

}
