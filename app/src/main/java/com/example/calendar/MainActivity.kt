package com.example.calendar

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

class MainActivity : AppCompatActivity() {

    // 儲存日期與對應網址的 Map
    private val eventDateMap = mutableMapOf<LocalDate, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val calendarView = findViewById<CalendarView>(R.id.calendarView)

        // 開始抓取活動資料
        fetchEventData(calendarView)

        // 定義日曆格子容器
        class DayViewContainer(view: View) : ViewContainer(view) {
            val textView: TextView = view.findViewById(R.id.calendarDayText)
            lateinit var day: CalendarDay
            
            init {
                view.setOnClickListener {
                    if (day.position == DayPosition.MonthDate) {
                        val url = eventDateMap[day.date]
                        if (url != null) {
                            // 點擊有活動的日期：開啟網頁
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            view.context.startActivity(intent)
                        } else {
                            // 點擊一般日期：顯示提示
                            Toast.makeText(view.context, "日期 ${day.date} 沒有活動內容", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        // 綁定日曆格子
        calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.day = data
                container.textView.text = data.date.dayOfMonth.toString()
                
                if (data.position == DayPosition.MonthDate) {
                    if (eventDateMap.containsKey(data.date)) {
                        // 有活動的日期：深藍色文字 + 淺藍背景
                        container.textView.setTextColor(Color.parseColor("#00008B"))
                        container.textView.setBackgroundColor(Color.parseColor("#E1F5FE"))
                    } else {
                        // 一般日期
                        container.textView.setTextColor(Color.BLACK)
                        container.textView.setBackgroundColor(Color.TRANSPARENT)
                    }
                } else {
                    // 非本月日期
                    container.textView.setTextColor(Color.GRAY)
                    container.textView.setBackgroundColor(Color.TRANSPARENT)
                }
            }
        }

        // 初始化日曆設定
        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(12)
        val endMonth = currentMonth.plusMonths(12)
        val firstDayOfWeek = firstDayOfWeekFromLocale()
        calendarView.setup(startMonth, endMonth, firstDayOfWeek)
        calendarView.scrollToMonth(currentMonth)

        // 月份標題更新
        val monthYearTv = findViewById<TextView>(R.id.monthYearTv)
        calendarView.monthScrollListener = {
            monthYearTv.text = "${it.yearMonth.year}年${it.yearMonth.monthValue}月"
        }

        // 星期標題更新
        val daysOfWeek = daysOfWeek()
        val titlesContainer = findViewById<ViewGroup>(R.id.titlesContainer)
        titlesContainer.children
            .map { it as TextView }
            .forEachIndexed { index, textView ->
                val dayOfWeek = daysOfWeek[index]
                val title = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                textView.text = title
            }
    }

    /**
     * 抓取網頁活動資料
     */
    private fun fetchEventData(calendarView: CalendarView) {
        lifecycleScope.launch {
            Log.d("Scraper", "開始抓取活動資料...")
            
            withContext(Dispatchers.IO) {
                // 使用並行處理加快速度，檢查 500-600 範圍
                (500..600).map { id ->
                    async {
                        val url = "https://taoyuan-walk.com.tw/event/$id"
                        try {
                            val doc = Jsoup.connect(url)
                                .userAgent("Mozilla/5.0")
                                .timeout(5000)
                                .get()
                            
                            // 在整頁文字中尋找日期格式 (如 2024-12-30 或 2024/12/30)
                            val bodyText = doc.text()
                            val dateRegex = Regex("(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})")
                            val match = dateRegex.find(bodyText)

                            if (match != null) {
                                val year = match.groupValues[1].toInt()
                                val month = match.groupValues[2].toInt()
                                val day = match.groupValues[3].toInt()
                                
                                val date = LocalDate.of(year, month, day)
                                synchronized(eventDateMap) {
                                    eventDateMap[date] = url
                                }
                                Log.d("Scraper", "找到活動：$date -> $url")
                                
                                // 立即更新 UI 上的該日期
                                withContext(Dispatchers.Main) {
                                    calendarView.notifyDateChanged(date)
                                }
                            }
                        } catch (e: Exception) {
                            // 忽略找不到或連線失敗的頁面
                        }
                    }
                }.awaitAll()
            }
            
            Log.d("Scraper", "抓取完成，共找到 ${eventDateMap.size} 個活動")
            Toast.makeText(this@MainActivity, "活動資料載入完成", Toast.LENGTH_SHORT).show()
        }
    }
}
