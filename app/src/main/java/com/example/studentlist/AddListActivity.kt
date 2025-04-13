package com.example.studentlist

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class AddListActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private var selectedColor: String = "green" // Couleur par défaut
    private var selectedIcon: String = "document" // Icône par défaut

    private lateinit var colorGreen: View
    private lateinit var colorBlue: View
    private lateinit var colorPurple: View
    private lateinit var colorRed: View
    private lateinit var colorOrange: View

    private lateinit var iconDocument: ImageView
    private lateinit var iconGrid: ImageView
    private lateinit var iconCart: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_addlist)

        // Initialiser Firebase
        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        // Initialiser les vues
        initViews()
        setupColorSelection()
        setupIconSelection()

        // Configuration des boutons
        val buttonCancel: Button = findViewById(R.id.buttonCancel)
        buttonCancel.setOnClickListener {
            finish()
        }

        val buttonCreate: Button = findViewById(R.id.buttonCreate)
        buttonCreate.setOnClickListener {
            createNewList()
        }
    }

    private fun initViews() {
        colorGreen = findViewById(R.id.colorGreen)
        colorBlue = findViewById(R.id.colorBlue)
        colorPurple = findViewById(R.id.colorPurple)
        colorRed = findViewById(R.id.colorRed)
        colorOrange = findViewById(R.id.colorOrange)

        iconDocument = findViewById(R.id.iconDocument)
        iconGrid = findViewById(R.id.iconGrid)
        iconCart = findViewById(R.id.iconCart)
    }

    private fun setupColorSelection() {
        val colorViews = listOf(colorGreen, colorBlue, colorPurple, colorRed, colorOrange)
        val colorValues = listOf("green", "blue", "purple", "red", "orange")

        colorViews.forEachIndexed { index, view ->
            view.setOnClickListener {
                // Reset toutes les vues
                colorViews.forEach { it.scaleX = 1.0f; it.scaleY = 1.0f }
                // Agrandir la couleur sélectionnée
                view.scaleX = 1.2f
                view.scaleY = 1.2f
                selectedColor = colorValues[index]
            }
        }

        // Sélection par défaut
        colorGreen.scaleX = 1.2f
        colorGreen.scaleY = 1.2f
    }

    private fun setupIconSelection() {
        val iconViews = listOf(iconDocument, iconGrid, iconCart)
        val iconValues = listOf("document", "grid", "cart")

        iconViews.forEachIndexed { index, view ->
            view.setOnClickListener {
                // Reset tous les fonds
                iconViews.forEach { it.setBackgroundResource(R.drawable.circle_background_light_gray) }

                // Changer le fond de l'icône sélectionnée selon la couleur choisie
                when (selectedColor) {
                    "green" -> view.setBackgroundResource(R.drawable.circle_background_green)
                    "blue" -> view.setBackgroundResource(R.drawable.circle_background_blue)
                    "purple" -> view.setBackgroundResource(R.drawable.circle_background_purple)
                    "red" -> view.setBackgroundResource(R.drawable.circle_background_red)
                    "orange" -> view.setBackgroundResource(R.drawable.circle_background_orange)
                }

                selectedIcon = iconValues[index]
            }
        }

        // Sélection par défaut
        iconDocument.setBackgroundResource(R.drawable.circle_background_green)
    }

    private fun createNewList() {
        val editTextListName: TextInputEditText = findViewById(R.id.editTextListName)
        val listName = editTextListName.text.toString().trim()

        if (listName.isEmpty()) {
            Toast.makeText(this, "Veuillez entrer un nom pour la liste", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show()
            return
        }

        // Générer une clé unique pour la liste
        val listId = database.child("lists").push().key
        if (listId == null) {
            Toast.makeText(this, "Erreur lors de la création de la liste", Toast.LENGTH_SHORT).show()
            return
        }

        // Créer l'objet liste
        val list = HashMap<String, Any>()
        list["name"] = listName
        list["color"] = selectedColor
        list["icon"] = selectedIcon
        list["owner"] = userId
        list["created_at"] = System.currentTimeMillis()

        // Sauvegarder la liste dans la base de données
        database.child("lists").child(listId).setValue(list)
            .addOnSuccessListener {
                Toast.makeText(this, "Liste créée avec succès", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}