package com.example.studentlist.activities

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
import com.example.studentlist.R
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
            startActivity(Intent(this, AddListActivity::class.java))
        }

        // Ajouter le bouton pour accéder aux invitations
        val invitationsButton: TextView = findViewById(R.id.invitationsButton)
        invitationsButton.setOnClickListener {
            startActivity(Intent(this, InvitationsActivity::class.java))
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
                    // Naviguer vers l'écran d'archives
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

    private fun checkPendingInvitations() {
        val currentUserId = auth.currentUser?.uid ?: return
        val invitationBadge: TextView = findViewById(R.id.invitationBadge)

        database.child("lists").orderByChild("members/$currentUserId").equalTo("pending")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val pendingCount = snapshot.childrenCount.toInt()
                    if (pendingCount > 0) {
                        invitationBadge.visibility = View.VISIBLE
                        invitationBadge.text = pendingCount.toString()
                    } else {
                        invitationBadge.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@TaskListActivity, "Erreur: ${error.message}", Toast.LENGTH_SHORT).show()
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
                // Effacer le conteneur avant de le remplir à nouveau
                listsContainer.removeAllViews()

                var hasLists = false

                // Parcourir toutes les listes et afficher celles dont l'utilisateur est propriétaire ou membre accepté
                for (listSnapshot in snapshot.children) {
                    val ownerId = listSnapshot.child("owner").getValue(String::class.java) ?: ""
                    val memberStatus = listSnapshot.child("members").child(userId).getValue(String::class.java)
                    val isArchived = listSnapshot.child("archived").getValue(Boolean::class.java) ?: false

                    // Afficher uniquement les listes non archivées
                    if (!isArchived && (ownerId == userId || memberStatus == "accepted")) {
                        val listId = listSnapshot.key ?: continue
                        val name = listSnapshot.child("name").getValue(String::class.java) ?: "Liste sans nom"
                        val color = listSnapshot.child("color").getValue(String::class.java) ?: "green"
                        val iconName = listSnapshot.child("icon").getValue(String::class.java) ?: "document"

                        addListToView(listId, name, color, iconName, ownerId == userId)
                        hasLists = true
                    }
                }

                // Si l'utilisateur n'a pas de listes, afficher les listes par défaut
                if (!hasLists) {
                    addDefaultLists()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TaskListActivity, "Erreur de chargement des listes: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Attacher l'écouteur
        databaseReference?.addValueEventListener(valueEventListener!!)
    }

    private fun detachDatabaseListener() {
        // Détacher l'écouteur s'il existe
        if (valueEventListener != null && databaseReference != null) {
            databaseReference?.removeEventListener(valueEventListener!!)
        }
    }

    private fun addDefaultLists() {
        // Créer et ajouter les listes par défaut dynamiquement
        addListToView("default_all_i_need", "All I need", "green", "document", true)
        addListToView("default_to_buy_later", "To buy later", "purple", "grid", true)
    }

    private fun addListToView(listId: String, name: String, color: String, iconName: String, isAdmin: Boolean) {
        // Créer une nouvelle CardView pour la liste
        val cardView = CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, resources.getDimensionPixelSize(R.dimen.card_margin_bottom))
            }
            radius = resources.getDimension(R.dimen.card_corner_radius)
            cardElevation = 0f
            setCardBackgroundColor(resources.getColor(R.color.cardBackground, theme))
        }

        // Créer le contenu de la CardView
        val contentLayout = LayoutInflater.from(this)
            .inflate(R.layout.item_list, cardView, false) as ConstraintLayout

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
                putExtra("is_admin", isAdmin)
                putExtra("is_archived", false)  // Ces listes ne sont pas archivées
            }
            startActivity(intent)
        }

        // Ajouter la CardView au conteneur
        listsContainer.addView(cardView)
    }
}