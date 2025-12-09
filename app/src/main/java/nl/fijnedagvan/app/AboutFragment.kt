/*
Type: UI (Scherm)

Testreadme please ignore

Functie: Toont de "Over Ons"-pagina.

Bijzonderheden: Bevat logica om specifieke woorden in de tekst klikbaar te maken (interne links naar Dagen, externe links naar de website) en laadt de header-afbeelding.
 */





package nl.fijnedagvan.app

/*
Dit zijn de imports die nodig zijn voor de Over Ons-pagina
 */


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import coil.load

class AboutFragment : Fragment(R.layout.fragment_about) {

    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var volledigeJaarLijst: List<DagVan> = emptyList()

    /**
     * Wordt aangeroepen direct nadat [onCreateView] is voltooid, maar voordat een eventuele
     * opgeslagen staat is hersteld in de view.
     *
     * Deze functie is verantwoordelijk voor het initialiseren van de view. Het laadt de header-afbeelding
     * met behulp van Coil en stelt een observer in op de `sharedViewModel.jaarLijst`. Zodra de lijst
     * met dagen is ontvangen, wordt de lokale `volledigeJaarLijst` gevuld en wordt `setupTextViews`
     * aangeroepen om de tekstuele inhoud van het scherm te configureren.
     *
     * @param view De View die is teruggegeven door [onCreateView].
     * @param savedInstanceState Als dit niet-null is, wordt dit fragment opnieuw opgebouwd
     * vanuit een eerder opgeslagen staat, zoals hier meegegeven.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val placeholderImageView: ImageView = view.findViewById(R.id.ivPlaceholderPhoto)
        val imageUrl = "https://fijnedagvan.nl/assets/img/pic/fijnedagvancrew.jpg"
        placeholderImageView.load(imageUrl) {
            crossfade(true)
            placeholder(R.mipmap.ic_launcher_round)
        }

        sharedViewModel.jaarLijst.observe(viewLifecycleOwner) { dagen ->
            Log.d("AboutFragment", "Data ontvangen van SharedViewModel: ${dagen.size} items.")
            volledigeJaarLijst = dagen
            setupTextViews()
        }
    }

    private fun setupTextViews() {
        val tvAboutText1: TextView = view?.findViewById(R.id.tvAboutText1) ?: return

        val text1 = "Fijne Dag Van is het grootste Nederlandse overzicht van Dagen Van. In ons overzicht staan nu ${volledigeJaarLijst.size} Dagen zoals Nationale Secretaressedag, de Dag van de Aarde, de Dag van de Buitenlands Gediplomeerde Tandarts of de Dag van de Duits-Nederlandse Rechtspraktijk." +
                "\n\nFijne Dag Van heeft een onafhankelijke redactie die alle Dagen verzamelt, categoriseert, beschrijft en bijhoudt. De redactie organiseert zelf nooit een Dag; alle Dagen die u ziet, bestaan echt en worden door externe organisaties gevierd."

        val internalLinks = mapOf(
            "Nationale Secretaressedag" to "Nationale Secretaressedag",
            "Dag van de Aarde" to "Dag van de Aarde",
            "de Dag van de Buitenlands Gediplomeerde Tandarts" to "Dag van de Buitenlands Gediplomeerde Tandarts",
            "de Dag van de Duits-Nederlandse Rechtspraktijk" to "Dag van de Duits-Nederlandse Rechtspraktijk"
        )
        setupClickableText(tvAboutText1, text1, internalLinks = internalLinks)

        // --- BLOK 2: Algemene Voorwaarden ---
        val tvTermsText: TextView = view?.findViewById(R.id.tvTermsText) ?: return

        // --- WIJZIGING 2: Extra alinea en link toegevoegd ---
        val text2 = "Op deze app, onze website, onze database en de content zijn algemene voorwaarden van toepassing. Als u wil lezen over het (her)gebruik van onze content, kunt u dat lezen op deze pagina." +
                "\n\nDeze app is gemaakt door Fijne Dag Van Media. Voor de agendaweergave(n) in deze app wordt gebruikgemaakt van Kizito Nwoses Calendar-repo voor Android."

        val externalLinks2 = mapOf(
            "kunt u dat lezen op deze pagina" to "https://fijnedagvan.nl/disclaimer/",
            "Kizito Nwoses Calendar" to "https://github.com/kizitonwose/Calendar" // Nieuwe link
        )
        setupClickableText(tvTermsText, text2, externalLinks = externalLinks2)

        // --- BLOK 3: Privacy ---
        val tvPrivacyText: TextView = view?.findViewById(R.id.tvPrivacyText) ?: return
        val text3 = "Deze app verzamelt geen persoonsgegevens en bevat geen tracking of analytische cookies. Onze website en andere diensten doen dat mogelijk wel. U vindt ons privacybeleid hier."
        val externalLinks3 = mapOf(
            "U vindt ons privacybeleid hier" to "https://www.fijnedagvan.nl/privacy/"
        )
        setupClickableText(tvPrivacyText, text3, externalLinks = externalLinks3)
    }

    /**
     * Makes specific parts of a text in a TextView clickable.
     *
     * This function takes a full string and two optional maps: one for internal links (navigating
     * within the app) and one for external links (opening a URL in a browser). It finds the specified
     * link texts within the full text and applies a `ClickableSpan` to them.
     *
     * @param textView The TextView where the clickable text will be displayed.
     * @param fullText The complete string to be displayed in the TextView.
     * @param internalLinks A map where the key is the text to be made clickable and the value is the
     * name of the `DagVan` to navigate to.
     * @param externalLinks A map where the key is the text to be made clickable and the value is the
     * URL to open.
     */
    private fun setupClickableText(textView: TextView, fullText: String, internalLinks: Map<String, String>? = null, externalLinks: Map<String, String>? = null) {
        val spannable = SpannableStringBuilder(fullText)

        internalLinks?.forEach { (linkText, dagNaam) ->
            val dag = volledigeJaarLijst.find { it.naam == dagNaam }
            if (dag != null) {
                val startIndex = fullText.indexOf(linkText)
                if (startIndex != -1) {
                    val endIndex = startIndex + linkText.length
                    spannable.setSpan(object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            navigateToDetail(dag)
                        }
                    }, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }

        externalLinks?.forEach { (linkText, url) ->
            val startIndex = fullText.indexOf(linkText)
            if (startIndex != -1) {
                val endIndex = startIndex + linkText.length
                spannable.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        openUrl(url)
                    }
                }, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        textView.text = spannable
        textView.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("AboutFragment", "Kan URL niet openen: $url")
        }
    }

    /**
     * Navigeert naar het detailscherm voor een specifieke "DagVan".
     *
     * Deze functie maakt gebruik van de Navigation Component om de gebruiker van
     * het "Over Ons"-scherm naar het detailscherm van de geselecteerde dag te sturen.
     *
     * @param dag Het [DagVan] object dat de data bevat voor het detailscherm dat getoond moet worden.
     */
    private fun navigateToDetail(dag: DagVan) {
        val action = AboutFragmentDirections.actionNavAboutToNavDetail(dag)
        findNavController().navigate(action)
    }
}