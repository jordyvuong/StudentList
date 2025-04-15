package com.example.studentlist.model

data class ListMember(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val status: String = "pending" // pending, accepted, rejected
)