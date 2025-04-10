package it.hamy.schedule.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.*
import it.hamy.schedule.databinding.FragmentTodayBinding



class TodayFragment : Fragment() {

    private var _binding: FragmentTodayBinding? = null
    private val binding get() = _binding!!

    private val lessons = mutableListOf<String>()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTodayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("settings", 0)
        val group = prefs.getString("group", null)

        if (group == null) {
            Toast.makeText(requireContext(), "Группа не выбрана", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            loadTodaySchedule(group)
        }
    }

    private suspend fun loadTodaySchedule(group: String) {
        val todayDate = dateFormat.format(Date())

        withContext(Dispatchers.IO) {
            try {
                val url = "http://schedule.ckstr.ru/cgi-bin/timetable.cgi?n=$group"
                val doc = Jsoup.connect(url).get()
                val tables = doc.select("table")

                if (tables.isEmpty()) return@withContext

                val todayTable = tables.firstOrNull { it.text().contains(todayDate) }

                lessons.clear()
                todayTable?.select("tr")?.forEachIndexed { index, row ->
                    if (index == 0) return@forEachIndexed // пропускаем заголовок
                    val cells = row.select("td")
                    if (cells.size >= 4) {
                        val number = cells[0].text()
                        val time = cells[1].text()
                        val subject = cells[2].text()
                        val teacher = cells[3].text()

                        val text = "$number. $time — $subject\n$teacher"
                        lessons.add(text)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Ошибка загрузки", Toast.LENGTH_SHORT).show()
                }
            }
        }

        withContext(Dispatchers.Main) {
            binding.progressBar.visibility = View.GONE
            binding.textView.text =
                if (lessons.isNotEmpty()) lessons.joinToString("\n\n") else "На сегодня нет занятий"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
