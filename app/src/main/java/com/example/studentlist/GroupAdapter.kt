package com.example.studentlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.studentlist.model.Group

class GroupsAdapter(
    private val groups: List<Group>,
    private val onGroupClick: (Group) -> Unit
) : RecyclerView.Adapter<GroupsAdapter.GroupViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]
        holder.bind(group)
        holder.itemView.setOnClickListener { onGroupClick(group) }
    }

    override fun getItemCount(): Int = groups.size

    class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val groupName: TextView = itemView.findViewById(R.id.groupName)
        private val groupMemberCount: TextView = itemView.findViewById(R.id.groupMemberCount)
        private val groupCard: CardView = itemView.findViewById(R.id.groupCard)
        private val adminBadge: TextView = itemView.findViewById(R.id.adminBadge)

        fun bind(group: Group) {
            groupName.text = group.name

            // Nombre de membres
            val memberCount = group.members.count { it.value }  // Compte uniquement les membres acceptés
            groupMemberCount.text = "$memberCount membres"

            // Badge admin
            if (group.isAdmin) {
                adminBadge.visibility = View.VISIBLE
            } else {
                adminBadge.visibility = View.GONE
            }

            // Couleur de fond différente pour les groupes dont on est admin
            if (group.isAdmin) {
                groupCard.setCardBackgroundColor(itemView.context.getColor(R.color.admin_group_color))
            } else {
                groupCard.setCardBackgroundColor(itemView.context.getColor(R.color.member_group_color))
            }
        }
    }
}