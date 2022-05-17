package se.kth.anderslm.movesense20.serialization;

import android.util.Log;

import com.jjoe64.graphview.series.DataPoint;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class DeserializeFromFile implements Serializable {

    private static final String LOG_TAG =
            DeserializeFromFile.class.getSimpleName();

//    private BtDeviceAdapter btDeviceAdapter;
    private DataPoint time;
    private DataPoint elevation;

    public DeserializeFromFile(DataPoint time, DataPoint elevation) {
        this.time = time;
        this.elevation = elevation;
    }


    public void readFromFile(File path) {
        List<Object> readData = new ArrayList<>();

        try {
            File file = new File(path, "graph_data");
            FileInputStream fileInputStream = new FileInputStream(file);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            readData = (ArrayList<Object>) objectInputStream.readObject();

            fileInputStream.close();
            objectInputStream.close();

        } catch (ClassNotFoundException | IOException e) {
            Log.d(LOG_TAG, "Something went wrong when trying to read from file");
            Log.e(LOG_TAG,e.toString());
            e.printStackTrace();
        }

        if(readData.size() == 0){
            Log.d(LOG_TAG,"File is empty");
        }
        Log.i(LOG_TAG, "The file has successfully been read!");
        try{
            int readTime = (int) readData.get(0);
            double readElevation = (double) readData.get(1);
            Log.d(LOG_TAG, "the time is: " + readTime + " | The elevation is: " + readElevation);
        }catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
            Log.d(LOG_TAG, "Could not post read data");
            e.printStackTrace();
        }
    }
}
