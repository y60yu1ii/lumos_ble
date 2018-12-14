package de.fishare.lumosble

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat

class ReqActivity : Activity() {
    private val BLE_REQ = 930577
    private val TAG = "Req Activity"
    private val handler by lazy { Handler() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermission()
    }

    private fun checkPermission(){
        val grant = ContextCompat.checkSelfPermission(this, CentralManager.BLE_PERMIT) == PackageManager.PERMISSION_GRANTED
        if(grant) {
            CentralManager.getInstance(applicationContext).refreshBluetoothState()
        }else{
            ActivityCompat.requestPermissions(this, arrayOf(CentralManager.BLE_PERMIT), BLE_REQ)
        }
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val isAllGranted = grantResults.none { it == PackageManager.PERMISSION_DENIED }
        if(requestCode == BLE_REQ && isAllGranted){
            CentralManager.getInstance(applicationContext).refreshBluetoothState()
            runOnUiThread { finish() }
        }
    }


}