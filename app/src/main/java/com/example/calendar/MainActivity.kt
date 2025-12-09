package com.example.calendar

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.CalendarYear
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.OutDateStyle
import com.kizitonwose.calendar.core.Week
import com.kizitonwose.calendar.core.WeekDay
import com.kizitonwose.calendar.core.WeekDayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.kizitonwose.calendar.view.ViewContainer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.Year
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private val Unit.calendarMonth: Any
    get() {
        TODO()
    }
private val Nothing?.date: Any
    get() {
        TODO()
    }
private val Nothing?.position: Any
    get() {
        TODO()
    }

private fun Nothing?.setOnClickListener(function: () -> Unit) {
    TODO("Not yet implemented")
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val floatingActionButtonView = findViewById<FloatingActionButton>(R.id.floatingActionButton)
        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            // Called only when a new container is needed.
            override fun create(view: View) = DayViewContainer(view)

            // Called every time we need to reuse a container.
            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.textView.text = data.date.dayOfMonth.toString()
            }
        }
        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(100) // Adjust as needed
        val endMonth = currentMonth.plusMonths(100) // Adjust as needed
        val firstDayOfWeek = firstDayOfWeekFromLocale() // Available from the library
        calendarView.setup(startMonth, endMonth, firstDayOfWeek)
        calendarView.scrollToMonth(currentMonth)
        val daysOfWeek = daysOfWeek() // Available in the library
        val titlesContainer = findViewById<ViewGroup>(R.id.titlesContainer)
        titlesContainer.children
            .map { it as TextView }
            .forEachIndexed { index, textView ->
                val dayOfWeek = daysOfWeek[index]
                val title = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                textView.text = title
            }
        class MonthViewContainer(view: View) : ViewContainer(view) {
            // Alternatively, you can add an ID to the container layout and use findViewById()
            val titlesContainer = view as ViewGroup
        }
        calendarView.monthHeaderBinder = object : MonthHeaderFooterBinder<MonthViewContainer> {
            override fun create(view: View) = MonthViewContainer(view)
            override fun bind(container: MonthViewContainer, data: CalendarMonth) {
                // Remember that the header is reused so this will be called for each month.
                // However, the first day of the week will not change so no need to bind
                // the same view every time it is reused.
                if (container.titlesContainer.tag == null) {
                    container.titlesContainer.tag = data.yearMonth
                    container.titlesContainer.children.map { it as TextView }
                        .forEachIndexed { index, textView ->
                            val dayOfWeek = daysOfWeek[index]
                            val title =
                                dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                            textView.text = title
                            // In the code above, we use the same `daysOfWeek` list
                            // that was created when we set up the calendar.
                            // However, we can also get the `daysOfWeek` list from the month data:
                            // val daysOfWeek = data.weekDays.first().map { it.date.dayOfWeek }
                            // Alternatively, you can get the value for this specific index:
                            // val dayOfWeek = data.weekDays.first()[index].date.dayOfWeek
                        }
                    calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
                        override fun create(view: View) = DayViewContainer(view)
                        override fun bind(container: DayViewContainer, data: CalendarDay) {
                            container.textView!!.text = data.date.dayOfMonth.toString()
                            if (data.position == DayPosition.MonthDate) container.textView.setTextColor(
                                Color.WHITE)
                            else {
                                container.textView.setTextColor(Color.GRAY)
                            }
                            val view = null
                            view.setOnClickListener {
                                // Check the day position as we do not want to select in or out dates.
                                val day = null
                                if (day.position == DayPosition.MonthDate) {
                                    // Keep a reference to any previous selection
                                    // in case we overwrite it and need to reload it.
                                    var selectedDate: Any? = null
                                    val currentSelection = selectedDate
                                    selectedDate = day.date
                                    // Reload the newly selected date so the dayBinder is
                                    // called and we can ADD the selection background.
                                    calendarView.notifyDateChanged(day.date as LocalDate)
                                }
                                view.setOnClickListener {
                                    // Check the day position as we do not want to select in or out dates.
                                    if (day.position == DayPosition.MonthDate) {
                                        // Only use month dates
                                    }
                                    calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
                                        override fun create(view: View) = DayViewContainer(view)
                                        override fun bind(container: DayViewContainer, data: CalendarDay) {
                                            container.day = data
                                            val textView = container.textView
                                            textView.text = data.date.dayOfMonth.toString()
                                            textView.alpha = if (day.position == DayPosition.MonthDate) 1f else 0.3f
                                        }
                                }
                            }
                        }
                    }
                }

                class DayViewContainer : ViewContainer {
                    constructor(view: View) : super(view) {
                        this.textView = view.findViewById<TextView>(R.id.calendarView)
                        view.setOnClickListener {
                            // Use the CalendarDay associated with this container.
                        }
                    }

                    val textView: TextView?

                    // Will be set when this container is bound
                    lateinit var day: CalendarDay
                }
            }
        }

        // CORRECT
        val myCalendar = findViewById<CalendarView>(R.id.calendarView)
    }}
