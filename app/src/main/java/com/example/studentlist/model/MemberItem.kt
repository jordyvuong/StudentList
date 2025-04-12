package com.example.studentlist.model

/**
 * Classe représentant un membre d'un groupe
 * @param id Identifiant unique du membre
 * @param name Nom d'affichage du membre
 * @param email Adresse email du membre
 * @param status Statut du membre (true = accepté, false = en attente)
 */
data class MemberItem(
    val id: String,
    val name: String,
    val email: String,
    val status: Boolean
)