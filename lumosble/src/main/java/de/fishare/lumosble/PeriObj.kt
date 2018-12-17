package de.fishare.lumosble

import android.bluetooth.BluetoothDevice
import android.content.Context
import java.util.logging.Handler

class PeriObj(val mac:String){
    var TAG = "Peri"
    interface StatusEvent{
        fun onStatusChanged(isConnected:Boolean, periObj: PeriObj){}
    }
    interface Listener{
        fun onRSSIChanged(rssi: Int, mac:String ){}
        fun onUpdated(label:String, value: Any, periObj: PeriObj){}
    }
    var device: BluetoothDevice? = null
    var controller: GattController? = null
    var isConnected = false
    var isConnecting = false
    var name:String = "name"

    var listener:Listener?=null
    var event:StatusEvent?=null

    var rssi = 0
        set(value) {
            if(value != field && rssi < 0){ listener?.onRSSIChanged(rssi, mac) }
            field = value
        }

    fun connect(dev: BluetoothDevice, context: Context){
        isConnecting = true
        this.device = dev
        name = dev.name
        controller = GattController().apply {
            gatt = device?.connectGatt(context, false, this)
            listener = controlHandler
        }
    }

    private fun customize(){
        writeTo("ffc1", "PQD".toByteArray())
        writeTo("ffc2", "aaaaaa".toByteArray())
        writeTo("ffc3", byteArrayOf(0x01))
        controller?.subscribeTo("ffe1")
    }

    private fun writeTo(uuidStr:String, data:ByteArray){
        controller?.writeTo(uuidStr, data, false)
    }

    private fun writeWithResponce(uuidStr:String, data:ByteArray){
        controller?.writeTo(uuidStr, data, true)
    }

    private val controlHandler = object : GattController.Listener {
        override fun didDiscovered() {
            super.didDiscovered()
            customize()
        }

        override fun didChangeState(isConnected: Boolean) {
            super.didChangeState(isConnected)
            print(TAG, "$mac is ${if(isConnected) "CONNECT" else "DISCONNECT" }")
            isConnecting = false
            event?.onStatusChanged(isConnected, this@PeriObj)
        }

        override fun onRSSIUpdated(rawRSSI: Int) {
        }

        override fun onUpdated(uuidStr: String, value: ByteArray, kind: GattController.UpdateKind) {
        }
    }

}