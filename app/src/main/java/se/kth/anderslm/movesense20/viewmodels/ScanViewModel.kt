package se.kth.anderslm.movesense20.viewmodels

import android.bluetooth.BluetoothDevice
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class ScanViewModel:ViewModel() {
    var textView:MutableState<String> = mutableStateOf("No devices found")
    var deviceList:MutableState<List<BluetoothDevice>> = mutableStateOf(mutableListOf())
    var isScanning:MutableState<Boolean> = mutableStateOf(false)


}