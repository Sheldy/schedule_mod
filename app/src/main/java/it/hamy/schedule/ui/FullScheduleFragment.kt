package it.hamy.schedule.ui

import android.Manifest
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.cardview.widget.CardView
import it.hamy.schedule.R
import it.hamy.schedule.databinding.FragmentFullScheduleBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.content.Context.CONNECTIVITY_SERVICE
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat.getSystemService

data class ScheduleItem(val day: String, val time: String, val subject: String, val teacher: String, val room: String)

class FullScheduleFragment : Fragment() {

    private var _binding: FragmentFullScheduleBinding? = null
    private val binding get() = _binding!!

    private val scheduleByDate = mutableMapOf<String, MutableList<ScheduleItem>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFullScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val prefs = requireContext().getSharedPreferences("settings", 0)
        val group = prefs.getString("group", null)

        if (group == null) {
            Toast.makeText(requireContext(), "Группа не выбрана", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            if (isNetworkAvailable()) {
                loadFullSchedule(group) // Если есть интернет, загружаем расписание
            } else {
                loadScheduleFromCache() // Если нет интернета, загружаем из кэша
            }
        }
    }

    private suspend fun loadFullSchedule(group: String) {
        withContext(Dispatchers.IO) {
            try {
                val url = "http://schedule.ckstr.ru/$group.htm"
                val response = Jsoup.connect(url).execute()
                val html = response.bodyAsBytes().toString(Charset.forName("windows-1251"))
                val doc: Document = Jsoup.parse(html)

                // Парсим расписание
                val scheduleList = parseSchedule(doc)

                // Группировка расписания по дням
                val groupedSchedule = groupScheduleByDay(scheduleList)

                // Преобразуем в нужный тип Map<String, MutableList<ScheduleItem>>
                val mutableGroupedSchedule = mutableMapOf<String, MutableList<ScheduleItem>>()
                for ((day, lessons) in groupedSchedule) {
                    mutableGroupedSchedule[day] = lessons.toMutableList()
                }

                // Сохраняем в кэш
                saveScheduleToCache(mutableGroupedSchedule)

                // Обновляем UI
                withContext(Dispatchers.Main) {
                    scheduleByDate.clear()
                    scheduleByDate.putAll(mutableGroupedSchedule)
                    showSchedule()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Ошибка загрузки расписания", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadScheduleFromCache() {
        val prefs = requireContext().getSharedPreferences("schedule_cache", Context.MODE_PRIVATE)
        val cachedSchedule = prefs.getString("cached_schedule", null)

        if (cachedSchedule != null) {
            // Преобразуем строку в Map (можно использовать Gson для парсинга JSON, или хранить просто текст)
            val scheduleList = parseScheduleFromCache(cachedSchedule)
            scheduleByDate.clear()
            scheduleByDate.putAll(scheduleList)
            activity?.runOnUiThread { showSchedule() }
        } else {
            activity?.runOnUiThread {
                Toast.makeText(requireContext(), "Нет сохранённого расписания", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun parseScheduleFromCache(cachedSchedule: String): Map<String, MutableList<ScheduleItem>> {
        // Преобразование строки в расписание (можно использовать Gson или любой другой парсер)
        // Для простоты мы будем предполагать, что данные сериализуются в простой строковый формат
        // Для полноценной реализации — используйте Gson или Moshi для парсинга JSON

        // Этот код должен быть адаптирован под ваш способ хранения данных
        return mutableMapOf() // Пример
    }

    private fun saveScheduleToCache(schedule: Map<String, MutableList<ScheduleItem>>) {
        val prefs = requireContext().getSharedPreferences("schedule_cache", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        // Преобразуем в строку (можно использовать Gson)
        val scheduleString = schedule.toString() // Необходимо использовать сериализацию (например, Gson)
        editor.putString("cached_schedule", scheduleString)
        editor.apply()
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = activity?.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun parseSchedule(doc: Document): List<ScheduleItem> {
        val scheduleList = mutableListOf<ScheduleItem>()
        var currentDay = ""

        // Находим таблицу с расписанием
        val rows: Elements = doc.select("table.inf tr")

        for (row in rows) {
            val dayCell = row.select("td.hd[rowspan]")
            if (dayCell.isNotEmpty()) {
                currentDay = dayCell.text()
            }

            val timeCell = row.select("td.hd").firstOrNull { it.attr("rowspan").isEmpty() }?.text()
            val subjectCell = row.select("a.z1").text()
            val teacherCell = row.select("a.z3").text() // Класс для преподавателя
            val roomCell = row.select("a.z2").text() // Класс для кабинета

            // Если есть время и предмет, добавляем в список
            if (!timeCell.isNullOrEmpty() && !subjectCell.isNullOrEmpty()) {
                scheduleList.add(ScheduleItem(currentDay, timeCell, subjectCell, teacherCell, roomCell))
            }
        }
        return scheduleList
    }

    private fun groupScheduleByDay(scheduleList: List<ScheduleItem>): Map<String, List<ScheduleItem>> {
        return scheduleList.groupBy { it.day }
    }

    private fun showSchedule() {
        if (scheduleByDate.isEmpty()) {
            binding.textView.text = "Расписание отсутствует"
            return
        }

        val scheduleContainer = binding.scheduleContainer
        scheduleContainer.removeAllViews()

        for ((date, lessons) in scheduleByDate) {
            val formattedDate = formatDate(date)
            val dateTitle = TextView(requireContext()).apply {
                text = formattedDate
                textSize = 20f
                setTypeface(null, Typeface.BOLD)
                setPadding(16, 16, 16, 8) // отступы
            }
            scheduleContainer.addView(dateTitle)

            for (lesson in lessons) {
                val cardView = LayoutInflater.from(requireContext()).inflate(R.layout.lesson_item, null) as CardView

                val lessonNumberTextView = cardView.findViewById<TextView>(R.id.lessonNumber)
                val lessonSubjectTextView = cardView.findViewById<TextView>(R.id.lessonSubject)
                val lessonTeacherTextView = cardView.findViewById<TextView>(R.id.lessonTeacher)
                val lessonRoomTextView = cardView.findViewById<TextView>(R.id.lessonRoom)

                lessonNumberTextView.text = lesson.time
                lessonSubjectTextView.text = lesson.subject
                lessonTeacherTextView.text = "${lesson.teacher}"
                lessonRoomTextView.text = "Кабинет: ${lesson.room}"

                scheduleContainer.addView(cardView)
            }
        }
    }

    private fun formatDate(date: String): String {
        val inputFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
        val outputFormat = SimpleDateFormat("d MMMM, EEEE", Locale("ru")) // Формат: 10 апреля, четверг
        val parsedDate: Date = inputFormat.parse(date) ?: return date
        return outputFormat.format(parsedDate)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
