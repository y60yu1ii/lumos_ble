package de.fishare.lumosbledemo.demos

import android.os.Handler
import de.fishare.lumosble.*

class AmbObj(mac: String) : PeriObj(mac) {
    override var TAG = "AmbObj"
    var handler = Handler()

    init {
        print(TAG, "init")
    }
    override fun setUp(){
        super.setUp()
        print(TAG, "auth and start")
        handler.postDelayed({
            print(TAG, "subscribe")
            controller?.subscribeTo("2a05")
        }, 2000)
    }
    override fun onUpdated(uuidStr: String, value: ByteArray, kind: GattController.UpdateKind) {
        if(kind == GattController.UpdateKind.Notify){
            print(TAG, "[Notify] is $uuidStr has ${value.hex4Human()} int is ${value.to2Int()}")
            listener?.onUpdated(uuidStr, value.to2Int(), this@AmbObj)
        }
    }
}