package de.fishare.lumosble

import java.io.File

class PKGSender{
    lateinit var file: File
    private var updateData: ByteArray = byteArrayOf()
    private var updateIndex: Int = 0
    private var packageSize  = 20
    private val DEFINED_PACKAGE_SIZE = 20
    lateinit var controller:GattController
    lateinit var writeUUID:String
    var isFinished:Boolean = false

    fun prepare(f: File, c:GattController){
        file = f
        controller = c
        updateData  = file.readBytes()
        updateIndex = updateData.indices.firstOrNull{ updateData[it] == 0xB5.toByte() }?:0
        send()
    }

    fun send(){
        val length = file.length().toInt()
        packageSize = if (updateIndex + DEFINED_PACKAGE_SIZE <= length) DEFINED_PACKAGE_SIZE else length - updateIndex
        val payload = updateData.copyOfRange(updateIndex, updateIndex + packageSize)
//       print("PKG", "agps pkg size is $packageSize and index is $updateIndex and file length is ${updateData.size}")
        controller.writeTo(writeUUID, payload, false)
    }

    fun onWrite(callback: (finished:Boolean)->Unit){
        val len = updateData.size
        updateIndex += packageSize

        if(updateIndex >= len){
            print("PKG", "update complete")
            isFinished = true
        } else{
            send()
        }
        callback(isFinished)
    }

    fun clear(){
        isFinished  = false
        updateIndex = 0
        updateData  = byteArrayOf()
    }
}