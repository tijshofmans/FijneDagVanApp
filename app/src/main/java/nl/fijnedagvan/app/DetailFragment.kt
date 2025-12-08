/*
Type: UI (Scherm)

Functie: De informatiepagina van één specifieke dag.

Logica: Toont alle info, regelt de "Delen"-knop, de "Toevoegen aan Agenda"-knop en de schakelaar voor de specifieke notificatie.
 */


package nl.fijnedagvan.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.activityViewModels // BELANGRIJKE NIEUWE IMPORT
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.switchmaterial.SwitchMaterial
import coil.load
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DetailFragment : Fragment(R.layout.fragment_detail) {

    private val args: DetailFragmentArgs by navArgs()

    // Krijg een referentie naar de gedeelde ViewModel
    private val sharedViewModel: SharedViewModel by activityViewModels()

    private val requestCalendarPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("DetailFragment", "Agenda permissie verleend na aanvraag.")
                addEventToCalendar(args.dagVan)
            } else {
                Log.d("DetailFragment", "Agenda permissie geweigerd.")
                Toast.makeText(requireContext(), "Toestemming voor agenda geweigerd.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val dagVan = args.dagVan

        populateViews(view, dagVan)
        setupActionButtons(view, dagVan)
    }

    private fun setupActionButtons(view: View, dagVan: DagVan) {
        val btnAddToCalendar: Button = view.findViewById(R.id.btnAddToCalendar)
        val btnShare: Button = view.findViewById(R.id.btnShare)

        btnAddToCalendar.setOnClickListener { addEventToCalendar(dagVan) }
        btnShare.setOnClickListener { shareDag(dagVan) }
    }

    private fun populateViews(view: View, dagVan: DagVan) {
        val tvNaam: TextView = view.findViewById(R.id.tvDetailNaam)
        val tvDatum: TextView = view.findViewById(R.id.tvDetailDatum)
        val tvDagsoort: TextView = view.findViewById(R.id.tvDetailDagsoort)
        val tvSchaal: TextView = view.findViewById(R.id.tvDetailSchaal)
        val tvOnderwerp: TextView = view.findViewById(R.id.tvDetailOnderwerp)
        val ivAfbeelding: ImageView = view.findViewById(R.id.ivDetailAfbeelding)
        val tvInfo: TextView = view.findViewById(R.id.tvDetailInfo)
        val tvWebsite: TextView = view.findViewById(R.id.tvDetailWebsite)
        val switchIndividualNotification: SwitchMaterial = view.findViewById(R.id.switchIndividualNotification)

        tvNaam.text = dagVan.naam ?: "Onbekende Dag"
        tvDatum.text = formatDisplayDate(dagVan.datumString) ?: ""

        updateMetadataView(tvDagsoort, dagVan.dagsoort)
        updateMetadataView(tvSchaal, dagVan.schaal)
        updateMetadataView(tvOnderwerp, dagVan.onderwerp)

        if (dagVan.imageUrl != null) {
            ivAfbeelding.visibility = View.VISIBLE
            ivAfbeelding.load(dagVan.imageUrl) { crossfade(true) }
        } else {
            ivAfbeelding.visibility = View.GONE
        }

        setupClickableInfoText(tvInfo, dagVan)

        if (!dagVan.websiteUrl.isNullOrEmpty() && dagVan.websiteUrl.startsWith("http")) {
            tvWebsite.visibility = View.VISIBLE
            tvWebsite.setOnClickListener { openUrl(dagVan.websiteUrl) }
        } else {
            tvWebsite.visibility = View.GONE
        }

        setupIndividualNotificationSwitch(switchIndividualNotification, dagVan)
    }

    private fun setupClickableInfoText(textView: TextView, dagVan: DagVan) {
        val infoText = dagVan.uitgebreideInfo
        if (infoText.isNullOrEmpty()) {
            textView.text = "Geen extra informatie beschikbaar."
            return
        }

        val displayDate = formatDisplayDate(dagVan.datumString) ?: ""
        val processedHtml = infoText.replace("\$datum", displayDate, ignoreCase = true)
            .replace("\r\n\r\n", "<br/><br/>")
            .replace("<br />", "<br/><br/>", ignoreCase = true)

        val sequence: Spanned = HtmlCompat.fromHtml(processedHtml, HtmlCompat.FROM_HTML_MODE_COMPACT)
        val spannable = SpannableStringBuilder(sequence)
        val spans = spannable.getSpans(0, sequence.length, android.text.style.URLSpan::class.java).toList()

        for (span in spans) {
            val start = spannable.getSpanStart(span)
            val end = spannable.getSpanEnd(span)
            val flags = spannable.getSpanFlags(span)
            val url = span.url ?: continue

            if (url.contains("fijnedagvan.nl/info")) {
                spannable.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        handleInternalLink(url)
                    }
                }, start, end, flags)
                spannable.removeSpan(span)
            }
        }
        textView.text = spannable
        textView.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun handleInternalLink(url: String) {
        val id = Uri.parse(url).getQueryParameter("id")
        if (id.isNullOrBlank()) {
            openUrl(url)
            return
        }

        // Gebruik de data uit de SharedViewModel om de juiste Dag te vinden
        val dagData = sharedViewModel.jaarLijst.value?.find { it.dagId == id }
        if (dagData != null) {
            val action = DetailFragmentDirections.actionNavDetailToSelf(dagData)
            findNavController().navigate(action)
        } else {
            Toast.makeText(context, "Kon de link niet openen.", Toast.LENGTH_SHORT).show()
            Log.e("DetailFragment", "Dag met ID $id niet gevonden in de cache.")
        }
    }

    // De functie fetchDataForId is nu VERWIJDERD, omdat we de data uit het ViewModel halen.

    private fun openUrl(url: String?) {
        if (url.isNullOrBlank()) return
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(browserIntent)
        } catch (e: Exception) {
            Log.e("DetailFragment", "Kon website niet openen: $url")
        }
    }

    private fun setupIndividualNotificationSwitch(switch: SwitchMaterial, dagVan: DagVan) {
        val dagId = dagVan.dagId ?: ""
        switch.isChecked = NotificationPrefsManager.isIndividualNotificationEnabled(requireContext(), dagId)
        switch.setOnCheckedChangeListener { _, isChecked ->
            if (dagId.isBlank()) return@setOnCheckedChangeListener
            if (isChecked) {
                NotificationPrefsManager.addIndividualSubscription(requireContext(), dagVan)
                IndividualNotificationScheduler.scheduleNotification(requireContext(), dagVan)
            } else {
                NotificationPrefsManager.removeIndividualSubscription(requireContext(), dagId)
                IndividualNotificationScheduler.cancelNotification(requireContext(), dagId)
            }
        }
    }

    private fun addEventToCalendar(dagVan: DagVan) {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_CALENDAR
            ) == PackageManager.PERMISSION_GRANTED -> {
                createCalendarIntent(dagVan)
            }
            else -> {
                requestCalendarPermissionLauncher.launch(Manifest.permission.WRITE_CALENDAR)
            }
        }
    }

    private fun createCalendarIntent(dagVan: DagVan) {
        val startTime = parseApiDateToMillis(dagVan.datumString)
        if (startTime == null) {
            Toast.makeText(requireContext(), "Kon de datum niet verwerken.", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, dagVan.naam)
            putExtra(CalendarContract.Events.DESCRIPTION, HtmlCompat.fromHtml(dagVan.uitgebreideInfo ?: "", HtmlCompat.FROM_HTML_MODE_COMPACT).toString())
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime)
            putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
        }

        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(requireContext(), "Geen agenda-app gevonden.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun parseApiDateToMillis(apiDate: String?): Long? {
        if (apiDate == null) return null
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            format.parse(apiDate)?.time
        } catch (e: Exception) { null }
    }

    private fun updateMetadataView(textView: TextView, data: String?) {
        if (!data.isNullOrEmpty()) {
            textView.text = data.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            textView.visibility = View.VISIBLE
        } else {
            textView.visibility = View.GONE
        }
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

    private fun shareDag(dagVan: DagVan) {
        val vandaagApiString = getCurrentApiDate()
        val isVandaag = (dagVan.datumString == vandaagApiString)
        val shareText = buildShareText(dagVan, isVandaag)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(shareIntent, "Deel deze Dag via..."))
    }

    private fun buildShareText(dag: DagVan, isToday: Boolean): String {
        val link = if (!dag.slug.isNullOrBlank()) "https://fijnedagvan.nl/info/${dag.slug}" else "https://fijnedagvan.nl"
        val dagNaam = dag.naam ?: "een speciale dag"
        val lidwoord = if (dagNaam.startsWith("Internationale", ignoreCase = true) || dagNaam.startsWith("Dag", ignoreCase = true)) "de " else ""
        return if (isToday) "Hoi! Ik wens je een hele fijne $lidwoord$dagNaam toe! Daar heb ik over gelezen op $link"
        else {
            val displayDate = formatDisplayDate(dag.datumString) ?: "een andere datum"
            "Wist je dat het op $displayDate $lidwoord$dagNaam is? Dat heb ik gelezen op $link"
        }
    }

    private fun getCurrentApiDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Calendar.getInstance().time)
    }
}