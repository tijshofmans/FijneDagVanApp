/*

README

Type: Adapter (Lijst-beheerder)

Functie: De "bouwvakker" voor de lijsten met Dagen.

Gebruik: Wordt gebruikt in HomeFragment, OverzichtFragment en SearchFragment. Het zet de ruwe DagVan-data om in de zichtbare blokjes (item_dag_van.xml) en regelt wat er gebeurt als je op een dag klikt.
 */

package nl.fijnedagvan.app

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load

// Deze enum definieert de mogelijke weergave stijlen voor onze adapter
enum class DisplayMode {
    FULL,           // Toont naam + intro/snippet + afbeelding
    NAME_ONLY       // Toont alleen de naam (en verbergt afbeelding + intro)
}

class DagVanAdapter(
    private var dagen: List<DagVan>,
    private val displayMode: DisplayMode, // De weergavemodus (FULL of NAME_ONLY)
    private val onItemClicked: (DagVan) -> Unit // Functie die wordt aangeroepen bij een klik
) : RecyclerView.Adapter<DagVanAdapter.DagVanViewHolder>() {

    /**
     * ViewHolder houdt de views voor één enkel item in de lijst vast.
     */
    class DagVanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val naamTextView: TextView = itemView.findViewById(R.id.tvDagNaamItem)
        val introTextView: TextView = itemView.findViewById(R.id.tvDagIntroItem)
        val afbeeldingImageView: ImageView = itemView.findViewById(R.id.ivDagAfbeelding)
    }

    /**
     * Wordt aangeroepen wanneer de RecyclerView een nieuwe, lege view voor een item moet maken.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DagVanViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dag_van, parent, false)
        return DagVanViewHolder(itemView)
    }

    /**
     * Wordt aangeroepen om de data van een 'DagVan' object te koppelen aan de views.
     */
    override fun onBindViewHolder(holder: DagVanViewHolder, position: Int) {
        val huidigeDag = dagen[position]
        val context = holder.itemView.context

        // Stel altijd de naam van de dag in
        holder.naamTextView.text = huidigeDag.naam ?: "Naam onbekend"

        // Controleer de weergavemodus
        if (displayMode == DisplayMode.FULL) {
            // --- VOLLEDIGE WEERGAVE: Toon afbeelding en intro/snippet ---
            holder.afbeeldingImageView.visibility = View.VISIBLE
            holder.introTextView.visibility = View.VISIBLE

            // Laad de afbeelding met Coil
            holder.afbeeldingImageView.load(huidigeDag.imageUrl) {
                crossfade(true)
                placeholder(R.mipmap.ic_launcher_round)
                error(R.mipmap.ic_launcher_round)
            }

            // Bepaal de tekst voor de intro/snippet
            if (!huidigeDag.intro.isNullOrEmpty()) {
                // Scenario 1: Er is een introductietekst
                holder.introTextView.text = huidigeDag.intro
                // ZET DE STIJL NU NAAR NORMAAL (NIET BOLD)
                holder.introTextView.typeface = Typeface.DEFAULT
                holder.introTextView.setTextColor(ContextCompat.getColor(context, android.R.color.secondary_text_light))

            } else if (!huidigeDag.uitgebreideInfo.isNullOrEmpty()) {
                // Scenario 2: Geen intro, maar wel 'info' tekst. Maak een snippet.
                val schoneInfoTekst = HtmlCompat.fromHtml(huidigeDag.uitgebreideInfo, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
                val snippet = schoneInfoTekst.split(' ', '\n').filter { it.isNotBlank() }.take(10).joinToString(" ")
                val leesMeerTekst = "... lees meer"
                val volledigeTekst = "$snippet$leesMeerTekst"

                val spannable = SpannableStringBuilder(volledigeTekst)

                // DE BOLD-STIJL IS HIER VERWIJDERD.

                // Pas de blauwe kleur toe op alleen het laatste deel
                spannable.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(context, R.color.link_blue)),
                    snippet.length,
                    volledigeTekst.length,
                    SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                holder.introTextView.text = spannable

            } else {
                holder.introTextView.visibility = View.GONE
            }
        } else {
            // --- SIMPELE WEERGAVE (NAME_ONLY): Verberg afbeelding en intro ---
            holder.introTextView.visibility = View.GONE
            holder.afbeeldingImageView.visibility = View.GONE
        }

        // Koppel de OnClickListener aan het hele item
        holder.itemView.setOnClickListener {
            onItemClicked(huidigeDag)
        }
    }

    /**
     * Geeft het totale aantal items in de lijst terug.
     */
    override fun getItemCount() = dagen.size

    /**
     * Een functie om de lijst met dagen in de adapter te updaten.
     */
    fun updateDagen(nieuweDagen: List<DagVan>) {
        dagen = nieuweDagen
        notifyDataSetChanged()
    }
}