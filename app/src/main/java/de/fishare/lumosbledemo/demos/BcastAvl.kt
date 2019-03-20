package de.fishare.lumosbledemo.demos

import android.bluetooth.BluetoothDevice
import de.fishare.lumosble.AvailObj
import de.fishare.lumosble.KalmanFilter
import de.fishare.lumosble.hex4Human
import de.fishare.lumosble.print

class BcastAvl(device: BluetoothDevice) : AvailObj(device) {
    override var TAG = "BcastAvl"
    val kf = KalmanFilter(2.0, 1.0)

    override fun onRawUpdate(data: ByteArray) {
//        print(TAG, "on Raw Update " + data.hex4Human())
    }

    override fun onRSSIChange(rssi: Int) {
        val nRSSI = kf.filter(rssi.toDouble())
//        print(TAG, "RSSI is $rssi and filtered index is ${nRSSI.toInt()}")
        listener?.onRSSIChanged(nRSSI.toInt(), this)
    }
}