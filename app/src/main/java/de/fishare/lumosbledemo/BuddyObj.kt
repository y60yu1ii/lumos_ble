package de.fishare.lumosbledemo

import de.fishare.lumosble.GattController
import de.fishare.lumosble.PeriObj

class BuddyObj(mac: String) : PeriObj(mac) {

   override fun disconnect() {
      writeWithResponse("ffc4", byteArrayOf(0x00))
   }

   override fun authAndSubscribe(){
      writeTo("ffc1", "PQD".toByteArray())
      writeTo("ffc2", "aaaaaa".toByteArray())
      writeTo("ffc3", byteArrayOf(0x01))
      controller?.subscribeTo("ffe1")
       super.authAndSubscribe()
   }

   override fun getUpdated(uuidStr: String, value: ByteArray, kind: GattController.UpdateKind) {
      when(uuidStr){
         "ffc3" -> {
            if(kind == GattController.UpdateKind.Read){
               isAuthSuccess = value[0].toInt() == 1
            }else if(kind == GattController.UpdateKind.Write){
               controller?.readFrom("ffc3")
            }
         }
         else->{
            if(kind == GattController.UpdateKind.Notify){
               listener?.onUpdated(uuidStr, value, this@BuddyObj)
            }
         }
      }
   }
}