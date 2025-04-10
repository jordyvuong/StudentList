package com.example.studentlist.model

data class Task(
    val name: String = "",
    val quantity: String = "",
    val assigned_to: String = "",
    var status: String = "not_completed",  // "not_completed" ou "completed"
    val due_date: String = "",
    val created_at: Long = 0L  // Firebase stocke les timestamps sous forme de Number, qui est converti en Long
)
