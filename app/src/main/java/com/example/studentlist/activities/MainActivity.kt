package com.example.studentlist.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.studentlist.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Trouver le bouton "Let's Start" et ajouter un listener
        val letsStartButton = findViewById<Button>(R.id.letsStartButton)

        letsStartButton.setOnClickListener {
            // Naviguer vers l'écran principal après le clic sur le bouton
            val intent = Intent(this, LoginActivity::class.java) // Remplace SomeOtherActivity par l'activité suivante
            startActivity(intent)
        }
    }
}
