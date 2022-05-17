package se.kth.anderslm.movesense20

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
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Button
import se.kth.anderslm.movesense20.timeAndElevation
import se.kth.anderslm.movesense20.utils.TypeConverter
import java.lang.Exception
import java.text.DecimalFormat
import java.util.*

class DeviceActivity : AppCompatActivity() {
    private var accPitch = 0.0
    private var comPitch = 0.0
    private var prevX = 0.0
    private var prevY = 0.0
    private var prevZ = 0.0
    private var prevGyroY = 0.0
    private var startTime: Long = 0
    private var accValuesList: MutableList<AnglePoint>? = null
    private var combinedValuesList: MutableList<AnglePoint>? = null
    private var recordButton: Button? = null
    private var isRecording = false
    private val IMU_COMMAND2 = "Meas/IMU6/13" // see documentation
    private val MOVESENSE_REQUEST: Byte = 1
    private val MOVESENSE_RESPONSE: Byte = 2
    private val REQUEST_ID: Byte = 99
    private var mSelectedDevice: BluetoothDevice? = null
    private var mBluetoothGatt: BluetoothGatt? = null
    private var mHandler: Handler? = null
    private var mDeviceView: TextView? = null
    private var mDataView: TextView? = null
    private var comPitchView: TextView? = null
    private var accPitchView: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device)
        mDeviceView = findViewById(R.id.device_view)
        mDataView = findViewById(R.id.data_view)
        comPitchView = findViewById(R.id.comPitch_view)
        accPitchView = findViewById(R.id.accPitch_view)
        recordButton = findViewById(R.id.record_button)
        accValuesList = ArrayList()
        combinedValuesList = ArrayList()
        val intent = intent
        // Get the selected device from the intent
        mSelectedDevice = intent.getParcelableExtra(ScanActivity.Companion.SELECTED_DEVICE)
        if (mSelectedDevice == null) {
            MsgUtils.createDialog("Error", "No device found", this).show()
            mDeviceView.setText(R.string.no_device)
        } else {
            mDeviceView.setText(mSelectedDevice!!.name)
        }
        mHandler = Handler()
    }

    fun startRecording(view: View?) {
        if (!isRecording) {
            mHandler!!.postDelayed({
                if (isRecording) {
                    isRecording = false
                    MsgUtils.showToast("Stopped recording data after 10s", this@DeviceActivity)
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
                    recordButton!!.setText(R.string.start_record)
                }
            }, RECORDING_LIMIT)
            combinedValuesList!!.clear()
            accValuesList!!.clear()
            isRecording = true
            startTime = System.currentTimeMillis()
        } else {
            isRecording = false
            MsgUtils.showToast("Stopped recording", this@DeviceActivity)
            recordButton!!.setText(R.string.start_record)
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
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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

    override fun onStart() {
        super.onStart()
        if (mSelectedDevice != null) {
            // Connect and register call backs for bluetooth gatt
            mBluetoothGatt = mSelectedDevice!!.connectGatt(this, false, mBtGattCallback)
        }
    }

    override fun onStop() {
        super.onStop()
        if (mBluetoothGatt != null) {
            mBluetoothGatt!!.disconnect()
            try {
                mBluetoothGatt!!.close()
            } catch (e: Exception) {
                // ugly, but this is to handle a bug in some versions in the Android BLE API
            }
        }
    }

    /**
     * Callbacks for bluetooth gatt changes/updates
     * The documentation is not always clear, but most callback methods seems to
     * be executed on a worker thread - hence use a Handler when updating the ui.
     */
    private val mBtGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                mBluetoothGatt = gatt
                mHandler!!.post { mDataView!!.setText(R.string.connected) }
                // Discover services
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // Close connection and display info in ui
                mBluetoothGatt = null
                mHandler!!.post { mDataView!!.setText(R.string.disconnected) }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Debug: list discovered services
                val services = gatt.services
                for (service in services) {
                    Log.i(LOG_TAG, service.uuid.toString())
                }

                // Get the Movesense 2.0 IMU service
                val movesenseService = gatt.getService(MOVESENSE_2_0_SERVICE)
                if (movesenseService != null) {
                    // debug: service present, list characteristics
                    val characteristics = movesenseService.characteristics
                    for (chara in characteristics) {
                        Log.i(LOG_TAG, chara.uuid.toString())
                    }

                    // Write a command, as a byte array, to the command characteristic
                    // Callback: onCharacteristicWrite
                    val commandChar = movesenseService.getCharacteristic(
                        MOVESENSE_2_0_COMMAND_CHARACTERISTIC
                    )

                    // command example: 1, 99, "/Meas/Acc/13"
                    val command = TypeConverter.stringToAsciiArray(REQUEST_ID, IMU_COMMAND2)
                    commandChar.value = command
                    val wasSuccess = mBluetoothGatt!!.writeCharacteristic(commandChar)
                    Log.i("writeCharacteristic", "was success=$wasSuccess")
                } else {
                    mHandler!!.post {
                        MsgUtils.createDialog(
                            "Alert!",
                            getString(R.string.service_not_found),
                            this@DeviceActivity
                        )
                            .show()
                    }
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            Log.i(LOG_TAG, "onCharacteristicWrite " + characteristic.uuid.toString())

            // Enable notifications on data from the sensor. First: Enable receiving
            // notifications on the client side, i.e. on this Android device.
            val movesenseService = gatt.getService(MOVESENSE_2_0_SERVICE)
            val dataCharacteristic = movesenseService.getCharacteristic(
                MOVESENSE_2_0_DATA_CHARACTERISTIC
            )
            // second arg: true, notification; false, indication
            val success = gatt.setCharacteristicNotification(dataCharacteristic, true)
            if (success) {
                Log.i(LOG_TAG, "setCharactNotification success")
                // Second: set enable notification server side (sensor). Why isn't
                // this done by setCharacteristicNotification - a flaw in the API?
                val descriptor = dataCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor) // callback: onDescriptorWrite
            } else {
                Log.i(LOG_TAG, "setCharacteristicNotification failed")
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            Log.i(LOG_TAG, "onDescriptorWrite, status $status")
            if (CLIENT_CHARACTERISTIC_CONFIG == descriptor.uuid) if (status == BluetoothGatt.GATT_SUCCESS) {
                // if success, we should receive data in onCharacteristicChanged
                mHandler!!.post { mDeviceView!!.setText(R.string.notifications_enabled) }
            }
        }

        /**
         * Callback called on characteristic changes, e.g. when a sensor data value is changed.
         * This is where we receive notifications on new sensor data.
         */
        @RequiresApi(api = Build.VERSION_CODES.M)
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            // debug
            // Log.i(LOG_TAG, "onCharacteristicChanged " + characteristic.getUuid());

            // if response and id matches
            if (MOVESENSE_2_0_DATA_CHARACTERISTIC == characteristic.uuid) {
                val data = characteristic.value
                //Log.i(LOG_TAG,"Response: " + data[0]);
                if (data[0] == MOVESENSE_RESPONSE && data[1] == REQUEST_ID) {
                    // NB! use length of the array to determine the number of values in this
                    // "packet", the number of values in the packet depends on the frequency set(!)
                    val len = data.size
                    //                    Log.i(LOG_TAG,"Data: " + Arrays.toString(data) + "data length: " + len);
                    if (len <= 30) {

                        //Log.d(LOG_TAG, "------------------------------" + "\n" + "The data length is: " + len);
                        //Log.d(LOG_TAG, "this is the data " + Arrays.toString(data));

                        // parse and interpret the data, ...
                        val time = TypeConverter.fourBytesToInt(data, 2)
                        val accX = TypeConverter.fourBytesToFloat(data, 6)
                        val accY = TypeConverter.fourBytesToFloat(data, 10)
                        val accZ = TypeConverter.fourBytesToFloat(data, 14)
                        val yGyro = TypeConverter.fourBytesToFloat(data, 22)

//                        prevAccX = accX;
//                        prevAccY = accY;
//                        prevAccZ = accZ;
                        prevX =
                            (FILTER_VALUE * prevX + (1 - FILTER_VALUE) * accX) as Float.toDouble()
                        prevY =
                            (FILTER_VALUE * prevY + (1 - FILTER_VALUE) * accY) as Float.toDouble()
                        prevZ =
                            (FILTER_VALUE * prevZ + (1 - FILTER_VALUE) * accZ) as Float.toDouble()
                        accPitch = 180 / Math.PI * Math.atan(
                            prevX / Math.sqrt(
                                Math.pow(
                                    prevY,
                                    2.0
                                ) + Math.pow(prevZ, 2.0)
                            )
                        )
                        prevGyroY =
                            (FILTER_VALUE * prevGyroY + (1 - FILTER_VALUE) * yGyro) as Float.toDouble()
                        comPitch =
                            (1 - FILTER_VALUE_COMB) * (comPitch + 0.07 * prevGyroY) + FILTER_VALUE_COMB * accPitch

                        // ... and then, filter data, calculate something interesting,
                        // ... display a graph or show values, ...
                        @SuppressLint("DefaultLocale") val viewDataStr =
                            String.format("%.2f, %.2f, %.2f", accX, accY, accZ)
                        mHandler!!.post {
                            mDeviceView!!.text = "$time ms"
                            mDataView!!.text = viewDataStr
                            comPitchView!!.text = "comPitch: " + df.format(comPitch)
                            accPitchView!!.text = "accPitch: " + df.format(accPitch)
                            if (isRecording) {
                                recordButton!!.text = "Stop Recording"
                                accValuesList!!.add(
                                    AnglePoint(
                                        accPitch,
                                        System.currentTimeMillis() - startTime
                                    )
                                )
                                combinedValuesList!!.add(
                                    AnglePoint(
                                        comPitch,
                                        System.currentTimeMillis() - startTime
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            Log.i(LOG_TAG, "onCharacteristicRead " + characteristic.uuid.toString())
        }
    }

    companion object {
        // Movesense 2.0 UUIDs (should be placed in resources file)
        val MOVESENSE_2_0_SERVICE = UUID.fromString("34802252-7185-4d5d-b431-630e7050e8f0")
        val MOVESENSE_2_0_COMMAND_CHARACTERISTIC =
            UUID.fromString("34800001-7185-4d5d-b431-630e7050e8f0")
        val MOVESENSE_2_0_DATA_CHARACTERISTIC =
            UUID.fromString("34800002-7185-4d5d-b431-630e7050e8f0")

        // UUID for the client characteristic, which is necessary for notifications
        val CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private const val FILTER_VALUE = 0.1
        private const val FILTER_VALUE_COMB = 0.5
        private const val RECORDING_LIMIT: Long = 10000
        private val df = DecimalFormat("0.00")
        private const val PERMISSION_REQUEST_CODE = 1
        private const val FILE_NAME_ACC = "accSens-data.json"
        private const val FILE_NAME_COMBINED = "combSens-data.json"
        private const val LOG_TAG = "DeviceActivity"
    }
}