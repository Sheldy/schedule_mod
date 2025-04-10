package it.hamy.schedule

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Context
import android.content.SharedPreferences
import it.hamy.schedule.ui.*

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val selectedGroup = sharedPref.getString("group", null)

        if (selectedGroup != null) {
            // Если группа выбрана, перенаправляем на экран с расписанием
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            // Если группа не выбрана, перенаправляем на экран выбора группы
            startActivity(Intent(this, GroupSelectActivity::class.java))
        }

        finish() // Закрываем SplashActivity
    }
}