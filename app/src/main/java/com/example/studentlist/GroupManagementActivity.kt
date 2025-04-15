package com.example.studentlist

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.studentlist.model.Group
import com.google.android.material.bottomnavigation.BottomNavigationView

class GroupManagementActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var groupsList: MutableList<Group>
    private lateinit var adapter: GroupsAdapter
    private lateinit var fabCreateGroup: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_management)

        recyclerView = findViewById(R.id.recyclerViewGroups)
        fabCreateGroup = findViewById(R.id.fabCreateGroup)

        // Configurer le RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        groupsList = mutableListOf()
        adapter = GroupsAdapter(groupsList) { group ->
            // Action lorsqu'un groupe est sélectionné
            val intent = Intent(this, ManageMembersActivity::class.java)
            intent.putExtra("GROUP_ID", group.id)
            intent.putExtra("GROUP_NAME", group.name)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        // Bouton pour créer un nouveau groupe
        fabCreateGroup.setOnClickListener {
            val intent = Intent(this, CreateGroupActivity::class.java)
            startActivity(intent)
        }

        // Configurer la navigation
        configureBottomNavigation()

        // Charger les groupes
        loadGroups()
    }

    private fun configureBottomNavigation() {
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottomNavigation)
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Aller vers la liste des tâches
                    val intent = Intent(this, TaskListActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_group -> {
                    // Déjà sur la page groupes
                    true
                }
                R.id.nav_task -> {
                    true
                }
                R.id.nav_settings -> {
                    // À implémenter plus tard
                    true
                }
                else -> false
            }
        }

        // Activer l'élément "group" dans la navbar
        bottomNavigation.selectedItemId = R.id.nav_group
    }

    override fun onResume() {
        super.onResume()
        loadGroups() // Recharger les groupes à chaque retour sur cette activité
    }


    private fun loadGroups() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Pour le débogage
        println("DEBUG: User ID actuel = $currentUserId")

        val database = FirebaseDatabase.getInstance().reference

        database.child("groups")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    groupsList.clear()

                    // Débogage
                    println("DEBUG: Nombre de groupes dans la base = ${snapshot.childrenCount}")


                    for (groupSnapshot in snapshot.children) {
                        try {
                            val groupId = groupSnapshot.key ?: continue
                            val groupName = groupSnapshot.child("name").getValue(String::class.java) ?: ""

                            // Adaptez ces lignes pour correspondre à votre structure
                            val description = "" // Pas dans votre JSON
                            val adminId = groupSnapshot.child("adminId").getValue(String::class.java) ?: continue
                            val createdAt = groupSnapshot.child("created_at").getValue(Long::class.java) ?: 0L

                            // Récupérer les membres
                            val membersMap = mutableMapOf<String, Boolean>()
                            groupSnapshot.child("members").children.forEach { memberSnapshot ->
                                val memberId = memberSnapshot.key ?: return@forEach
                                // Convertir "accepted" en true, "pending" en false
                                val status = memberSnapshot.getValue(String::class.java)
                                val isAccepted = status == "accepted"
                                membersMap[memberId] = isAccepted
                            }

                            // Vérifier si l'utilisateur est admin ou membre du groupe
                            val isAdmin = adminId == currentUserId
                            val isMember = membersMap.containsKey(currentUserId) && membersMap[currentUserId] == true

                            // Débogage
                            println("DEBUG: Groupe $groupId - name=$groupName, isAdmin=$isAdmin, isMember=$isMember")

                            if (isMember || isAdmin) {
                                val group = Group(
                                    id = groupId,
                                    name = groupName,
                                    description = description,
                                    adminId = adminId,
                                    isAdmin = isAdmin,
                                    createdAt = createdAt,
                                    members = membersMap,
                                    archivedLists = mutableListOf()
                                )
                                groupsList.add(group)
                            }
                        } catch (e: Exception) {
                            println("DEBUG: Erreur lors du traitement du groupe: ${e.message}")
                            e.printStackTrace()
                        }
                    }

                    // Trier les groupes
                    groupsList.sortWith(compareByDescending<Group> { it.isAdmin }
                        .thenByDescending { it.createdAt })

                    // Débogage
                    println("DEBUG: Nombre de groupes après filtrage = ${groupsList.size}")
                    groupsList.forEach { println("DEBUG: Groupe chargé: ${it.name}") }

                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    println("DEBUG: Erreur Firebase: ${error.message}")
                }
            })
    }
}