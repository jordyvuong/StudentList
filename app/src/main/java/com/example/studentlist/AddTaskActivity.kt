package com.example.studentlist

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.studentlist.databinding.ActivityAddTaskBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class AddTaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTaskBinding
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var selectedGroupId: String? = null
    private var selectedDate: String? = null
    private var selectedUserId: String? = null

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val groups = HashMap<String, String>() // groupId -> groupName
    private val users = HashMap<String, String>() // userId -> userName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialiser Firebase
        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        setupBottomNavigation()
        setupDatePicker()
        loadGroups()

        // Configurer le bouton Enregistrer
        binding.buttonSave.setOnClickListener {
            saveTask()
        }

        // Configurer le bouton Annuler
        binding.buttonCancel.setOnClickListener {
            finish()
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottomNavigation)
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_add_task -> {
                    true
                }
                R.id.nav_home -> {
                    val intent = Intent(this, TaskListActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_group -> {

                    val intent = Intent(this, GroupManagementActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_task -> {
                    // Gérer l'action "Documents"
                    Toast.makeText(this, "Documents clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_settings -> {
                    // Gérer l'action "Settings"
                    Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
        bottomNavigation.selectedItemId = R.id.action_add_task
    }

    private fun setupDatePicker() {
        binding.buttonSelectDate.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                selectedDate = dateFormat.format(calendar.time)
                binding.textViewSelectedDate.setText(selectedDate)
            }, year, month, day).show()
        }
    }

    private fun loadGroups() {
        val currentUserId = auth.currentUser?.uid ?: return

        database.child("groups").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val groupsList = ArrayList<String>()
                groups.clear()

                for (groupSnapshot in snapshot.children) {
                    val groupId = groupSnapshot.key ?: continue
                    val membersSnapshot = groupSnapshot.child("members")

                    if (membersSnapshot.hasChild(currentUserId)) {
                        val memberStatus = membersSnapshot.child(currentUserId).getValue()
                        val isAccepted = when (memberStatus) {
                            is Boolean -> memberStatus // Si c'est déjà un booléen
                            is String -> memberStatus == "accepted" // Si c'est une chaîne
                            else -> false
                        }

                        if (isAccepted) {
                            val groupName = groupSnapshot.child("name").getValue(String::class.java) ?: "Groupe sans nom"
                            groups[groupId] = groupName
                            groupsList.add(groupName)
                        }
                    }
                }

                if (groupsList.isNotEmpty()) {
                    setupGroupSpinner(groupsList)
                } else {
                    Toast.makeText(this@AddTaskActivity,
                        "Vous n'êtes membre d'aucun groupe", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AddTaskActivity,
                    "Erreur: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupGroupSpinner(groupNames: ArrayList<String>) {
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, groupNames)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerGroup.adapter = spinnerAdapter

        binding.spinnerGroup.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedGroupName = groupNames[position]
                selectedGroupId = groups.entries.find { it.value == selectedGroupName }?.key
                selectedGroupId?.let { loadGroupMembers(it) }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedGroupId = null
            }
        })
    }

    private fun loadGroupMembers(groupId: String) {
        users.clear()
        val usersList = ArrayList<String>()

        database.child("groups").child(groupId).child("members")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val memberIds = ArrayList<String>()

                    for (memberSnapshot in snapshot.children) {
                        val memberId = memberSnapshot.key ?: continue
                        val memberStatus = memberSnapshot.getValue()
                        val isAccepted = when (memberStatus) {
                            is Boolean -> memberStatus // Si c'est déjà un booléen
                            is String -> memberStatus == "accepted" // Si c'est une chaîne
                            else -> false
                        }

                        if (isAccepted) {
                            memberIds.add(memberId)
                        }
                    }

                    if (memberIds.isNotEmpty()) {
                        fetchUserDetails(memberIds, usersList)
                    } else {
                        setupUserSpinner(usersList)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(this@AddTaskActivity,
                        "Erreur: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun fetchUserDetails(userIds: List<String>, usersList: ArrayList<String>) {
        var completedQueries = 0

        for (userId in userIds) {
            database.child("users").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val displayName = snapshot.child("displayName").getValue(String::class.java)
                            ?: "Utilisateur inconnu"
                        users[userId] = displayName
                        usersList.add(displayName)

                        completedQueries++
                        if (completedQueries == userIds.size) {
                            setupUserSpinner(usersList)
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        completedQueries++
                        if (completedQueries == userIds.size) {
                            setupUserSpinner(usersList)
                        }
                    }
                })
        }
    }

    private fun setupUserSpinner(userNames: ArrayList<String>) {
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, userNames)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerAssignTo.adapter = spinnerAdapter

        binding.spinnerAssignTo.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (userNames.isNotEmpty()) {
                    val selectedUserName = userNames[position]
                    selectedUserId = users.entries.find { it.value == selectedUserName }?.key
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedUserId = null
            }
        })
    }

    private fun saveTask() {
        val taskName = binding.editTextTaskName.text.toString().trim()
        val quantity = binding.editTextQuantity.text.toString().trim()

        // Validation
        if (taskName.isEmpty()) {
            binding.textInputLayoutTaskName.error = "Veuillez entrer un nom de tâche"
            return
        } else {
            binding.textInputLayoutTaskName.error = null
        }

        if (selectedGroupId == null) {
            Toast.makeText(this, "Veuillez sélectionner un groupe", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedUserId == null) {
            Toast.makeText(this, "Veuillez sélectionner un membre", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedDate == null) {
            Toast.makeText(this, "Veuillez sélectionner une date d'échéance", Toast.LENGTH_SHORT).show()
            return
        }

        // Créer les données de la tâche
        val taskId = database.child("tasks").push().key ?: return
        val currentTimeSeconds = System.currentTimeMillis() / 1000

        val taskData = HashMap<String, Any>()
        taskData["name"] = taskName
        taskData["quantity"] = quantity
        taskData["assigned_to"] = selectedUserId!!
        taskData["status"] = "pending"
        taskData["due_date"] = selectedDate!!
        taskData["created_at"] = currentTimeSeconds
        taskData["group_id"] = selectedGroupId!!

        // Enregistrer la tâche dans la base de données
        database.child("tasks").child(taskId).setValue(taskData)
            .addOnSuccessListener {
                Toast.makeText(this, "Tâche ajoutée avec succès", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}