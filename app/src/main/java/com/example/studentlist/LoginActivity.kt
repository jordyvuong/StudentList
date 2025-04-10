package com.example.studentlist

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val emailEditText = findViewById<EditText>(R.id.editTextEmail)
        val passwordEditText = findViewById<EditText>(R.id.editTextPassword)
        val loginButton = findViewById<Button>(R.id.buttonLogin)
        val signUpText = findViewById<TextView>(R.id.signUpText)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
            } else {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Connexion réussie", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            val errorMessage = task.exception?.message ?: "Erreur de connexion"
                            Toast.makeText(this, "Erreur : $errorMessage", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        signUpText.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }
    }
}
