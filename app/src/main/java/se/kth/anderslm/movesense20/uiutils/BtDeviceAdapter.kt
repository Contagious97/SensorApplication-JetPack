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
import android.util.Log
import android.view.View
import se.kth.anderslm.movesense20.timeAndElevation

// Adapter for the RecyclerView displaying BT devices
class BtDeviceAdapter(
    private val mDeviceList: List<BluetoothDevice>,
    private val mOnItemSelectedCallback: (Any) -> Unit
) : RecyclerView.Adapter<BtDeviceAdapter.ViewHolder>() {
    // interface for callbacks when item selected
    interface IOnItemSelectedCallBack {
        fun onItemClicked(position: Int)
    }

    // Represents the the item view, and its internal views
    class ViewHolder internal constructor(
        itemView: View,
        onItemSelectedCallback: IOnItemSelectedCallBack
    ) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var deviceNameView: TextView? = null
        var deviceInfoView: TextView? = null
        private val mOnItemSelectedCallback: IOnItemSelectedCallBack

        // Handles the item (row) being being clicked
        override fun onClick(view: View) {
            val position = adapterPosition // gets item (row) position
            mOnItemSelectedCallback.onItemClicked(position)
        }

        init {
            itemView.setOnClickListener(this)
            mOnItemSelectedCallback = onItemSelectedCallback
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // create a new item view
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.scan_res_item, parent, false)
        val vh = ViewHolder(itemView, mOnItemSelectedCallback)
        vh.deviceNameView = itemView.findViewById(R.id.device_name)
        vh.deviceInfoView = itemView.findViewById(R.id.device_info)
        return vh
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(vh: ViewHolder, position: Int) {
        val device = mDeviceList[position]
        val name = device.name
        vh.deviceNameView!!.text = name ?: "Unknown"
        vh.deviceInfoView!!.text = device.bluetoothClass.toString() + ", " + device.address
        Log.i("ScanActivity", "onBindViewHolder")
    }

    override fun getItemCount(): Int {
        return mDeviceList.size
    }
}