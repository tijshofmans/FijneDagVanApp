package nl.fijnedagvan.app

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kizitonwose.calendar.core.WeekDay
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.view.ViewContainer
import com.kizitonwose.calendar.view.WeekCalendarView
import com.kizitonwose.calendar.view.WeekDayBinder
import nl.fijnedagvan.app.databinding.ItemCalendarWeekDayBinding
import java.time.LocalDate

// We gebruiken hier dezelfde ViewContainer die je al hebt
class TestWeekDayViewContainer(view: View) : ViewContainer(view) {
    val binding = ItemCalendarWeekDayBinding.bind(view)
}

class TestFragment : Fragment(R.layout.fragment_test) {

    private lateinit var weekCalendarView: WeekCalendarView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        weekCalendarView = view.findViewById(R.id.testWeekCalendar)

        // Setup de binder
        weekCalendarView.dayBinder = object : WeekDayBinder<TestWeekDayViewContainer> {

            // --- HIER ZIT DE OPLOSSING ---
            override fun create(view: View): TestWeekDayViewContainer {
                // Forceer de hoogte van de cel wanneer deze wordt aangemaakt.
                val heightInPixels = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 100f, // Test met een duidelijke hoogte van 100dp
                    view.resources.displayMetrics
                ).toInt()

                // Pak de bestaande layout-parameters en pas alleen de hoogte aan
                val layoutParams = view.layoutParams
                layoutParams.height = heightInPixels
                view.layoutParams = layoutParams

                return TestWeekDayViewContainer(view)
            }

            override fun bind(container: TestWeekDayViewContainer, day: WeekDay) {
                container.binding.tvWeekDayNumber.text = day.date.dayOfMonth.toString()

                // Laat een voorbeeld-event zien om de layout te testen
                container.binding.tvWeekDayEventName.visibility = View.VISIBLE
                container.binding.tvWeekDayEventName.text = "Deze cel zou nu 100dp hoog moeten zijn"
            }
        }

        // Setup de kalender
        val today = LocalDate.now()
        val firstDayOfWeek = firstDayOfWeekFromLocale()
        weekCalendarView.setup(today.minusMonths(12), today.plusMonths(12), firstDayOfWeek)
        weekCalendarView.scrollToWeek(today)
    }
}