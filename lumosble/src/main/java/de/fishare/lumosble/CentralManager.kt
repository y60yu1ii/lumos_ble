package de.fishare.lumosble

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.support.v4.content.ContextCompat
import kotlin.concurrent.thread

class CentralManager private constructor(val context : Context) {
    interface EventListener{
        fun onRefresh()
        fun didDiscover()
        fun didConnect()
        fun didDisconnect()
    }
    companion object : SingletonHolder<CentralManager, Context>(::CentralManager) {
        const val TAG  = "CentralManager"
        val BLE_PERMIT = android.Manifest.permission.ACCESS_COARSE_LOCATION
    }
    private var CONNECT_THRESHOLD = -65f
    private var FILTERS  = listOf("1802")
    var event : EventListener? = null
    private val dataMgr = DataManager.getInstance(context)
    private val handler = Handler()

    var avails:MutableList<AvailObj> = mutableListOf()
    var periMap: MutableMap<String, PeriObj> = mutableMapOf()
    var history:List<PeriObj>  = mutableListOf()
        get() {
           return  periMap.values.toList()
        }

    private val scanner by lazy {
        Discover(FILTERS, scanCallback, context )
    }

    private var scanCallback = object : ScanResultCallback {
        override fun onDiscover(device: BluetoothDevice, RSSI: Int, data: ByteArray, record: Any?) {
            thread(start=true, name = "discover", isDaemon = true) {
//                print(TAG, "[onDiscover] device is ${device.address} and RSSI is $RSSI")
                avails.singleOrNull { it.mac == device.address }?.let { it.rssi = RSSI } ?: run{ if(RSSI > CONNECT_THRESHOLD) addAvail(device) }
            }
        }
    }

    init {
        loadHistory()
    }

    private fun addAvail(device:BluetoothDevice){
        val avl = AvailObj(device)
        avails.singleOrNull { it.mac == device.address } ?: run {
            print(TAG, "[ADD TO AVAIL] count ${avails.size} mac is ${avl.mac}")
            avails.add(avl)
            event?.didDiscover()
        }
    }


/**
 *  Public methods
 * */

    fun scan(){
        print(TAG, "Start scanning")
        scanner.startScan()
    }

    fun stopScan(){
        scanner.stopScan()
    }

    fun checkPermit(activity: Activity){
        val granted = ContextCompat.checkSelfPermission(context, BLE_PERMIT) == PackageManager.PERMISSION_GRANTED
        if(granted){
           scan()
        }else{
            val req = context.getString(R.string.req_action)
            val intent = Intent().apply { action = req }
            activity.startActivity( intent )
        }
    }

    fun refreshBluetoothState(){
//        print(TAG, "refresh turned off ble")
        BluetoothAdapter.getDefaultAdapter()?.disable()
        delay(1f){
//            print(TAG, "refresh turned on ble")
            BluetoothAdapter.getDefaultAdapter()?.enable()
        }
        delay(2f){
            scan()
        }
    }

    private fun loadHistory(){
        dataMgr.getHistory().forEach { mac -> periMap[mac] = PeriObj(mac) }
    }

    private fun delay(sec:Float, lambda: () -> Unit){
        handler.postDelayed({lambda()}, (sec * 1000).toLong())
    }

}