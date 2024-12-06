package com.scsa.andr.catcher.news

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class NewsPagerAdapter(
    activity: FragmentActivity, private val rssUrls: List<String>
) : FragmentStateAdapter(activity) {
    override fun getItemCount() = rssUrls.size

    override fun createFragment(position: Int): Fragment {
        return NewsFragment.newInstance(rssUrls[position])
    }
}
