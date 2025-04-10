package it.hamy.schedule.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import it.hamy.schedule.MainActivity
import it.hamy.schedule.databinding.ActivityGroupSelectBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup



class GroupSelectActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGroupSelectBinding
    private val groupList = mutableListOf<Pair<String, String>>() // (Имя, Код)
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        binding.listView.adapter = adapter

        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            loadGroups()
        }

        binding.listView.setOnItemClickListener { _, _, position, _ ->
            val (_, groupCode) = groupList[position] // только код
            getSharedPreferences("settings", MODE_PRIVATE)
                .edit()
                .putString("group", groupCode) // сохраняем код (например, "cg107")
                .apply()

            val intent = Intent(this@GroupSelectActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private suspend fun loadGroups() {
        withContext(Dispatchers.IO) {
            try {
                val doc = Jsoup.connect("http://schedule.ckstr.ru/cg.htm").get()
                val links = doc.select("a.z0") // <a class="z0" href="cg107.htm">ИСП-22</a>
                for (link in links) {
                    val groupName = link.text().trim()
                    val href = link.attr("href").removeSuffix(".htm").trim() // cg107

                    if (groupName.isNotEmpty() && href.isNotEmpty()) {
                        groupList.add(groupName to href)
                        Log.d("GroupParse", "Группа: $groupName — Код: $href")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@GroupSelectActivity, "Ошибка загрузки групп", Toast.LENGTH_SHORT).show()
                }
            }
        }

        withContext(Dispatchers.Main) {
            adapter.clear()
            adapter.addAll(groupList.map { it.first }) // Только имена в список
            adapter.notifyDataSetChanged()
            binding.progressBar.visibility = View.GONE
        }
    }
}
