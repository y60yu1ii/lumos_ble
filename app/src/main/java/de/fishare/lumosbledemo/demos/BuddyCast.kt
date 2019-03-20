package de.fishare.lumosbledemo.demos

import android.bluetooth.BluetoothDevice
import de.fishare.lumosble.*

class BuddyCast(device: BluetoothDevice) : AvailObj(device) {
    override var TAG = "BuddyCast"
    val kf = KalmanFilter(2.0, 1.0)

    override fun onRawUpdate(data: ByteArray) {
        val dict:Map<Int, ByteArray> = parseScanRecord(data)
        var raw = ""
        dict.keys.forEach { k ->
            raw += "[$k] ${dict[k]?.hex4Human()}\n"
//            print(TAG, "on Raw Update [$k] ${dict[k]?.hex4Human()}")
        }
        listener?.onUpdated("raw", raw, this)
    }

    override fun onRSSIChange(rssi: Int) {
        val nRSSI = kf.filter(rssi.toDouble())
//        print(TAG, "RSSI is $rssi and filtered index is ${nRSSI.toInt()}")
        listener?.onRSSIChanged(nRSSI.toInt(), this)
    }
}