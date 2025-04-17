package com.example.studentlist.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import com.example.studentlist.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AddMemberDialog(
    context: Context,
    private val listId: String,
    private val onMemberAdded: () -> Unit
) : Dialog(context) {

    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var phoneInputLayout: TextInputLayout
    private lateinit var emailInput: TextInputEditText
    private lateinit var phoneInput: TextInputEditText
    private lateinit var searchTypeTabs: TabLayout
    private lateinit var addButton: Button
    private lateinit var cancelButton: Button

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Demande une fenêtre sans titre
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        setContentView(R.layout.dialog_add_member)

        // Configuration de la fenêtre du dialogue
        window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )

            // Définir les marges pour que le dialogue ne prenne pas tout l'écran
            val params = attributes
            params?.width = (context.resources.displayMetrics.widthPixels * 0.9).toInt() // 90% de la largeur de l'écran
            attributes = params

            // Fond blanc par défaut
            decorView.setBackgroundResource(android.R.color.white)
        }

        // Initialiser Firebase
        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        // Trouver les vues
        emailInputLayout = findViewById(R.id.emailInputLayout)
        phoneInputLayout = findViewById(R.id.phoneInputLayout)
        emailInput = findViewById(R.id.emailInput)
        phoneInput = findViewById(R.id.phoneInput)
        searchTypeTabs = findViewById(R.id.searchTypeTabs)
        addButton = findViewById(R.id.addButton)
        cancelButton = findViewById(R.id.cancelButton)

        // Configurer les onglets
        searchTypeTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        emailInputLayout.visibility = View.VISIBLE
                        phoneInputLayout.visibility = View.GONE
                    }
                    1 -> {
                        emailInputLayout.visibility = View.GONE
                        phoneInputLayout.visibility = View.VISIBLE
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Style des boutons
        addButton.setBackgroundColor(context.getColor(R.color.button_color))
        addButton.setTextColor(context.getColor(R.color.white))

        // Configurer les boutons
        cancelButton.setOnClickListener { dismiss() }
        addButton.setOnClickListener { addMember() }
    }

    private fun addMember() {
        val currentTab = searchTypeTabs.selectedTabPosition
        val searchValue = when (currentTab) {
            0 -> emailInput.text.toString().trim()
            1 -> phoneInput.text.toString().trim()
            else -> ""
        }

        val searchField = when (currentTab) {
            0 -> "email"
            1 -> "phone"
            else -> ""
        }

        if (searchValue.isEmpty()) {
            Toast.makeText(context, "Veuillez entrer une valeur", Toast.LENGTH_SHORT).show()
            return
        }

        // Chercher l'utilisateur dans la base de données
        val usersRef = database.child("users")
        val query = usersRef.orderByChild(searchField).equalTo(searchValue)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Utilisateur trouvé
                    for (userSnapshot in snapshot.children) {
                        val userId = userSnapshot.key ?: continue

                        // Vérifier si l'utilisateur n'est pas déjà membre
                        database.child("lists").child(listId).child("members").child(userId)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(memberSnapshot: DataSnapshot) {
                                    if (memberSnapshot.exists()) {
                                        Toast.makeText(context, "Cet utilisateur est déjà membre", Toast.LENGTH_SHORT).show()
                                    } else {
                                        // Ajouter l'utilisateur en tant que membre
                                        addUserToList(userId)
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Toast.makeText(context, "Erreur: ${error.message}", Toast.LENGTH_SHORT).show()
                                }
                            })
                        break
                    }
                } else {
                    // Aucun utilisateur trouvé
                    Toast.makeText(context, "Aucun utilisateur trouvé avec ces informations", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Erreur: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addUserToList(userId: String) {
        database.child("lists").child(listId).child("members").child(userId).setValue("pending")
            .addOnSuccessListener {
                Toast.makeText(context, "Invitation envoyée avec succès", Toast.LENGTH_SHORT).show()
                onMemberAdded()
                dismiss()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}