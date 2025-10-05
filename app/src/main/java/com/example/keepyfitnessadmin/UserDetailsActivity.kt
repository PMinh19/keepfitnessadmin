package com.example.keepyfitnessadmin

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class UserDetailsActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private var userId: String? = null
    private var userEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_details)

        userId = intent.getStringExtra("USER_ID")
        userEmail = intent.getStringExtra("USER_EMAIL")

        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)

        val adapter = UserDetailsViewPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "Workouts"
                1 -> tab.text = "Heart Rate"
                2 -> tab.text = "Schedules"
            }
        }.attach()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    inner class UserDetailsViewPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> AdminWorkoutsFragment().apply {
                    arguments = Bundle().apply {
                        putString("USER_ID", userId)
                        putString("USER_EMAIL", userEmail)
                    }
                }
                1 -> AdminHeartRateFragment().apply {
                    arguments = Bundle().apply {
                        putString("USER_ID", userId)
                        putString("USER_EMAIL", userEmail)
                    }
                }
                2 -> AdminSchedulesFragment().apply {
                    arguments = Bundle().apply {
                        putString("USER_ID", userId)
                        putString("USER_EMAIL", userEmail)
                    }
                }
                else -> AdminWorkoutsFragment().apply {
                    arguments = Bundle().apply {
                        putString("USER_ID", userId)
                        putString("USER_EMAIL", userEmail)
                    }
                }
            }
        }
    }
}
