package com.example.studentlist.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.studentlist.R
import com.example.studentlist.model.ListMember
import com.google.android.material.chip.Chip

class ListMemberAdapter(
    private val onMemberClick: (ListMember) -> Unit
) : RecyclerView.Adapter<ListMemberAdapter.MemberViewHolder>() {

    private val members = mutableListOf<ListMember>()

    fun updateMembers(newMembers: List<ListMember>) {
        members.clear()
        members.addAll(newMembers)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_list_member, parent, false)
        return MemberViewHolder(view, onMemberClick)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(members[position])
    }

    override fun getItemCount(): Int = members.size

    class MemberViewHolder(
        itemView: View,
        private val onMemberClick: (ListMember) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val memberName: TextView = itemView.findViewById(R.id.memberName)
        private val memberEmail: TextView = itemView.findViewById(R.id.memberEmail)
        private val memberStatusChip: Chip = itemView.findViewById(R.id.memberStatusChip)

        fun bind(member: ListMember) {
            memberName.text = member.name
            memberEmail.text = member.email

            // Configuration de l'affichage du statut
            when (member.status) {
                "accepted" -> {
                    memberStatusChip.text = "Accepté"
                    memberStatusChip.setChipBackgroundColorResource(R.color.green)
                }
                "pending" -> {
                    memberStatusChip.text = "En attente"
                    memberStatusChip.setChipBackgroundColorResource(R.color.orange)
                }
                else -> {
                    memberStatusChip.text = "Inconnu"
                    memberStatusChip.setChipBackgroundColorResource(R.color.grey)
                }
            }

            itemView.setOnClickListener {
                onMemberClick(member)
            }
        }
    }
}