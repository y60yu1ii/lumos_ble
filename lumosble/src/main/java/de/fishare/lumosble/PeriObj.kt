package de.fishare.lumosble

import android.bluetooth.BluetoothDevice
import android.content.Context

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

    fun connect(dev: BluetoothDevice, context: Context){
        this.device = dev
        name = dev.name
        controller = GattController().apply {
            gatt = device?.connectGatt(context, false, this)
            listener = controlHandler
        }
    }

    private val controlHandler = object : GattController.Listener {
        override fun didConnect() {
            print(TAG, "$mac is connected")
            listener?.onStatusChanged(true, this@PeriObj)
        }

        override fun didDisconnect() {
            print(TAG, "$mac is disconnected")
            listener?.onStatusChanged(false, this@PeriObj)
        }

        override fun onRSSIUpdated(rawRSSI: Int) {
        }

        override fun onUpdated(uuidStr: String, value: ByteArray, kind: GattController.UpdateKind) {
        }
    }

}