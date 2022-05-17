package se.kth.anderslm.movesense20;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static se.kth.anderslm.movesense20.uiutils.MsgUtils.showToast;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.List;

import se.kth.anderslm.movesense20.models.AnglePoint;
import se.kth.anderslm.movesense20.serialization.SerializeToFile;

public class DeviceSensorActivity extends AppCompatActivity implements SensorEventListener {

    private final String LOG_TAG = "Device Sensor Activity";
    private static final long RECORDING_LIMIT = 10000;
    private boolean isRecording;
    private Handler mHandler;
    private TextView dataView;
    private TextView deviceView;
    private TextView accAndGyroView;
    private Button recordButton;
    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private GraphView graphView;

    private static final double FILTER_VALUE_ACC = 0.1;
    private static final double FILTER_VALUE_COMB = 0.5;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String FILE_NAME_ACC = "acc-data.json";
    private static final String FILE_NAME_COMBINED = "comb-data.json";

    private double prevAccValue;
    private double prevFilteredAccValue;
    private double prevCombinedValue;
    private float prevX;
    private float prevY;
    private float prevZ;
    private float prevGyroY;


    private double prevFilteredGyroValue;
    private List<AnglePoint> accValuesList;
    private List<AnglePoint> combinedValuesList;
    private long startTime;


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        updateSensorValues(sensorEvent);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_sensor);
        dataView = findViewById(R.id.data_view);
        accAndGyroView = findViewById(R.id.acc_and_gyro_view);
        deviceView = findViewById(R.id.device_view);
        graphView = (GraphView) findViewById(R.id.graphView);
        recordButton = findViewById(R.id.start_record_button);
        mHandler = new Handler();

        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isRecording){
                    startRecording();
                    showToast("Started recording",DeviceSensorActivity.this);
                } else{
                    isRecording = false;
                    showToast("Stopped recording",DeviceSensorActivity.this);
                    updateGraph();
                }
            }
        });


        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        mSensorManager.registerListener(this,accelerometer,SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this,gyroscope,SensorManager.SENSOR_DELAY_NORMAL);
        accValuesList = new ArrayList<>();
        combinedValuesList = new ArrayList<>();
    }


    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(DeviceSensorActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(DeviceSensorActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e("value", "Permission Granted, Now you can use local drive .");
                    SerializeToFile.writeToFile(accValuesList,getExternalFilesDir(DIRECTORY_DOWNLOADS),FILE_NAME_ACC);
                    SerializeToFile.writeToFile(combinedValuesList,getExternalFilesDir(DIRECTORY_DOWNLOADS),FILE_NAME_COMBINED);

                } else {
                    Log.e("value", "Permission Denied, You cannot use local drive .");
                }
                break;
        }
    }

    private void startRecording() {
        if (!isRecording){
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isRecording) {
                        isRecording = false;
                        showToast("Stopped recording data after 10s", DeviceSensorActivity.this);
                        updateGraph();
                    }
                }
            }, RECORDING_LIMIT);

            combinedValuesList.clear();
            accValuesList.clear();
            isRecording = true;
            startTime = System.currentTimeMillis();
            graphView.removeAllSeries();
        }
    }

    private void updateGraph(){

        LineGraphSeries<DataPoint> accValuesSeries = new LineGraphSeries<>();
        LineGraphSeries<DataPoint> combinedValuesSeries = new LineGraphSeries<>();
        accValuesSeries.setTitle("Angle from acc");
        combinedValuesSeries.setTitle("Angle from acc + gyro");
        accValuesSeries.setColor(Color.BLUE);
        combinedValuesSeries.setColor(Color.RED);

        for (AnglePoint a:
             accValuesList) {
            accValuesSeries.appendData(new DataPoint(a.getTimeMillis(),a.getElevationAngle()),true,180);
        }
        for (AnglePoint a:
                combinedValuesList) {
            combinedValuesSeries.appendData(new DataPoint(a.getTimeMillis(),a.getElevationAngle()),true,180);
        }

        if (checkPermission()){
            Log.d(LOG_TAG,"Permission is present");
            SerializeToFile.writeToFile(accValuesList,getExternalFilesDir(DIRECTORY_DOWNLOADS),FILE_NAME_ACC);
            SerializeToFile.writeToFile(combinedValuesList,getExternalFilesDir(DIRECTORY_DOWNLOADS),FILE_NAME_COMBINED);
        } else {
            Log.d(LOG_TAG,"Permission doesn't exist");
            requestPermission();
        }

        graphView.addSeries(accValuesSeries);
        graphView.addSeries(combinedValuesSeries);
        graphView.getViewport().setMinX(0);
        graphView.getViewport().setMaxX(10000);
        graphView.getViewport().setMinY(-100);
        graphView.getViewport().setMaxY(100);

        graphView.getViewport().setYAxisBoundsManual(true);
        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setScrollable(true);
        graphView.setTitle("Angle change based on time");
        graphView.getGridLabelRenderer().setHorizontalAxisTitle("Time in ms");
        graphView.getGridLabelRenderer().setVerticalAxisTitle("Angle in degrees");

    }


    @Override
    protected void onResume(){
        super.onResume();
        mSensorManager.registerListener(this,accelerometer,SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this,gyroscope,SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause(){
        super.onPause();
        mSensorManager.unregisterListener(this,accelerometer);
        mSensorManager.unregisterListener(this,gyroscope);

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void updateSensorValues(SensorEvent sensorEvent){
        Sensor sensor = sensorEvent.sensor;
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            prevX = (float)(FILTER_VALUE_ACC *prevX +(1- FILTER_VALUE_ACC)*sensorEvent.values[0]);
            prevY = (float)(FILTER_VALUE_ACC *prevY +(1- FILTER_VALUE_ACC)*sensorEvent.values[1]);
            prevZ = (float)(FILTER_VALUE_ACC *prevZ +(1- FILTER_VALUE_ACC)*sensorEvent.values[2]);

            prevAccValue = (180/Math.PI)*Math.atan(prevX/(Math.sqrt(prevY*prevY + prevZ*prevZ)));
            //prevAccValue = Math.abs(Math.atan2(prevY,prevZ)*(180/Math.PI));
            //Log.i(LOG_TAG, "Unfiltered: " + String.valueOf(prevAccValue));

            prevFilteredAccValue = FILTER_VALUE_ACC * prevFilteredAccValue + (1- FILTER_VALUE_ACC)*prevAccValue;

            //Log.i(LOG_TAG,"Filtered: " + String.valueOf(prevFilteredAccValue));

            dataView.setText(R.string.acc_pitch);
            dataView.append(" ");
            dataView.append(String.format("%.3f",prevAccValue));
            dataView.append(" degrees");
        }
        else if (sensor.getType() == Sensor.TYPE_GYROSCOPE){
            //prevGyroY = (float)(FILTER_VALUE*prevGyroY +(1-FILTER_VALUE)*sensorEvent.values[1]);
            prevGyroY = sensorEvent.values[1];
            prevCombinedValue = (1- FILTER_VALUE_COMB)*(prevCombinedValue + prevGyroY) + FILTER_VALUE_COMB *prevAccValue;

            accAndGyroView.setText(R.string.comm_pitch);
            dataView.append(" ");
            accAndGyroView.append(String.format("%.3f",prevCombinedValue));
            accAndGyroView.append(" degrees");
        }

        if (isRecording){
            accValuesList.add(new AnglePoint(prevAccValue,System.currentTimeMillis()-startTime));
            combinedValuesList.add(new AnglePoint(prevCombinedValue,System.currentTimeMillis()-startTime));
        }
    }
}
