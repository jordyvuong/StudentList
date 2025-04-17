package com.example.studentlist.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studentlist.adapters.ListMemberAdapter
import com.example.studentlist.R
import com.example.studentlist.model.ListMember
import com.google.firebase.database.*

class ListMembersFragment : Fragment() {

    private lateinit var membersRecyclerView: RecyclerView
    private lateinit var emptyMembersText: TextView
    private lateinit var memberAdapter: ListMemberAdapter
    private lateinit var database: DatabaseReference

    private var listId: String = ""
    private var isAdmin: Boolean = false

    companion object {
        fun newInstance(listId: String, isAdmin: Boolean = false): ListMembersFragment {
            val fragment = ListMembersFragment()
            val args = Bundle()
            args.putString("list_id", listId)
            args.putBoolean("is_admin", isAdmin)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            listId = it.getString("list_id", "")
            isAdmin = it.getBoolean("is_admin", false)
        }
        database = FirebaseDatabase.getInstance().reference
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_list_members, container, false)

        membersRecyclerView = view.findViewById(R.id.membersRecyclerView)
        emptyMembersText = view.findViewById(R.id.emptyMembersText)

        // Configurer le RecyclerView
        memberAdapter = ListMemberAdapter { member ->
            // Action lors du clic sur un membre
            if (isAdmin) {
                showMemberOptionsDialog(member)
            }
        }

        membersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = memberAdapter
        }

        loadMembers()

        return view
    }

    internal fun loadMembers() {
        val query = database.child("lists").child(listId).child("members")

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val members = mutableListOf<ListMember>()
                val userIds = mutableSetOf<String>()

                for (memberSnapshot in snapshot.children) {
                    val userId = memberSnapshot.key ?: continue
                    val status = memberSnapshot.getValue(String::class.java) ?: "pending"

                    userIds.add(userId)
                    members.add(ListMember(id = userId, status = status))
                }

                if (members.isEmpty()) {
                    emptyMembersText.visibility = View.VISIBLE
                    membersRecyclerView.visibility = View.GONE
                } else {
                    emptyMembersText.visibility = View.GONE
                    membersRecyclerView.visibility = View.VISIBLE

                    // Charger les infos des utilisateurs
                    loadUserDetails(members, userIds)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(),
                    "Erreur de chargement des membres: ${error.message}",
                    Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadUserDetails(members: List<ListMember>, userIds: Set<String>) {
        val tempMembers = members.toMutableList()
        var completedQueries = 0

        for (userId in userIds) {
            database.child("users").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val username = snapshot.child("username").getValue(String::class.java) ?: "Utilisateur inconnu"
                        val email = snapshot.child("email").getValue(String::class.java) ?: ""

                        // Mettre à jour les infos de l'utilisateur pour chaque membre
                        tempMembers.forEachIndexed { index, member ->
                            if (member.id == userId) {
                                tempMembers[index] = member.copy(name = username, email = email)
                            }
                        }

                        completedQueries++
                        if (completedQueries == userIds.size) {
                            memberAdapter.updateMembers(tempMembers)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        completedQueries++
                        if (completedQueries == userIds.size) {
                            memberAdapter.updateMembers(tempMembers)
                        }
                    }
                })
        }
    }

    private fun showMemberOptionsDialog(member: ListMember) {
        if (!isAdmin) return

        // Pour les administrateurs, afficher un menu d'options pour gérer ce membre
        val options = arrayOf("Supprimer", "Changer le statut", "Annuler")

        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Options pour ${member.name}")
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> removeMember(member)
                1 -> showChangeStatusDialog(member)
            }
            dialog.dismiss()
        }
        builder.show()
    }

    private fun removeMember(member: ListMember) {
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Confirmation")
        builder.setMessage("Êtes-vous sûr de vouloir retirer ${member.name} de la liste?")
        builder.setPositiveButton("Oui") { _, _ ->
            database.child("lists").child(listId).child("members").child(member.id).removeValue()
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Membre retiré avec succès", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
        builder.setNegativeButton("Non", null)
        builder.show()
    }

    private fun showChangeStatusDialog(member: ListMember) {
        val statusOptions = arrayOf("En attente", "Accepté")
        val currentStatusIndex = if (member.status == "accepted") 1 else 0

        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Changer le statut de ${member.name}")
        builder.setSingleChoiceItems(statusOptions, currentStatusIndex) { dialog, which ->
            val newStatus = if (which == 1) "accepted" else "pending"
            updateMemberStatus(member, newStatus)
            dialog.dismiss()
        }
        builder.setNegativeButton("Annuler", null)
        builder.show()
    }

    private fun updateMemberStatus(member: ListMember, newStatus: String) {
        database.child("lists").child(listId).child("members").child(member.id).setValue(newStatus)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Statut mis à jour", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}