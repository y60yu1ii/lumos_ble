package de.fishare.lumosbledemo.demos

import android.os.Handler
import de.fishare.lumosble.GattController
import de.fishare.lumosble.PeriObj
import de.fishare.lumosble.hex4Human
import de.fishare.lumosble.print


class UARTObj(mac: String) : PeriObj(mac) {
    var handler = Handler()

    override fun setUp(){
        controller?.subscribeTo("0003")
        writeTo("0002", "%AUTH=123456\n".toByteArray())
        super.setUp()

        handler.postDelayed({
            writeTo("0002", "%CALL=0\n".toByteArray())
        }, 2000)
        handler.postDelayed({
            writeTo("0002", "%CALL=2\n".toByteArray())
        }, 5000)
        handler.postDelayed({
            writeTo("0002", "%CALL=1\n".toByteArray())
        }, 10000)
        handler.postDelayed({
            writeTo("0002", "%CALL=2\n".toByteArray())
        }, 5000)
    }

    override fun onUpdated(uuidStr: String, value: ByteArray, kind: GattController.UpdateKind) {
        if(kind == GattController.UpdateKind.Notify){
            print(TAG, "[Notify] is $uuidStr has ${value.hex4Human()}")
            listener?.onUpdated(uuidStr, value, this@UARTObj)
        }
    }
}