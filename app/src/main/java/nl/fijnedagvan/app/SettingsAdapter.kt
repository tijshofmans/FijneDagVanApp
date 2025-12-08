/*
Type: Adapter (Lijst-beheerder)

Functie: Specifieke adapter voor de lijst op de Instellingen-pagina. Toont de dagen waarvoor een melding is ingesteld en bevat de logica voor de aan/uit-schakelaars in die lijst.
 */



package nl.fijnedagvan.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.SimpleDateFormat
import java.util.Locale

class SettingsAdapter(
    private var subscriptions: List<NotificationPrefsManager.Subscription>,
    private val onSwitchToggled: (NotificationPrefsManager.Subscription, Boolean) -> Unit
) : RecyclerView.Adapter<SettingsAdapter.SettingsViewHolder>() {

    class SettingsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val naamTextView: TextView = itemView.findViewById(R.id.tvSettingDagNaam)
        val datumTextView: TextView = itemView.findViewById(R.id.tvSettingDagDatum)
        val notificationSwitch: SwitchMaterial = itemView.findViewById(R.id.switchSettingDag)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_setting_dag, parent, false)
        return SettingsViewHolder(view)
    }

    override fun getItemCount() = subscriptions.size

    override fun onBindViewHolder(holder: SettingsViewHolder, position: Int) {
        val sub = subscriptions[position]
        holder.naamTextView.text = sub.dagNaam
        holder.datumTextView.text = formatDisplayDate(sub.datumString)

        // Stel de switch in ZONDER de listener te triggeren
        holder.notificationSwitch.setOnCheckedChangeListener(null)
        // De switch is altijd 'aan' voor items in deze lijst
        holder.notificationSwitch.isChecked = true

        // Zet nu de listener die reageert op de actie van de gebruiker
        holder.notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            onSwitchToggled(sub, isChecked)
        }
    }

    fun updateSubscriptions(newSubs: List<NotificationPrefsManager.Subscription>) {
        subscriptions = newSubs
        notifyDataSetChanged()
    }

    private fun formatDisplayDate(apiDate: String?): String? {
        if (apiDate == null) return null
        return try {
            val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = apiDateFormat.parse(apiDate)
            val displayDateFormat = SimpleDateFormat("d MMMM", Locale("nl", "NL"))
            date?.let { displayDateFormat.format(it) }
        } catch (e: Exception) { null }
    }
}