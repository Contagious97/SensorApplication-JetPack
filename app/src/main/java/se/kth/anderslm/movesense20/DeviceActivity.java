package se.kth.anderslm.movesense20;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static se.kth.anderslm.movesense20.uiutils.MsgUtils.showToast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import se.kth.anderslm.movesense20.models.AnglePoint;
import se.kth.anderslm.movesense20.serialization.SerializeToFile;
import se.kth.anderslm.movesense20.uiutils.MsgUtils;
import se.kth.anderslm.movesense20.utils.TypeConverter;

public class DeviceActivity extends AppCompatActivity {

    // Movesense 2.0 UUIDs (should be placed in resources file)
    public static final UUID MOVESENSE_2_0_SERVICE =
            UUID.fromString("34802252-7185-4d5d-b431-630e7050e8f0");
    public static final UUID MOVESENSE_2_0_COMMAND_CHARACTERISTIC =
            UUID.fromString("34800001-7185-4d5d-b431-630e7050e8f0");
    public static final UUID MOVESENSE_2_0_DATA_CHARACTERISTIC =
            UUID.fromString("34800002-7185-4d5d-b431-630e7050e8f0");
    // UUID for the client characteristic, which is necessary for notifications
    public static final UUID CLIENT_CHARACTERISTIC_CONFIG =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final double FILTER_VALUE = 0.1;
    private static final double FILTER_VALUE_COMB = 0.5;

    private double accPitch;
    private double comPitch;
    private double prevX;
    private double prevY;
    private double prevZ;
    private double prevGyroY;
    private static final long RECORDING_LIMIT = 10000;

    private long startTime;
    private List<AnglePoint> accValuesList;
    private List<AnglePoint> combinedValuesList;

    private Button recordButton;
    private boolean isRecording;

    private static final DecimalFormat df = new DecimalFormat("0.00");

    private final String IMU_COMMAND2 = "Meas/IMU6/13"; // see documentation
    private final byte MOVESENSE_REQUEST = 1, MOVESENSE_RESPONSE = 2, REQUEST_ID = 99;

    private BluetoothDevice mSelectedDevice = null;
    private BluetoothGatt mBluetoothGatt = null;

    private Handler mHandler;

    private TextView mDeviceView;
    private TextView mDataView;
    private TextView comPitchView;
    private TextView accPitchView;

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String FILE_NAME_ACC = "accSens-data.json";
    private static final String FILE_NAME_COMBINED = "combSens-data.json";

    private static final String LOG_TAG = "DeviceActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        mDeviceView = findViewById(R.id.device_view);
        mDataView = findViewById(R.id.data_view);
        comPitchView = findViewById(R.id.comPitch_view);
        accPitchView = findViewById(R.id.accPitch_view);
        recordButton = findViewById(R.id.record_button);
        accValuesList = new ArrayList<>();
        combinedValuesList = new ArrayList<>();

        Intent intent = getIntent();
        // Get the selected device from the intent
        mSelectedDevice = intent.getParcelableExtra(ScanActivity.SELECTED_DEVICE);
        if (mSelectedDevice == null) {
            MsgUtils.createDialog("Error", "No device found", this).show();
            mDeviceView.setText(R.string.no_device);
        } else {
            mDeviceView.setText(mSelectedDevice.getName());
        }

