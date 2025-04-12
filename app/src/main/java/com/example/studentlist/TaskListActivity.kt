package com.example.studentlist

import android.content.Intent
import com.example.studentlist.model.Task
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class TaskListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var taskList: MutableList<Task>
    private lateinit var emptyTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_list)

        recyclerView = findViewById(R.id.recyclerViewTasks)
        emptyTextView = findViewById(R.id.emptyTextView)

        // Configuration du RecyclerView pour afficher les tâches
        recyclerView.layoutManager = LinearLayoutManager(this)
        taskList = mutableListOf()
        taskAdapter = TaskAdapter(taskList)
        recyclerView.adapter = taskAdapter

        // Configuration du bouton d'ajout de tâche
        val fabAddTask: FloatingActionButton = findViewById(R.id.fabAddTask)
        fabAddTask.setOnClickListener {
            val intent = Intent(this, AddTaskActivity::class.java)
            startActivity(intent)
        }

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
                    val intent = Intent(this, TaskListActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_group -> {
                    // Gérer l'action "Calendar"
                    val intent = Intent(this, GroupManagementActivity::class.java)
                    startActivity(intent)
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

    // Important: recharger les tâches à chaque retour sur cette activité
    override fun onResume() {
        super.onResume()
        loadTasks() // Recharger les tâches quand on revient sur l'activité
    }

    // Charger les tâches depuis Firebase
    private fun loadTasks() {
        val database = FirebaseDatabase.getInstance()
        val tasksRef = database.reference.child("tasks")

        tasksRef.get().addOnSuccessListener { snapshot ->
            taskList.clear()
            for (taskSnapshot in snapshot.children) {
                val taskMap = taskSnapshot.value as? Map<String, Any>
                if (taskMap != null) {
                    val createdAt = (taskMap["created_at"] as? Number)?.toLong() ?: 0L
                    val task = Task(
                        name = taskMap["name"] as? String ?: "",
                        quantity = taskMap["quantity"] as? String ?: "",
                        assigned_to = taskMap["assigned_to"] as? String ?: "",
                        status = taskMap["status"] as? String ?: "pending",
                        due_date = taskMap["due_date"] as? String ?: "",
                        created_at = createdAt,
                        group_id = taskMap["group_id"] as? String ?: ""  // Récupérer le group_id
                    )
                    taskList.add(task)
                }
            }

            // Mise à jour de l'affichage
            taskAdapter.notifyDataSetChanged()

            // Afficher un message si aucune tâche n'est trouvée
            if (taskList.isEmpty()) {
                emptyTextView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyTextView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }

        }.addOnFailureListener { error ->
            Toast.makeText(this, "Erreur lors du chargement des tâches: ${error.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Ajouter une nouvelle tâche dans Firebase (pour référence future)
    private fun addTask(taskName: String) {
        val taskId = FirebaseDatabase.getInstance().reference.child("tasks").push().key ?: return
        val task = Task(
            name = taskName,
            quantity = "1",
            assigned_to = "user_12345",
            status = "pending",  // Utiliser "pending" au lieu de "not_completed"
            due_date = "2025-05-01",
            created_at = System.currentTimeMillis()
        )

        FirebaseDatabase.getInstance().reference.child("tasks").child(taskId).setValue(task)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(this, "Tâche ajoutée avec succès", Toast.LENGTH_SHORT).show()
                    loadTasks()
                } else {
                    Toast.makeText(this, "Erreur lors de l'ajout de la tâche", Toast.LENGTH_SHORT).show()
                }
            }
    }
}