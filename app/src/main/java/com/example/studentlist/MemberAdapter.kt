package com.example.studentlist.model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.studentlist.R

class MemberAdapter(
    private val membersList: List<MemberItem>,
    private val isAdmin: Boolean,
    private val onAccept: (String) -> Unit,
    private val onReject: (String) -> Unit,
    private val onRemove: (String) -> Unit
) : RecyclerView.Adapter<MemberAdapter.MemberViewHolder>() {

    class MemberViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvMemberName)
        val tvEmail: TextView = view.findViewById(R.id.tvMemberEmail)
        val tvStatus: TextView = view.findViewById(R.id.tvMemberStatus)
        val btnAccept: Button = view.findViewById(R.id.btnAccept)
        val btnReject: Button = view.findViewById(R.id.btnReject)
        val btnRemove: Button = view.findViewById(R.id.btnRemoveMember)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val member = membersList[position]

        holder.tvName.text = member.name
        holder.tvEmail.text = member.email

        // Afficher "Admin" au lieu de "Member" si le membre est admin
        if (member.name.contains("(Admin)")) {
            holder.tvStatus.text = "Admin"
        } else {
            holder.tvStatus.text = if (member.status) "Member" else "Pending"
        }

        // Afficher/cacher les boutons selon le statut et les droits d'admin
        if (isAdmin) {
            if (member.status) {
                // Membre accepté - montrer juste le bouton supprimer
                holder.btnAccept.visibility = View.GONE
                holder.btnReject.visibility = View.GONE
                holder.btnRemove.visibility = View.VISIBLE
            } else {
                // Membre en attente - montrer les boutons accepter et rejeter
                holder.btnAccept.visibility = View.VISIBLE
                holder.btnReject.visibility = View.VISIBLE
                holder.btnRemove.visibility = View.GONE
            }
        } else {
            // Non-admin ne voit aucun bouton
            holder.btnAccept.visibility = View.GONE
            holder.btnReject.visibility = View.GONE
            holder.btnRemove.visibility = View.GONE
        }

        // Configurer les listeners des boutons
        holder.btnAccept.setOnClickListener { onAccept(member.id) }
        holder.btnReject.setOnClickListener { onReject(member.id) }
        holder.btnRemove.setOnClickListener { onRemove(member.id) }
    }

    override fun getItemCount() = membersList.size
}