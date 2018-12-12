package de.fishare.lumosble

import android.content.Context
import android.os.Handler

class CentralManager private constructor(context : Context) {
    companion object : SingletonHolder<CentralManager, Context>(::CentralManager) {
        const val TAG = "CentralManager"
        const val CONNECT_FILTER: Float = -58f
        const val IGNORE_FILTER:  Float = -65f
        val BLE_PERMISSIONS = arrayOf(
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )
        const val NAME_REGX = ""
    }
    interface EventListener{
        fun onRefreshed()
    }

    private val ctx = context
    private val handler = Handler()
}