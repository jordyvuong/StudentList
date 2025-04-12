package com.example.studentlist

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studentlist.model.MemberAdapter
import com.example.studentlist.model.MemberItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ManageMembersActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var btnInvite: Button
    private lateinit var recyclerViewMembers: RecyclerView
    private lateinit var progressBar: ProgressBar

    private lateinit var memberAdapter: MemberAdapter
    private val membersList = mutableListOf<MemberItem>()

    private val database = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    private var groupId: String = ""
    private var isAdmin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_members)

        // Get group ID from intent
        groupId = intent.getStringExtra("GROUP_ID") ?: ""
        if (groupId.isEmpty()) {
            Toast.makeText(this, "Error: Group ID not provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize views
        etEmail = findViewById(R.id.etEmail)
        btnInvite = findViewById(R.id.btnInvite)
        recyclerViewMembers = findViewById(R.id.recyclerViewMembers)
        progressBar = findViewById(R.id.progressBar)

        // Set up RecyclerView
        recyclerViewMembers.layoutManager = LinearLayoutManager(this)
        memberAdapter = MemberAdapter(
            membersList,
            isAdmin,
            { userId -> acceptMember(userId) },
            { userId -> rejectMember(userId) },
            { userId -> removeMember(userId) }
        )
        recyclerViewMembers.adapter = memberAdapter

        // Check if current user is admin and load members
        checkAdminStatus()

        // Set invite button click listener
        btnInvite.setOnClickListener {
            inviteMember()
        }
    }

    private fun checkAdminStatus() {
        progressBar.visibility = View.VISIBLE

        val currentUserId = auth.currentUser?.uid ?: ""
        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Vous devez être connecté", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        database.child("groups").child(groupId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Récupérer manuellement l'adminId au lieu de désérialiser tout le groupe
                    val adminId = snapshot.child("adminId").getValue(String::class.java) ?: ""
                    isAdmin = adminId == currentUserId

                    // Mettre à jour l'interface selon le statut d'administrateur
                    updateUIForAdminStatus()

                    // Charger les membres
                    loadMembers()
                } else {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@ManageMembersActivity, "Groupe non trouvé", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@ManageMembersActivity, "Erreur: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateUIForAdminStatus() {
        if (isAdmin) {
            // Admin can invite members and see all controls
            etEmail.visibility = View.VISIBLE
            btnInvite.visibility = View.VISIBLE

            // Update adapter with admin status
            memberAdapter = MemberAdapter(
                membersList,
                true,
                { userId -> acceptMember(userId) },
                { userId -> rejectMember(userId) },
                { userId -> removeMember(userId) }
            )
            recyclerViewMembers.adapter = memberAdapter
        } else {
            // Non-admins can only view members
            etEmail.visibility = View.GONE
            btnInvite.visibility = View.GONE

            // Update adapter with non-admin status
            memberAdapter = MemberAdapter(
                membersList,
                false,
                { userId -> acceptMember(userId) },
                { userId -> rejectMember(userId) },
                { userId -> removeMember(userId) }
            )
            recyclerViewMembers.adapter = memberAdapter
        }
    }

    private fun loadMembers() {
        progressBar.visibility = View.VISIBLE
        membersList.clear()

        database.child("groups").child(groupId).child("members").addListenerForSingleValueEvent(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val memberIds = snapshot.children.mapNotNull { it.key }

                    // Si aucun membre, mettre à jour l'interface et retourner
                    if (memberIds.isEmpty()) {
                        memberAdapter.notifyDataSetChanged()
                        progressBar.visibility = View.GONE
                        return
                    }

                    // Récupérer l'adminId pour marquer les administrateurs
                    database.child("groups").child(groupId).child("adminId").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(adminSnapshot: DataSnapshot) {
                            val adminId = adminSnapshot.getValue(String::class.java) ?: ""

                            // Charger les détails des membres depuis la collection d'utilisateurs
                            var loadedMembersCount = 0
                            for (memberId in memberIds) {
                                // Récupérer le statut du membre
                                val memberStatus = snapshot.child(memberId).getValue(String::class.java) == "accepted"

                                database.child("users").child(memberId).addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(userSnapshot: DataSnapshot) {
                                        loadedMembersCount++

                                        if (userSnapshot.exists()) {
                                            val userName = userSnapshot.child("username").getValue(String::class.java)
                                                ?: "Utilisateur ${memberId.substring(0, 5)}"

                                            val userEmail = userSnapshot.child("email").getValue(String::class.java) ?: "Email non disponible"

                                            // Ajouter des vérifications pour les valeurs vides
                                            val displayName = if (userName.isNotEmpty()) userName else "Utilisateur sans nom"
                                            val email = if (userEmail.isNotEmpty()) userEmail else "Email non défini"

                                            // Marquer l'administrateur
                                            val nameWithStatus = if (memberId == adminId) "$displayName (Admin)" else displayName

                                            membersList.add(MemberItem(memberId, nameWithStatus, email, memberStatus))
                                            memberAdapter.notifyDataSetChanged()
                                        } else {
                                            // Si l'utilisateur n'existe pas dans la base de données, créer une entrée temporaire
                                            membersList.add(MemberItem(
                                                memberId,
                                                if (memberId == adminId) "Utilisateur inconnu (Admin)" else "Utilisateur inconnu",
                                                "Email non disponible",
                                                memberStatus
                                            ))
                                            memberAdapter.notifyDataSetChanged()
                                        }

                                        if (loadedMembersCount >= memberIds.size) {
                                            progressBar.visibility = View.GONE
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        loadedMembersCount++
                                        Toast.makeText(this@ManageMembersActivity, "Erreur de chargement d'un membre: ${error.message}", Toast.LENGTH_SHORT).show()

                                        if (loadedMembersCount >= memberIds.size) {
                                            progressBar.visibility = View.GONE
                                        }
                                    }
                                })
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            progressBar.visibility = View.GONE
                            Toast.makeText(this@ManageMembersActivity, "Erreur lors de la récupération de l'admin: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                } else {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@ManageMembersActivity, "Aucun membre trouvé", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@ManageMembersActivity, "Erreur: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun inviteMember() {
        if (!isAdmin) {
            Toast.makeText(this, "Only admins can invite members", Toast.LENGTH_SHORT).show()
            return
        }

        val email = etEmail.text.toString().trim()
        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            return
        }

        progressBar.visibility = View.VISIBLE

        // Find user by email using query in Realtime Database
        database.child("users").orderByChild("email").equalTo(email)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (!dataSnapshot.exists() || dataSnapshot.childrenCount == 0.toLong()) {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this@ManageMembersActivity, "User not found with this email", Toast.LENGTH_SHORT).show()
                        return
                    }

                    // Get the first user with this email
                    val userSnapshot = dataSnapshot.children.first()
                    val userId = userSnapshot.key ?: return

                    // Check if user is already a member
                    database.child("groups").child(groupId).child("members").child(userId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(memberSnapshot: DataSnapshot) {
                                if (memberSnapshot.exists()) {
                                    progressBar.visibility = View.GONE
                                    Toast.makeText(this@ManageMembersActivity, "User is already a member or has a pending invitation", Toast.LENGTH_SHORT).show()
                                    return
                                }

                                // Add user as pending member
                                database.child("groups").child(groupId).child("members").child(userId).setValue("pending")
                                    .addOnSuccessListener {
                                        progressBar.visibility = View.GONE
                                        Toast.makeText(this@ManageMembersActivity, "Invitation sent", Toast.LENGTH_SHORT).show()
                                        etEmail.text.clear()

                                        // Reload members list
                                        loadMembers()
                                    }
                                    .addOnFailureListener { e ->
                                        progressBar.visibility = View.GONE
                                        Toast.makeText(this@ManageMembersActivity, "Error sending invitation: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                progressBar.visibility = View.GONE
                                Toast.makeText(this@ManageMembersActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@ManageMembersActivity, "Error searching for user: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun acceptMember(userId: String) {
        if (!isAdmin) {
            Toast.makeText(this, "Only admins can accept members", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE

        database.child("groups").child(groupId).child("members").child(userId).setValue("accepted")
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Member accepted", Toast.LENGTH_SHORT).show()

                // Update local list and adapter
                val memberIndex = membersList.indexOfFirst { it.id == userId }
                if (memberIndex != -1) {
                    val updatedMember = membersList[memberIndex].copy(status = true)
                    membersList[memberIndex] = updatedMember
                    memberAdapter.notifyItemChanged(memberIndex)
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Error accepting member: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun rejectMember(userId: String) {
        removeMember(userId) // Rejection is same as removal for pending members
    }

    private fun removeMember(userId: String) {
        if (!isAdmin) {
            Toast.makeText(this, "Only admins can remove members", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE

        // Remove the member from the group
        database.child("groups").child(groupId).child("members").child(userId).removeValue()
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Member removed", Toast.LENGTH_SHORT).show()

                // Update local list and adapter
                val memberIndex = membersList.indexOfFirst { it.id == userId }
                if (memberIndex != -1) {
                    membersList.removeAt(memberIndex)
                    memberAdapter.notifyItemRemoved(memberIndex)
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Error removing member: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}