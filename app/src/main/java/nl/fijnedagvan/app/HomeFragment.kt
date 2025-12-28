/*
Type: UI (Scherm)

Functie: Het startscherm. Toont de datum van vandaag, de lijst met dagen van vandaag en een simpele kalender-widget om een andere datum te prikken. Luistert naar het SharedViewModel voor data.
 */


package nl.fijnedagvan.app

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.kizitonwose.calendar.core.*
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.kizitonwose.calendar.view.ViewContainer
import nl.fijnedagvan.app.databinding.CalendarDayLayoutBinding
import nl.fijnedagvan.app.databinding.FragmentHomeBinding
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

// ViewContainer voor een dag in de kalender
class DayViewContainer(view: View, val onClick: (LocalDate) -> Unit) : ViewContainer(view) {
    var day: LocalDate? = null
    val binding = CalendarDayLayoutBinding.bind(view)
    init {
        view.setOnClickListener { day?.let(onClick) }
    }
}

// ViewContainer voor de maand-header in de kalender
class MonthViewContainer(view: View) : ViewContainer(view) {
    val textView = view.findViewById<TextView>(R.id.headerTextView)
}


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()

    private var selectedDate: LocalDate? = null
    private val today: LocalDate = LocalDate.now()

    private val eventsByDate = mutableMapOf<LocalDate, List<DagVan>>()

    // Adapters voor de twee RecyclerViews
    private lateinit var todayAdapter: DagVanAdapter
    private lateinit var selectedDayAdapter: DagVanAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.tvTodayDate.text = "Hallo, het is vandaag ${formatDateToDisplay(today)}"

        // Initialiseer beide RecyclerViews
        setupTodayRecyclerView()
        setupSelectedDayRecyclerView()

        setupDateSpinners()
        setupCustomCalendar()

    }

    private fun observeViewModel() {
        sharedViewModel.jaarLijst.observe(viewLifecycleOwner) { dagen ->
            if (dagen != null) {
                // Update de "vandaag" sectie
                val todayApiString = formatDateToApi(today)
                val todayEvents = dagen.filter { it.datumString == todayApiString }
                updateTodayDisplay(todayEvents)

                // Groepeer alle evenementen per datum voor de kalender
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val groupedByDate = dagen.filter { it.datumString != null }.groupBy { LocalDate.parse(it.datumString, formatter) }
                eventsByDate.clear()
                eventsByDate.putAll(groupedByDate)

                // Vertel de kalender dat de data is veranderd (voor de bolletjes)
                binding.cvCalendar.notifyCalendarChanged()
            }
        }
    }

    private fun setupTodayRecyclerView() {
        todayAdapter = DagVanAdapter(emptyList(), DisplayMode.FULL) { geklikteDag -> navigateToDetail(geklikteDag) }
        binding.rvTodayEvents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = todayAdapter
        }
    }

    private fun setupSelectedDayRecyclerView() {
        selectedDayAdapter = DagVanAdapter(emptyList(), DisplayMode.FULL) { geklikteDag -> navigateToDetail(geklikteDag) }
        binding.rvSelectedDayEvents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = selectedDayAdapter
        }
    }

    private fun updateTodayDisplay(events: List<DagVan>) {
        if (events.isEmpty()) {
            binding.tvTodayIntro.text = "Er zijn vandaag geen speciale Dagen te vieren."
            binding.rvTodayEvents.isVisible = false
        } else {
            binding.tvTodayIntro.text = "We vieren vandaag de volgende Dagen:"
            binding.rvTodayEvents.isVisible = true
            todayAdapter.updateDagen(events) // Gebruik de correcte functienaam
        }
    }

    private fun setupDateSpinners() {
        // De lijsten met dagen en maanden blijven hetzelfde.
        val days = mutableListOf("Dag").apply { addAll((1..31).map { it.toString() }) }
        val months = mutableListOf("Maand").apply {
            addAll(Month.values().map {
                it.getDisplayName(TextStyle.FULL, Locale("nl")).replaceFirstChar { char -> char.titlecase(Locale("nl")) }
            })
        }

        val dayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, days)
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spDag.adapter = dayAdapter

        val monthAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, months)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spMaand.adapter = monthAdapter

        // NIEUWE LOGICA: Listener om automatisch te zoeken na selectie.
        val spinnerListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Alleen uitvoeren als de gebruiker niet de hint-tekst ("Dag" of "Maand") selecteert.
                if (binding.spDag.selectedItemPosition > 0 && binding.spMaand.selectedItemPosition > 0) {
                    onSpinnerDateSelected()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Doe niets
            }
        }

        binding.spDag.onItemSelectedListener = spinnerListener
        binding.spMaand.onItemSelectedListener = spinnerListener
    }

    private fun onSpinnerDateSelected() {
        if (binding.spDag.selectedItemPosition == 0 || binding.spMaand.selectedItemPosition == 0) {
            return
        }

        val day = binding.spDag.selectedItem.toString().toInt()
        val month = binding.spMaand.selectedItemPosition
        val year = today.year

        try {
            val date = LocalDate.of(year, month, day)
            binding.cvCalendar.scrollToMonth(YearMonth.from(date))
            onDayClicked(date) // Roep onDayClicked aan om de selectie te verwerken
        } catch (e: Exception) {
            // Ongeldige datum, bv. 31 februari
        }
    }

    private fun setupCustomCalendar() {
        val homeCalendarView = binding.cvCalendar
        val currentMonth = YearMonth.now()
        val startMonth = YearMonth.of(2000, 1)
        val endMonth = YearMonth.of(2050, 12)
        val firstDayOfWeek = firstDayOfWeekFromLocale()

        homeCalendarView.setup(startMonth, endMonth, firstDayOfWeek)
        homeCalendarView.scrollToMonth(currentMonth)

        // Dag binder
        homeCalendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view) { date -> onDayClicked(date) }
            override fun bind(container: DayViewContainer, day: CalendarDay) {
                container.day = day.date
                bindSimpleDayStyling(container, day)
            }
        }

        // Maand-header binder (DE NIEUWE, CORRECTE CODE)
        homeCalendarView.monthHeaderBinder = object :
            MonthHeaderFooterBinder<MonthViewContainer> {
            // De 'create' methode blaast nu zelf de layout op.
            // Dit is de juiste manier.
            override fun create(view: View): MonthViewContainer {
                val headerView = LayoutInflater.from(view.context)
                    .inflate(R.layout.calendar_header_layout, view as ViewGroup, false)
                return MonthViewContainer(headerView)
            }

            override fun bind(container: MonthViewContainer, month: CalendarMonth) {
                val monthTitle =
                    "${
                        month.yearMonth.month.getDisplayName(TextStyle.FULL, Locale("nl"))
                            .replaceFirstChar { it.titlecase(Locale("nl")) }
                    } ${month.yearMonth.year}"
                // Nu is container.textView niet meer null, omdat hij is gevonden in de
                // correct "opgeblazen" headerView.
                container.textView.text = monthTitle
            }
        }
    }

        private fun bindSimpleDayStyling(container: DayViewContainer, day: CalendarDay) {
        val textView = container.binding.calendarDayText
        textView.text = day.date.dayOfMonth.toString()

        if (day.position == DayPosition.MonthDate) {
            textView.setTextColor(Color.BLACK)
            // Highlight de geselecteerde dag
            if (day.date == selectedDate) {
                textView.setBackgroundResource(R.drawable.calendar_selected_bg) // Aangepaste achtergrond
                textView.setTextColor(Color.WHITE)
            } else {
                textView.background = null
            }
        } else {
            // Dagen buiten de huidige maand zijn onzichtbaar
            textView.setTextColor(Color.TRANSPARENT)
            textView.background = null
        }

        // Toon indicatoren voor evenementen
        val eventCount = eventsByDate[day.date]?.size ?: 0
        container.binding.eventIndicatorDot1.isVisible = day.position == DayPosition.MonthDate && eventCount >= 1
        container.binding.eventIndicatorDot2.isVisible = day.position == DayPosition.MonthDate && eventCount >= 2
        container.binding.eventIndicatorPlus.isVisible = day.position == DayPosition.MonthDate && eventCount >= 3
    }


    private fun onDayClicked(date: LocalDate) {
        val oldDate = selectedDate
        val homeCalendarView = binding.cvCalendar

        if (oldDate == date) {
            // Deselecteer de dag
            selectedDate = null
            homeCalendarView.notifyDateChanged(date) // Update de UI voor de geklikte dag
        } else {
            // Selecteer een nieuwe dag
            selectedDate = date
            homeCalendarView.notifyDateChanged(date) // Update UI voor de nieuwe selectie
            oldDate?.let { homeCalendarView.notifyDateChanged(it) } // Deselecteer de oude dag in de UI
        }
        // Update de sectie onder de kalender met de nieuwe selectie (of verberg het)
        updateSelectedDayDisplay(selectedDate)
    }

    /**
     * Vult de UI sectie onder de kalender met de evenementen voor de gegeven datum.
     * Verbergt de sectie als de datum null is.
     */
    private fun updateSelectedDayDisplay(date: LocalDate?) {
        val container = binding.selectedDayContainer

        // Als de datum null is (deselectie), verberg de hele container en stop.
        if (date == null) {
            container.isVisible = false
            return
        }

        // Maak de container zichtbaar
        container.isVisible = true

        val eventsForDate = eventsByDate[date] ?: emptyList()

        if (eventsForDate.isEmpty()) {
            // Geen evenementen: verberg de lijst, toon de 'lege' tekst
            binding.rvSelectedDayEvents.isVisible = false
            binding.tvSelectedDayEmpty.isVisible = true
        } else {
            // Wel evenementen: toon de lijst, verberg de 'lege' tekst
            binding.rvSelectedDayEvents.isVisible = true
            binding.tvSelectedDayEmpty.isVisible = false
            selectedDayAdapter.updateDagen(eventsForDate)
        }
    }


    private fun navigateToDetail(dag: DagVan) {
        val action = HomeFragmentDirections.actionNavHomeToDetailFragment(dag)
        findNavController().navigate(action)
    }

    private fun formatDateToDisplay(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("d MMMM", Locale("nl"))
        return date.format(formatter)
    }

    private fun formatDateToApi(date: LocalDate): String {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE) // yyyy-MM-dd
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
