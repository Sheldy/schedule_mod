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
import java.text.SimpleDateFormat
import java.util.*
import android.net.ConnectivityManager
import android.content.Context.CONNECTIVITY_SERVICE
import android.widget.LinearLayout
import androidx.annotation.RequiresPermission
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import it.hamy.schedule.model.ScheduleItem

class FullScheduleFragment : Fragment() {

    private var _binding: FragmentFullScheduleBinding? = null
    private val binding get() = _binding!!

    private val scheduleByDate = linkedMapOf<String, MutableList<ScheduleItem>>()
    private val gson = Gson()

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
                loadFullSchedule(group)
            } else {
                loadScheduleFromCache()
            }
        }
    }

    private suspend fun loadFullSchedule(group: String) {
        withContext(Dispatchers.IO) {
            try {
                val url = "http://schedule.ckstr.ru/$group.htm"
                val response = Jsoup.connect(url).execute()
                val html = String(response.bodyAsBytes(), charset("windows-1251"))
                val doc: Document = Jsoup.parse(html)

                val scheduleList = parseSchedule(doc)
                val groupedSchedule = groupScheduleByDay(scheduleList)

                val mutableGroupedSchedule = linkedMapOf<String, MutableList<ScheduleItem>>()
                for ((day, lessons) in groupedSchedule) {
                    mutableGroupedSchedule[day] = lessons.toMutableList()
                }

                saveScheduleToCache(mutableGroupedSchedule)

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
        val cachedJson = prefs.getString("cached_schedule", null)

        if (cachedJson != null) {
            val type = object : TypeToken<LinkedHashMap<String, MutableList<ScheduleItem>>>() {}.type
            val scheduleList: LinkedHashMap<String, MutableList<ScheduleItem>> = gson.fromJson(cachedJson, type)
            scheduleByDate.clear()
            scheduleByDate.putAll(scheduleList)
            activity?.runOnUiThread { showSchedule() }
        } else {
            activity?.runOnUiThread {
                Toast.makeText(requireContext(), "Нет сохранённого расписания", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveScheduleToCache(schedule: Map<String, MutableList<ScheduleItem>>) {
        val prefs = requireContext().getSharedPreferences("schedule_cache", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val json = gson.toJson(schedule)
        editor.putString("cached_schedule", json)
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

        val rows: Elements = doc.select("table.inf tr")

        for (row in rows) {
            val dayCell = row.select("td.hd[rowspan]")
            if (dayCell.isNotEmpty()) {
                currentDay = formatDate(dayCell.text())
            }

            val timeCell = row.select("td.hd").firstOrNull { it.attr("rowspan").isEmpty() }?.text()
            val subjectRaw = row.select("a.z1").map { it.text() }
            val teacherRaw = row.select("a.z3").map { it.text() }
            val roomRaw = row.select("a.z2").map { it.text() }

            val subject = subjectRaw.distinct().joinToString(" / ")
            val teacher = teacherRaw.distinct().joinToString(" / ")
            val room = roomRaw.distinct().joinToString(" / ")

            if (!timeCell.isNullOrEmpty() && subject.isNotEmpty()) {
                scheduleList.add(ScheduleItem(currentDay, timeCell, subject, teacher, room))
            }
        }

        return scheduleList
    }

    private fun groupScheduleByDay(scheduleList: List<ScheduleItem>): Map<String, List<ScheduleItem>> {
        return scheduleList.groupBy { it.day }
    }

    private fun showSchedule() {
        binding.progressBar.visibility = View.GONE

        if (scheduleByDate.isEmpty()) {
            binding.textView.text = "Расписание отсутствует"
            return
        }
        else
        {
            binding.textView.visibility = View.GONE

        }

        val scheduleContainer = binding.scheduleContainer
        scheduleContainer.removeAllViews()

        for ((date, lessons) in scheduleByDate) {
            val dateTitle = TextView(requireContext()).apply {
                text = date
                textSize = 20f
                setTypeface(null, Typeface.BOLD)
                setPadding(16, 16, 16, 8)
            }
            scheduleContainer.addView(dateTitle)

            for (lesson in lessons) {
                // Inflating the layout for the card view
                val cardView = LayoutInflater.from(requireContext()).inflate(R.layout.lesson_item, null) as CardView

                // Accessing the TextViews within the card
                val lessonNumberTextView = cardView.findViewById<TextView>(R.id.lessonNumber)
                val lessonSubjectTextView = cardView.findViewById<TextView>(R.id.lessonSubject)
                val lessonTeacherTextView = cardView.findViewById<TextView>(R.id.lessonTeacher)
                val lessonRoomTextView = cardView.findViewById<TextView>(R.id.lessonRoom)

                // Setting text to the TextViews
                lessonNumberTextView.text = lesson.time
                lessonSubjectTextView.text = lesson.subject
                lessonTeacherTextView.text = lesson.teacher
                lessonRoomTextView.text = if (lesson.room.isNotBlank()) {
                    "Кабинет: ${lesson.room}"
                } else {
                    "Дистант"
                }


                // Set LayoutParams with margin for cardView
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(16, 16, 16, 16) // Set bottom margin to add spacing between cards
                cardView.layoutParams = params

                // Adding the card view to the container
                scheduleContainer.addView(cardView)
            }

        }
    }





    private fun formatDate(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
            val date = inputFormat.parse(dateStr)

            val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale("ru"))
            val dayMonthFormat = SimpleDateFormat("d MMMM", Locale("ru"))

            val dayOfWeek = dayOfWeekFormat.format(date!!)
            val dayMonth = dayMonthFormat.format(date)

            "$dayMonth, $dayOfWeek"
        } catch (e: Exception) {
            dateStr
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
