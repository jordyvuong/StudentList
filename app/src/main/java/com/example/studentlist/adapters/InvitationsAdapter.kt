package com.example.studentlist.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.studentlist.R
import com.example.studentlist.model.Invitation

class InvitationsAdapter(
    private val onAccept: (String) -> Unit,
    private val onReject: (String) -> Unit
) : RecyclerView.Adapter<InvitationsAdapter.InvitationViewHolder>() {

    private val invitations = mutableListOf<Invitation>()

    fun updateInvitations(newInvitations: List<Invitation>) {
        invitations.clear()
        invitations.addAll(newInvitations)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvitationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_invitation, parent, false)
        return InvitationViewHolder(view)
    }

    override fun onBindViewHolder(holder: InvitationViewHolder, position: Int) {
        holder.bind(invitations[position])
    }

    override fun getItemCount(): Int = invitations.size

    inner class InvitationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val listNameText: TextView = itemView.findViewById(R.id.listNameText)
        private val ownerNameText: TextView = itemView.findViewById(R.id.ownerNameText)
        private val acceptButton: Button = itemView.findViewById(R.id.acceptButton)
        private val rejectButton: Button = itemView.findViewById(R.id.rejectButton)

        fun bind(invitation: Invitation) {
            listNameText.text = invitation.listName
            ownerNameText.text = "De: ${invitation.ownerName}"

            acceptButton.setOnClickListener {
                onAccept(invitation.listId)
            }

            rejectButton.setOnClickListener {
                onReject(invitation.listId)
            }
        }
    }
}