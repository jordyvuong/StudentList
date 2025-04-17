package com.example.studentlist

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*

class SettingsActivity : AppCompatActivity() {

    private lateinit var usernameText: TextView
    private lateinit var emailText: TextView
    private lateinit var appVersionText: TextView
    private lateinit var notificationsSwitch: SwitchCompat
    private lateinit var privacyPolicyOption: LinearLayout
    private lateinit var cookiesOption: LinearLayout
    private lateinit var logoutOption: LinearLayout
    private lateinit var editProfileButton: ImageView // Changé de LinearLayout à ImageView
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var mAuth: FirebaseAuth
    private lateinit var userRef: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialiser Firebase Auth
        mAuth = FirebaseAuth.getInstance()
        val currentUser = mAuth.currentUser

        // Initialiser les préférences partagées
        sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE)

        // Initialiser les vues
        usernameText = findViewById(R.id.usernameText)
        emailText = findViewById(R.id.emailText)
        appVersionText = findViewById(R.id.appVersionText)
        notificationsSwitch = findViewById(R.id.notificationsSwitch)
        privacyPolicyOption = findViewById(R.id.privacyPolicyOption)
        cookiesOption = findViewById(R.id.cookiesOption)
        logoutOption = findViewById(R.id.logoutOption)
        editProfileButton = findViewById(R.id.editProfileButton) // Cette ligne cause l'erreur
        bottomNavigation = findViewById(R.id.bottomNavigation)

        // Configurer la barre de navigation du bas
        setupBottomNavigation()

        // Afficher la version de l'application
        displayAppVersion()

        // Charger les données utilisateur
        currentUser?.let {
            loadUserData(it.uid)
            emailText.text = it.email
        }

        // Configurer le switch de notifications
        setupNotificationsSwitch()

        // Configurer les options cliquables
        setupClickableOptions()
    }

    private fun setupBottomNavigation() {
        bottomNavigation.selectedItemId = R.id.nav_settings
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this@SettingsActivity, TaskListActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_task -> {
                    startActivity(Intent(this@SettingsActivity, ArchivesActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_settings -> true
                else -> false
            }
        }
    }

    private fun displayAppVersion() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            appVersionText.text = packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            appVersionText.text = "1.0.0"
        }
    }

    private fun loadUserData(userId: String) {
        userRef = FirebaseDatabase.getInstance().reference.child("users").child(userId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Récupérer le nom d'utilisateur
                    val username = dataSnapshot.child("username").getValue(String::class.java)
                    username?.let {
                        usernameText.text = it
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@SettingsActivity, "Erreur lors du chargement des données utilisateur", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupNotificationsSwitch() {
        // Charger l'état actuel depuis les préférences
        val notificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", true)
        notificationsSwitch.isChecked = notificationsEnabled

        // Ajouter un listener pour sauvegarder les changements
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPreferences.edit()
            editor.putBoolean("notifications_enabled", isChecked)
            editor.apply()

            // Afficher un message de confirmation
            val message = if (isChecked) "Notifications activées" else "Notifications désactivées"
            Toast.makeText(this@SettingsActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickableOptions() {
        // Option Politique de confidentialité
        privacyPolicyOption.setOnClickListener {
            val intent = Intent(this@SettingsActivity, PrivacyPolicyActivity::class.java)
            startActivity(intent)
        }

        // Option Gestion des cookies
        cookiesOption.setOnClickListener {
            val intent = Intent(this@SettingsActivity, CookiesPolicyActivity::class.java)
            startActivity(intent)
        }

        // Option Déconnexion
        logoutOption.setOnClickListener {
            // Déconnexion de Firebase
            mAuth.signOut()

            // Rediriger vers l'écran de connexion
            val intent = Intent(this@SettingsActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Bouton Éditer le profil
        editProfileButton.setOnClickListener {
            Toast.makeText(this@SettingsActivity, "Édition du profil - Fonctionnalité à venir", Toast.LENGTH_SHORT).show()
        }
    }
}