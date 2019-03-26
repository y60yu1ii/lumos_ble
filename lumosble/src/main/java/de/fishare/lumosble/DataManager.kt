package de.fishare.lumosble

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import java.util.*

class DataManager private constructor(var context : Context) {
    companion object : SingletonHolder<DataManager, Context>(::DataManager) {
        const val TAG = "DataManager"
        const val GLOBAL = "global"
        const val HISTORY = "history"
        const val NULL = "string.null"
    }

    fun getPhoneUUID(): String {
        var uuid = defaults.getString("GUUID", "NULL")
        if (uuid.equals("NULL", true)) {
            uuid = UUID.randomUUID().toString()
            val editor = defaults.edit()
            editor.putString("GUUID", uuid)
            editor.apply()
        }
        return uuid
    }

    private val defaults: SharedPreferences by lazy{
        context.getSharedPreferences(GLOBAL, Context.MODE_PRIVATE)
    }

    fun addToHistory(mac: String) {
        val history = HashSet<String>(defaults.getStringSet(HISTORY, HashSet<String>()))
        if (!history.contains(mac)) {
            history.add(mac)
            print(TAG, "save $history")
            val editor = defaults.edit()
            editor.putStringSet(HISTORY, history)
            editor.apply()
        }
    }

    fun removeFromHistory(mac: String) {
        print(TAG, "remove mac $mac")
        val history = HashSet<String>(defaults.getStringSet(HISTORY, HashSet<String>()))
        val copy = HashSet(history)
        copy.remove(mac)
        val editor = defaults.edit()
        editor.remove(HISTORY)
        editor.putStringSet(HISTORY, copy)
        editor.apply()
    }

    fun getHistory(): List<String> {
        val history = HashSet<String>(defaults.getStringSet(HISTORY, HashSet<String>()))
        print(TAG, "history has $history")
        return history.toList()
    }

    fun saveProfile(mac:String, para:String, value:Any){
        context.getSharedPreferences(mac, Context.MODE_PRIVATE).edit().also{
            if(value is String) {
                it.putString(para, value)
            }
        }.apply()
    }

    fun loadProfileInString(mac:String, para: String):String{
        return context.getSharedPreferences(mac, Context.MODE_PRIVATE).getString(para, NULL) ?: NULL
    }
}