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
import com.jjoe64.graphview.series.DataPoint
import se.kth.anderslm.movesense20.timeAndElevation
import java.io.*
import java.lang.Exception
import java.util.ArrayList

class DeserializeFromFile(//    private BtDeviceAdapter btDeviceAdapter;
    private val time: DataPoint?, private val elevation: DataPoint?
) : Serializable {
    fun readFromFile(path: File?) {
        var readData: List<Any> = ArrayList()
        try {
            val file = File(path, "graph_data")
            val fileInputStream = FileInputStream(file)
            val objectInputStream = ObjectInputStream(fileInputStream)
            readData = objectInputStream.readObject() as ArrayList<Any>
            fileInputStream.close()
            objectInputStream.close()
        } catch (e: ClassNotFoundException) {
            Log.d(LOG_TAG, "Something went wrong when trying to read from file")
            Log.e(LOG_TAG, e.toString())
            e.printStackTrace()
        } catch (e: IOException) {
            Log.d(LOG_TAG, "Something went wrong when trying to read from file")
            Log.e(LOG_TAG, e.toString())
            e.printStackTrace()
        }
        if (readData.size == 0) {
            Log.d(LOG_TAG, "File is empty")
        }
        Log.i(LOG_TAG, "The file has successfully been read!")
        try {
            val readTime = readData[0] as Int
            val readElevation = readData[1] as Double
            Log.d(LOG_TAG, "the time is: $readTime | The elevation is: $readElevation")
        } catch (e: Exception) {
            Log.e(LOG_TAG, e.toString())
            Log.d(LOG_TAG, "Could not post read data")
            e.printStackTrace()
        }
    }

    companion object {
        private val LOG_TAG = DeserializeFromFile::class.java.simpleName
    }
}