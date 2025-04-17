package com.example.studentlist // Remplacez par votre package

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class CookiesPolicyActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var preferenceCookiesSwitch: SwitchCompat
    private lateinit var analyticsCookiesSwitch: SwitchCompat
    private lateinit var saveCookiePreferencesButton: Button
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cookies_policy)

        sharedPreferences = getSharedPreferences("cookie_preferences", MODE_PRIVATE)
        backButton = findViewById(R.id.backButton)
        preferenceCookiesSwitch = findViewById(R.id.preferenceCookiesSwitch)
        analyticsCookiesSwitch = findViewById(R.id.analyticsCookiesSwitch)
        saveCookiePreferencesButton = findViewById(R.id.saveCookiePreferencesButton)
        backButton.setOnClickListener {
            onBackPressed()
        }

        loadCookiePreferences()

        saveCookiePreferencesButton.setOnClickListener {
            saveCookiePreferences()
            Toast.makeText(this@CookiesPolicyActivity, "Préférences de cookies sauvegardées", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadCookiePreferences() {
        val preferenceCookiesEnabled = sharedPreferences.getBoolean("preference_cookies_enabled", true)
        val analyticsCookiesEnabled = sharedPreferences.getBoolean("analytics_cookies_enabled", true)

        preferenceCookiesSwitch.isChecked = preferenceCookiesEnabled
        analyticsCookiesSwitch.isChecked = analyticsCookiesEnabled
    }

    private fun saveCookiePreferences() {
        val editor = sharedPreferences.edit()
        editor.putBoolean("preference_cookies_enabled", preferenceCookiesSwitch.isChecked)
        editor.putBoolean("analytics_cookies_enabled", analyticsCookiesSwitch.isChecked)
        editor.apply()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}