package com.example.studentlist

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialiser Firebase
        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()
        currentUserId = auth.currentUser?.uid ?: ""

        // Récupérer les données de la liste
        listId = intent.getStringExtra("list_id") ?: ""
        listName = intent.getStringExtra("list_name") ?: ""
        listColor = intent.getStringExtra("list_color") ?: ""
        listIcon = intent.getStringExtra("list_icon") ?: ""

        // Vérifier l'accès de l'utilisateur avant d'afficher les détails
        checkUserAccess()
    }

    private fun setupUI() {
        // Configurer le titre
        binding.listTitleText.text = listName

        // Initialiser la visibilité du FAB
        binding.fabAdd.visibility = View.VISIBLE
        binding.addTaskButton.visibility = View.GONE

        // Configurer la couleur de la barre d'état
        window.statusBarColor = getColor(R.color.button_color)

        // Configurer les boutons
        setupButtons()

        // Configurer le ViewPager
        setupViewPager(isAdmin)

        // Mettre à jour la visibilité du FAB
        updateFabVisibility(binding.viewPager.currentItem)

        // Configurer la navigation du bas
        setupBottomNavigation()
    }

    private fun setupButtons() {
        binding.backButton.setOnClickListener {
            finish()
        }

        binding.addTaskButton.setOnClickListener {
            val intent = Intent(this, AddTaskActivity::class.java)
            intent.putExtra("list_id", listId)
            startActivity(intent)
        }

        binding.fabAdd.setOnClickListener {
            when (currentTabPosition) {
                0 -> {
                    // Ajouter une tâche
                    val intent = Intent(this, AddTaskActivity::class.java)
                    intent.putExtra("list_id", listId)
                    startActivity(intent)
                }
                1 -> {
                    // Ajouter un membre
                    showAddMemberDialog()
                }
            }
        }
    }

    private fun checkUserAccess() {
        database.child("lists").child(listId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Toast.makeText(this@ListDetailActivity,
                            "Cette liste n'existe pas", Toast.LENGTH_SHORT).show()
                        finish()
                        return
                    }

                    val ownerId = snapshot.child("owner").getValue(String::class.java) ?: ""
                    val memberStatus = snapshot.child("members")
                        .child(currentUserId)
                        .getValue(String::class.java)

                    val isOwner = ownerId == currentUserId
                    val isAcceptedMember = memberStatus == "accepted"

                    if (!isOwner && !isAcceptedMember) {
                        Toast.makeText(this@ListDetailActivity,
                            "Vous n'avez pas accès à cette liste", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        // L'utilisateur a accès à la liste
                        isAdmin = isOwner
                        setupUI()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ListDetailActivity,
                        "Erreur: ${error.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
    }

    private fun updateFabVisibility(position: Int) {
        currentTabPosition = position

        // Toujours afficher le FAB pour l'onglet Tâches
        if (position == 0) {
            binding.fabAdd.visibility = View.VISIBLE
        }
        // Pour l'onglet Membres, afficher le FAB seulement si l'utilisateur est admin
        else if (position == 1) {
            binding.fabAdd.visibility = if (isAdmin) View.VISIBLE else View.GONE
        }

        binding.addTaskButton.visibility = View.GONE
    }

    private fun setupViewPager(userIsAdmin: Boolean = false) {
        // Configurer l'adaptateur du ViewPager
        pagerAdapter = ListDetailPagerAdapter(this, listId, userIsAdmin)
        binding.viewPager.adapter = pagerAdapter

        // Lier TabLayout au ViewPager
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Tâches"
                1 -> "Membres"
                else -> ""
            }
        }.attach()

        // Écouter les changements d'onglet
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateFabVisibility(position)
            }
        })
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, TaskListActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    true
                }
                R.id.nav_task -> {
                    Toast.makeText(this, "Documents clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_settings -> {
                    Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }

    private fun showAddMemberDialog() {
        if (isAdmin) {
            AddMemberDialog(this, listId) {
                // Au lieu d'appeler directement loadMembers() sur le fragment,
                // rechargez simplement la page entière
                val currentPosition = binding.viewPager.currentItem
                pagerAdapter = ListDetailPagerAdapter(this, listId, isAdmin)
                binding.viewPager.adapter = pagerAdapter
                binding.viewPager.setCurrentItem(currentPosition, false)
            }.show()
        } else {
            Toast.makeText(this, "Seul l'administrateur peut ajouter des membres", Toast.LENGTH_SHORT).show()
        }
    }
}