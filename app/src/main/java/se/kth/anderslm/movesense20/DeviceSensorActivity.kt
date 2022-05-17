package se.kth.anderslm.movesense20

import android.Manifest
import se.kth.anderslm.movesense20.serialization.DeserializeFromFile
import se.kth.anderslm.movesense20.models.AnglePoint
import se.kth.anderslm.movesense20.serialization.SerializeToFile
import com.google.gson.Gson
import android.bluetooth.BluetoothDevice
import se.kth.anderslm.movesense20.uiutils.BtDeviceAdapter.IOnItemSelectedCallBack
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import android.view.ViewGroup
import android.view.LayoutInflater
import se.kth.anderslm.movesense20.R
import android.widget.Toast
import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.bluetooth.BluetoothGatt
import android.os.Bundle
import android.content.Intent
import se.kth.anderslm.movesense20.ScanActivity
import se.kth.anderslm.movesense20.uiutils.MsgUtils
import android.os.Environment
import se.kth.anderslm.movesense20.DeviceActivity
import android.content.pm.PackageManager
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import androidx.annotation.RequiresApi
import android.os.Build
import android.annotation.SuppressLint
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.jjoe64.graphview.GraphView
import android.hardware.SensorEvent
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import se.kth.anderslm.movesense20.DeviceSensorActivity
import com.jjoe64.graphview.series.LineGraphSeries
import android.bluetooth.BluetoothAdapter
import se.kth.anderslm.movesense20.uiutils.BtDeviceAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.app.Activity
import android.graphics.Color
import android.hardware.Sensor
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Button
import com.jjoe64.graphview.series.DataPoint
import se.kth.anderslm.movesense20.timeAndElevation
import java.util.ArrayList

class DeviceSensorActivity : AppCompatActivity(), SensorEventListener {
    private val LOG_TAG = "Device Sensor Activity"
    private var isRecording = false
    private var mHandler: Handler? = null
    private var dataView: TextView? = null
    private var deviceView: TextView? = null
    private var accAndGyroView: TextView? = null
    private var recordButton: Button? = null
    private var mSensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var graphView: GraphView? = null
    private var prevAccValue = 0.0
    private var prevFilteredAccValue = 0.0
    private var prevCombinedValue = 0.0
    private var prevX = 0f
    private var prevY = 0f
    private var prevZ = 0f
    private var prevGyroY = 0f
    private val prevFilteredGyroValue = 0.0
    private var accValuesList: MutableList<AnglePoint>? = null
    private var combinedValuesList: MutableList<AnglePoint>? = null
    private var startTime: Long = 0
    override fun onSensorChanged(sensorEvent: SensorEvent) {
        updateSensorValues(sensorEvent)
    }

