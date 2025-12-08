/*
Type: Achtergrondtaak (Worker)

Functie: De "postbode" die elke dag (bijvoorbeeld om 07:30) wakker wordt.

Taken: Controleert of er internet is, haalt de data voor vandaag op, checkt de instellingen, bepaalt de begroeting ("Goedemorgen"), kiest de juiste afbeelding en verstuurt de notificatie.
 */

package nl.fijnedagvan.app

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.app.PendingIntent
import android.content.Intent

class DailyNotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("DailyNotificationWorker", "Worker gestart om notificatie te checken.")

        // 1. Haal de datum van vandaag op
        val todayApiDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)

        // 2. NIEUW: Forceer een refresh van de data voor vandaag
        try {
            Log.d("DailyNotificationWorker", "Start geforceerde data refresh voor datum: $todayApiDate")
            // Deze functie moet je nog aanmaken in DataCacheManager
            DataCacheManager.refreshDagenForDate(context, todayApiDate)
            Log.d("DailyNotificationWorker", "Data refresh succesvol voltooid.")
        } catch (e: Exception) {
            // Als de refresh mislukt (bv. geen netwerk), probeer het later opnieuw.
            // Dit voorkomt het sturen van een foute notificatie op basis van verouderde/lege data.
            Log.e("DailyNotificationWorker", "Fout tijdens geforceerde data refresh. Worker wordt opnieuw ingepland.", e)
            return Result.retry()
        }

        // 3. AANGEPAST: Haal instellingen op (nu na de data refresh)
        val isDailyEnabled = NotificationPrefsManager.areNotificationsEnabled(context)
        val isNoDayEnabled = NotificationPrefsManager.areNoDayNotificationsEnabled(context)
        if (!isDailyEnabled && !isNoDayEnabled) {
            Log.d("DailyNotificationWorker", "Alle meldingen staan uit. Taak stopt.")
            rescheduleNextDay()
            return Result.success()
        }

        // 4. AANGEPAST: Haal data op uit de nu gegarandeerd verse cache
        val dagenVanVandaag = DataCacheManager.getDagenForDate(context, todayApiDate)
            .filter { it.datumCheck == "1" || it.datumCheck == "1.0" }

        // --- DEBUG LOGS (deze zijn nog steeds nuttig) ---
        Log.d("DailyNotificationWorker", "--- DEBUG INFO ---")
        Log.d("DailyNotificationWorker", "isDailyEnabled: $isDailyEnabled")
        Log.d("DailyNotificationWorker", "isNoDayEnabled: $isNoDayEnabled")
        Log.d("DailyNotificationWorker", "Aantal gevonden dagen na refresh: ${dagenVanVandaag.size}")
        Log.d("DailyNotificationWorker", "--- EINDE DEBUG INFO ---")

        // 5. Bepaal welke notificatie verstuurd moet worden (DEZE LOGICA BLIJFT HETZELFDE)
        when {
            dagenVanVandaag.size == 1 && isDailyEnabled -> {
                Log.d("DailyNotificationWorker", "CASE 1: Eén dag gevonden, roep sendDagVanNotification aan...")
                val dag = dagenVanVandaag.first()
                sendDagVanNotification(context, dag)
            }
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
            dagenVanVandaag.isEmpty() && isNoDayEnabled -> {
                Log.d("DailyNotificationWorker", "CASE 3: Geen dagen gevonden, stuur 'geen dag' melding...")
                val titel = "We vieren vandaag helaas geen Dag Van."
                val tekst = "Maar u kunt er genoeg vinden in ons overzicht!"
                val pendingIntent = createMainActivityPendingIntent(context)
                buildAndSendNotification(context, titel, tekst, null, pendingIntent, -1)
            }
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

        val targetTime = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        val delay = targetTime.timeInMillis - System.currentTimeMillis()

        val dailyWorkRequest = OneTimeWorkRequestBuilder<DailyNotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, dailyWorkRequest)
        Log.d("DailyNotificationWorker", "Reschedule voor volgende dag, over ${TimeUnit.MILLISECONDS.toMinutes(delay)} minuten")
    }

    private fun createMainActivityPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }
}