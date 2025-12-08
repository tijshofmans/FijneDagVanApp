/*
Type: UI (Scherm)

Functie: De uitgebreide agenda-pagina.

Complexiteit: Bevat de Kizitonwose-kalender, de logica voor maand/week-wissel, de filters (dagsoort, schaal, onderwerp) en de lijst met resultaten onder de kalender.
 */



package nl.fijnedagvan.app

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.transition.TransitionManager
import androidx.viewpager2.widget.ViewPager2

import com.kizitonwose.calendar.core.*
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import com.kizitonwose.calendar.view.WeekDayBinder

import nl.fijnedagvan.app.databinding.CalendarDayLayoutBinding
import nl.fijnedagvan.app.databinding.FragmentOverzichtBinding
import nl.fijnedagvan.app.databinding.ItemCalendarWeekDayBinding

import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

// ViewContainers
class MonthDayViewContainer(view: View, val onClick: (LocalDate) -> Unit) : ViewContainer(view) {
    var day: LocalDate? = null
    val binding = CalendarDayLayoutBinding.bind(view)
    init { view.setOnClickListener { day?.let(onClick) } }
}

class WeekDayViewContainer(view: View, val onClick: (LocalDate) -> Unit) : ViewContainer(view) {
    var day: LocalDate? = null
    val binding = ItemCalendarWeekDayBinding.bind(view)
    init { view.setOnClickListener { day?.let(onClick) } }
}


class OverzichtFragment : Fragment() {

    private var _binding: FragmentOverzichtBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()

    private lateinit var calendarEventsAdapter: DagVanAdapter
    private lateinit var dayViewPagerAdapter: DayViewPagerAdapter
    private lateinit var yearGridAdapter: YearGridAdapter

    private var selectedDate: LocalDate? = null
    private val today = LocalDate.now()
    private var currentYear = Year.now()

    private val eventsByDate = mutableMapOf<LocalDate, List<DagVan>>()
    private val pagerStartDate = today.minusYears(10)

