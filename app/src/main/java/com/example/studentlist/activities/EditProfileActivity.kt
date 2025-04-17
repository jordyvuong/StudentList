package com.example.studentlist.activities

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.studentlist.R
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class EditProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private lateinit var usernameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var currentPasswordEditText: EditText
    private lateinit var newPasswordEditText: EditText
    private lateinit var confirmNewPasswordEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        val databaseUrl = "https://studentlist-d8d52-default-rtdb.europe-west1.firebasedatabase.app"
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(databaseUrl)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Vous devez être connecté pour modifier votre profil", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        usernameEditText = findViewById(R.id.editTextUsername)
        emailEditText = findViewById(R.id.editTextEmail)
        phoneEditText = findViewById(R.id.editTextPhone)
        currentPasswordEditText = findViewById(R.id.editTextCurrentPassword)
        newPasswordEditText = findViewById(R.id.editTextNewPassword)
        confirmNewPasswordEditText = findViewById(R.id.editTextConfirmNewPassword)

        emailEditText.setText(currentUser.email)

        loadUserData(currentUser.uid)

        val saveButton = findViewById<Button>(R.id.buttonSaveProfile)
        saveButton.setOnClickListener {
            saveProfileChanges()
        }

        val cancelButton = findViewById<Button>(R.id.buttonCancel)
        cancelButton.setOnClickListener {
            finish()
        }
    }

    private fun loadUserData(userId: String) {
        val userRef = database.reference.child("users").child(userId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val username = snapshot.child("username").getValue(String::class.java) ?: ""
                    val phone = snapshot.child("phone").getValue(String::class.java) ?: ""

                    usernameEditText.setText(username)
                    phoneEditText.setText(phone)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("EditProfileActivity", "Erreur lors du chargement des données utilisateur", error.toException())
                Toast.makeText(this@EditProfileActivity, "Erreur lors du chargement des données", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveProfileChanges() {
        val currentUser = auth.currentUser ?: return
        val username = usernameEditText.text.toString().trim()
        val phone = phoneEditText.text.toString().trim()
        val currentPassword = currentPasswordEditText.text.toString().trim()
        val newPassword = newPasswordEditText.text.toString().trim()
        val confirmNewPassword = confirmNewPasswordEditText.text.toString().trim()

        if (username.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs obligatoires", Toast.LENGTH_SHORT).show()
            return
        }

        val userUpdates = HashMap<String, Any>()
        userUpdates["username"] = username
        userUpdates["phone"] = phone

        updateUserData(currentUser.uid, userUpdates)

        if (currentPassword.isNotEmpty() && newPassword.isNotEmpty() && confirmNewPassword.isNotEmpty()) {
            if (newPassword != confirmNewPassword) {
                Toast.makeText(this, "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show()
                return
            }

            if (newPassword.length < 6) {
                Toast.makeText(this, "Le mot de passe doit contenir au moins 6 caractères", Toast.LENGTH_SHORT).show()
                return
            }

            val credential = EmailAuthProvider.getCredential(currentUser.email!!, currentPassword)
            currentUser.reauthenticate(credential)
                .addOnSuccessListener {
                    currentUser.updatePassword(newPassword)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Mot de passe mis à jour avec succès", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Erreur lors de la mise à jour du mot de passe: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erreur d'authentification: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateUserData(userId: String, userUpdates: Map<String, Any>) {
        database.reference.child("users").child(userId)
            .updateChildren(userUpdates)
            .addOnSuccessListener {
                Toast.makeText(this, "Profil mis à jour avec succès", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erreur lors de la mise à jour du profil: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
