package com.example.studentlist

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.studentlist.model.Group
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class CreateGroupActivity : AppCompatActivity() {

    private lateinit var groupNameEditText: TextInputEditText
    private lateinit var groupDescriptionEditText: TextInputEditText
    private lateinit var createGroupButton: Button

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_group)

        // Initialiser les vues
        groupNameEditText = findViewById(R.id.groupNameEditText)
        groupDescriptionEditText = findViewById(R.id.groupDescriptionEditText)
        createGroupButton = findViewById(R.id.createGroupButton)

        // Configurer le bouton de création
        createGroupButton.setOnClickListener {
            createNewGroup()
        }
    }

    private fun createNewGroup() {
        val groupName = groupNameEditText.text.toString().trim()
        val groupDescription = groupDescriptionEditText.text.toString().trim()
        val currentUserId = auth.currentUser?.uid

        if (groupName.isEmpty()) {
            groupNameEditText.error = "Group name is required"
            return
        }

        if (currentUserId == null) {
            Toast.makeText(this, "You must be logged in to create a group", Toast.LENGTH_SHORT).show()
            return
        }

        // Créer une référence pour le nouveau groupe
        val groupRef = database.child("groups").push()
        val groupId = groupRef.key ?: return

        // Initialiser le groupe avec l'utilisateur courant comme admin et membre
        val newGroup = Group(
            id = groupId,
            name = groupName,
            description = groupDescription,
            adminId = currentUserId,
            members = mutableMapOf(currentUserId to true)  // Ajoute l'admin comme membre accepté
        )

        // Enregistrer le groupe dans la base de données
        groupRef.setValue(newGroup)
            .addOnSuccessListener {
                // Ajouter le groupe à la liste des groupes de l'utilisateur
                database.child("users").child(currentUserId).child("groups").child(groupId).setValue(true)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Group created successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}