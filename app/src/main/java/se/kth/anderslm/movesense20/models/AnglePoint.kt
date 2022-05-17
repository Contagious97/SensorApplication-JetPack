package se.kth.anderslm.movesense20.models;

import java.io.Serializable;

public class AnglePoint implements Serializable {

    long timeMillis;
    double elevationAngle;

    public AnglePoint(double elevationAngle, long timeMillis){
        this.elevationAngle = elevationAngle;
        this.timeMillis = timeMillis;
    }

    public long getTimeMillis() {
        return timeMillis;
    }

    public void setTimeMillis(long timeMillis) {
        this.timeMillis = timeMillis;
    }

    public double getElevationAngle() {
        return elevationAngle;
    }

    public void setElevationAngle(float elevationAngle) {
        this.elevationAngle = elevationAngle;
    }
}
