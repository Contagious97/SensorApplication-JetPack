package se.kth.anderslm.movesense20;

public class timeAndElevation {
    private static int time;
    private static double elevation;

    private timeAndElevation() {
    }

    public static int getTime() {
        return time;
    }

    public static void setTime(int time) {
        timeAndElevation.time = time;
    }

    public static double getElevation() {
        return elevation;
    }

    public static void setElevation(double elevation) {
        timeAndElevation.elevation = elevation;
    }

}
