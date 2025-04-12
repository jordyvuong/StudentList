package com.example.studentlist.model

data class Task(
    val name: String = "",
    val quantity: String = "",
    val assigned_to: String = "",
    var status: String = "pending",
    val due_date: String = "",
    val created_at: Long = 0,
    val group_id: String = ""  // Cette propriété est peut-être manquante
)