        mHandler = new Handler();
    }

    public void startRecording(View view) {
        if (!isRecording) {
            mHandler.postDelayed(() -> {
                if (isRecording) {
                    isRecording = false;
                    showToast("Stopped recording data after 10s", DeviceActivity.this);
                    SerializeToFile.writeToFile(accValuesList, getExternalFilesDir(DIRECTORY_DOWNLOADS), FILE_NAME_ACC);
                    SerializeToFile.writeToFile(combinedValuesList, getExternalFilesDir(DIRECTORY_DOWNLOADS), FILE_NAME_COMBINED);
                    recordButton.setText(R.string.start_record);
                }
            }, RECORDING_LIMIT);

            combinedValuesList.clear();
            accValuesList.clear();
            isRecording = true;
            startTime = System.currentTimeMillis();
        }
        else{
            isRecording = false;
            showToast("Stopped recording",DeviceActivity.this);
            recordButton.setText(R.string.start_record);
            SerializeToFile.writeToFile(accValuesList, getExternalFilesDir(DIRECTORY_DOWNLOADS), FILE_NAME_ACC);
            SerializeToFile.writeToFile(combinedValuesList, getExternalFilesDir(DIRECTORY_DOWNLOADS), FILE_NAME_COMBINED);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.e("value", "Permission Granted, Now you can use local drive .");
                SerializeToFile.writeToFile(accValuesList, getExternalFilesDir(DIRECTORY_DOWNLOADS), FILE_NAME_ACC);
                SerializeToFile.writeToFile(combinedValuesList, getExternalFilesDir(DIRECTORY_DOWNLOADS), FILE_NAME_COMBINED);

            } else {
                Log.e("value", "Permission Denied, You cannot use local drive .");
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mSelectedDevice != null) {
            // Connect and register call backs for bluetooth gatt
            mBluetoothGatt =
                    mSelectedDevice.connectGatt(this, false, mBtGattCallback);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            try {
                mBluetoothGatt.close();
            } catch (Exception e) {
                // ugly, but this is to handle a bug in some versions in the Android BLE API
            }
        }
    }

    /**
     * Callbacks for bluetooth gatt changes/updates
     * The documentation is not always clear, but most callback methods seems to
     * be executed on a worker thread - hence use a Handler when updating the ui.
     */
    private final BluetoothGattCallback mBtGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                mBluetoothGatt = gatt;
                mHandler.post(new Runnable() {
                    public void run() {
                        mDataView.setText(R.string.connected);
                    }
                });
                // Discover services
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // Close connection and display info in ui
                mBluetoothGatt = null;
                mHandler.post(new Runnable() {
                    public void run() {
                        mDataView.setText(R.string.disconnected);
                    }
                });
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Debug: list discovered services
                List<BluetoothGattService> services = gatt.getServices();
                for (BluetoothGattService service : services) {
                    Log.i(LOG_TAG, service.getUuid().toString());
                }

                // Get the Movesense 2.0 IMU service
                BluetoothGattService movesenseService = gatt.getService(MOVESENSE_2_0_SERVICE);
                if (movesenseService != null) {
                    // debug: service present, list characteristics
                    List<BluetoothGattCharacteristic> characteristics =
                            movesenseService.getCharacteristics();
                    for (BluetoothGattCharacteristic chara : characteristics) {
                        Log.i(LOG_TAG, chara.getUuid().toString());
                    }

                    // Write a command, as a byte array, to the command characteristic
                    // Callback: onCharacteristicWrite
                    BluetoothGattCharacteristic commandChar =
                            movesenseService.getCharacteristic(
                                    MOVESENSE_2_0_COMMAND_CHARACTERISTIC);

                    // command example: 1, 99, "/Meas/Acc/13"
                    byte[] command =
                            TypeConverter.stringToAsciiArray(REQUEST_ID, IMU_COMMAND2);
                    commandChar.setValue(command);
                    boolean wasSuccess = mBluetoothGatt.writeCharacteristic(commandChar);
                    Log.i("writeCharacteristic", "was success=" + wasSuccess);
                } else {
                    mHandler.post(() -> MsgUtils.createDialog("Alert!",
                            getString(R.string.service_not_found),
                            DeviceActivity.this)
                            .show());
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic
                characteristic, int status) {
            Log.i(LOG_TAG, "onCharacteristicWrite " + characteristic.getUuid().toString());

            // Enable notifications on data from the sensor. First: Enable receiving
            // notifications on the client side, i.e. on this Android device.
            BluetoothGattService movesenseService = gatt.getService(MOVESENSE_2_0_SERVICE);
            BluetoothGattCharacteristic dataCharacteristic =
                    movesenseService.getCharacteristic(MOVESENSE_2_0_DATA_CHARACTERISTIC);
            // second arg: true, notification; false, indication
            boolean success = gatt.setCharacteristicNotification(dataCharacteristic, true);
            if (success) {
                Log.i(LOG_TAG, "setCharactNotification success");
                // Second: set enable notification server side (sensor). Why isn't
                // this done by setCharacteristicNotification - a flaw in the API?
                BluetoothGattDescriptor descriptor =
                        dataCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor); // callback: onDescriptorWrite
            } else {
                Log.i(LOG_TAG, "setCharacteristicNotification failed");
            }
        }

        @Override
        public void onDescriptorWrite(final BluetoothGatt gatt, BluetoothGattDescriptor
                descriptor, int status) {
            Log.i(LOG_TAG, "onDescriptorWrite, status " + status);

            if (CLIENT_CHARACTERISTIC_CONFIG.equals(descriptor.getUuid()))
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // if success, we should receive data in onCharacteristicChanged
                    mHandler.post(new Runnable() {
                        public void run() {
                            mDeviceView.setText(R.string.notifications_enabled);
                        }
                    });
                }
        }

        /**
         * Callback called on characteristic changes, e.g. when a sensor data value is changed.
         * This is where we receive notifications on new sensor data.
         */
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic
                characteristic) {
            // debug
            // Log.i(LOG_TAG, "onCharacteristicChanged " + characteristic.getUuid());

            // if response and id matches
            if (MOVESENSE_2_0_DATA_CHARACTERISTIC.equals(characteristic.getUuid())) {
                byte[] data = characteristic.getValue();
                //Log.i(LOG_TAG,"Response: " + data[0]);
                if (data[0] == MOVESENSE_RESPONSE && data[1] == REQUEST_ID) {
                    // NB! use length of the array to determine the number of values in this
                    // "packet", the number of values in the packet depends on the frequency set(!)
                    int len = data.length;
//                    Log.i(LOG_TAG,"Data: " + Arrays.toString(data) + "data length: " + len);
                    if (len <= 30) {

                        //Log.d(LOG_TAG, "------------------------------" + "\n" + "The data length is: " + len);
                        //Log.d(LOG_TAG, "this is the data " + Arrays.toString(data));

                        // parse and interpret the data, ...
                        int time = TypeConverter.fourBytesToInt(data, 2);
                        float accX = TypeConverter.fourBytesToFloat(data, 6);
                        float accY = TypeConverter.fourBytesToFloat(data, 10);
                        float accZ = TypeConverter.fourBytesToFloat(data, 14);

                        float yGyro = TypeConverter.fourBytesToFloat(data, 22);

//                        prevAccX = accX;
//                        prevAccY = accY;
//                        prevAccZ = accZ;

                        prevX = (float) (FILTER_VALUE * prevX + (1 - FILTER_VALUE) * accX);
                        prevY = (float) (FILTER_VALUE * prevY + (1 - FILTER_VALUE) * accY);
                        prevZ = (float) (FILTER_VALUE * prevZ + (1 - FILTER_VALUE) * accZ);

                        accPitch = (180 / Math.PI) * (Math.atan((prevX) / (Math.sqrt(Math.pow(prevY, 2) + Math.pow(prevZ, 2)))));

                        prevGyroY = (float) (FILTER_VALUE * prevGyroY + (1 - FILTER_VALUE) * yGyro);
                        comPitch = (1 - FILTER_VALUE_COMB) * (comPitch + 0.07 * prevGyroY) + FILTER_VALUE_COMB * accPitch;

                        // ... and then, filter data, calculate something interesting,
                        // ... display a graph or show values, ...


                        @SuppressLint("DefaultLocale") final String viewDataStr = String.format("%.2f, %.2f, %.2f", accX, accY, accZ);
                        mHandler.post(new Runnable() {
                            @SuppressLint("SetTextI18n")
                            public void run() {
                                mDeviceView.setText("" + time + " ms");
                                mDataView.setText(viewDataStr);
                                comPitchView.setText("comPitch: " + df.format(comPitch));
                                accPitchView.setText("accPitch: " + df.format(accPitch));
                                if (isRecording) {
                                    recordButton.setText("Stop Recording");
                                    accValuesList.add(new AnglePoint(accPitch, System.currentTimeMillis() - startTime));
                                    combinedValuesList.add(new AnglePoint(comPitch, System.currentTimeMillis() - startTime));
                                }
                            }
                        });
                    }
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic
                characteristic, int status) {
            Log.i(LOG_TAG, "onCharacteristicRead " + characteristic.getUuid().toString());
        }
    };

}