    private val geselecteerdeDagsoorten = mutableSetOf<String>()
    private val geselecteerdeSchalen = mutableSetOf<String>()
    private val geselecteerdeOnderwerpen = mutableSetOf<String>()
    private var alleDagenCache: List<DagVan> = emptyList()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOverzichtBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
            binding.monthYearText, 14, 22, 2, TypedValue.COMPLEX_UNIT_SP
        )

        setupRecyclerView()
        setupViewPager()
        setupYearGrid()
        setupDayBinders()
        setupToggleAndNavigation()
        setupFilterControls()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observeViewModel() {
        sharedViewModel.jaarLijst.observe(viewLifecycleOwner) { dagen ->
            if (dagen != null) {
                alleDagenCache = dagen
                processDagenData()
            }
        }

        sharedViewModel.allDagsoorten.observe(viewLifecycleOwner) {
            binding.btnFilterDagsoort.isEnabled = !it.isNullOrEmpty()
        }
        sharedViewModel.allSchalen.observe(viewLifecycleOwner) {
            binding.btnFilterSchaal.isEnabled = !it.isNullOrEmpty()
        }
        sharedViewModel.allOnderwerpen.observe(viewLifecycleOwner) {
            binding.btnFilterOnderwerp.isEnabled = !it.isNullOrEmpty()
        }
    }

    private fun processDagenData() {
        var gefilterdeDagen = alleDagenCache.filter { it.datumCheck == "1" || it.datumCheck == "1.0" }

        // --- GECORRIGEERD: .lowercase() toegevoegd ---
        fun itemMatchesFilter(dataString: String?, selectedFilters: Set<String>): Boolean {
            if (selectedFilters.isEmpty()) return true
            if (dataString.isNullOrEmpty()) return false
            // Vergelijk alles met kleine letters
            return dataString.split(',')
                .map { it.trim().lowercase() }
                .any { it in selectedFilters }
        }

        gefilterdeDagen = gefilterdeDagen.filter { dag ->
            val dagsoortMatch = itemMatchesFilter(dag.dagsoort, geselecteerdeDagsoorten)
            val schaalMatch = itemMatchesFilter(dag.schaal, geselecteerdeSchalen)
            val onderwerpMatch = itemMatchesFilter(dag.onderwerp, geselecteerdeOnderwerpen)
            dagsoortMatch && schaalMatch && onderwerpMatch
        }

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val groupedByDate = gefilterdeDagen
            .filter { it.datumString != null }
            .groupBy { LocalDate.parse(it.datumString, formatter) }

        eventsByDate.clear()
        eventsByDate.putAll(groupedByDate)

        if(::dayViewPagerAdapter.isInitialized) dayViewPagerAdapter.notifyDataSetChanged()
        if(::yearGridAdapter.isInitialized) {
            val eventsPerMonth = groupedByDate
                .map { entry -> YearMonth.from(entry.key) to entry.value.size }
                .groupBy({ it.first }) { it.second }
                .mapValues { entry -> entry.value.sum() }
            yearGridAdapter.updateYearAndEvents(currentYear.value, eventsPerMonth)
        }
        binding.monthCalendarView.notifyCalendarChanged()
        binding.weekCalendarView.notifyCalendarChanged()
    }

    private fun setupFilterControls() {
        binding.btnFilterDagsoort.isEnabled = false
        binding.btnFilterSchaal.isEnabled = false
        binding.btnFilterOnderwerp.isEnabled = false

        binding.btnToggleFilter.setOnClickListener {
            TransitionManager.beginDelayedTransition(binding.root as ViewGroup)
            binding.llFilterControls.isVisible = !binding.llFilterControls.isVisible
        }

        binding.btnFilterDagsoort.setOnClickListener {
            val dagsoorten = sharedViewModel.allDagsoorten.value?.toTypedArray() ?: emptyArray()
            showMultiSelectFilterDialog("Filter op Dagsoort", dagsoorten, geselecteerdeDagsoorten) {
                processDagenData()
            }
        }

        binding.btnFilterSchaal.setOnClickListener {
            val schalen = sharedViewModel.allSchalen.value?.toTypedArray() ?: emptyArray()
            showMultiSelectFilterDialog("Filter op Schaal", schalen, geselecteerdeSchalen) {
                processDagenData()
            }
        }

        binding.btnFilterOnderwerp.setOnClickListener {
            val onderwerpen = sharedViewModel.allOnderwerpen.value?.toTypedArray() ?: emptyArray()
            showMultiSelectFilterDialog("Filter op Onderwerp", onderwerpen, geselecteerdeOnderwerpen) {
                processDagenData()
            }
        }
    }

    private fun showMultiSelectFilterDialog(
        title: String,
        options: Array<String>,
        selectedItems: MutableSet<String>,
        onConfirm: () -> Unit
    ) {
        if (options.isEmpty()) {
            Toast.makeText(context, "Er zijn geen opties voor dit filter.", Toast.LENGTH_SHORT).show()
            return
        }

        val checkedItems = options.map { it in selectedItems }.toBooleanArray()
        val tempSelected = selectedItems.toMutableSet()

        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMultiChoiceItems(options, checkedItems) { _, which, isChecked ->
                val selected = options[which]
                if (isChecked) {
                    tempSelected.add(selected)
                } else {
                    tempSelected.remove(selected)
                }
            }
            .setPositiveButton("Ok") { dialog, _ ->
                selectedItems.clear()
                selectedItems.addAll(tempSelected)
                onConfirm()
                dialog.dismiss()
            }
            .setNeutralButton("Reset") { dialog, _ ->
                selectedItems.clear()
                onConfirm()
                dialog.dismiss()
            }
            .setNegativeButton("Annuleren", null)
            .show()
    }

    // --- De rest van de code is ongewijzigd ---

    private fun setupRecyclerView() {
        calendarEventsAdapter = DagVanAdapter(emptyList(), DisplayMode.FULL) { dag ->
            navigateToDetail(dag)
        }
        binding.rvDagenVan.adapter = calendarEventsAdapter
    }

    private fun setupViewPager() {
        dayViewPagerAdapter = DayViewPagerAdapter(pagerStartDate, eventsByDate) { dag ->
            navigateToDetail(dag)
        }
        binding.dayViewPager.adapter = dayViewPagerAdapter

        val todayIndex = ChronoUnit.DAYS.between(pagerStartDate, today).toInt()
        binding.dayViewPager.setCurrentItem(todayIndex, false)

        binding.dayViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val date = pagerStartDate.plusDays(position.toLong())
                updateTitle(date = date)
            }
        })
    }

    private fun setupYearGrid() {
        yearGridAdapter = YearGridAdapter(
            onMonthClicked = { clickedMonth ->
                binding.toggleButtonGroup.check(R.id.buttonMonth)
                binding.monthCalendarView.scrollToMonth(clickedMonth)
            },
            eventsCountPerMonth = emptyMap()
        )
        binding.yearGrid.adapter = yearGridAdapter
        binding.yearGrid.layoutManager = GridLayoutManager(requireContext(), 3)
    }

    private fun setupToggleAndNavigation() {
        binding.toggleButtonGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            TransitionManager.beginDelayedTransition(binding.root as ViewGroup)

            binding.calendarScrollView.isVisible = (checkedId == R.id.buttonMonth || checkedId == R.id.buttonWeek)
            binding.dayViewPager.isVisible = (checkedId == R.id.buttonDay)
            binding.yearGrid.isVisible = (checkedId == R.id.buttonYear)
            binding.calendarHeader.isVisible = true
            binding.tvDayViewEmptyMessage.isVisible = false

            if (binding.calendarScrollView.isVisible) {
                binding.monthCalendarView.isVisible = (checkedId == R.id.buttonMonth)
                binding.weekCalendarView.isVisible = (checkedId == R.id.buttonWeek)
            }

            when (checkedId) {
                R.id.buttonMonth -> updateTitle(month = binding.monthCalendarView.findFirstVisibleMonth())
                R.id.buttonWeek -> updateTitle(date = binding.weekCalendarView.findFirstVisibleWeek()?.days?.firstOrNull()?.date)
                R.id.buttonDay -> {
                    val todayIndex = ChronoUnit.DAYS.between(pagerStartDate, today).toInt()
                    binding.dayViewPager.setCurrentItem(todayIndex, true)
                    updateTitle(date = today)
                }
                R.id.buttonYear -> updateTitle()
            }
        }

        binding.nextMonthButton.setOnClickListener {
            when (binding.toggleButtonGroup.checkedButtonId) {
                R.id.buttonMonth -> binding.monthCalendarView.findFirstVisibleMonth()?.let { it.yearMonth.nextMonth.let { next -> binding.monthCalendarView.smoothScrollToMonth(next) } }
                R.id.buttonWeek -> binding.weekCalendarView.findFirstVisibleWeek()?.let { it.days.first().date.plusWeeks(1).let { next -> binding.weekCalendarView.smoothScrollToWeek(next) } }
                R.id.buttonDay -> binding.dayViewPager.currentItem += 1
                R.id.buttonYear -> {
                    currentYear = currentYear.plusYears(1)
                    sharedViewModel.fetchDataForYear(currentYear.value)
                    updateTitle()
                }
            }
        }
        binding.previousMonthButton.setOnClickListener {
            when (binding.toggleButtonGroup.checkedButtonId) {
                R.id.buttonMonth -> binding.monthCalendarView.findFirstVisibleMonth()?.let { it.yearMonth.previousMonth.let { prev -> binding.monthCalendarView.smoothScrollToMonth(prev) } }
                R.id.buttonWeek -> binding.weekCalendarView.findFirstVisibleWeek()?.let { it.days.first().date.minusWeeks(1).let { prev -> binding.weekCalendarView.smoothScrollToWeek(prev) } }
                R.id.buttonDay -> binding.dayViewPager.currentItem -= 1
                R.id.buttonYear -> {
                    currentYear = currentYear.minusYears(1)
                    sharedViewModel.fetchDataForYear(currentYear.value)
                    updateTitle()
                }
            }
        }
    }

    private fun setupDayBinders() {
        val firstDayOfWeek = firstDayOfWeekFromLocale()
        val currentMonth = YearMonth.now()

        binding.monthCalendarView.setup(currentMonth.minusMonths(24), currentMonth.plusMonths(24), firstDayOfWeek)
        binding.monthCalendarView.scrollToMonth(currentMonth)
        binding.monthCalendarView.monthScrollListener = { updateTitle(month = it) }
        binding.monthCalendarView.dayBinder = object : MonthDayBinder<MonthDayViewContainer> {
            override fun create(view: View) = MonthDayViewContainer(view, this@OverzichtFragment::onDayClicked)
            override fun bind(container: MonthDayViewContainer, day: CalendarDay) {
                container.day = day.date
                bindSimpleDayStyling(container.binding.calendarDayText, day.date, day.position == DayPosition.MonthDate)

                val eventCount = eventsByDate[day.date]?.size ?: 0
                container.binding.eventIndicatorDot1.isVisible = eventCount >= 1
                container.binding.eventIndicatorDot2.isVisible = eventCount >= 2
                container.binding.eventIndicatorPlus.isVisible = eventCount >= 3
            }
        }

        binding.weekCalendarView.dayViewResource = R.layout.item_calendar_week_day
        binding.weekCalendarView.setup(today.minusMonths(24), today.plusMonths(24), firstDayOfWeek)
        binding.weekCalendarView.scrollToWeek(today)
        binding.weekCalendarView.weekScrollListener = { week ->
            week.days.firstOrNull()?.date?.let { updateTitle(date = it) }
        }
        binding.weekCalendarView.dayBinder = object : WeekDayBinder<WeekDayViewContainer> {
            override fun create(view: View) = WeekDayViewContainer(view, this@OverzichtFragment::onDayClicked)
            override fun bind(container: WeekDayViewContainer, day: WeekDay) {
                container.day = day.date
                bindSimpleDayStyling(container.binding.tvWeekDayNumber, day.date, true)

                val events = eventsByDate[day.date]
                val eventCount = events?.size ?: 0

                container.binding.tvWeekDayEventName.isVisible = false
                container.binding.llWeekDayIndicators.isVisible = false

                if (eventCount > 0) {
                    container.binding.tvWeekDayEventName.isVisible = true
                    container.binding.tvWeekDayEventName.text = events?.first()?.naam

                    if (eventCount > 1) {
                        val remainingEvents = eventCount - 1
                        container.binding.llWeekDayIndicators.isVisible = true
                        container.binding.eventIndicatorDot1.isVisible = remainingEvents >= 1
                        container.binding.eventIndicatorDot2.isVisible = remainingEvents >= 2
                        container.binding.eventIndicatorPlus.isVisible = remainingEvents >= 3
                    }
                }
            }
        }
    }

    private fun onDayClicked(date: LocalDate) {
        val oldDate = selectedDate
        selectedDate = if (selectedDate == date) null else date

        binding.monthCalendarView.notifyDateChanged(date)
        oldDate?.let { binding.monthCalendarView.notifyDateChanged(it) }
        binding.weekCalendarView.notifyDateChanged(date)
        oldDate?.let { binding.weekCalendarView.notifyDateChanged(it) }

        updateAdapterForDate(selectedDate)
    }

    private fun bindSimpleDayStyling(textView: TextView, date: LocalDate, isFromCurrentMonth: Boolean) {
        textView.text = date.dayOfMonth.toString()
        textView.background = null
        val isNightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        if (isFromCurrentMonth) {
            textView.setTextColor(if (isNightMode) Color.WHITE else Color.BLACK)
            if (date == selectedDate) {
                textView.background = requireContext().getDrawable(R.drawable.calendar_selected_bg)
                textView.setTextColor(if (isNightMode) Color.WHITE else Color.BLACK)
            } else if (date == today) {
                textView.background = requireContext().getDrawable(R.drawable.calendar_today_bg)
            }
        } else {
            textView.setTextColor(Color.GRAY)
        }
    }

    private fun updateTitle(month: CalendarMonth? = null, date: LocalDate? = null) {
        val titleText = when (binding.toggleButtonGroup.checkedButtonId) {
            R.id.buttonDay -> {
                val formatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale("nl"))
                (date ?: today).format(formatter).replaceFirstChar { it.titlecase(Locale("nl")) }
            }
            R.id.buttonYear -> {
                currentYear.value.toString()
            }
            else -> {
                if (month == null && date == null) return
                val yearMonth = month?.yearMonth ?: YearMonth.from(date)
                val monthName = yearMonth.month.getDisplayName(TextStyle.FULL, Locale("nl"))
                "${monthName.replaceFirstChar { it.titlecase(Locale("nl")) }} ${yearMonth.year}"
            }
        }
        binding.monthYearText.text = titleText
    }

    private fun navigateToDetail(dag: DagVan) {
        try {
            val action = OverzichtFragmentDirections.actionNavOverzichtToNavDetail(dag)
            findNavController().navigate(action)
        } catch (e: Exception) {
            Log.e("NavigationError", "Kon niet navigeren naar detailpagina.", e)
        }
    }

    private fun updateAdapterForDate(date: LocalDate?) {
        binding.tvSelectedDateEventsTitle.visibility = View.GONE
        binding.rvDagenVan.visibility = View.GONE
        binding.tvNoEventsMessage.visibility = View.GONE
        if (date == null) {
            calendarEventsAdapter.updateDagen(emptyList())
            return
        }
        val eventsForDate = eventsByDate[date] ?: emptyList()
        if (eventsForDate.isNotEmpty()) {
            calendarEventsAdapter.updateDagen(eventsForDate)
            val formatter = DateTimeFormatter.ofPattern("d MMMM", Locale("nl"))
            binding.tvSelectedDateEventsTitle.text = "Dagen op ${date.format(formatter)}"
            binding.tvSelectedDateEventsTitle.visibility = View.VISIBLE
            binding.rvDagenVan.visibility = View.VISIBLE
        } else {
            binding.tvNoEventsMessage.visibility = View.VISIBLE
        }
    }
}