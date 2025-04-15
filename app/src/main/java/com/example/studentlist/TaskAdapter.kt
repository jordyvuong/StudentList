package com.example.studentlist

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.studentlist.model.Task

class TaskAdapter(
    private val onStatusChange: (Task, Boolean) -> Unit,
    private val onLongClick: (Task) -> Unit,
    private var isAdmin: Boolean = false
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private var tasks: List<Task> = listOf()

    fun updateTasks(newTasks: List<Task>) {
        this.tasks = newTasks
        notifyDataSetChanged()
    }

    fun updateAdminStatus(isAdmin: Boolean) {
        this.isAdmin = isAdmin
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.bind(task, isAdmin)
    }

    override fun getItemCount(): Int = tasks.size

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val taskName: TextView = itemView.findViewById(R.id.taskName)
        private val taskQuantity: TextView = itemView.findViewById(R.id.taskQuantity)
        private val assignedTo: TextView = itemView.findViewById(R.id.assignedTo)
        private val dueDate: TextView = itemView.findViewById(R.id.dueDate)
        private val checkBox: CheckBox = itemView.findViewById(R.id.statusCheckBox)

        fun bind(task: Task, isAdmin: Boolean) {
            taskName.text = task.name
            taskQuantity.text = task.quantity

            // Appliquer le style barré si la tâche est complétée
            if (task.isCompleted) {
                taskName.paintFlags = taskName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                taskQuantity.paintFlags = taskQuantity.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                taskName.paintFlags = taskName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                taskQuantity.paintFlags = taskQuantity.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            // Afficher le nom de l'utilisateur assigné s'il existe
            if (task.assigneeName.isNotEmpty()) {
                assignedTo.text = "Assigné à: ${task.assigneeName}"
                assignedTo.visibility = View.VISIBLE
            } else if (task.assignedTo.isNotEmpty()) {
                assignedTo.text = "Assigné à: Utilisateur ${task.assignedTo}"
                assignedTo.visibility = View.VISIBLE
            } else {
                assignedTo.visibility = View.GONE
            }

            // Afficher la date d'échéance si elle existe
            if (task.dueDate.isNotEmpty()) {
                dueDate.text = "Échéance: ${task.dueDate}"
                dueDate.visibility = View.VISIBLE
            } else {
                dueDate.visibility = View.GONE
            }

            // Configurer la case à cocher
            checkBox.isChecked = task.isCompleted
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                onStatusChange(task, isChecked)
            }

            // Configurer l'appui long pour supprimer la tâche (uniquement pour l'admin)
            if (isAdmin) {
                itemView.setOnLongClickListener {
                    onLongClick(task)
                    true
                }
            } else {
                itemView.setOnLongClickListener(null)
                itemView.isLongClickable = false
            }
        }
    }
}