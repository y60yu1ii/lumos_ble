package de.fishare.lumosble

import android.content.Intent
import android.os.ParcelUuid
import android.os.Parcelable
import android.util.Log
import java.util.*

val CONNECTION_EVENT = "de.fishare.connection"

fun UUID.short():String {return this.toString().substring(4, 8)}
fun String.getUUID(): UUID {return UUID.fromString("0000$this-0000-1000-8000-00805f9b34fb")}
fun String.toParcelUUID(): ParcelUuid {return ParcelUuid.fromString("0000$this-0000-1000-8000-00805f9b34fb")}

fun print(tag: String, log:Any) { if (BuildConfig.DEBUG) Log.e(tag, log.toString()) }
fun print(log:Any) { if (BuildConfig.DEBUG) Log.e("DEBUG", log.toString()) }

fun ByteArray.hex4EasyRead():String{
    val sb = StringBuilder()
    for (b in this) sb.append(String.format("%02X ", b))
    return sb.toString()
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
    val rawData: ByteArray?
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