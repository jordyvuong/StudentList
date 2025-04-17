package com.example.studentlist.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.studentlist.R
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
    private var selectedDate: String? = null
    private var selectedUserId: String? = null
    private var listId: String? = null

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val users = HashMap<String, String>() // userId -> userName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Récupérer l'ID de la liste depuis l'intent
        listId = intent.getStringExtra("list_id")
        if (listId == null) {
            Toast.makeText(this, "Identifiant de liste manquant", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialiser Firebase
        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        setupBottomNavigation()
        setupDatePicker()
        setupCheckboxListener()
        loadListMembers()

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
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, TaskListActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_task -> {
                    val intent = Intent(this, ArchivesActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupDatePicker() {
        binding.buttonSelectDate.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                selectedDate = dateFormat.format(calendar.time)
                binding.textViewSelectedDate.text = selectedDate
            }, year, month, day).show()
        }
    }

    private fun setupCheckboxListener() {
        val checkBox: CheckBox = findViewById(R.id.checkBoxAssignTask)
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            binding.textViewMember.visibility = if (isChecked) View.VISIBLE else View.GONE
            binding.spinnerAssignTo.visibility = if (isChecked) View.VISIBLE else View.GONE

            // Si décoché, réinitialiser la sélection
            if (!isChecked) {
                selectedUserId = null
            }
        }
    }

    private fun loadListMembers() {
        val listRef = database.child("lists").child(listId!!)

        listRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(this@AddTaskActivity, "Liste non trouvée", Toast.LENGTH_SHORT).show()
                    finish()
                    return
                }

                val memberIds = ArrayList<String>()
                val membersNode = snapshot.child("members")

                for (memberSnapshot in membersNode.children) {
                    val memberId = memberSnapshot.key ?: continue
                    val status = memberSnapshot.getValue(String::class.java)

                    if (status == "accepted") {
                        memberIds.add(memberId)
                    }
                }

                // Ajouter également le propriétaire
                val ownerId = snapshot.child("owner").getValue(String::class.java)
                if (ownerId != null && !memberIds.contains(ownerId)) {
                    memberIds.add(ownerId)
                }

                if (memberIds.isNotEmpty()) {
                    fetchUserDetails(memberIds)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AddTaskActivity, "Erreur: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchUserDetails(userIds: List<String>) {
        val usersList = ArrayList<String>()
        var completedQueries = 0

        for (userId in userIds) {
            database.child("users").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val username = snapshot.child("username").getValue(String::class.java) ?: "Utilisateur $userId"

                        users[userId] = username
                        usersList.add(username)

                        completedQueries++
                        if (completedQueries == userIds.size) {
                            setupUserSpinner(usersList)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
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
        val assignTask = binding.checkBoxAssignTask.isChecked

        // Validation
        if (taskName.isEmpty()) {
            binding.textInputLayoutTaskName.error = "Veuillez entrer un nom de tâche"
            return
        } else {
            binding.textInputLayoutTaskName.error = null
        }

        if (assignTask && selectedUserId == null) {
            Toast.makeText(this, "Veuillez sélectionner un membre", Toast.LENGTH_SHORT).show()
            return
        }

        // Créer les données de la tâche
        val taskId = database.child("tasks").push().key ?: return
        val currentTimeMillis = System.currentTimeMillis()

        val taskData = HashMap<String, Any>()
        taskData["name"] = taskName
        taskData["quantity"] = quantity
        taskData["status"] = "pending"
        taskData["createdAt"] = currentTimeMillis
        taskData["listId"] = listId!!

        if (selectedDate != null) {
            taskData["dueDate"] = selectedDate!!
        }

        if (assignTask && selectedUserId != null) {
            taskData["assignedTo"] = selectedUserId!!

            // Récupérer et enregistrer le nom de l'utilisateur assigné
            val assigneeName = users[selectedUserId] ?: "Utilisateur inconnu"
            taskData["assigneeName"] = assigneeName
        }

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