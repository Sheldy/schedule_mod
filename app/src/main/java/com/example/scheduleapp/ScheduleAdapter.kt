package it.hamy.shedule

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ScheduleAdapter : RecyclerView.Adapter<ScheduleAdapter.DayViewHolder>() {

    private val groupedSchedule = mutableMapOf<String, List<ScheduleItem>>()

    fun updateSchedule(newSchedule: Map<String, List<ScheduleItem>>) {
        groupedSchedule.clear()
        groupedSchedule.putAll(newSchedule)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_schedule, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = groupedSchedule.keys.elementAt(position)
        val items = groupedSchedule[day] ?: emptyList()
        holder.bind(day, items)
    }

    override fun getItemCount(): Int = groupedSchedule.size

    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dayTextView: TextView = itemView.findViewById(R.id.dayTextView)
        private val lessonsContainer: LinearLayout = itemView.findViewById(R.id.lessonsContainer)

        fun bind(day: String, items: List<ScheduleItem>) {
            dayTextView.text = formatDate(day) // Форматируем дату
            lessonsContainer.removeAllViews() // Очистка контейнера перед добавлением новых элементов

            for (item in items) {
                val lessonView = LayoutInflater.from(itemView.context).inflate(R.layout.item_lesson, lessonsContainer, false)
                val timeTextView: TextView = lessonView.findViewById(R.id.timeTextView)
                val subjectTextView: TextView = lessonView.findViewById(R.id.subjectTextView)
                val teacherTextView: TextView = lessonView.findViewById(R.id.teacherTextView)
                val roomTextView: TextView = lessonView.findViewById(R.id.roomTextView)

                timeTextView.text = item.time
                subjectTextView.text = item.subject
                teacherTextView.text = "Преподаватель: ${item.teacher}"
                roomTextView.text = "Кабинет: ${item.room}"

                lessonsContainer.addView(lessonView) // Добавление нового блока занятия в контейнер
            }
        }

        private fun formatDate(dateString: String): String {
            val inputFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
            val outputFormat = SimpleDateFormat("dd MMMM, EEEE", Locale("ru"))
            val date: Date = inputFormat.parse(dateString.split(" ")[0]) // Извлекаем только дату
            return outputFormat.format(date)
        }
    }
}