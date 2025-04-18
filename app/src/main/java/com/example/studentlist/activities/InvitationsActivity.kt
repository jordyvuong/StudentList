package com.example.studentlist.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studentlist.R
import com.example.studentlist.adapters.InvitationsAdapter
import com.example.studentlist.databinding.ActivityInvitationsBinding
import com.example.studentlist.model.Invitation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class InvitationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInvitationsBinding
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUserId: String
    private lateinit var invitationsAdapter: InvitationsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInvitationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()
        currentUserId = auth.currentUser?.uid ?: ""

        setupBottomNavigation()
        setupRecyclerView()
        loadInvitations()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_home
        binding.bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, TaskListActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_task -> {
                    startActivity(Intent(this, ArchivesActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        invitationsAdapter = InvitationsAdapter(
            onAccept = { listId -> acceptInvitation(listId) },
            onReject = { listId -> rejectInvitation(listId) }
        )
        binding.invitationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@InvitationsActivity)
            adapter = invitationsAdapter
        }
    }

    private fun loadInvitations() {
        database.child("lists").orderByChild("members/$currentUserId").equalTo("pending")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val invitations = mutableListOf<Invitation>()

                    for (listSnapshot in snapshot.children) {
                        val listId = listSnapshot.key ?: continue
                        val listName = listSnapshot.child("name").getValue(String::class.java) ?: "Liste sans nom"
                        val ownerId = listSnapshot.child("owner").getValue(String::class.java) ?: ""

                        invitations.add(Invitation(listId, listName, ownerId))
                    }

                    updateEmptyState(invitations.isEmpty())

                    // Charger les détails des propriétaires
                    loadOwnerDetails(invitations)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@InvitationsActivity,
                        "Erreur: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.apply {
            emptyStateContainer.visibility = if (isEmpty) View.VISIBLE else View.GONE
            invitationsRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }

    private fun loadOwnerDetails(invitations: List<Invitation>) {
        val tempInvitations = invitations.toMutableList()
        var completedQueries = 0

        if (invitations.isEmpty()) {
            invitationsAdapter.updateInvitations(emptyList())
            return
        }

        for (invitation in invitations) {
            database.child("users").child(invitation.ownerId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val ownerName = snapshot.child("username").getValue(String::class.java) ?: "Utilisateur inconnu"

                        tempInvitations.forEachIndexed { index, inv ->
                            if (inv.listId == invitation.listId) {
                                tempInvitations[index] = inv.copy(ownerName = ownerName)
                            }
                        }

                        completedQueries++
                        if (completedQueries == invitations.size) {
                            invitationsAdapter.updateInvitations(tempInvitations)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        completedQueries++
                        if (completedQueries == invitations.size) {
                            invitationsAdapter.updateInvitations(tempInvitations)
                        }
                    }
                })
        }
    }

    private fun acceptInvitation(listId: String) {
        database.child("lists").child(listId).child("members").child(currentUserId)
            .setValue("accepted")
            .addOnSuccessListener {
                Toast.makeText(this, "Invitation acceptée", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun rejectInvitation(listId: String) {
        database.child("lists").child(listId).child("members").child(currentUserId)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Invitation refusée", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}