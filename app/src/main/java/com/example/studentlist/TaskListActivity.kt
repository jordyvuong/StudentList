package com.example.studentlist

import android.content.Intent
import com.example.studentlist.model.Task
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import com.google.android.material.bottomnavigation.BottomNavigationView

class TaskListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var taskList: MutableList<Task>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_list)

        recyclerView = findViewById(R.id.recyclerViewTasks)

        // Configuration du RecyclerView pour afficher les tâches
        recyclerView.layoutManager = LinearLayoutManager(this)
        taskList = mutableListOf()
        taskAdapter = TaskAdapter(taskList)
        recyclerView.adapter = taskAdapter

        loadTasks()

        // Gérer les clics sur la barre de navigation
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottomNavigation)
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_add_task -> {
                    // Démarrer AddTaskActivity lorsque le bouton "+" est cliqué
                    val intent = Intent(this, AddTaskActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_home -> {
                    // Gérer l'action "Home"
                    Toast.makeText(this, "Home clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_calendar -> {
                    // Gérer l'action "Calendar"
                    Toast.makeText(this, "Calendar clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_task -> {
                    // Gérer l'action "Documents"
                    Toast.makeText(this, "Documents clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_settings -> {
                    // Gérer l'action "Settings"
                    Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }

    // Charger les tâches depuis Firebase
    private fun loadTasks() {
        val database = FirebaseDatabase.getInstance()
        val tasksRef = database.reference.child("tasks")

        tasksRef.get().addOnSuccessListener { snapshot ->
            taskList.clear()
            for (taskSnapshot in snapshot.children) {
                val taskMap = taskSnapshot.value as? Map<String, Any>  // Récupération manuelle des données
                if (taskMap != null) {
                    // Assurez-vous que `created_at` est bien un Long et convertissez-le
                    val createdAt = (taskMap["created_at"] as? Number)?.toLong() ?: 0L  // Convertir en Long
                    val task = Task(
                        name = taskMap["name"] as? String ?: "",
                        quantity = taskMap["quantity"] as? String ?: "",
                        assigned_to = taskMap["assigned_to"] as? String ?: "",
                        status = taskMap["status"] as? String ?: "not_completed",
                        due_date = taskMap["due_date"] as? String ?: "",
                        created_at = createdAt  // Utiliser Long pour `created_at`
                    )
                    taskList.add(task)
                }
            }
            taskAdapter.notifyDataSetChanged()
        }.addOnFailureListener {
            Toast.makeText(this, "Error loading tasks", Toast.LENGTH_SHORT).show()
        }
    }

    // Ajouter une nouvelle tâche dans Firebase
    private fun addTask(taskName: String) {
        val taskId = FirebaseDatabase.getInstance().reference.child("tasks").push().key ?: return
        val task = Task(
            name = taskName,
            quantity = "1",  // Quantité par défaut
            assigned_to = "user_12345",  // Exemple d'utilisateur assigné
            status = "not_completed",
            due_date = "2025-05-01",  // Date d'échéance par défaut
            created_at = System.currentTimeMillis()  // Utiliser un timestamp Long pour la création
        )

        FirebaseDatabase.getInstance().reference.child("tasks").child(taskId).setValue(task)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(this, "Task added successfully", Toast.LENGTH_SHORT).show()
                    loadTasks()  // Recharger la liste des tâches
                } else {
                    Toast.makeText(this, "Error adding task", Toast.LENGTH_SHORT).show()
                }
            }
    }
}