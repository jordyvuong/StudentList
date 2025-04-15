package com.example.studentlist

import android.app.AlertDialog
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
    private lateinit var auth: FirebaseAuth

    private var listId: String = ""
    private var isAdmin: Boolean = false
    private var currentUserId: String = ""

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
        auth = FirebaseAuth.getInstance()
        currentUserId = auth.currentUser?.uid ?: ""

        // Vérifier si l'utilisateur est administrateur de cette liste
        checkAdminStatus()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_list_tasks, container, false)

        tasksRecyclerView = view.findViewById(R.id.tasksRecyclerView)
        emptyTasksText = view.findViewById(R.id.emptyTasksText)

        // Configurer le RecyclerView avec la gestion des actions sur les tâches
        taskAdapter = TaskAdapter(
            onStatusChange = { task, isCompleted ->
                updateTaskStatus(task.id, if (isCompleted) "completed" else "pending")
            },
            onLongClick = { task ->
                if (isAdmin) {
                    showTaskOptionsDialog(task)
                }
            },
            isAdmin = isAdmin
        )

        tasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = taskAdapter
        }

        loadTasks()

        return view
    }

    private fun checkAdminStatus() {
        database.child("lists").child(listId).child("owner")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val ownerId = snapshot.getValue(String::class.java) ?: ""
                    isAdmin = (ownerId == currentUserId)

                    // Mettre à jour l'adaptateur avec le statut d'admin
                    if (::taskAdapter.isInitialized) {
                        taskAdapter.updateAdminStatus(isAdmin)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Erreur lors de la vérification des droits", Toast.LENGTH_SHORT).show()
                }
            })
    }

    fun loadTasks() {
        val query = database.child("tasks")
            .orderByChild("listId")
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

                        // Collecter les IDs utilisateurs pour charger leurs détails
                        if (assignedTo.isNotEmpty() && assigneeName.isEmpty()) {
                            userIds.add(assignedTo)
                        }
                    }
                }

                // Mettre à jour l'UI
                if (tasks.isEmpty()) {
                    showEmptyState()
                } else {
                    showTasks(tasks)
                    // Si des utilisateurs sont assignés mais sans nom, charger leurs détails
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

    private fun showTaskOptionsDialog(task: Task) {
        val options = arrayOf("Supprimer", "Annuler")

        AlertDialog.Builder(requireContext())
            .setTitle("Options pour \"${task.name}\"")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> showDeleteConfirmationDialog(task)
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun showDeleteConfirmationDialog(task: Task) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmation")
            .setMessage("Êtes-vous sûr de vouloir supprimer cette tâche ?")
            .setPositiveButton("Oui") { _, _ ->
                deleteTask(task.id)
            }
            .setNegativeButton("Non", null)
            .show()
    }

    private fun deleteTask(taskId: String) {
        database.child("tasks").child(taskId).removeValue()
            .addOnSuccessListener {
                Toast.makeText(context, "Tâche supprimée avec succès", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
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