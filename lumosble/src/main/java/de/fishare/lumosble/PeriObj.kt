package de.fishare.lumosble

import android.bluetooth.BluetoothDevice
import android.content.Context
import java.util.*
import kotlin.concurrent.schedule
import kotlin.properties.Delegates

interface StatusEvent{
    fun onStatusChanged(isConnected:Boolean, periObj: PeriObj){}
}
interface PeriObjListener{
    fun onRSSIChanged(rssi: Int, periObj: PeriObj){}
    fun onUpdated(label:String, value: Any, periObj: PeriObj){}
}

open class PeriObj(val mac:String){
    open var TAG = "Peri"
    var device: BluetoothDevice? = null
    var controller: GattController? = null
    var name:String = "name"
    var markDelete = false
    var blocked = false
    var connectingLock = false
    var isConnecting = false
    var isConnected = false

    var listener:PeriObjListener?=null
    var event:StatusEvent?=null

    var rssi = 0

    open fun onUpdated(uuidStr: String, value: ByteArray, kind: GattController.UpdateKind) {}
    open fun onRSSIChange(rssi: Int) {}

    open fun setUp(){
        isConnecting = false
        loopReadRSSI()
    }

    open fun connect(dev: BluetoothDevice, context: Context){
        isConnecting = true
        connectingLock = true
        this.device = dev
        name = dev.name
        controller = GattController().apply {
            desiredServices = getDesiredServices()
            gatt = device?.connectGatt(context, false, this)
            listener = controlHandler
        }
    }
    open fun getDesiredServices():List<String>{
        return emptyList()
    }
    open fun disconnect(){}
    open fun connectionDropped(){}
    fun clear(){
        controller?.disconnect()
        event?.onStatusChanged(false, this)
    }

    fun loopReadRSSI(){
        postpone(0.6f){
            controller?.gatt?.readRemoteRssi()
        }
    }

    fun writeTo(uuidStr:String, data:ByteArray){
        controller?.writeTo(uuidStr, data, false)
    }

    fun writeWithResponse(uuidStr:String, data:ByteArray){
        controller?.writeTo(uuidStr, data, true)
    }

    fun subscribeTo(uuidStr: String){
       controller?.subscribeTo(uuidStr)
    }


    private val controlHandler = object : GattController.Listener {
        override fun didDiscoverServices() {
            setUp()
        }

        override fun onRSSIUpdated(rawRSSI: Int) {
            postpone(1.2f){
                controller?.gatt?.readRemoteRssi()
            }
            rssi = rawRSSI
            listener?.onRSSIChanged(rawRSSI, this@PeriObj)
        }

        override fun onUpdated(uuidStr: String, value: ByteArray, kind: GattController.UpdateKind) {
            this@PeriObj.onUpdated(uuidStr, value, kind)
        }

        override fun didChangeState(isConnected: Boolean) {
            print(TAG, "$mac is ${if(isConnected) "CONNECT" else "DISCONNECT" }")
            this@PeriObj.isConnected = isConnected
            if(!isConnected){ connectionDropped() }
            event?.onStatusChanged(isConnected, this@PeriObj)
        }
    }
}