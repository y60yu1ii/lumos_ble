package de.fishare.lumosble

import android.annotation.TargetApi
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build

enum class ScanType(val i:Int) {
    PowerSave(0), Balanced(1), Aggressive(2)
}

interface ScanResultCallback{
    fun onDiscover(device: BluetoothDevice, RSSI:Int, data:ByteArray, record:Any?)
    fun onLost(device: BluetoothDevice, RSSI:Int){}
}

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class Discover(val uuids:List<String>, callback: ScanResultCallback, val context: Context){
    companion object {
        const val TAG = "Discover"
        var isKeepScanning = false
    }
    private val scanFilters = ArrayList<ScanFilter>()
    private var scanSettings: ScanSettings? = null
    private var scanSettingsBuilder: ScanSettings.Builder = ScanSettings.Builder()
    private val bluetoothManager : BluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    private val adapter by lazy {
        bluetoothManager.adapter
    }

    init {
        setFilters()
    }

    private fun setFilters(){
        //after Android 8.0, scanning without filter is no longer available
        print(TAG, "set uuids are ${uuids}")
        uuids.forEach {
            val filter = ScanFilter.Builder().setServiceUuid(it.toParcelUUID()).build()
            scanFilters.add(filter)
        }
        scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            scanSettingsBuilder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
        }

        scanSettings = scanSettingsBuilder.build()
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(callbackType == ScanSettings.CALLBACK_TYPE_MATCH_LOST){
                    callback.onLost(result.device, result.rssi)
                }
            }
            callback.onDiscover(result.device, result.rssi, result.scanRecord!!.bytes, result)
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun startScan() {
        if (adapter.isEnabled) {
            print(TAG, "Start scanning")
            adapter?.bluetoothLeScanner?.startScan(scanFilters, scanSettings, scanCallback)
        }
    }

    fun stopScan(){
        print(TAG, "Stop scanning")
        isKeepScanning = false
        bluetoothManager.adapter?.bluetoothLeScanner?.stopScan(scanCallback)
    }

    fun pause(){
        isKeepScanning = true
        bluetoothManager.adapter?.bluetoothLeScanner?.stopScan(scanCallback)
    }

    fun resume(){
       if(isKeepScanning) {
           startScan()
       }
    }


}
