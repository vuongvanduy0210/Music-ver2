package com.vuongvanduy.music.SharedPreferences

import android.annotation.SuppressLint
import android.content.Context
import com.vuongvanduy.music.util.KEY_THEME_MODE

@SuppressLint("StaticFieldLeak")
object DataLocalManager {

    private var instance: DataLocalManager? = null
    private var sharedPreferences: MySharedPreferences? = null

    fun init(context: Context) {
        instance = DataLocalManager
        instance!!.sharedPreferences = MySharedPreferences(context)
    }

    private fun getInstance(): DataLocalManager? {
        if (instance == null) {
            instance = DataLocalManager
        }
        return instance
    }

    fun putStringThemeMode(value: String) {
        getInstance()?.sharedPreferences?.putStringValue(KEY_THEME_MODE, value)
    }

    fun getStringThemeMode(): String? {
        return getInstance()?.sharedPreferences?.getStringValue(KEY_THEME_MODE)
    }
}