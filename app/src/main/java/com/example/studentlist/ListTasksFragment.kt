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
            updateTaskStatus(task.id, isCompleted)
        }

        tasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = taskAdapter
        }

        loadTasks()

        return view
    }

    fun loadTasks() {
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
                    emptyTasksText.visibility = View.VISIBLE
                    tasksRecyclerView.visibility = View.GONE
                } else {
                    emptyTasksText.visibility = View.GONE
                    tasksRecyclerView.visibility = View.VISIBLE

                    // Charger les noms des assignés si nécessaire
                    if (userIds.isNotEmpty()) {
                        loadUserNames(tasks, userIds)
                    } else {
                        taskAdapter.updateTasks(tasks)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(),
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
                        val displayName = snapshot.child("username").getValue(String::class.java)
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