package com.example.studentlist

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
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

        // Configuration du bouton flottant pour ajouter une liste
        val fabAddTask: FloatingActionButton = findViewById(R.id.fabAddTask)
        fabAddTask.setOnClickListener {
            val intent = Intent(this, AddListActivity::class.java)
            startActivity(intent)
        }

        // Ajouter le bouton pour accéder aux invitations
        val invitationsButton: TextView = findViewById(R.id.invitationsButton)
        invitationsButton.setOnClickListener {
            val intent = Intent(this, InvitationsActivity::class.java)
            startActivity(intent)
        }

        // Gérer les clics sur la barre de navigation
        setupBottomNavigation()

        // Attacher l'écouteur pour charger les listes
        attachDatabaseListener()
    }

    override fun onResume() {
        super.onResume()
        // Recharger les listes au retour de l'utilisateur sur l'activité
        attachDatabaseListener()
        // Vérifier s'il y a des invitations en attente
        checkPendingInvitations()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Détacher l'écouteur lorsque l'activité est détruite
        detachDatabaseListener()
    }

    private fun setupBottomNavigation() {
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottomNavigation)
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Déjà sur l'écran d'accueil
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

    private fun checkPendingInvitations() {
        val currentUserId = auth.currentUser?.uid ?: return
        val invitationBadge: TextView = findViewById(R.id.invitationBadge)

        database.child("lists")
            .orderByChild("members/$currentUserId")
            .equalTo("pending")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val invitationCount = snapshot.childrenCount.toInt()

                    if (invitationCount > 0) {
                        invitationBadge.visibility = View.VISIBLE
                        invitationBadge.text = invitationCount.toString()
                    } else {
                        invitationBadge.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Ne rien faire
                }
            })
    }

    private fun attachDatabaseListener() {
        val userId = auth.currentUser?.uid ?: return

        // Détacher l'ancien écouteur s'il existe
        detachDatabaseListener()

        // Créer une référence à la base de données pour toutes les listes
        databaseReference = database.child("lists").ref

        // Créer un nouvel écouteur
        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Effacer les listes existantes dans la vue
                listsContainer.removeAllViews()

                // Vérifier si des listes existent
                var listsFound = false

                for (listSnapshot in snapshot.children) {
                    val listId = listSnapshot.key ?: continue
                    val owner = listSnapshot.child("owner").getValue(String::class.java) ?: continue
                    val isOwner = owner == userId

                    // Vérifier si l'utilisateur est un membre accepté
                    val memberStatus = listSnapshot.child("members").child(userId).getValue(String::class.java)
                    val isAcceptedMember = memberStatus == "accepted"

                    // Afficher la liste uniquement si l'utilisateur est propriétaire ou membre accepté
                    if (isOwner || isAcceptedMember) {
                        val name = listSnapshot.child("name").getValue(String::class.java) ?: "Sans nom"
                        val color = listSnapshot.child("color").getValue(String::class.java) ?: "green"
                        val icon = listSnapshot.child("icon").getValue(String::class.java) ?: "document"

                        addListToView(listId, name, color, icon)
                        listsFound = true
                    }
                }

                // Si aucune liste n'est trouvée, afficher les listes par défaut
                if (!listsFound) {
                    addDefaultLists()
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