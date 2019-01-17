package de.fishare.lumosbledemo

import android.bluetooth.BluetoothDevice
import de.fishare.lumosble.AvailObj
import de.fishare.lumosble.hex4Human
import de.fishare.lumosble.print

class BcastAvl(device: BluetoothDevice) : AvailObj(device) {
    override var TAG = "BcastAvl"
    override fun onRawUpdate(data: ByteArray) {
        print(TAG, "on Raw Update " + data.hex4Human())
    }
}