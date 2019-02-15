package de.fishare.lumosbledemo.demos

import de.fishare.lumosble.GattController
import de.fishare.lumosble.PeriObj

class SampleObj(mac: String) : PeriObj(mac) {
   override var TAG = "SampleObj"
   override fun disconnect() {
      writeWithResponse("xx", byteArrayOf(0x00))
   }

   override fun setUp(){
      writeTo("ffe1", "LOCK".toByteArray())
      writeTo("ffe2", "key".toByteArray())
      writeTo("ffe3", byteArrayOf(0x05))
      controller?.subscribeTo("ffe1")
       super.setUp()
   }

   override fun onUpdated(uuidStr: String, value: ByteArray, kind: GattController.UpdateKind) {
      when(uuidStr){
         "ffe3" -> {
            if(kind == GattController.UpdateKind.Read){
            }else if(kind == GattController.UpdateKind.Write){
               controller?.readFrom("ffe3")
            }
         }
         else->{
            if(kind == GattController.UpdateKind.Notify){
               listener?.onUpdated(uuidStr, value, this@SampleObj)
            }
         }
      }
   }
}