package com.example.studentlist.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.studentlist.fragments.ListMembersFragment
import com.example.studentlist.fragments.ListTasksFragment

class ListDetailPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val listId: String,
    private val isAdmin: Boolean
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ListTasksFragment.newInstance(listId)
            1 -> ListMembersFragment.newInstance(listId, isAdmin)
            else -> throw IllegalArgumentException("Position invalide: $position")
        }
    }
}