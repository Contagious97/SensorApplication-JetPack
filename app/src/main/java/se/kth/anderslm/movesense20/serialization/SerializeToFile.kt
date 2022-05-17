package se.kth.anderslm.movesense20.serialization;

import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import se.kth.anderslm.movesense20.models.AnglePoint;
import se.kth.anderslm.movesense20.timeAndElevation;

public class SerializeToFile {

    private static final String LOG_TAG =
            SerializeToFile.class.getSimpleName();

    public static void writeToFile(List<AnglePoint> dataList,File folderPath, String fileName) {
       /* List<Object> listOfData = new ArrayList<>();
        Log.d(LOG_TAG,"Time and elevation: " + timeAndElevation.getTime() + "s |" +  timeAndElevation.getElevation() + "°");
        listOfData.add(timeAndElevation.getTime());
        listOfData.add(timeAndElevation.getElevation());*/
        Log.d(LOG_TAG,"Folder path: " + folderPath.getAbsolutePath());
        try {
            File myFilePath = new File(folderPath,fileName);
            if(!myFilePath.exists()){
                myFilePath.createNewFile();
            }
            Gson gson = new Gson();
            String json = gson.toJson(dataList);
            Log.d(LOG_TAG,json);
            FileOutputStream fileOutputStream = new FileOutputStream(myFilePath);
            fileOutputStream.write(json.getBytes());
            fileOutputStream.close();

//            Log.i(LOG_TAG,"The list of objects were successfully written to a file");

        } catch (Exception ex) {
            Log.e(LOG_TAG, "Exception thrown in writeToFile: " + ex.toString());
            Log.d(LOG_TAG,"Could not write to file");
            ex.printStackTrace();
        }
    }
}
