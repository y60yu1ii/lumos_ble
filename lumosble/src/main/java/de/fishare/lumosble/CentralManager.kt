package de.fishare.lumosble

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.support.v4.content.ContextCompat

class CentralManagerBuilder(var serviceUUIDs : List<String> = listOf()){
    fun build(context: Context):CentralManager{
        CentralManager.serviceUUIDs = serviceUUIDs
        return CentralManager.getInstance(context)
    }
}

class CentralManager private constructor(val context : Context): PeriObj.StatusEvent {
    interface EventListener{
        fun onRefresh()
        fun didDiscover(availObj: AvailObj)
    }

    interface Setting{
        fun isValidName(name:String?):Boolean
        fun getCustomObj(availObj: AvailObj):PeriObj
        fun getCustomAvl(device: BluetoothDevice):AvailObj
    }

    companion object : SingletonHolder<CentralManager, Context>(::CentralManager) {
        const val TAG  = "CentralManager"
        val BLE_PERMIT = arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION)
        var serviceUUIDs: List<String> = listOf()
    }
    private var CONNECT_THRESHOLD = -75f
    var event : EventListener? = null
    var setting : Setting? = null
    private val dataMgr = DataManager.getInstance(context)
    private val handler = Handler()

    var avails:MutableList<AvailObj> = mutableListOf()
    var periMap: MutableMap<String, PeriObj> = mutableMapOf()
    var peris:MutableList<PeriObj>  = mutableListOf()
        get() { return  periMap.values.toMutableList() }

    private val scanner by lazy {
        Discover(serviceUUIDs, scanCallback, context )
    }

    private var scanCallback = object : ScanResultCallback {
        override fun onDiscover(device: BluetoothDevice, RSSI: Int, data: ByteArray, record: Any?) {
//            print(TAG, "device is ${device.address} and rssi is $RSSI")
            if(setting?.isValidName(device.name) == false) return
            val avl = avails.firstOrNull { it.mac == device.address }
            if(avl != null){
                avl.rssi = RSSI
                avl.rawData = data
            }else if(RSSI > CONNECT_THRESHOLD){
                addAvail(device, data)
            }
        }
    }

    init {
        loadHistory()
    }

    private fun addAvail(device:BluetoothDevice, rawData:ByteArray){
        val avl = setting?.getCustomAvl(device) ?: AvailObj(device)
        avl.rawData = rawData
        avails.singleOrNull { it.mac == device.address } ?: run {
            print(TAG, "[ADD TO AVAIL] count ${avails.size} mac is ${avl.mac}")
            avails.add(avl)
            event?.didDiscover(avl)
        }
    }

    private fun connect(avl: AvailObj){
        print(TAG, "Connecting")
        val periObj = periMap[avl.mac] ?: setting?.getCustomObj(avl)!!
        if(periObj.connectingLock.not()){
            scanner.pause()
            handler.post { periObj.connect(avl.device, context) }
            periObj.rssi = avl.rssi
            periMap[avl.mac] = periObj
            avl.listener = null
            avails.removeAll { it.mac == avl.mac }
            periObj.event = this@CentralManager
        }
    }

    private fun disconnect(peri: PeriObj){
        print(TAG, "Disconnecting")
        peri.markDelete = true
        peri.event = this@CentralManager
        peri.disconnect()
    }

    //Collect all peri event
    override fun onStatusChanged(isConnected: Boolean, periObj: PeriObj) {
        print(TAG, "[didConnect] peri is ${periObj.mac} and isConnected $isConnected")
        if(isConnected.not()){
            if(periObj.markDelete || periObj.isAuthSuccess.not()){
                periMap.remove(periObj.mac)
            }
        }

        context.sendBroadcast(Intent(CONNECTION_EVENT).apply {
            putExtra("mac", periObj.mac)
            putExtra("connected", isConnected)
        })
        scanner.resume()
    }

/**
 *  Public methods
 * */

    fun connect(mac:String){
        val avl = avails.singleOrNull { it.mac == mac }
        if(avl != null){ connect(avl) }
    }

    fun disconnect(mac:String){
        val peri = periMap[mac]
        if(peri != null){ disconnect(peri) }
    }

    fun scan(){
        scanner.startScan()
    }

    fun stopScan(){
        scanner.stopScan()
    }

    fun checkPermit(activity: Activity){
        var granted = false
        BLE_PERMIT.forEach {
            granted = ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
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