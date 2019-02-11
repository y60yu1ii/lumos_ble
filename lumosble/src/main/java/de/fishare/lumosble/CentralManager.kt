package de.fishare.lumosble

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.support.v4.app.ActivityCompat

class CentralManagerBuilder(var serviceUUIDs : List<String> = listOf()){
    fun build(context: Context):CentralManager{
        CentralManager.serviceUUIDs = serviceUUIDs
        return CentralManager.getInstance(context)
    }
}

class CentralManager private constructor(val context : Context): PeriObj.StatusEvent {
    interface EventListener{
        fun didDiscover(availObj: AvailObj)
    }

    interface Setting{
        fun getNameRule():String{return REGX_ALL}//Match everything
        fun getCustomObj(mac: String, name:String):PeriObj{ return PeriObj(mac) }
        fun getCustomAvl(device: BluetoothDevice):AvailObj{ return AvailObj(device) }
    }

    companion object : SingletonHolder<CentralManager, Context>(::CentralManager) {
        const val TAG  = "CentralManager"
        val BLE_PERMIT = arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION)
        var serviceUUIDs: List<String> = listOf()
        const val REGX_ALL = ".*?"
    }

    private var CONNECT_THRESHOLD = -75f
    private var OUTDATE_PERIOD = 10// in second
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
            if(!isValidName(device.name)) return
            val peri = periMap[device.address]
            if(peri != null && !peri.markDelete){
                val avl = setting?.getCustomAvl(device) ?: AvailObj(device)
                avl.rawData = data
                connect(avl)
                return
            }
            val avl = avails.firstOrNull { it.mac == device.address }
            if(avl != null){
                avl.rssi = RSSI
                avl.rawData = data
            }else if(RSSI > CONNECT_THRESHOLD){
                addAvail(device, data)
            }
        }

        override fun onLost(device: BluetoothDevice, RSSI: Int) {
            print(TAG, "device LOST is ${device.address} and rssi is $RSSI")
            avails.removeAll { it.mac == device.address }
                .apply { context.sendBroadcast(Intent(Event.REFRESH)) }
        }
    }

    fun isValidName(name:String?):Boolean{
        return if(name == null) false
        else Regex(setting?.getNameRule() ?: REGX_ALL).matches(name)
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
        val periObj = periMap[avl.mac] ?: setting?.getCustomObj(avl.mac, avl.name) ?: PeriObj(avl.mac)
        if(periObj.connectingLock.not()){
            scanner.pause()
            handler.post { periObj.connect(avl.device, context) }
            periObj.rssi = avl.rssi
            periMap[avl.mac] = periObj
            avl.listener = null
            avails.removeAll { it.mac == avl.mac }
            periObj.event = this@CentralManager
            DataManager.getInstance(context).addToHistory(avl.mac)
            DataManager.getInstance(context).saveProfile(avl.mac, "name", avl.name)
        }
    }

    private fun disconnect(peri: PeriObj, isRemove: Boolean){
        print(TAG, "Disconnecting")
        peri.markDelete = isRemove
        peri.event = this@CentralManager
        peri.disconnect()
        if(isRemove){ DataManager.getInstance(context).removeFromHistory(peri.mac) }
    }

    //Collect all peri event
    override fun onStatusChanged(isConnected: Boolean, periObj: PeriObj) {
        print(TAG, "[didConnect] peri is ${periObj.mac} and isConnected $isConnected")
        if(isConnected.not()){
            if(periObj.markDelete || periObj.isAuthSuccess.not()){
                periMap.remove(periObj.mac)
            }
        }

        context.sendBroadcast(Intent(Event.CONNECTION).apply {
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
        if(peri != null){
            disconnect(peri, false)
        }
    }

    fun remove(mac:String){
        val peri = periMap[mac]
        if(peri != null){
            disconnect(peri, isRemove = true)
        }
    }

    fun scan(){
        scanner.startScan()
    }

    fun stopScan(){
        scanner.stopScan()
    }

    fun clearAvl(){
        avails.removeAll{true}.apply { context.sendBroadcast(Intent(Event.REFRESH)) }
    }

    fun clearOutdateAvl(){
        avails.removeAll { (System.currentTimeMillis() - it.lastUpdateTime) > OUTDATE_PERIOD * 1000 }
            .apply { context.sendBroadcast(Intent(Event.REFRESH)) }
    }

    fun checkPermit(activity: Activity){
        var granted = false
        BLE_PERMIT.forEach {
            granted = ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
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
        //force re-open bluetooth after getting permission to start scanning
        BluetoothAdapter.getDefaultAdapter()?.disable()
        delay(1f){ BluetoothAdapter.getDefaultAdapter()?.enable() }
        delay(2f){ scan() }
    }

    fun loadHistory(){
        dataMgr.getHistory().forEach {
           mac ->
            val name = DataManager.getInstance(context).loadProfileInString(mac, "name")
            periMap[mac] = (setting?.getCustomObj(mac, name) ?: PeriObj(mac))
                .apply { event = this@CentralManager }
        }
    }

    private fun delay(sec:Float, lambda: () -> Unit){
        handler.postDelayed({lambda()}, (sec * 1000).toLong())
    }
}