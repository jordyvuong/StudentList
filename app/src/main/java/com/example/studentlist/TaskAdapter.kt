package com.example.studentlist

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.studentlist.databinding.ItemTaskBinding
import com.example.studentlist.model.Task

class TaskAdapter(
    private val tasks: MutableList<Task> = mutableListOf(),
    private val onTaskStatusChanged: (Task, Boolean) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root)

    fun updateTasks(newTasks: List<Task>) {
        tasks.clear()
        tasks.addAll(newTasks)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]

        with(holder.binding) {
            // Configurer le titre de la tâche
            taskTitle.text = task.name

            // Configurer la quantité
            if (task.quantity.isNotEmpty() && task.quantity != "0") {
                taskQuantity.text = task.quantity
                taskQuantity.visibility = android.view.View.VISIBLE
            } else {
                taskQuantity.visibility = android.view.View.GONE
            }

            // Configurer l'assigné
            taskAssignee.text = "Assigné à: ${task.assigneeName.ifEmpty { "Non assigné" }}"

            // Configurer l'état de la checkbox sans déclencher le listener
            taskCheckbox.setOnCheckedChangeListener(null)
            taskCheckbox.isChecked = task.isCompleted

            // Appliquer un style barré si la tâche est complétée
            if (task.isCompleted) {
                taskTitle.paintFlags = taskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                taskTitle.paintFlags = taskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            // Ajouter le listener après avoir configuré l'état
            taskCheckbox.setOnCheckedChangeListener { _, isChecked ->
                onTaskStatusChanged(task, isChecked)
            }
        }
    }

    override fun getItemCount() = tasks.size
}