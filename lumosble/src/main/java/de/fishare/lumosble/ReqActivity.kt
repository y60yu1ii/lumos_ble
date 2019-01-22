package de.fishare.lumosble

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import de.fishare.lumosble.CentralManager.Companion.BLE_PERMIT

class ReqActivity : Activity() {
    private val BLE_REQ = 930577
    private val TAG = "Req Activity"
    var errMsg = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermission()
    }

    private fun checkPermission(){
        var granted = false
        BLE_PERMIT.forEach {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                granted = checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
            }
        }
        if(granted) {
            CentralManager.getInstance(applicationContext).refreshBluetoothState()
        }else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(CentralManager.BLE_PERMIT, BLE_REQ)
            }
        }
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val isAllGranted = grantResults.none { it == PackageManager.PERMISSION_DENIED }
        if(isAllGranted){
            CentralManager.getInstance(applicationContext).refreshBluetoothState()
            runOnUiThread { finish() }
        }else{
            permissions.forEach { print(TAG, "permission are $it") }
            grantResults.forEach { print(TAG, "results are $it") }
        }
    }


}