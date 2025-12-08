

package nl.fijnedagvan.app // Controleer je package naam

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.text.HtmlCompat

class DetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        // Haal de doorgestuurde data op uit de Intent
        // Gebruik de 'key' "EXTRA_DAG_VAN" die we in MainActivity hebben gebruikt
        val dagVan = intent.getParcelableExtra<DagVan>("EXTRA_DAG_VAN")

        // Controleer of de data daadwerkelijk is ontvangen
        if (dagVan == null) {
            Log.e("DetailActivity", "Geen DagVan object ontvangen in de intent.")
            // Toon eventueel een foutmelding aan de gebruiker en sluit de activity
            finish() // Ga terug naar het vorige scherm
            return
        }

        // Initialiseer de views uit de layout
        val tvNaam: TextView = findViewById(R.id.tvDetailNaam)
        val tvDatum: TextView = findViewById(R.id.tvDetailDatum)
        val tvInfo: TextView = findViewById(R.id.tvDetailInfo)
        val tvWebsite: TextView = findViewById(R.id.tvDetailWebsite)

        // Vul de views met de data uit het DagVan object
        tvNaam.text = dagVan.naam ?: "Onbekende Dag"
        tvDatum.text = dagVan.datumString ?: "Geen datum"

        // De 'info' tekst bevat HTML, dus we parsen die voor correcte weergave
        if (!dagVan.uitgebreideInfo.isNullOrEmpty()) {
            tvInfo.text = HtmlCompat.fromHtml(dagVan.uitgebreideInfo, HtmlCompat.FROM_HTML_MODE_COMPACT)
            tvInfo.movementMethod = LinkMovementMethod.getInstance() // Maakt links in de info-tekst klikbaar
        } else {
            tvInfo.text = "Geen extra informatie beschikbaar."
        }

        // Maak de website link klikbaar
        if (!dagVan.websiteUrl.isNullOrEmpty() && dagVan.websiteUrl.startsWith("http")) {
            tvWebsite.visibility = View.VISIBLE
            tvWebsite.text = "Bezoek de officiële website" // Of toon de URL zelf: dagVan.websiteUrl
            tvWebsite.setOnClickListener {
                try {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(dagVan.websiteUrl))
                    startActivity(browserIntent)
                } catch (e: Exception) {
                    Log.e("DetailActivity", "Kon website niet openen: ${dagVan.websiteUrl}", e)
                    // Toon eventueel een melding aan de gebruiker
                }
            }
        } else {
            tvWebsite.visibility = View.GONE // Verberg als er geen geldige website URL is
        }
    }
}