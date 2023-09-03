/**
 * Copyright (c) 2023 Natalia Molinero Mingorance
 * All rights reserved.
 */

package harmony.valley.wamboocam

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter


/// This class is used to inflate fragment as a tab by viewpager
class ViewPagerAdapter(fragmentManager: FragmentActivity) : FragmentStateAdapter(fragmentManager) {

    /// below function used to tell total number to tabs displayed in viewpager
    override fun getItemCount(): Int {
        return 3
    }

    /// return specific fragment based on index.
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment.newInstance()
            1 -> InfoFragment.newInstance()
            else -> HistoryFragment.newInstance()
        }
    }
}