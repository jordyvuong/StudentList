package com.example.studentlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.studentlist.model.Task

class TaskAdapter(private val taskList: MutableList<Task>) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]
        holder.taskName.text = task.name

        val displayTime = if (task.due_date.isNotEmpty()) {
            task.due_date
        } else {
            "No time"
        }

        holder.taskTime.text = displayTime
        holder.taskStatus.isChecked = task.status == "completed"

        holder.taskStatus.setOnCheckedChangeListener { _, isChecked ->
            task.status = if (isChecked) "completed" else "not_completed"

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