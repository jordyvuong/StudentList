package com.example.studentlist.model

data class Task(
    val id: String = "",
    val name: String = "",
    val quantity: String = "",
    val assignedTo: String = "",
    val assigneeName: String = "",
    val status: String = "pending",
    val dueDate: String = "",
    val createdAt: Long = 0,
    val groupId: String = ""
) {
    val isCompleted: Boolean
        get() = status == "completed"
}