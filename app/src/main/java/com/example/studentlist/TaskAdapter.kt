package com.example.studentlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.studentlist.model.Task
import com.google.firebase.database.FirebaseDatabase

class TaskAdapter(private val taskList: MutableList<Task>) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]

        // Affichage du nom de la tâche
        holder.taskName.text = task.name

        // Affichage de la quantité (si disponible)
        if (task.quantity.isNotEmpty() && task.quantity != "1") {
            holder.taskName.text = "${task.name} (${task.quantity})"
        } else {
            holder.taskName.text = task.name
        }

        // Affichage de la date d'échéance
        val displayTime = if (task.due_date.isNotEmpty()) {
            task.due_date
        } else {
            "Pas de date limite"
        }
        holder.taskTime.text = displayTime

        // Afficher l'état de la tâche (cochée ou non)
        holder.taskStatus.isChecked = task.status == "completed"

        // Gérer le changement d'état de la case à cocher
        holder.taskStatus.setOnCheckedChangeListener { _, isChecked ->
            val newStatus = if (isChecked) "completed" else "pending"

            // Ne mettre à jour que si le statut a changé
            if (task.status != newStatus) {
                task.status = newStatus

                // Mettre à jour la tâche dans Firebase
                updateTaskStatusInFirebase(task, position, holder.itemView)
            }
        }
    }

    private fun updateTaskStatusInFirebase(task: Task, position: Int, view: View) {
        // Trouver l'ID de la tâche dans Firebase
        val database = FirebaseDatabase.getInstance()
        val tasksRef = database.reference.child("tasks")

        // Rechercher la tâche par ses propriétés
        tasksRef.get().addOnSuccessListener { snapshot ->
            var taskFound = false

            // Parcourir toutes les tâches pour trouver celle à mettre à jour
            for (taskSnapshot in snapshot.children) {
                val taskMap = taskSnapshot.value as? Map<String, Any>

                if (taskMap != null &&
                    taskMap["name"] == task.name &&
                    taskMap["created_at"] == task.created_at) {

                    // Mise à jour du statut dans Firebase
                    taskSnapshot.ref.child("status").setValue(task.status)
                        .addOnSuccessListener {
                            // Notification de succès
                            notifyItemChanged(position)
                        }
                        .addOnFailureListener { err ->
                            // En cas d'erreur, revert du changement et affichage d'un message
                            task.status = if (task.status == "completed") "pending" else "completed"
                            notifyItemChanged(position)
                            Toast.makeText(view.context,
                                "Erreur: Impossible de mettre à jour la tâche: ${err.message}",
                                Toast.LENGTH_SHORT).show()
                        }

                    taskFound = true
                    break
                }
            }

            // Si la tâche n'a pas été trouvée
            if (!taskFound) {
                Toast.makeText(view.context,
                    "Erreur: Tâche non trouvée dans la base de données",
                    Toast.LENGTH_SHORT).show()

                // Revert du changement dans l'UI
                task.status = if (task.status == "completed") "pending" else "completed"
                notifyItemChanged(position)
            }
        }.addOnFailureListener { err ->
            // En cas d'erreur de connexion à Firebase
            Toast.makeText(view.context,
                "Erreur de connexion à la base de données: ${err.message}",
                Toast.LENGTH_SHORT).show()

            // Revert du changement dans l'UI
            task.status = if (task.status == "completed") "pending" else "completed"
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int {
        return taskList.size
    }

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val taskName: TextView = itemView.findViewById(R.id.taskNameTextView)
        val taskTime: TextView = itemView.findViewById(R.id.taskTimeTextView)
        val taskStatus: CheckBox = itemView.findViewById(R.id.taskStatusCheckBox)
    }
}