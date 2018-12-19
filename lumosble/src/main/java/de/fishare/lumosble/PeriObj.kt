package de.fishare.lumosble

import android.bluetooth.BluetoothDevice
import android.content.Context
import java.util.*
import java.util.logging.Handler
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.schedule
import kotlin.properties.Delegates

open class PeriObj(val mac:String){
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
    var name:String = "name"
    var connectingLock = false
    var isConnected = false
    var markDelete = false
    var isAuthSuccess : Boolean by Delegates.observable(
        initialValue = false,
        onChange = { _, _, new ->
            dealWithAuthState(new)
        }
    )

    var listener:Listener?=null
    var event:StatusEvent?=null

    var rssi = 0
        set(value) {
            if(value != field && rssi < 0){ listener?.onRSSIChanged(rssi, mac) }
            field = value
        }

    open fun connect(dev: BluetoothDevice, context: Context){
        connectingLock = true
        this.device = dev
        name = dev.name
        controller = GattController().apply {
            gatt = device?.connectGatt(context, false, this)
            listener = controlHandler
        }
    }

    open fun disconnect(){

    }

    open fun authAndSubscribe(){
        connectingLock = false
        loopReadRSSI()
    }

    open fun loopReadRSSI(){
        Timer("RSSI", false).schedule(600){
            controller?.gatt?.readRemoteRssi()
        }
    }

    open fun dealWithAuthState(auth:Boolean){
        print(TAG, "authentication status is ${if(auth)"Y" else "N"}")
    }

    open fun writeTo(uuidStr:String, data:ByteArray){
        controller?.writeTo(uuidStr, data, false)
    }

    open fun writeWithResponse(uuidStr:String, data:ByteArray){
        controller?.writeTo(uuidStr, data, true)
    }

    open fun getUpdated(uuidStr: String, value: ByteArray, kind: GattController.UpdateKind) {

    }

    private val controlHandler = object : GattController.Listener {
        override fun didDiscoverServices() {
            authAndSubscribe()
        }

        override fun didChangeState(isConnected: Boolean) {
            print(TAG, "$mac is ${if(isConnected) "CONNECT" else "DISCONNECT" }")
            this@PeriObj.isConnected = isConnected
            event?.onStatusChanged(isConnected, this@PeriObj)
        }

        override fun onRSSIUpdated(rawRSSI: Int) {
//            print(TAG, "$mac RSSI is $rawRSSI listener $listener")
            Timer("RSSI", false).schedule(1200){
                controller?.gatt?.readRemoteRssi()
            }
            listener?.onRSSIChanged(rawRSSI, mac)
        }

        override fun onUpdated(uuidStr: String, value: ByteArray, kind: GattController.UpdateKind) {
            getUpdated(uuidStr, value, kind)
        }
    }
}