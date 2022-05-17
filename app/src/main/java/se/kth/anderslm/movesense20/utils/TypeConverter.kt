package se.kth.anderslm.movesense20.utils

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
import se.kth.anderslm.movesense20.timeAndElevation
import java.nio.ByteBuffer
import java.nio.ByteOrder

object TypeConverter {
    /**
     * Convert *four* bytes to an int.
     * @param bytes an array with bytes, of length four or greater
     * @param offset Index of the first byte in the sequence of four.
     * @return The (Java) int corresponding to the four bytes.
     */
    fun fourBytesToInt(bytes: ByteArray?, offset: Int): Int {
        return ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int
    }

    /**
     * Convert *four* bytes to a float.
     * @param bytes an array with bytes, of length four or greater
     * @param offset Index of the first byte in the sequence of four.
     * @return The (Java) float corresponding to the four bytes.
     */
    fun fourBytesToFloat(bytes: ByteArray?, offset: Int): Float {
        return ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).float
    }

    /**
     * Create a an array of bytes representing a Movesense 2.0 command string, ASCII encoded..
     * The first byte is always set to 1.
     *
     * @param id      The id used to identify this command, and incoming data from sensor.
     * @param command The command, see http://www.movesense.com/docs/esw/api_reference/.
     * @return An array of bytes representing a Movesense 2.0 command string.
     */
    fun stringToAsciiArray(id: Byte, command: String): ByteArray {
        require(id <= 127) { "id= $id" }
        val chars = command.trim { it <= ' ' }.toCharArray()
        val ascii = ByteArray(chars.size + 2)
        ascii[0] = 1
        ascii[1] = id
        for (i in chars.indices) {
            require(chars[i] <= 127) {
                "ascii val= " + chars[i]
                    .toInt()
            }
            ascii[i + 2] = chars[i].toByte()
        }
        return ascii
    }

    fun stringToAsciiArray(str: String): ByteArray {
        val chars = str.trim { it <= ' ' }.toCharArray()
        val ascii = ByteArray(chars.size)
        for (i in chars.indices) {
            require(chars[i] <= 127) {
                "ascii val= " + chars[i]
                    .toInt()
            }
            ascii[i] = chars[i].toByte()
        }
        return ascii
    }
}