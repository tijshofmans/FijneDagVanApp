/*
Type: Achtergrondtaak (Worker)

Functie: De "postbode" die elke dag (bijvoorbeeld om 07:30) wakker wordt.

Taken: Controleert of er internet is, haalt de data voor vandaag op, checkt de instellingen, bepaalt de begroeting ("Goedemorgen"), kiest de juiste afbeelding en verstuurt de notificatie.
 */

// In DailyNotificationWorker.kt - VERVANG ALLES MET DEZE CODE

package nl.fijnedagvan.app

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import nl.fijnedagvan.app.NotificationPrefsManager
import nl.fijnedagvan.app.buildAndSendNotification
import nl.fijnedagvan.app.fetchImageBitmap
import nl.fijnedagvan.app.getLidwoord
import nl.fijnedagvan.app.sendDagVanNotification
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class DailyNotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("DailyNotificationWorker", "Worker gestart om notificatie te checken.")

        // 1. Haal de datum van vandaag op
        val todayApiDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)

        // 2. AANGEPAST: Forceer een refresh van ALLE benodigde data
        try {
            Log.d("DailyNotificationWorker", "Start geforceerde data refresh voor datum: $todayApiDate")
            DataCacheManager.refreshDagenForDate(context, todayApiDate)
            DataCacheManager.refreshFunFacts(context) // NIEUW: Ververs ook de fun facts
            Log.d("DailyNotificationWorker", "Data refresh succesvol voltooid.")
        } catch (e: Exception) {
            // Als de refresh mislukt (bv. geen netwerk), probeer het later opnieuw.
            Log.e("DailyNotificationWorker", "Fout tijdens data refresh. Worker wordt opnieuw ingepland.", e)
            return Result.retry()
        }

        // 3. Haal instellingen op
        val isDailyEnabled = NotificationPrefsManager.areNotificationsEnabled(context)
        val isNoDayEnabled = NotificationPrefsManager.areNoDayNotificationsEnabled(context)
        if (!isDailyEnabled && !isNoDayEnabled) {
            Log.d("DailyNotificationWorker", "Alle meldingen staan uit. Taak stopt.")
            rescheduleNextDay()
            return Result.success()
        }

        // 4. Haal data op uit de nu gegarandeerd verse cache
        val dagenVanVandaag = DataCacheManager.getDagenForDate(context, todayApiDate)
            .filter { it.datumCheck == "1" || it.datumCheck == "1.0" }

        // --- DEBUG LOGS ---
        Log.d("DailyNotificationWorker", "--- DEBUG INFO ---")
        Log.d("DailyNotificationWorker", "isDailyEnabled: $isDailyEnabled")
        Log.d("DailyNotificationWorker", "isNoDayEnabled: $isNoDayEnabled")
        Log.d("DailyNotificationWorker", "Aantal gevonden dagen na refresh: ${dagenVanVandaag.size}")
        Log.d("DailyNotificationWorker", "--- EINDE DEBUG INFO ---")

        // 5. Bepaal welke notificatie verstuurd moet worden
        when {
            // CASE 1: Eén 'Dag Van' gevonden
            dagenVanVandaag.size == 1 && isDailyEnabled -> {
                Log.d("DailyNotificationWorker", "CASE 1: Eén dag gevonden, roep sendDagVanNotification aan...")
                val dag = dagenVanVandaag.first()
                sendDagVanNotification(context, dag)
            }

            // CASE 2: Meerdere 'Dagen Van' gevonden
            dagenVanVandaag.size > 1 && isDailyEnabled -> {
                Log.d("DailyNotificationWorker", "CASE 2: Meerdere dagen gevonden, maak samenvatting...")
                val hoofddag = dagenVanVandaag.minByOrNull { it.naam?.length ?: Int.MAX_VALUE }!!
                val andereDagen = dagenVanVandaag.filter { it.dagId != hoofddag.dagId }
                val hoofddagNaam = hoofddag.naam ?: "een speciale dag"

                val titel = "Vandaag is het onder andere ${getLidwoord(hoofddagNaam)}$hoofddagNaam"
                val tekst = "En verder is het ook nog " + andereDagen.joinToString(", ") { it.naam ?: "" }

                val bitmap = fetchImageBitmap(context, hoofddag.imageUrl)
                val pendingIntent = createMainActivityPendingIntent(context)
                buildAndSendNotification(context, titel, tekst, bitmap, pendingIntent, 1)
            }

            // ==================================================================
            // HIER IS DE AANGEPASTE LOGICA
            // ==================================================================
            // CASE 3: Geen 'Dag Van' gevonden, maar 'geen dag' notificaties staan aan
            dagenVanVandaag.isEmpty() && isNoDayEnabled -> {
                Log.d("DailyNotificationWorker", "CASE 3: Geen dagen gevonden, probeer een fun fact te sturen...")

                // NIEUW: Haal een willekeurig feitje op
                val funFact = DataCacheManager.getRandomFunFact(context)

                if (funFact != null && !funFact.feitje.isNullOrBlank()) {
                    // We hebben een geldig feitje gevonden!
                    Log.d("DailyNotificationWorker", "Fun fact gevonden: ${funFact.feitje}")
                    val titel = "Geen Dag Van, wel een leuk feitje:"
                    val tekst = funFact.feitje
                    val pendingIntent = createMainActivityPendingIntent(context)
                    buildAndSendNotification(context, titel, tekst, null, pendingIntent, -1)
                } else {
                    // Fallback voor als er (nog) geen funfacts zijn of er iets misging
                    Log.w("DailyNotificationWorker", "Geen fun fact gevonden, gebruik standaard melding.")
                    val titel = "We vieren vandaag helaas geen Dag Van."
                    val tekst = "Maar u kunt er genoeg vinden in ons overzicht!"
                    val pendingIntent = createMainActivityPendingIntent(context)
                    buildAndSendNotification(context, titel, tekst, null, pendingIntent, -1)
                }
            }
            // ==================================================================
            // EINDE AANGEPASTE LOGICA
            // ==================================================================

            else -> {
                Log.d("DailyNotificationWorker", "CASE ELSE: Geen enkele 'when' conditie is waar. Geen actie.")
            }
        }

        rescheduleNextDay()
        return Result.success()
    }

    private fun rescheduleNextDay() {
        val workManager = WorkManager.getInstance(context)
        val workName = "daily_dagvan_notification"

        val hour = NotificationPrefsManager.getNotificationHour(context)
        val minute = NotificationPrefsManager.getNotificationMinute(context)

        // Zet de tijd voor de volgende notificatie op de volgende dag
        val targetTime = Calendar.getInstance().apply {
            // Controleer eerst of de tijd voor vandaag al voorbij is
            val now = Calendar.getInstance()
            val tempTarget = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
            }
            if (tempTarget.before(now)) {
                // Als de tijd vandaag al geweest is, plan voor morgen
                add(Calendar.DAY_OF_YEAR, 1)
            }
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        val delay = targetTime.timeInMillis - System.currentTimeMillis()

        if (delay > 0) {
            val dailyWorkRequest = OneTimeWorkRequestBuilder<DailyNotificationWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()

            workManager.enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, dailyWorkRequest)
            Log.d("DailyNotificationWorker", "Reschedule voor volgende dag, over ${TimeUnit.MILLISECONDS.toMinutes(delay)} minuten om $hour:$minute")
        } else {
            Log.w("DailyNotificationWorker", "Negatieve of geen delay berekend ($delay ms), volgende taak wordt niet ingepland.")
        }
    }

    private fun createMainActivityPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
