package de.fishare.lumosble

import android.bluetooth.BluetoothDevice

class AvailObj(device: BluetoothDevice){
    var TAG = "Avail"
    interface Listener{
        fun onRSSIChanged(rssi: Int, mac:String )
    }

    var name:String = device.name
    var mac:String  = device.address

    var listener:Listener?=null
    var rssi = 0
        set(value) {
            if(value != field && rssi < 0){
                print("avail $mac rssi is $rssi")
                listener?.onRSSIChanged(rssi, mac)
            }
            field = value
        }
}