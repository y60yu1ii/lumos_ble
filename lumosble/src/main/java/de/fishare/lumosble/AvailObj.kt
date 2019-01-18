package de.fishare.lumosble

import android.bluetooth.BluetoothDevice

open class AvailObj(val device: BluetoothDevice){
    open var TAG = "Avail"
    interface Listener{
        fun onRSSIChanged(rssi: Int, mac:String ){}
        fun onUpdated(label:String, value: Any, availObj: AvailObj){}
    }

    var name:String = device.name ?: ""
    var mac:String  = device.address
    var lastUpdatTime:Long = 0
    var rawData:ByteArray = byteArrayOf()
        set(value) {
            onRawUpdate(value)
            field = value
        }
    open fun onRawUpdate(data:ByteArray){}

    var listener:Listener?=null
    var rssi = 0
        set(value) {
            if(value != field && rssi < 0){
//                print("avail $mac rssi is $rssi")
                listener?.onRSSIChanged(rssi, mac)
            }
            field = value
            lastUpdatTime = System.currentTimeMillis()
        }
}