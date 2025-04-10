package it.hamy.schedule.ui

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.cardview.widget.CardView
import it.hamy.schedule.R
import it.hamy.schedule.databinding.FragmentTodayBinding
import it.hamy.schedule.model.ScheduleItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

class TodayFragment : Fragment() {

    private var _binding: FragmentTodayBinding? = null
    private val binding get() = _binding!!
    private val scheduleItems = mutableListOf<ScheduleItem>()
    private val gson = com.google.gson.Gson()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTodayBinding.inflate(inflater, container, false)
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
                loadTodaySchedule(group)
            } else {
                loadScheduleFromCache()
            }
        }






    }

    private suspend fun loadTodaySchedule(group: String) {
        withContext(Dispatchers.IO) {
            try {
                val url = "http://schedule.ckstr.ru/hg.htm"
                val response = Jsoup.connect(url).execute()
                val html = String(response.bodyAsBytes(), charset("windows-1251"))
                val doc: Document = Jsoup.parse(html)

                val date = parseDate(doc)
                parseSchedule(doc, group, date)

                saveScheduleToCache(scheduleItems)

                withContext(Dispatchers.Main) {
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

    private fun parseDate(doc: Document): String {
        val dateElement = doc.select("ul.zg li.zgr").first()
        return dateElement?.text() ?: SimpleDateFormat("dd.MM.yyyy", Locale("ru")).format(Date())
    }

    private fun parseSchedule(doc: Document, group: String, date: String) {
        scheduleItems.clear()

        val rows: Elements = doc.select("table.inf tr")
        var currentGroupLink = ""

        for (row in rows) {
            // Ищем ячейку с классом 'hd' и атрибутом 'rowspan', внутри которой есть ссылка
            val groupCell = row.select("td.hd[rowspan] a[href]")

            if (groupCell.isNotEmpty()) {
                // Извлекаем ссылку целиком из атрибута 'href'
                val groupLink = groupCell.attr("href").substringBefore(".htm") // Теперь сохраняем всю ссылку до .htm
                currentGroupLink = groupLink

                // Выводим в лог название группы и её ссылку для сравнения
                Log.d("GroupComparison", "Group: $group, Link: $currentGroupLink")
            }

            // Пропускаем строки, если ссылка группы не совпадает
            if (currentGroupLink != group) continue

            val timeCell = row.select("td.hd").firstOrNull { it.attr("rowspan").isEmpty() }?.text()
            val subjectRaw = row.select("a.z1").map { it.text() }
            val teacherRaw = row.select("a.z3").map { it.text() }
            val roomRaw = row.select("a.z2").map { it.text() }

            val subject = subjectRaw.distinct().joinToString(" / ")
            val teacher = teacherRaw.distinct().joinToString(" / ")
            val room = roomRaw.distinct().joinToString(" / ")

            if (!timeCell.isNullOrEmpty() && subject.isNotEmpty()) {
                scheduleItems.add(ScheduleItem(formatDate(date), timeCell, subject, teacher, room))
            }
        }
    }

    private fun formatDate(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("dd.MM.yyyy E", Locale("ru"))
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

    private fun loadScheduleFromCache() {
        val prefs = requireContext().getSharedPreferences("schedule_cache", Context.MODE_PRIVATE)
        val cachedJson = prefs.getString("cached_today_schedule", null)

        if (cachedJson != null) {
            val type = object : com.google.gson.reflect.TypeToken<List<ScheduleItem>>() {}.type
            val cachedList: List<ScheduleItem> = gson.fromJson(cachedJson, type)
            scheduleItems.clear()
            scheduleItems.addAll(cachedList)
            activity?.runOnUiThread { showSchedule() }
        } else {
            activity?.runOnUiThread {
                Toast.makeText(requireContext(), "Нет сохранённого расписания", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveScheduleToCache(schedule: List<ScheduleItem>) {
        val prefs = requireContext().getSharedPreferences("schedule_cache", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val json = gson.toJson(schedule)
        editor.putString("cached_today_schedule", json)
        editor.apply()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = activity?.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun showSchedule() {
        binding.progressBar.visibility = View.GONE

        if (scheduleItems.isEmpty()) {
            binding.textView.text = "Расписание отсутствует"
            return
        }
        else
        {
            binding.textView.visibility = View.GONE
        }

        val scheduleContainer = binding.scheduleContainer
        scheduleContainer.removeAllViews()


        // Add greeting above the schedule
        val greetingTextView = TextView(requireContext()).apply {
            text = getGreetingBasedOnTime()  // Приветствие
            textSize = 28f
            setTypeface(null, Typeface.BOLD)
            setPadding(26, 16, 26, 8)
        }
        binding.scheduleContainer.addView(greetingTextView)

        // Add date title
        val dateTitle = TextView(requireContext()).apply {
            text = scheduleItems.firstOrNull()?.day ?: "Сегодня"        // дата
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setPadding(26, 16, 26, 8)
        }
        scheduleContainer.addView(dateTitle)

// Флаг для отслеживания текущего состояния текста
        var isTimeToNextLesson = false

// Add greeting above the schedule
        val timeTextView = TextView(requireContext()).apply {
            text = getCurrentTimeString()  // текущее время
            textSize = 36f
            setTypeface(null, Typeface.BOLD)
            setPadding(26, 16, 26, 12)
        }

// Обработчик клика для чередования текста
        timeTextView.setOnClickListener {
            if (isTimeToNextLesson) {
                // Если показываем время до звонка, меняем на текущее время
                timeTextView.text = getCurrentTimeString()
                timeTextView.textSize = 36f  // Устанавливаем размер шрифта для текущего времени
            } else {
                // Если показываем текущее время, меняем на время до звонка
                val timeToNextLesson = getTimeToNextLesson()
                timeTextView.text = timeToNextLesson
                timeTextView.textSize = 22f  // Устанавливаем размер шрифта для времени до звонка
            }

            // Меняем флаг
            isTimeToNextLesson = !isTimeToNextLesson
        }

// Добавляем TextView в контейнер расписания
        binding.scheduleContainer.addView(timeTextView)



        // Add lessons
        for (lesson in scheduleItems) {
            val cardView = LayoutInflater.from(requireContext()).inflate(R.layout.lesson_item, null) as CardView

            val lessonNumberTextView = cardView.findViewById<TextView>(R.id.lessonNumber)
            val lessonSubjectTextView = cardView.findViewById<TextView>(R.id.lessonSubject)
            val lessonTeacherTextView = cardView.findViewById<TextView>(R.id.lessonTeacher)
            val lessonRoomTextView = cardView.findViewById<TextView>(R.id.lessonRoom)
            val lessonTimeTextView = cardView.findViewById<TextView>(R.id.lessonTime)

            lessonNumberTextView.text = lesson.time
            lessonSubjectTextView.text = lesson.subject
            lessonTeacherTextView.text = lesson.teacher
            lessonRoomTextView.text = if (lesson.room.isNotBlank()) {
                "Кабинет: ${lesson.room}"
            } else {
                "Дистант"
            }

            val lessonTime = getLessonTime(lesson.time)

            lessonTimeTextView.text = lessonTime


            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            params.setMargins(16, 16, 16, 16)  // 16dp margin for all edges
            cardView.layoutParams = params

            scheduleContainer.addView(cardView)
        }
    }

    private fun getLessonTime(lessonNumber: String): String {
        val currentDayOfWeek = SimpleDateFormat("EEEE", Locale("ru")).format(Date())

        return when (currentDayOfWeek) {
            "понедельник" -> {
                when (lessonNumber) {
                    "1" -> "08:30 – 10:00"
                    "2" -> "10:10 – 11:40"
                    "3" -> "12:10 – 13:40"
                    "4" -> "13:50 – 15:20"
                    "5" -> "15:30 – 16:50"
                    "6" -> "17:00 – 18:20"
                    else -> "Время не указано"
                }
            }
            "вторник" -> {
                when (lessonNumber) {
                    "1" -> "08:30 – 10:00"
                    "2" -> "10:10 – 11:40"
                    "3" -> "12:10 – 13:40"
                    "4" -> "13:50 – 15:20"
                    "5" -> "15:30 – 16:50"
                    "6" -> "17:00 – 18:20"
                    else -> "Время не указано"
                }
            }
            "среда" -> { // Среда
                when (lessonNumber) {
                    "1" -> "09:00 – 10:30"
                    "2" -> "10:40 – 12:10"
                    "3" -> "12:30 – 14:00"
                    "4" -> "14:10 – 15:40"
                    "5" -> "15:50 – 17:10"
                    "6" -> "17:20 – 18:40"
                    else -> "Время не указано"
                }
            }
            "четверг" -> {
                when (lessonNumber) {
                    "1" -> "08:30 – 10:00"
                    "2" -> "10:10 – 11:40"
                    "3" -> "12:10 – 13:40"
                    "4" -> "13:50 – 15:20"
                    "5" -> "15:30 – 16:50"
                    "6" -> "17:00 – 18:20"
                    else -> "Время не указано"
                }
            }
            "пятница" -> { // Пятница
                when (lessonNumber) {
                    "1" -> "08:30 – 10:00"
                    "2" -> "10:10 – 11:40"
                    "3" -> "12:10 – 13:40"
                    "4" -> "13:50 – 15:20"
                    "5" -> "15:30 – 16:50"
                    "6" -> "17:00 – 18:20"
                    else -> "Время не указано"
                }
            }

            "суббота" -> { // Суббота
                ""  // В субботу не выводим расписание
            }

            else -> { // Для всех остальных дней
                when (lessonNumber) {
                    "1" -> "08:30 – 10:00"
                    "2" -> "10:10 – 11:40"
                    "3" -> "11:50 – 13:20"
                    "4" -> "13:30 – 15:00"
                    "5" -> "15:10 – 16:40"
                    "6" -> "16:50 – 18:20"
                    else -> "Время не указано"
                }
            }
        }
    }

    private fun getGreetingBasedOnTime(): String {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        return when {
            currentHour in 5..11 -> "Доброе утро ☺\uFE0F"
            currentHour in 12..17 -> "Добрый день \uD83E\uDD2A"
            currentHour in 18..21 -> "Добрый вечер \uD83D\uDE0C"
            else -> "Доброй ночи \uD83D\uDE34"
        }
    }


    // Метод для получения текущего времени в строковом формате
    private fun getCurrentTimeString(): String {
        val currentTime = SimpleDateFormat("HH:mm", Locale("ru")).format(Date())
        return currentTime
    }



    fun getTimeToNextLesson(): String {
        val currentTime = Calendar.getInstance()  // Текущее время
        val currentDayOfWeek = SimpleDateFormat("EEEE", Locale("ru")).format(Date())  // Текущий день недели

        // Получаем все занятия на текущий день
        for (lessonNumber in 1..6) {  // Проходим по всем 6 возможным парам
            val lessonTime = getLessonTime(lessonNumber.toString())  // Получаем время текущего занятия

            if (lessonTime != "Время не указано" && lessonTime != "") {
                val lessonTimes = lessonTime.split(" – ")
                val startLessonTime = lessonTimes[0]
                val endLessonTime = lessonTimes[1]

                // Преобразуем время начала и конца занятия в объекты Calendar
                val startTimeCalendar = Calendar.getInstance()
                val endTimeCalendar = Calendar.getInstance()
                val timeFormat = SimpleDateFormat("HH:mm", Locale("ru"))

                startTimeCalendar.time = timeFormat.parse(startLessonTime)
                endTimeCalendar.time = timeFormat.parse(endLessonTime)

                // Привязываем дату текущего дня к времени начала и окончания урока
                startTimeCalendar.set(Calendar.YEAR, currentTime.get(Calendar.YEAR))
                startTimeCalendar.set(Calendar.MONTH, currentTime.get(Calendar.MONTH))
                startTimeCalendar.set(Calendar.DAY_OF_MONTH, currentTime.get(Calendar.DAY_OF_MONTH))

                endTimeCalendar.set(Calendar.YEAR, currentTime.get(Calendar.YEAR))
                endTimeCalendar.set(Calendar.MONTH, currentTime.get(Calendar.MONTH))
                endTimeCalendar.set(Calendar.DAY_OF_MONTH, currentTime.get(Calendar.DAY_OF_MONTH))

                // Если текущее время до начала занятия
                if (currentTime.before(startTimeCalendar)) {
                    val diffMillis = startTimeCalendar.timeInMillis - currentTime.timeInMillis
                    val diffMinutes = diffMillis / (1000 * 60)  // Разница в минутах
                    return "До пар: $diffMinutes минут"
                }

                // Если текущее время в пределах занятия (сейчас идет урок)
                if (currentTime.after(startTimeCalendar) && currentTime.before(endTimeCalendar)) {
                    val diffMillis = endTimeCalendar.timeInMillis - currentTime.timeInMillis
                    val diffMinutes = diffMillis / (1000 * 60)  // Разница в минутах
                    return "До конца пары: $diffMinutes минут"
                }
            }
        }

        // Если на текущий момент нет активных занятий
        return "Занятий на сегодня больше нет или все занятия завершены."
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
