package de.fishare.lumosble

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.support.v4.content.ContextCompat

class CentralManager private constructor(val context : Context): PeriObj.StatusEvent {
    interface EventListener{
        fun onRefresh()
        fun didDiscover(availObj: AvailObj)
    }
    companion object : SingletonHolder<CentralManager, Context>(::CentralManager) {
        const val TAG  = "CentralManager"
        val BLE_PERMIT = android.Manifest.permission.ACCESS_COARSE_LOCATION
    }
    private var CONNECT_THRESHOLD = -75f
    private var FILTERS: List<String> = listOf()
    var event : EventListener? = null
    private val dataMgr = DataManager.getInstance(context)
    private val handler = Handler()

    var avails:MutableList<AvailObj> = mutableListOf()
    var periMap: MutableMap<String, PeriObj> = mutableMapOf()
    var peris:MutableList<PeriObj>  = mutableListOf()
        get() { return  periMap.values.toMutableList() }

    private val scanner by lazy {
        Discover(FILTERS, scanCallback, context )
    }

    private var scanCallback = object : ScanResultCallback {
        override fun onDiscover(device: BluetoothDevice, RSSI: Int, data: ByteArray, record: Any?) {
            if(device.name == null) return
            val avl = avails.firstOrNull { it.mac == device.address }
            if(avl != null){
                avl.rssi = RSSI
            }else if(RSSI > CONNECT_THRESHOLD){
                addAvail(device)
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
            event?.didDiscover(avl)
        }
    }

    fun connect(mac:String){
        val avl = avails.singleOrNull { it.mac == mac }
        if(avl != null){ connect(avl.device) }
    }

    private fun connect(device: BluetoothDevice){
        print(TAG, "Connecting")
        val periObj = periMap[device.address] ?: PeriObj(device.address)
        stopScan()
        if(periObj.isConnecting.not()){
            handler.post { periObj.connect(device, context) }
            periMap[device.address] = periObj
            avails.removeAll { it.mac == device.address }
            periObj.event = this@CentralManager
        }
    }

    override fun onStatusChanged(isConnected: Boolean, periObj: PeriObj) {
//        print(TAG, "[didConnect] peri is ${periObj.mac} and isConnected $isConnected")
        context.sendBroadcast(Intent(CONNECTION_EVENT).apply {
            putExtra("mac", periObj.mac)
            putExtra("connected", isConnected)
        })
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
        dataMgr.getHistory().forEach {
                mac -> periMap[mac] = PeriObj(mac).apply { event = this@CentralManager }
        }
    }

    private fun delay(sec:Float, lambda: () -> Unit){
        handler.postDelayed({lambda()}, (sec * 1000).toLong())
    }
}