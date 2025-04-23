package it.hamy.schedule.ui

import android.graphics.Typeface
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import it.hamy.schedule.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import it.hamy.schedule.model.LessonResult

class ResultsActivity : AppCompatActivity() {

    private lateinit var resultsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results)

        resultsContainer = findViewById(R.id.resultsContainer)
        fetchAndDisplayResults()
    }

    private fun fetchAndDisplayResults() {
        val savedGroup = getSharedPreferences("settings", MODE_PRIVATE).getString("group", "") ?: return
        val resultsGroup = savedGroup.replaceFirst("cg", "vg")
        val url = "http://schedule.ckstr.ru/${resultsGroup}.htm"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val doc = Jsoup.connect(url).get()
                val rows = doc.select("tr:has(td.vp)")
                val results = rows.map { row ->
                    val tds = row.select("td")
                    LessonResult(
                        teacher = tds[1].text(),
                        subject = tds[4].text(),
                        type = tds[5].text(),
                        totalHours = tds[6].text().toIntOrNull() ?: 0,
                        actualHours = tds[8].text().toIntOrNull() ?: 0,
                        remainingHours = tds[9].text().toIntOrNull() ?: 0,
                        endDate = tds[12].text(),
                        percent = tds[13].select("img").attr("alt").replace("%", "").toIntOrNull() ?: 0
                    )
                }

                withContext(Dispatchers.Main) {
                    results.forEach { result -> addResultView(result) }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ResultsActivity, "Ошибка загрузки", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun addResultView(result: LessonResult) {
        val view = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 24)
        }

        val subjectText = TextView(this).apply {
            text = "${result.subject} (${result.type})"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
        }

        val teacherText = TextView(this).apply {
            text = "Преподаватель: ${result.teacher}"
        }

        val remainingLessons = result.remainingHours / 2
        val hoursText = TextView(this).apply {
            text = "Часов: ${result.actualHours} из ${result.totalHours} (осталось: ${result.remainingHours} часов, $remainingLessons занятий)"
        }

        val endDateText = TextView(this).apply {
            text = "Окончание: ${result.endDate}"
        }

        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = result.percent
            progressDrawable = ContextCompat.getDrawable(this@ResultsActivity, android.R.drawable.progress_horizontal)
        }

        val percentText = TextView(this).apply {
            text = "Выполнено: ${result.percent}%"
        }

        view.addView(subjectText)
        view.addView(teacherText)
        view.addView(hoursText)
        view.addView(endDateText)
        view.addView(progressBar)
        view.addView(percentText)

        resultsContainer.addView(view)
    }
}