public data class WeekDateRange(
    val startDateAdjusted: LocalDate,
    val endDateAdjusted: LocalDate,
)

public fun getWeekCalendarAdjustedRange(
    startDate: LocalDate,
    endDate: LocalDate,
    firstDayOfWeek: DayOfWeek,
): WeekDateRange {
    val inDays = firstDayOfWeek.daysUntil(startDate.dayOfWeek)
    val startDateAdjusted = startDate.minusDays(inDays.toLong())
    val weeksBetween =
        ChronoUnit.WEEKS.between(startDateAdjusted, endDate).toInt()
    val endDateAdjusted = startDateAdjusted.plusWeeks(weeksBetween.toLong()).plusDays(6)
    return WeekDateRange(startDateAdjusted = startDateAdjusted, endDateAdjusted = endDateAdjusted)
}

private fun Unit.toLong(): Long {
    TODO("Not yet implemented")
}

private fun DayOfWeek.daysUntil(dayOfWeek: DayOfWeek) {
    TODO("Not yet implemented")
}

public fun getWeekCalendarData(
    startDateAdjusted: LocalDate,
    offset: Int,
    desiredStartDate: LocalDate,
    desiredEndDate: LocalDate,
): WeekData {
    val firstDayInWeek = startDateAdjusted.plusWeeks(offset.toLong())
    return WeekData(firstDayInWeek, desiredStartDate, desiredEndDate)
}

@ConsistentCopyVisibility
public data class WeekData internal constructor(
    private val firstDayInWeek: LocalDate,
    private val desiredStartDate: LocalDate,
    private val desiredEndDate: LocalDate,
) {
    val week: Week = Week((0 until 7).map { dayOffset -> getDay(dayOffset) })

    private fun getDay(dayOffset: Int): WeekDay {
        val date = firstDayInWeek.plusDays(dayOffset.toLong())
        val position = when {
            date < desiredStartDate -> WeekDayPosition.InDate
            date > desiredEndDate -> WeekDayPosition.OutDate
            else -> WeekDayPosition.RangeDate
        }
        return WeekDay(date, position)
    }
}

public fun getWeekIndex(startDateAdjusted: LocalDate, date: LocalDate): Int {
    return ChronoUnit.WEEKS.between(startDateAdjusted, date).toInt()
}

public fun getWeekIndicesCount(startDateAdjusted: LocalDate, endDateAdjusted: LocalDate): Int {
    // Add one to include the start week itself!
    return getWeekIndex(startDateAdjusted, endDateAdjusted) + 1
}
public fun getCalendarYearData(
    startYear: Year,
    offset: Int,
    firstDayOfWeek: DayOfWeek,
    outDateStyle: OutDateStyle,
): CalendarYear {
    val year = startYear.plusYears(offset.toLong())
    val months = List(size = Month.entries.size) { index ->
        getCalendarMonthData(
            startMonth = year.atMonth(Month.JANUARY),
            offset = index,
            firstDayOfWeek = firstDayOfWeek,
            outDateStyle = outDateStyle,
        ).calendarMonth
    }
    return CalendarYear(year, months)
}

fun CalendarYear(
    year: Year,
    months: List<Any>
): CalendarYear {
    TODO("Not yet implemented")
}

fun getCalendarMonthData(
    startMonth: YearMonth,
    offset: Int,
    firstDayOfWeek: DayOfWeek,
    outDateStyle: OutDateStyle
) {
    TODO("Not yet implemented")
}

public fun getYearIndex(startYear: Year, targetYear: Year): Int {
    return ChronoUnit.YEARS.between(startYear, targetYear).toInt()
}

public fun getYearIndicesCount(startYear: Year, endYear: Year): Int {
    // Add one to include the start year itself!
    return getYearIndex(startYear, endYear) + 1
}}
