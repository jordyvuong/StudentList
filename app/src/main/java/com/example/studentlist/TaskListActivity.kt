package com.example.studentlist

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class TaskListActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var listsContainer: LinearLayout
    private var valueEventListener: ValueEventListener? = null
    private var databaseReference: DatabaseReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_list)

        // Initialiser Firebase
        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        // Initialiser le conteneur de listes
        listsContainer = findViewById(R.id.listsContainer)

        // Configuration du bouton "Add list"
        val addListButton: TextView = findViewById(R.id.addListButton)
        addListButton.setOnClickListener {
            val intent = Intent(this, AddListActivity::class.java)
            startActivity(intent)
        }

        // Configuration du bouton d'ajout de tâche
        val fabAddTask: FloatingActionButton = findViewById(R.id.fabAddTask)
        fabAddTask.setOnClickListener {
            val intent = Intent(this, AddTaskActivity::class.java)
            startActivity(intent)
        }

        // Gérer les clics sur la barre de navigation
        setupBottomNavigation()

        // Attacher l'écouteur pour charger les listes
        attachDatabaseListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Détacher l'écouteur lorsque l'activité est détruite
        detachDatabaseListener()
    }

    private fun setupBottomNavigation() {
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottomNavigation)
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_add_task -> {
                    val intent = Intent(this, AddTaskActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_home -> {
                    // Déjà sur l'écran d'accueil
                    true
                }
                R.id.nav_group -> {
                    val intent = Intent(this, GroupManagementActivity::class.java)
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

    private fun attachDatabaseListener() {
        val userId = auth.currentUser?.uid ?: return

        // Détacher l'ancien écouteur s'il existe
        detachDatabaseListener()

        // Créer une référence à la base de données pour les listes de l'utilisateur
        databaseReference = database.child("lists").orderByChild("owner").equalTo(userId).ref

        // Créer un nouvel écouteur
        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Effacer les listes existantes dans la vue
                listsContainer.removeAllViews()

                if (!snapshot.exists()) {
                    // Ajouter les listes par défaut
                    addDefaultLists()
                    return
                }

                for (listSnapshot in snapshot.children) {
                    val listId = listSnapshot.key ?: continue
                    val name = listSnapshot.child("name").getValue(String::class.java) ?: "Sans nom"
                    val color = listSnapshot.child("color").getValue(String::class.java) ?: "green"
                    val icon = listSnapshot.child("icon").getValue(String::class.java) ?: "document"

                    addListToView(listId, name, color, icon)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TaskListActivity,
                    "Erreur de chargement des listes: ${error.message}",
                    Toast.LENGTH_SHORT).show()
            }
        }

        // Attacher l'écouteur
        databaseReference?.addValueEventListener(valueEventListener!!)
    }

    private fun detachDatabaseListener() {
        // Détacher l'écouteur s'il existe
        if (valueEventListener != null && databaseReference != null) {
            databaseReference?.removeEventListener(valueEventListener!!)
            valueEventListener = null
            databaseReference = null
        }
    }

    private fun addDefaultLists() {
        // Créer et ajouter les listes par défaut dynamiquement
        addListToView("default_all_i_need", "All I need", "green", "document")
        addListToView("default_to_buy_later", "To buy later", "purple", "grid")
    }

    private fun addListToView(listId: String, name: String, color: String, iconName: String) {
        // Créer une nouvelle CardView pour la liste
        val cardView = CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = resources.getDimensionPixelSize(R.dimen.card_margin_vertical)
                bottomMargin = resources.getDimensionPixelSize(R.dimen.card_margin_vertical)
            }
            radius = resources.getDimension(R.dimen.card_corner_radius)
            setCardBackgroundColor(resources.getColor(R.color.cardBackground, null))
            cardElevation = 0f
        }

        // Créer le contenu de la CardView
        val contentLayout = LayoutInflater.from(this)
            .inflate(R.layout.item_list, null) as ConstraintLayout

        // Configurer l'icône
        val iconView = contentLayout.findViewById<ImageView>(R.id.listIcon)
        val backgroundResId = when (color) {
            "blue" -> R.drawable.circle_background_blue
            "purple" -> R.drawable.circle_background_purple
            "red" -> R.drawable.circle_background_red
            "orange" -> R.drawable.circle_background_orange
            else -> R.drawable.circle_background_green
        }
        iconView.setBackgroundResource(backgroundResId)

        val iconResId = when (iconName) {
            "grid" -> R.drawable.ic_grid
            "cart" -> R.drawable.shopping
            else -> R.drawable.ic_document
        }
        iconView.setImageResource(iconResId)

        // Configurer le nom de la liste
        val nameView = contentLayout.findViewById<TextView>(R.id.listName)
        nameView.text = name

        // Ajouter le contenu à la CardView
        cardView.addView(contentLayout)

        // Ajouter un gestionnaire de clic pour naviguer vers ListDetailActivity
        cardView.setOnClickListener {
            val intent = Intent(this, ListDetailActivity::class.java).apply {
                putExtra("list_id", listId)
                putExtra("list_name", name)
                putExtra("list_color", color)
                putExtra("list_icon", iconName)
            }
            startActivity(intent)
        }

        // Ajouter la CardView au conteneur
        listsContainer.addView(cardView)
    }
}