    override fun onAccuracyChanged(sensor: Sensor, i: Int) {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_sensor)
        dataView = findViewById(R.id.data_view)
        accAndGyroView = findViewById(R.id.acc_and_gyro_view)
        deviceView = findViewById(R.id.device_view)
        graphView = findViewById<View>(R.id.graphView) as GraphView
        recordButton = findViewById(R.id.start_record_button)
        mHandler = Handler()
        recordButton.setOnClickListener(View.OnClickListener {
            if (!isRecording) {
                startRecording()
                MsgUtils.showToast("Started recording", this@DeviceSensorActivity)
            } else {
                isRecording = false
                MsgUtils.showToast("Stopped recording", this@DeviceSensorActivity)
                updateGraph()
            }
        })
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = mSensorManager!!.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        mSensorManager!!.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager!!.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL)
        accValuesList = ArrayList()
        combinedValuesList = ArrayList()
    }

    private fun checkPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(
            this@DeviceSensorActivity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this@DeviceSensorActivity,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.e("value", "Permission Granted, Now you can use local drive .")
                SerializeToFile.writeToFile(
                    accValuesList,
                    getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    FILE_NAME_ACC
                )
                SerializeToFile.writeToFile(
                    combinedValuesList,
                    getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    FILE_NAME_COMBINED
                )
            } else {
                Log.e("value", "Permission Denied, You cannot use local drive .")
            }
        }
    }

    private fun startRecording() {
        if (!isRecording) {
            mHandler!!.postDelayed({
                if (isRecording) {
                    isRecording = false
                    MsgUtils.showToast(
                        "Stopped recording data after 10s",
                        this@DeviceSensorActivity
                    )
                    updateGraph()
                }
            }, RECORDING_LIMIT)
            combinedValuesList!!.clear()
            accValuesList!!.clear()
            isRecording = true
            startTime = System.currentTimeMillis()
            graphView!!.removeAllSeries()
        }
    }

    private fun updateGraph() {
        val accValuesSeries = LineGraphSeries<DataPoint>()
        val combinedValuesSeries = LineGraphSeries<DataPoint>()
        accValuesSeries.title = "Angle from acc"
        combinedValuesSeries.title = "Angle from acc + gyro"
        accValuesSeries.color = Color.BLUE
        combinedValuesSeries.color = Color.RED
        for (a in accValuesList!!) {
            accValuesSeries.appendData(DataPoint(a.timeMillis, a.elevationAngle), true, 180)
        }
        for (a in combinedValuesList!!) {
            combinedValuesSeries.appendData(DataPoint(a.timeMillis, a.elevationAngle), true, 180)
        }
        if (checkPermission()) {
            Log.d(LOG_TAG, "Permission is present")
            SerializeToFile.writeToFile(
                accValuesList,
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                FILE_NAME_ACC
            )
            SerializeToFile.writeToFile(
                combinedValuesList,
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                FILE_NAME_COMBINED
            )
        } else {
            Log.d(LOG_TAG, "Permission doesn't exist")
            requestPermission()
        }
        graphView!!.addSeries(accValuesSeries)
        graphView!!.addSeries(combinedValuesSeries)
        graphView!!.viewport.setMinX(0.0)
        graphView!!.viewport.setMaxX(10000.0)
        graphView!!.viewport.setMinY(-100.0)
        graphView!!.viewport.setMaxY(100.0)
        graphView!!.viewport.isYAxisBoundsManual = true
        graphView!!.viewport.isXAxisBoundsManual = true
        graphView!!.viewport.isScrollable = true
        graphView!!.title = "Angle change based on time"
        graphView!!.gridLabelRenderer.horizontalAxisTitle = "Time in ms"
        graphView!!.gridLabelRenderer.verticalAxisTitle = "Angle in degrees"
    }

    override fun onResume() {
        super.onResume()
        mSensorManager!!.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager!!.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        mSensorManager!!.unregisterListener(this, accelerometer)
        mSensorManager!!.unregisterListener(this, gyroscope)
    }

    override fun onStop() {
        super.onStop()
    }

    private fun updateSensorValues(sensorEvent: SensorEvent) {
        val sensor = sensorEvent.sensor
        if (sensor.type == Sensor.TYPE_ACCELEROMETER) {
            prevX =
                (FILTER_VALUE_ACC * prevX + (1 - FILTER_VALUE_ACC) * sensorEvent.values[0]).toFloat()
            prevY =
                (FILTER_VALUE_ACC * prevY + (1 - FILTER_VALUE_ACC) * sensorEvent.values[1]).toFloat()
            prevZ =
                (FILTER_VALUE_ACC * prevZ + (1 - FILTER_VALUE_ACC) * sensorEvent.values[2]).toFloat()
            prevAccValue =
                180 / Math.PI * Math.atan(prevX / Math.sqrt((prevY * prevY + prevZ * prevZ).toDouble()))
            //prevAccValue = Math.abs(Math.atan2(prevY,prevZ)*(180/Math.PI));
            //Log.i(LOG_TAG, "Unfiltered: " + String.valueOf(prevAccValue));
            prevFilteredAccValue =
                FILTER_VALUE_ACC * prevFilteredAccValue + (1 - FILTER_VALUE_ACC) * prevAccValue

            //Log.i(LOG_TAG,"Filtered: " + String.valueOf(prevFilteredAccValue));
            dataView!!.setText(R.string.acc_pitch)
            dataView!!.append(" ")
            dataView!!.append(String.format("%.3f", prevAccValue))
            dataView!!.append(" degrees")
        } else if (sensor.type == Sensor.TYPE_GYROSCOPE) {
            //prevGyroY = (float)(FILTER_VALUE*prevGyroY +(1-FILTER_VALUE)*sensorEvent.values[1]);
            prevGyroY = sensorEvent.values[1]
            prevCombinedValue =
                (1 - FILTER_VALUE_COMB) * (prevCombinedValue + prevGyroY) + FILTER_VALUE_COMB * prevAccValue
            accAndGyroView!!.setText(R.string.comm_pitch)
            dataView!!.append(" ")
            accAndGyroView!!.append(String.format("%.3f", prevCombinedValue))
            accAndGyroView!!.append(" degrees")
        }
        if (isRecording) {
            accValuesList!!.add(AnglePoint(prevAccValue, System.currentTimeMillis() - startTime))
            combinedValuesList!!.add(
                AnglePoint(
                    prevCombinedValue,
                    System.currentTimeMillis() - startTime
                )
            )
        }
    }

    companion object {
        private const val RECORDING_LIMIT: Long = 10000
        private const val FILTER_VALUE_ACC = 0.1
        private const val FILTER_VALUE_COMB = 0.5
        private const val PERMISSION_REQUEST_CODE = 1
        private const val FILE_NAME_ACC = "acc-data.json"
        private const val FILE_NAME_COMBINED = "comb-data.json"
    }
}