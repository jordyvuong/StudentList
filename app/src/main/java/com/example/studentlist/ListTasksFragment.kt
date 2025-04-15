package com.example.studentlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studentlist.model.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ListTasksFragment : Fragment() {

    private lateinit var tasksRecyclerView: RecyclerView
    private lateinit var emptyTasksText: TextView
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var database: DatabaseReference

    private var listId: String = ""

    companion object {
        fun newInstance(listId: String): ListTasksFragment {
            val fragment = ListTasksFragment()
            val args = Bundle()
            args.putString("list_id", listId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            listId = it.getString("list_id", "")
        }
        database = FirebaseDatabase.getInstance().reference
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_list_tasks, container, false)

        tasksRecyclerView = view.findViewById(R.id.tasksRecyclerView)
        emptyTasksText = view.findViewById(R.id.emptyTasksText)

        // Configurer le RecyclerView
        taskAdapter = TaskAdapter { task, isCompleted ->
            updateTaskStatus(task.id, if (isCompleted) "completed" else "pending")
        }

        tasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = taskAdapter
        }

        loadTasks()

        return view
    }

    fun loadTasks() {
        val query = database.child("tasks")
            .orderByChild("listId")  // Correction: utiliser listId au lieu de list_id
            .equalTo(listId)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tasks = mutableListOf<Task>()
                val userIds = mutableSetOf<String>()

                for (taskSnapshot in snapshot.children) {
                    val taskId = taskSnapshot.key ?: continue
                    val name = taskSnapshot.child("name").getValue(String::class.java) ?: ""
                    val quantity = taskSnapshot.child("quantity").getValue(String::class.java) ?: ""
                    val assignedTo = taskSnapshot.child("assignedTo").getValue(String::class.java) ?: ""
                    val assigneeName = taskSnapshot.child("assigneeName").getValue(String::class.java) ?: ""
                    val status = taskSnapshot.child("status").getValue(String::class.java) ?: "pending"
                    val dueDate = taskSnapshot.child("dueDate").getValue(String::class.java) ?: ""
                    val createdAt = taskSnapshot.child("createdAt").getValue(Long::class.java) ?: 0L
                    val taskListId = taskSnapshot.child("listId").getValue(String::class.java) ?: ""

                    // Vérifier que cette tâche appartient bien à notre liste
                    if (taskListId == listId) {
                        val task = Task(
                            id = taskId,
                            name = name,
                            quantity = quantity,
                            assignedTo = assignedTo,
                            assigneeName = assigneeName,
                            status = status,
                            dueDate = dueDate,
                            createdAt = createdAt,
                            listId = taskListId
                        )

                        tasks.add(task)
                        if (assignedTo.isNotEmpty()) {
                            userIds.add(assignedTo)
                        }
                    }
                }

                // Mettre à jour l'UI
                if (tasks.isEmpty()) {
                    showEmptyState()
                } else {
                    showTasks(tasks)
                    // Si nous avons des utilisateurs assignés, récupérer leurs noms
                    if (userIds.isNotEmpty()) {
                        loadUserDetails(tasks, userIds)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Erreur de chargement des tâches: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadUserDetails(tasks: List<Task>, userIds: Set<String>) {
        val tempTasks = tasks.toMutableList()
        var completedQueries = 0

        for (userId in userIds) {
            database.child("users").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val username = snapshot.child("username").getValue(String::class.java) ?: "Utilisateur inconnu"

                        // Mettre à jour les tâches assignées à cet utilisateur avec son nom
                        tempTasks.forEachIndexed { index, task ->
                            if (task.assignedTo == userId && task.assigneeName.isEmpty()) {
                                tempTasks[index] = task.copy(assigneeName = username)
                            }
                        }

                        completedQueries++
                        if (completedQueries == userIds.size) {
                            // Mise à jour de l'adaptateur une seule fois à la fin
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

    private fun updateTaskStatus(taskId: String, status: String) {
        database.child("tasks").child(taskId).child("status").setValue(status)
            .addOnFailureListener { e ->
                Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showTasks(tasks: List<Task>) {
        emptyTasksText.visibility = View.GONE
        tasksRecyclerView.visibility = View.VISIBLE
        taskAdapter.updateTasks(tasks)
    }

    private fun showEmptyState() {
        emptyTasksText.visibility = View.VISIBLE
        tasksRecyclerView.visibility = View.GONE
    }
}