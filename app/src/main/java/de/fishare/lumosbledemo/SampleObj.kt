package de.fishare.lumosbledemo

import de.fishare.lumosble.GattController
import de.fishare.lumosble.PeriObj

class SampleObj(mac: String) : PeriObj(mac) {

   override fun disconnect() {
      writeWithResponse("xx", byteArrayOf(0x00))
   }

   override fun authAndSubscribe(){
      writeTo("ffe1", "LOCK".toByteArray())
      writeTo("ffe2", "key".toByteArray())
      writeTo("ffe3", byteArrayOf(0x05))
      controller?.subscribeTo("ffe1")
       super.authAndSubscribe()
   }

   override fun getUpdated(uuidStr: String, value: ByteArray, kind: GattController.UpdateKind) {
      when(uuidStr){
         "ffe3" -> {
            if(kind == GattController.UpdateKind.Read){
               isAuthSuccess = value[0].toInt() == 1
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