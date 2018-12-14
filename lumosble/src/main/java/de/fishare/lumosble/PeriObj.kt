package de.fishare.lumosble

import android.bluetooth.BluetoothDevice

class PeriObj(val mac:String){
    var TAG = "Peri"
    interface Listener{
        fun onStatusChanged(isConnected:Boolean, periObj: PeriObj)
        fun onRSSIChanged(rssi: Int, mac:String )
        fun onUpdated(label:String, value: Any, periObj: PeriObj)
    }
    var device: BluetoothDevice? = null
    var controller: GattController? = null
    var isConnected = false
    var name:String = "name"

    var listener:Listener?=null
    var rssi = 0
        set(value) {
            if(value != field && rssi < 0){ listener?.onRSSIChanged(rssi, mac) }
            field = value
        }
}