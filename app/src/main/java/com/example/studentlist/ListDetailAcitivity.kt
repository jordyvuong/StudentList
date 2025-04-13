package com.example.studentlist

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studentlist.databinding.ActivityListDetailBinding
import com.example.studentlist.model.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ListDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListDetailBinding
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var taskAdapter: TaskAdapter

    private var listId: String = ""
    private var listName: String = ""
    private var listColor: String = ""
    private var listIcon: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialiser Firebase
        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        // Récupérer les données de la liste
        listId = intent.getStringExtra("list_id") ?: ""
        listName = intent.getStringExtra("list_name") ?: ""
        listColor = intent.getStringExtra("list_color") ?: ""
        listIcon = intent.getStringExtra("list_icon") ?: ""

        // Configurer le titre
        binding.listTitleText.text = listName

        // Configurer le RecyclerView
        taskAdapter = TaskAdapter { task, isCompleted ->
            updateTaskStatus(task.id, isCompleted)
        }

        binding.tasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ListDetailActivity)
            adapter = taskAdapter
        }

        // Configurer les boutons
        binding.backButton.setOnClickListener {
            finish()
        }

        binding.addTaskButton.setOnClickListener {
            val intent = Intent(this, AddTaskActivity::class.java)
            intent.putExtra("list_id", listId)
            startActivity(intent)
        }

        // Configurer la navigation
        setupBottomNavigation()

        // Charger les tâches
        loadTasks()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_add_task -> {
                    val intent = Intent(this, AddTaskActivity::class.java)
                    intent.putExtra("list_id", listId)
                    startActivity(intent)
                    true
                }
                R.id.nav_home -> {
                    val intent = Intent(this, TaskListActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    true
                }
                R.id.nav_group -> {
                    val intent = Intent(this, GroupManagementActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_task -> {
                    Toast.makeText(this, "Documents clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_settings -> {
                    Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }

    private fun loadTasks() {
        val query = database.child("tasks")
            .orderByChild("list_id")
            .equalTo(listId)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tasks = mutableListOf<Task>()
                val userIds = mutableSetOf<String>()

                for (taskSnapshot in snapshot.children) {
                    val taskId = taskSnapshot.key ?: continue
                    val name = taskSnapshot.child("name").getValue(String::class.java) ?: ""
                    val quantity = taskSnapshot.child("quantity").getValue(String::class.java) ?: ""
                    val assignedTo = taskSnapshot.child("assigned_to").getValue(String::class.java) ?: ""
                    val status = taskSnapshot.child("status").getValue(String::class.java) ?: "pending"
                    val dueDate = taskSnapshot.child("due_date").getValue(String::class.java) ?: ""
                    val createdAt = taskSnapshot.child("created_at").getValue(Long::class.java) ?: 0L
                    val groupId = taskSnapshot.child("group_id").getValue(String::class.java) ?: ""

                    val task = Task(
                        id = taskId,
                        name = name,
                        quantity = quantity,
                        assignedTo = assignedTo,
                        status = status,
                        dueDate = dueDate,
                        createdAt = createdAt,
                        groupId = groupId
                    )

                    tasks.add(task)
                    if (assignedTo.isNotEmpty()) {
                        userIds.add(assignedTo)
                    }
                }

                if (tasks.isEmpty()) {
                    binding.emptyListText.visibility = View.VISIBLE
                    binding.tasksRecyclerView.visibility = View.GONE
                } else {
                    binding.emptyListText.visibility = View.GONE
                    binding.tasksRecyclerView.visibility = View.VISIBLE

                    // Charger les noms des assignés si nécessaire
                    if (userIds.isNotEmpty()) {
                        loadUserNames(tasks, userIds)
                    } else {
                        taskAdapter.updateTasks(tasks)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ListDetailActivity,
                    "Erreur de chargement des tâches: ${error.message}",
                    Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadUserNames(tasks: List<Task>, userIds: Set<String>) {
        val tempTasks = tasks.toMutableList()
        var completedQueries = 0

        for (userId in userIds) {
            database.child("users").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val displayName = snapshot.child("displayName").getValue(String::class.java)
                            ?: "Utilisateur inconnu"

                        // Mettre à jour le nom de l'assigné pour chaque tâche
                        tempTasks.forEachIndexed { index, task ->
                            if (task.assignedTo == userId) {
                                tempTasks[index] = task.copy(assigneeName = displayName)
                            }
                        }

                        completedQueries++
                        if (completedQueries == userIds.size) {
                            taskAdapter.updateTasks(tempTasks)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        completedQueries++
                        if (completedQueries == userIds.size) {
                            taskAdapter.updateTasks(tempTasks)
                        }
                    }
                })
        }
    }

    private fun updateTaskStatus(taskId: String, isCompleted: Boolean) {
        val status = if (isCompleted) "completed" else "pending"
        database.child("tasks").child(taskId).child("status").setValue(status)
    }
}