package it.hamy.shedule

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.nio.charset.Charset

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var scheduleAdapter: ScheduleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        scheduleAdapter = ScheduleAdapter()
        recyclerView.adapter = scheduleAdapter

        fetchSchedule()
    }

    private fun fetchSchedule() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Загрузка HTML-страницы с указанием кодировки windows-1251
                val url = "http://schedule.ckstr.ru/cg107.htm"
                val response = Jsoup.connect(url).execute()
                val html = response.bodyAsBytes().toString(Charset.forName("windows-1251")) // Преобразуем в нужную кодировку
                val doc: Document = Jsoup.parse(html)

                // Парсинг расписания
                val scheduleList = parseSchedule(doc)

                // Обновление UI
                withContext(Dispatchers.Main) {
                    scheduleAdapter.updateSchedule(scheduleList)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Ошибка загрузки расписания: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun parseSchedule(doc: Document): List<ScheduleItem> {
        val scheduleList = mutableListOf<ScheduleItem>()

        // Поиск таблицы с расписанием
        val rows: Elements = doc.select("table.inf tr")
        var currentDay = ""

        for (row in rows) {
            val dayCell = row.select("td.hd[rowspan]")
            if (dayCell.isNotEmpty()) {
                currentDay = dayCell.text()
            }

            val timeCell = row.select("td.hd").firstOrNull()?.text()
            val subjectCell = row.select("td.ur").text()

            if (!timeCell.isNullOrEmpty() && !subjectCell.isNullOrEmpty()) {
                scheduleList.add(ScheduleItem(currentDay, timeCell, subjectCell))
            }
        }

        return scheduleList
    }
}