package com.example.studentlist.model

data class Group(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val adminId: String = "",
    val isAdmin: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val members: MutableMap<String, Boolean> = mutableMapOf(),
    val archivedLists: MutableList<String> = mutableListOf()
)