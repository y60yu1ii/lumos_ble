package de.fishare.lumosble

import android.bluetooth.BluetoothDevice

open class AvailObj(val device: BluetoothDevice){
    open var TAG = "Avail"
    interface Listener{
        fun onRSSIChanged(rssi: Int, availObj:AvailObj ){}
        fun onUpdated(label:String, value: Any, availObj: AvailObj){}
    }

    var name:String = device.name ?: "name"
    var mac:String  = device.address
    var lastUpdateTime:Long = 0
    var rawData:ByteArray = byteArrayOf()
        set(value) {
            onRawUpdate(value)
            field = value
        }
    open fun onRSSIChange(rssi:Int){
        listener?.onRSSIChanged(rssi, this)
    }
    open fun onRawUpdate(data:ByteArray){}
    open fun setUp(){}

    var listener:Listener?=null
    var rssi = 0
        set(value) {
            if(value != field && rssi < 0){
                onRSSIChange(rssi)
            }
            field = value
            lastUpdateTime = System.currentTimeMillis()
        }
}