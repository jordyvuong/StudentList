package com.example.studentlist.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.studentlist.dialogs.AddMemberDialog
import com.example.studentlist.adapters.ListDetailPagerAdapter
import com.example.studentlist.R
import com.example.studentlist.databinding.ActivityListDetailBinding
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ListDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListDetailBinding
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var pagerAdapter: ListDetailPagerAdapter

    private var listId: String = ""
    private var listName: String = ""
    private var listColor: String = ""
    private var listIcon: String = ""
    private var currentUserId: String = ""
    private var isAdmin: Boolean = false
    private var currentTabPosition: Int = 0
    private var isArchived: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()
        currentUserId = auth.currentUser?.uid ?: ""

        listId = intent.getStringExtra("list_id") ?: ""
        listName = intent.getStringExtra("list_name") ?: ""
        listColor = intent.getStringExtra("list_color") ?: ""
        listIcon = intent.getStringExtra("list_icon") ?: ""
        isArchived = intent.getBooleanExtra("is_archived", false)

        checkUserAccess()
    }

    private fun setupUI() {
        binding.listTitleText.text = listName

        binding.fabAdd.visibility = View.VISIBLE

        window.statusBarColor = getColor(R.color.button_color)

        if (isAdmin) {
            binding.archiveButton.visibility = View.VISIBLE
            if (isArchived) {
                binding.archiveButton.text = "Restaurer"
            } else {
                binding.archiveButton.text = "Archiver"
            }
            binding.archiveButton.setOnClickListener {
                if (isArchived) {
                    showRestoreConfirmationDialog()
                } else {
                    showArchiveConfirmationDialog()
                }
            }
        } else {
            binding.archiveButton.visibility = View.GONE
        }

        setupButtons()

        setupViewPager(isAdmin)

        updateFabVisibility(binding.viewPager.currentItem)

        setupBottomNavigation()
    }

    private fun setupButtons() {
        binding.backButton.setOnClickListener {
            finish()
        }

        binding.fabAdd.setOnClickListener {
            if (currentTabPosition == 0) { // Onglet des tâches
                val intent = Intent(this, AddTaskActivity::class.java)
                intent.putExtra("list_id", listId)
                startActivity(intent)
            } else if (currentTabPosition == 1) { // Onglet des membres
                // Créer et afficher le dialogue directement
                val dialog = AddMemberDialog(
                    this,
                    listId,
                    {
                        // Fonction appelée quand un membre est ajouté
                        // Rafraîchir la liste des membres
                        pagerAdapter.notifyDataSetChanged()
                    }
                )
                dialog.show()
            }
        }
    }

    private fun checkUserAccess() {
        val listRef = database.child("lists").child(listId)

        listRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(this@ListDetailActivity,
                        "Cette liste n'existe pas", Toast.LENGTH_SHORT).show()
                    finish()
                    return
                }

                val ownerId = snapshot.child("owner").getValue(String::class.java) ?: ""
                isAdmin = (ownerId == currentUserId)

                val memberStatus = snapshot.child("members").child(currentUserId).getValue(String::class.java)
                val isAcceptedMember = memberStatus == "accepted"

                // Vérifier si la liste est archivée
                isArchived = snapshot.child("archived").getValue(Boolean::class.java) ?: false

                if (isAdmin || isAcceptedMember) {
                    setupUI()
                } else {
                    Toast.makeText(this@ListDetailActivity,
                        "Vous n'avez pas accès à cette liste", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ListDetailActivity,
                    "Erreur d'accès à la liste: ${error.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }

    private fun setupViewPager(isAdmin: Boolean) {
        pagerAdapter = ListDetailPagerAdapter(this, listId, isAdmin)
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = if (position == 0) "Tâches" else "Membres"
        }.attach()

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentTabPosition = position
                updateFabVisibility(position)
            }
        })
    }

    private fun updateFabVisibility(position: Int) {
        // Dans une liste archivée, on désactive l'ajout de tâches et de membres
        if (isArchived) {
            binding.fabAdd.visibility = View.GONE
            return
        }

        if (position == 0) { // Onglet des tâches
            binding.fabAdd.visibility = View.VISIBLE
        } else if (position == 1) { // Onglet des membres
            if (isAdmin) {
                binding.fabAdd.visibility = View.VISIBLE
            } else {
                binding.fabAdd.visibility = View.GONE
            }
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = binding.bottomNavigation
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, TaskListActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
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

    private fun showArchiveConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Archiver la liste")
            .setMessage("Êtes-vous sûr de vouloir archiver cette liste? Elle sera déplacée dans les archives.")
            .setPositiveButton("Archiver") { _, _ ->
                archiveList()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showRestoreConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Restaurer la liste")
            .setMessage("Êtes-vous sûr de vouloir restaurer cette liste? Elle sera à nouveau active.")
            .setPositiveButton("Restaurer") { _, _ ->
                restoreList()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun archiveList() {
        database.child("lists").child(listId).child("archived").setValue(true)
            .addOnSuccessListener {
                Toast.makeText(this, "Liste archivée avec succès", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun restoreList() {
        database.child("lists").child(listId).child("archived").setValue(false)
            .addOnSuccessListener {
                Toast.makeText(this, "Liste restaurée avec succès", Toast.LENGTH_SHORT).show()
                isArchived = false
                binding.archiveButton.text = "Archiver"
                updateFabVisibility(currentTabPosition)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}