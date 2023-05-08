package com.vuongvanduy.music.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth
import com.vuongvanduy.music.R
import com.vuongvanduy.music.shared_preferences.DataLocalManager
import com.vuongvanduy.music.util.DARK_MODE
import com.vuongvanduy.music.util.LIGHT_MODE
import com.vuongvanduy.music.util.SYSTEM_MODE

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        setThemeMode()

        Looper.myLooper()?.let {
            Handler(it).postDelayed({ nextActivity() }, 2000)
        }
    }

    private fun setThemeMode() {
        Log.e("Splash Activity", "${DataLocalManager.getStringThemeMode()}")
        when (DataLocalManager.getStringThemeMode()) {
            SYSTEM_MODE ->
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

            LIGHT_MODE ->
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

            DARK_MODE ->
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    private fun nextActivity() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            // chua login
            val intent = Intent(this@SplashActivity, SignInActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // da login
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}