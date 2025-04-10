package com.example.studentlist

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import android.util.Log

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // Spécifie l'URL de la base de données Firebase
        val databaseUrl = "https://studentlist-d8d52-default-rtdb.europe-west1.firebasedatabase.app"
        database = FirebaseDatabase.getInstance(databaseUrl)

        auth = FirebaseAuth.getInstance()

        val usernameEditText = findViewById<EditText>(R.id.editTextUsername)
        val emailEditText = findViewById<EditText>(R.id.editTextEmail)
        val phoneEditText = findViewById<EditText>(R.id.editTextPhone)
        val passwordEditText = findViewById<EditText>(R.id.editTextPassword)
        val registerButton = findViewById<Button>(R.id.buttonRegister)

        // Lorsque l'utilisateur clique sur le bouton "Register"
        registerButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val email = emailEditText.text.toString()
            val phone = phoneEditText.text.toString()
            val password = passwordEditText.text.toString()

            // Validation des champs
            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
            } else if (password.length < 6) {
                Toast.makeText(this, "Le mot de passe doit contenir au moins 6 caractères", Toast.LENGTH_SHORT).show()
            } else {
                registerUser(username, email, phone, password)
            }
        }

        // Lien pour rediriger vers l'écran de connexion
        val signInTextView = findViewById<TextView>(R.id.signInText)
        signInTextView.setOnClickListener {
            // Naviguer vers l'activité de connexion
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun registerUser(username: String, email: String, phone: String, password: String) {
        // Créer l'utilisateur avec l'email et le mot de passe
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // L'utilisateur est inscrit, on récupère l'ID de l'utilisateur
                    val user = auth.currentUser
                    val userId = user?.uid

                    // Prépare les données utilisateur pour Firebase Realtime Database
                    val userMap = mapOf(
                        "username" to username,
                        "email" to email,
                        "phone" to phone,
                        "role" to "user", // Par défaut, l'utilisateur a le rôle "user"
                        "created_at" to System.currentTimeMillis().toString() // Ajout de la date de création
                    )

                    if (userId != null) {
                        // Enregistrer l'utilisateur dans Firebase Realtime Database
                        database.reference.child("users").child(userId).setValue(userMap)
                            .addOnCompleteListener {
                                if (it.isSuccessful) {
                                    // Inscription réussie, on informe l'utilisateur
                                    Toast.makeText(this, "Inscription réussie", Toast.LENGTH_SHORT).show()

                                    // Rediriger vers MainActivity après inscription
                                    val intent = Intent(this, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()  // Ferme SignupActivity pour ne pas revenir en arrière
                                } else {
                                    // Erreur lors de l'enregistrement des données dans la base de données
                                    Toast.makeText(this, "Erreur lors de l'inscription dans la base de données", Toast.LENGTH_SHORT).show()
                                    Log.e("SignupActivity", "Erreur lors de l'inscription dans la base de données: ${it.exception?.message}")
                                }
                            }
                    }
                } else {
                    // Afficher l'erreur spécifique si l'inscription échoue
                    val errorMessage = task.exception?.message ?: "Erreur inconnue"
                    Toast.makeText(this, "Erreur lors de l'inscription: $errorMessage", Toast.LENGTH_SHORT).show()
                    Log.e("SignupActivity", "Erreur d'inscription: $errorMessage")
                }
            }
    }
}
