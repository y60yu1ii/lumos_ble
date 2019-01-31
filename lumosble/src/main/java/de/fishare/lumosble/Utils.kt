package de.fishare.lumosble

import android.content.Intent
import android.os.ParcelUuid
import android.os.Parcelable
import android.util.Log
import java.util.*
object Event{
    @JvmField val CONNECTION = "de.fishare.connection"
    @JvmField val REFRESH = "de.fishare.refresh"
}


fun UUID.short():String {return this.toString().substring(4, 8)}
fun String.getUUID(): UUID {return UUID.fromString("0000$this-0000-1000-8000-00805f9b34fb")}
fun String.toParcelUUID(): ParcelUuid {return ParcelUuid.fromString("0000$this-0000-1000-8000-00805f9b34fb")}

fun print(tag: String, log:Any) {Log.e(tag, log.toString()) }
fun print(log:Any) {Log.e("DEBUG", log.toString()) }

fun ByteArray.hex4Human():String{
    val sb = StringBuilder()
    for (b in this) sb.append(String.format("%02X ", b))
    return sb.toString()
}

fun ByteArray.to2Int(): Int =
    (this[1].toInt() shl 8) or (this[0].toInt() and 0xFF)
fun ByteArray.to2unsignedInt(): Int =
    (this[1].toInt() and 0xFF) shl 8 or (this[0].toInt() and 0xFF)

fun ByteArray.to4Int(): Int =
    (this[3].toInt() shl 24) or (this[2].toInt() and 0xFF) or (this[1].toInt() shl 8) or (this[0].toInt() and 0xFF)

fun ByteArray.to4unsignedInt(): Long =
    ((this[3].toInt() and 0xFF) shl 24 or (this[2].toInt() and 0xFF) shl 16 or (this[1].toInt() and 0xFF) shl 8 or (this[0].toInt() and 0xFF)).toLong()

fun Int.remap(fromX:Int, fromY:Int, toX:Int, toY:Int):Int{
    val r =  this *  (toY - toX) / (fromY - fromX)
    return if(r > toY) toY else if(r < toX) toX else r
}

fun Intent.addExtra(key: String, value: Any?) {
    when (value) {
        is Long -> putExtra(key, value)
        is String -> putExtra(key, value)
        is Boolean -> putExtra(key, value)
        is Float -> putExtra(key, value)
        is Double -> putExtra(key, value)
        is Int -> putExtra(key, value)
        is Parcelable -> putExtra(key, value)
    }
}

fun parseScanRecord(scanRecord: ByteArray): Map<Int,ByteArray> {
    val dict = mutableMapOf<Int, ByteArray>()
    var index = 0
    while (index < scanRecord.size) {
        val length = scanRecord[index++].toInt()
        //if no record
        if (length == 0) break
        //type
        val type = scanRecord[index].toInt()
        //if not valid type
//        print("UTILS", "[MANUFACTURE] type is $type")
//        print("UTILS", "[MANUFACTURE] length is $length")
        if (type == 0) break
        dict[type] = Arrays.copyOfRange(scanRecord, index + 1, index + length)
        //next
        index += length
    }

    return dict
}