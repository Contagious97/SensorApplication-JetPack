package se.kth.anderslm.movesense20.uiutils

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
import android.app.Dialog
import android.content.Context
import androidx.appcompat.app.AlertDialog
import se.kth.anderslm.movesense20.timeAndElevation

object MsgUtils {
    // short message
    fun showToast(msg: String?, context: Context?) {
        val toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT)
        toast.show()
    }

    // alert message
    fun createDialog(title: String?, msg: String?, context: Context?): Dialog {
        val builder = AlertDialog.Builder(
            context!!
        )
        builder.setTitle(title)
        builder.setMessage(msg)
        builder.setPositiveButton(" Ok") { dialog, id ->
            // do nothing, just close the alert
        }
        return builder.create()
    }
}