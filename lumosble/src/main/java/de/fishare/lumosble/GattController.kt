package de.fishare.lumosble

import android.bluetooth.*
import android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
import android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
import android.os.Handler
import android.util.Log
import java.util.*

class GattController : BluetoothGattCallback() {
    enum class UpdateKind{ Write, Read, Notify }
    interface Listener{
        fun didChangeState(isConnected:Boolean){}
        fun didDiscoverServices(){}
        fun onRSSIUpdated(rawRSSI: Int){}
        fun onUpdated(uuidStr: String, value: ByteArray, kind:UpdateKind){}
    }
    companion object {
       private const val TAG = "Controller"
       private const val WRITE_DELAY:Float = 0.2f// delay write for 150 ms
       private const val DESCRIPTOR_STR = "00002902-0000-1000-8000-00805f9b34fb"
    }

    var isConnected : Boolean = false
    var gatt : BluetoothGatt? = null
    private var writeQueue: WriteQueue = WriteQueue()
    private var handler:Handler = Handler()
    private var chMap : MutableMap<String, BluetoothGattCharacteristic> = mutableMapOf()
    var listener: Listener?=null

    fun disconnect(){
        gatt?.disconnect()
        gatt?.close()
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)
        print(TAG, "############### CONNECTION CHANGED")
        if( status == BluetoothGatt.GATT_SUCCESS ){
            when(newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    //connected
                    gatt?.discoverServices()
                    isConnected = true
                    listener?.didChangeState(isConnected)
                }
                BluetoothProfile.STATE_DISCONNECTED ->{
                    //disconnected
                    disable()
                }
                else->{
                    disable()
                }
            }
        }else{
            disable()
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        super.onServicesDiscovered(gatt, status)
//        print(TAG, " === start discovering service === ")
        for (service in gatt.services) {
            print(TAG, "Service is ${service.uuid.short()}")
//            print(TAG, "Service is ${service.uuid.short()}")
            for (characteristic in service.characteristics) {
                val uuidStr = characteristic.uuid.short()
                chMap[uuidStr] = characteristic
                print(TAG, "\t\t ch ${uuidStr}")
            }
        }
        print(TAG, " === end discovering service === ")
        listener?.didDiscoverServices()
    }

    fun writeTo(uuidStr:String, data:ByteArray, response: Boolean){
        chMap[uuidStr]?.let { writeTo(it, data, response) }
    }

    fun readFrom(uuidStr:String){
        chMap[uuidStr]?.let { readFrom(it) }
    }

    fun subscribeTo(uuidStr: String){
        chMap[uuidStr]?.let { subscribeTo(it) }
    }

    private fun writeTo(ch: BluetoothGattCharacteristic, data:ByteArray, resp :Boolean){
        writeQueue.offer(object : WriteQueue.WritingRunnable {
            override fun writeAction() {
                ch.let{
                    ch.writeType = if (resp) WRITE_TYPE_DEFAULT else WRITE_TYPE_NO_RESPONSE
                    ch.value = data
                    gatt?.writeCharacteristic(ch)
                }
            }
        })
    }

    private fun readFrom(ch: BluetoothGattCharacteristic){
        writeQueue.offer(object : WriteQueue.WritingRunnable {
            override fun writeAction() {
                ch.let {
                    gatt?.readCharacteristic(ch)
                }
            }
        })
    }

    private fun subscribeTo(ch: BluetoothGattCharacteristic){
        writeQueue.offer(object : WriteQueue.WritingRunnable {
            override fun writeAction() {
                ch.let {
                    print(TAG, "--------------------------- Subscribe to  ${ch.uuid?.short()}    ------------------------------------------")
                    gatt?.setCharacteristicNotification(ch, true)
                    val config = ch.getDescriptor(UUID.fromString(DESCRIPTOR_STR))
                    config?.let{
                        config.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt?.writeDescriptor(config)
                    }
                }
            }
        })
    }

    override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
        super.onCharacteristicRead(gatt, characteristic, status)
        val value = characteristic?.value?: byteArrayOf()
        val uuidStr = characteristic?.uuid?.short() ?: ""
        print(TAG, "[READ FROM] $uuidStr ${value.hex4Human()}")
        popWriteQueue()
        listener?.onUpdated(uuidStr, value, UpdateKind.Read)
    }

    override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
        super.onCharacteristicWrite(gatt, characteristic, status)
        val value = characteristic?.value?: byteArrayOf()
        val uuidStr = characteristic?.uuid?.short()?:""
        print(TAG, "[WRITE TO] $uuidStr with ${value.hex4Human()}")
        popWriteQueue()
        listener?.onUpdated(uuidStr, value, UpdateKind.Write)
    }

    override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
        super.onDescriptorWrite(gatt, descriptor, status)
        val uuidStr = descriptor?.uuid?.short()
        print(TAG, "[WRITE TO DESCRPT] $uuidStr status $status")
        popWriteQueue()
    }

    override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
        super.onCharacteristicChanged(gatt, characteristic)
        val value = characteristic?.value?: byteArrayOf()
        val uuidStr = characteristic?.uuid?.short()?:""
        print(TAG, "[NOTIFY] $uuidStr notify ${value.hex4Human()}")
        listener?.onUpdated(uuidStr, value, UpdateKind.Notify)
    }

    override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
        super.onReadRemoteRssi(gatt, rssi, status)
        listener?.onRSSIUpdated(rssi)
    }

    private fun popWriteQueue() {
        synchronized(writeQueue) {
            writeQueue.remove()
            if(writeQueue.isNotEmpty()){
                delay(WRITE_DELAY){
                    writeQueue.write()
                }
            }
        }
    }

    private fun disable(){
        print(TAG, "Connection is dropped")
        refreshDeviceCache(gatt)
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        isConnected = false
        listener?.didChangeState(isConnected)

    }

    private fun refreshDeviceCache(gatt: BluetoothGatt?): Boolean {
        try {
            return gatt?.javaClass?.getMethod("refresh")?.invoke(gatt) as Boolean
        } catch (localException: Exception) {
            Log.e("de", "An exception occurred while refreshing device")
        }
        return false
    }

    private fun delay(sec:Float, lambda: () -> Unit){
        handler.postDelayed({lambda()}, (sec * 1000).toLong())
    }

}



