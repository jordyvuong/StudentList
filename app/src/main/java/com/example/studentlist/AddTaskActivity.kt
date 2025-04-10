package com.example.studentlist

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AddTaskActivity : AppCompatActivity() {

    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_task)

        // Initialisation de Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val taskNameEditText = findViewById<EditText>(R.id.taskNameEditText)
        val taskQuantityEditText = findViewById<EditText>(R.id.taskQuantityEditText)
        val taskDueDateEditText = findViewById<EditText>(R.id.taskDueDateEditText)
        val taskAssigneeSpinner = findViewById<Spinner>(R.id.taskAssigneeSpinner)
        val addTaskButton = findViewById<Button>(R.id.buttonAddTask)

        // Lorsque l'utilisateur clique sur "Ajouter la tâche"
        addTaskButton.setOnClickListener {
            val taskName = taskNameEditText.text.toString()
            val taskQuantity = taskQuantityEditText.text.toString()
            val taskDueDate = taskDueDateEditText.text.toString()
            val taskAssignee = taskAssigneeSpinner.selectedItem.toString()  // À adapter en fonction des utilisateurs du groupe

            if (taskName.isEmpty() || taskQuantity.isEmpty() || taskDueDate.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
            } else {
                addTaskToDatabase(taskName, taskQuantity, taskDueDate, taskAssignee)
            }
        }
    }

    private fun addTaskToDatabase(taskName: String, taskQuantity: String, taskDueDate: String, taskAssignee: String) {
        val taskId = database.reference.child("tasks").push().key ?: return

        // Créer la tâche à ajouter à Firebase
        val taskMap = mapOf(
            "name" to taskName,
            "quantity" to taskQuantity,
            "assigned_to" to taskAssignee,
            "status" to "not_completed",
            "due_date" to taskDueDate,
            "created_at" to System.currentTimeMillis().toString()
        )

        // Ajouter la tâche à la base de données
        database.reference.child("tasks").child(taskId).setValue(taskMap)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(this, "Tâche ajoutée avec succès", Toast.LENGTH_SHORT).show()
                    finish()  // Ferme l'activité après l'ajout de la tâche
                } else {
                    Toast.makeText(this, "Erreur lors de l'ajout de la tâche", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
