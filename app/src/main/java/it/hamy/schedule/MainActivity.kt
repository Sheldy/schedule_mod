package it.hamy.schedule

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

import it.hamy.schedule.ui.FullScheduleFragment
import it.hamy.schedule.ui.GroupSelectActivity
import it.hamy.schedule.ui.SettingsFragment
import it.hamy.schedule.ui.TodayFragment
import it.hamy.schedule.R
import it.hamy.schedule.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val todayFragment = TodayFragment()
    private val fullScheduleFragment = FullScheduleFragment()
    private val settingsFragment = SettingsFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
        openFragment(todayFragment) // по умолчанию "Сегодня"


        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val group = prefs.getString("group", null)
        if (group == null) {
            startActivity(Intent(this, GroupSelectActivity::class.java))
            finish()
            return
        }

    }

    private fun setupBottomNavigation() {
        val nav = binding.bottomNav
        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_today -> openFragment(todayFragment)
                R.id.nav_schedule -> openFragment(fullScheduleFragment)
                R.id.nav_settings -> openFragment(settingsFragment)
            }
            true
        }
    }

    private fun openFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
