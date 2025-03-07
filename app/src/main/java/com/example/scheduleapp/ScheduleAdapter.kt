package it.hamy.shedule

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ScheduleAdapter : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    private val scheduleList = mutableListOf<ScheduleItem>()

    fun updateSchedule(newSchedule: List<ScheduleItem>) {
        scheduleList.clear()
        scheduleList.addAll(newSchedule)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_schedule, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val item = scheduleList[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = scheduleList.size

    class ScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dayTextView: TextView = itemView.findViewById(R.id.dayTextView)
        private val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        private val subjectTextView: TextView = itemView.findViewById(R.id.subjectTextView)

        fun bind(item: ScheduleItem) {
            dayTextView.text = item.day
            timeTextView.text = item.time
            subjectTextView.text = item.subject
        }
    }
}