package se.kth.anderslm.movesense20.serialization

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
import android.util.Log
import se.kth.anderslm.movesense20.timeAndElevation
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

object SerializeToFile {
    private val LOG_TAG = SerializeToFile::class.java.simpleName
    fun writeToFile(dataList: List<AnglePoint>?, folderPath: File?, fileName: String?) {
        /* List<Object> listOfData = new ArrayList<>();
        Log.d(LOG_TAG,"Time and elevation: " + timeAndElevation.getTime() + "s |" +  timeAndElevation.getElevation() + "°");
        listOfData.add(timeAndElevation.getTime());
        listOfData.add(timeAndElevation.getElevation());*/
        Log.d(LOG_TAG, "Folder path: " + folderPath!!.absolutePath)
        try {
            val myFilePath = File(folderPath, fileName)
            if (!myFilePath.exists()) {
                myFilePath.createNewFile()
            }
            val gson = Gson()
            val json = gson.toJson(dataList)
            Log.d(LOG_TAG, json)
            val fileOutputStream = FileOutputStream(myFilePath)
            fileOutputStream.write(json.toByteArray())
            fileOutputStream.close()

//            Log.i(LOG_TAG,"The list of objects were successfully written to a file");
        } catch (ex: Exception) {
            Log.e(LOG_TAG, "Exception thrown in writeToFile: $ex")
            Log.d(LOG_TAG, "Could not write to file")
            ex.printStackTrace()
        }
    }
}