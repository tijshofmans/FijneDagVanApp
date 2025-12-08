package nl.fijnedagvan.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class DayViewPagerAdapter(
    private val startDate: LocalDate, // De startdatum van de pager
    private val eventsByDate: Map<LocalDate, List<DagVan>>, // De kaart met alle evenementen
    private val onItemClicked: (DagVan) -> Unit
) : RecyclerView.Adapter<DayViewPagerAdapter.DayViewHolder>() {

    // Het aantal dagen in de pager is nu gigantisch, zodat we vrij kunnen scrollen
    override fun getItemCount(): Int = Int.MAX_VALUE

    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.tvDayPageTitle)
        val eventsRecyclerView: RecyclerView = itemView.findViewById(R.id.rvDayPageEvents)
        val emptyTextView: TextView = itemView.findViewById(R.id.tvDayPageEmpty)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.page_day_view, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        // Bereken de datum voor deze pagina
        val date = startDate.plusDays(position.toLong())
        val eventsForDate = eventsByDate[date] ?: emptyList()

        // Stel de titel in
        val formatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale("nl"))
        holder.titleTextView.text = date.format(formatter).replaceFirstChar { it.titlecase(Locale("nl")) }

        // Toon de lijst of de 'lege staat' boodschap
        if (eventsForDate.isEmpty()) {
            holder.eventsRecyclerView.isVisible = false
            holder.emptyTextView.isVisible = true
        } else {
            holder.eventsRecyclerView.isVisible = true
            holder.emptyTextView.isVisible = false
            holder.eventsRecyclerView.adapter = DagVanAdapter(eventsForDate, DisplayMode.FULL, onItemClicked)
        }
    }
}