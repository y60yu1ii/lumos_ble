package de.fishare.lumosble

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Handler

class CentralManager private constructor(context : Context) {
    companion object : SingletonHolder<CentralManager, Context>(::CentralManager) {
        const val TAG = "CentralManager"
        val BLE_PERMISSIONS = arrayOf(
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
    var CONNECT_THRESHOLD = -58f
    var FILTERS  = listOf("1802")

    private val scanner by lazy {
        Discover(FILTERS, scanCallback, context )
    }

    private var scanCallback = object : ScanResultCallback {
        override fun onDiscover(device: BluetoothDevice, RSSI: Int, data: ByteArray, record: Any?) {
        }
    }

    interface EventListener{
        fun onRefreshed()
    }

    private val ctx = context
    private val handler = Handler()
}