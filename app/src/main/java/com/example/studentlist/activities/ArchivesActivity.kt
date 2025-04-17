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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ArchivesActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var listsContainer: LinearLayout
    private var valueEventListener: ValueEventListener? = null
    private var databaseReference: DatabaseReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_archives)

        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()
        listsContainer = findViewById(R.id.listsContainer)

        setupBottomNavigation()
        attachDatabaseListener()
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
                    val intent = Intent(this, TaskListActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    true
                }
                R.id.nav_task -> {
                    // Déjà sur l'écran d'archives
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
        bottomNavigation.selectedItemId = R.id.nav_task
    }

    private fun attachDatabaseListener() {
        val userId = auth.currentUser?.uid ?: return

        detachDatabaseListener()

        databaseReference = database.child("lists").ref

        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Effacer le conteneur avant de le remplir à nouveau
                listsContainer.removeAllViews()

                var hasArchivedLists = false

                // Parcourir toutes les listes et afficher celles qui sont archivées
                for (listSnapshot in snapshot.children) {
                    val ownerId = listSnapshot.child("owner").getValue(String::class.java) ?: ""
                    val memberStatus = listSnapshot.child("members").child(userId).getValue(String::class.java)
                    val isArchived = listSnapshot.child("archived").getValue(Boolean::class.java) ?: false

                    // Afficher uniquement les listes archivées
                    if (isArchived && (ownerId == userId || memberStatus == "accepted")) {
                        val listId = listSnapshot.key ?: continue
                        val name = listSnapshot.child("name").getValue(String::class.java) ?: "Liste sans nom"
                        val color = listSnapshot.child("color").getValue(String::class.java) ?: "green"
                        val iconName = listSnapshot.child("icon").getValue(String::class.java) ?: "document"

                        addArchivedListToView(listId, name, color, iconName, ownerId == userId)
                        hasArchivedLists = true
                    }
                }

                if (!hasArchivedLists) {
                    showEmptyState()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ArchivesActivity, "Erreur de chargement des archives: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        databaseReference?.addValueEventListener(valueEventListener!!)
    }

    private fun detachDatabaseListener() {
        if (valueEventListener != null && databaseReference != null) {
            databaseReference?.removeEventListener(valueEventListener!!)
        }
    }

    private fun showEmptyState() {
        val emptyView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = "Aucune liste archivée"
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 100, 0, 0)
        }
        listsContainer.addView(emptyView)
    }

    private fun addArchivedListToView(listId: String, name: String, color: String, iconName: String, isAdmin: Boolean) {
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

        val contentLayout = LayoutInflater.from(this)
            .inflate(R.layout.item_archived_list, cardView, false) as ConstraintLayout

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

        // Configurer le nom de la liste avec indication "archivée"
        val nameView = contentLayout.findViewById<TextView>(R.id.listName)
        nameView.text = "$name (archivée)"

        // Ajouter un bouton de restauration si l'utilisateur est admin
        if (isAdmin) {
            val restoreButton = contentLayout.findViewById<TextView>(R.id.restoreButton)
            restoreButton.visibility = View.VISIBLE
            restoreButton.setOnClickListener {
                restoreList(listId)
            }
        }

        cardView.addView(contentLayout)

        // Ajouter un gestionnaire de clic pour naviguer vers ListDetailActivity
        cardView.setOnClickListener {
            val intent = Intent(this, ListDetailActivity::class.java).apply {
                putExtra("list_id", listId)
                putExtra("list_name", name)
                putExtra("list_color", color)
                putExtra("list_icon", iconName)
                putExtra("is_admin", isAdmin)
                putExtra("is_archived", true)  // Ces listes sont archivées
            }
            startActivity(intent)
        }

        listsContainer.addView(cardView)
    }

    private fun restoreList(listId: String) {
        database.child("lists").child(listId).child("archived").setValue(false)
            .addOnSuccessListener {
                Toast.makeText(this, "Liste restaurée avec succès", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}