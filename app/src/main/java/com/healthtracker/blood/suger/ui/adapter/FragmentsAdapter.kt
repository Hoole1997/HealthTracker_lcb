package com.healthtracker.blood.suger.ui.adapter

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class FragmentsAdapter(
    fm: FragmentManager,
    private val count: Int,
    private val callback: Callback
): FragmentPagerAdapter(fm) {
    interface Callback {
        fun getPageTitle(position: Int): String? {
            return null
        }

        fun createInstance(position: Int): Fragment

        fun onInstance(position: Int, fragment: Fragment)
    }

    override fun getCount() = count

    override fun getItemId(position: Int) = position.toLong()

    override fun getPageTitle(position: Int) = callback.getPageTitle(position)

    override fun getItem(position: Int) = callback.createInstance(position)

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        return super.instantiateItem(container, position).also {
            if (it is Fragment) callback.onInstance(position, it)
        }
    }
}