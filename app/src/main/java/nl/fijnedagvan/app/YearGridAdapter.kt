package nl.fijnedagvan.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

class YearGridAdapter(
    private val onMonthClicked: (YearMonth) -> Unit,
    private var eventsCountPerMonth: Map<YearMonth, Int> = emptyMap()
) : RecyclerView.Adapter<YearGridAdapter.YearMonthViewHolder>() {

    private val months = mutableListOf<YearMonth>()
    private var currentYear = YearMonth.now().year

    init {
        updateYearList(currentYear)
    }

    private fun updateYearList(year: Int) {
        months.clear()
        for (i in 1..12) {
            months.add(YearMonth.of(year, i))
        }
    }

    inner class YearMonthViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) { // Let op: itemView hier
        private val monthText: TextView = itemView.findViewById(R.id.monthText) // Correcte ID
        private val daysCountText: TextView = itemView.findViewById(R.id.daysCountText) // Correcte ID

        init {
            itemView.setOnClickListener {
                onMonthClicked(months[bindingAdapterPosition])
            }
        }

        fun bind(month: YearMonth) {
            monthText.text = month.month.getDisplayName(TextStyle.FULL, Locale("nl")).replaceFirstChar { it.titlecase(Locale("nl")) }

            val daysCount = eventsCountPerMonth[month] ?: 0
            daysCountText.text = "$daysCount Dagen"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): YearMonthViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_year_month, parent, false)
        return YearMonthViewHolder(view)
    }

    override fun onBindViewHolder(holder: YearMonthViewHolder, position: Int) {
        holder.bind(months[position])
    }

    override fun getItemCount(): Int = months.size

    fun updateYearAndEvents(year: Int, newEventsCountPerMonth: Map<YearMonth, Int>) {
        if (this.currentYear != year) {
            updateYearList(year)
            this.currentYear = year
        }
        this.eventsCountPerMonth = newEventsCountPerMonth
        notifyDataSetChanged()
    }
}