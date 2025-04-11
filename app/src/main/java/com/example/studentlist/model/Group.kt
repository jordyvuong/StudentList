package com.example.studentlist.model
data class Group(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val adminId: String = "",  // ID de l'utilisateur admin
    val createdAt: Long = System.currentTimeMillis(),
    val members: MutableMap<String, Boolean> = mutableMapOf(),  // Map des ID utilisateurs et leur statut (true = accepté, false = en attente)
    val archivedLists: MutableList<String> = mutableListOf()  // IDs des listes archivées
)