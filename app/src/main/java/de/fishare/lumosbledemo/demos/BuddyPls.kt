package de.fishare.lumosbledemo.demos

import android.os.Handler
import de.fishare.lumosble.GattController
import de.fishare.lumosble.PeriObj
import de.fishare.lumosble.hex4Human
import de.fishare.lumosble.print

class BuddyPls(mac: String) : PeriObj(mac) {
   override var TAG = "BuddyObj"
   private var connectingCount = 0
    val handler = Handler()

   override fun setUp(){
       if(connectingLock){
           connectingCount += 1
           blocked = connectingCount >= 3
           if(blocked){
               print(TAG, "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB $name is blocked count is $connectingCount")
               listener?.onUpdated("BLK", "not your tag", this@BuddyPls)
           }
       }
       writeTo("ffc1", "PQD".toByteArray())
       writeTo("ffc2", "22222222222".toByteArray())
       writeTo("ffc3", byteArrayOf(0x01))
       subscribeTo("ffe1")
       super.setUp()

   }
    override fun connectionDropped() {
        super.connectionDropped()
        clear()
    }

    override fun disconnect() {
        writeWithResponse("ffc4", byteArrayOf(0x01))
       handler.postDelayed({
           listener?.onUpdated("CON", "drop it", this@BuddyPls)
           connectionDropped()
       }, 2000)
    }

    override fun onUpdated(uuidStr: String, value: ByteArray, kind: GattController.UpdateKind) {
        when(uuidStr){
            "ffc3" -> {
                if(kind == GattController.UpdateKind.Read){
                    print(TAG, "FFC3 result is ${value.hex4Human()}")
                    markDelete = value[0].toInt() == 0
                    connectingLock = false
                    connectingCount = 0
                }else if(kind == GattController.UpdateKind.Write){
                    controller?.readFrom("ffc3")
                }
            }
            else->{
                if(kind == GattController.UpdateKind.Notify){
                    print(TAG, "$uuidStr is ${value.hex4Human()}")
                    listener?.onUpdated(uuidStr, value, this@BuddyPls)
                }
            }
        }
    }
}