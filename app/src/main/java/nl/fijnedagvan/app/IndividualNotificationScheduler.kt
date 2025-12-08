/*
Type: Helper (Planner)

Functie: Een hulpje dat de SpecificDayNotificationWorker inplant.

Logica: Berekent precies hoeveel milliseconden het nog duurt tot de datum van die specifieke dag (op het ingestelde tijdstip) en geeft die opdracht aan Android.
 */

package nl.fijnedagvan.app

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

object IndividualNotificationScheduler {

    fun scheduleNotification(context: Context, dag: DagVan) {
        if (dag.dagId.isNullOrEmpty()) {
            Log.e("NotificationScheduler", "Kan melding niet plannen: dagId is leeg.")
            return
        }

        val workManager = WorkManager.getInstance(context)
        val workName = "specific_day_${dag.dagId}"

        val hour = NotificationPrefsManager.getNotificationHour(context)
        val minute = NotificationPrefsManager.getNotificationMinute(context)

        val dagDatum = parseApiDate(dag.datumString)
        if (dagDatum == null) {
            Log.e("NotificationScheduler", "Kan melding niet plannen: ongeldige datumstring ${dag.datumString}")
            return
        }

        val targetTime = Calendar.getInstance().apply {
            time = dagDatum
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        val currentTime = Calendar.getInstance()
        while (targetTime.before(currentTime)) {
            targetTime.add(Calendar.YEAR, 1)
        }

        val initialDelay = targetTime.timeInMillis - currentTime.timeInMillis

        val dagVanJson = Gson().toJson(dag)

        // DEZE LOG IS CRUCIAAL
        Log.d("NotificationScheduler", "Data wordt verstuurd naar worker: $dagVanJson")

        val inputData = Data.Builder()
            .putString(Constants.KEY_DAG_VAN_JSON, dagVanJson)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<SpecificDayNotificationWorker>()
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        workManager.enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, workRequest)
        Log.d("NotificationScheduler", "Specifieke melding voor '${dag.naam}' ingepland op ${targetTime.time}.")
    }

    fun cancelNotification(context: Context, dagId: String?) {
        if (dagId.isNullOrEmpty()) return
        val workManager = WorkManager.getInstance(context)
        val workName = "specific_day_$dagId"
        workManager.cancelUniqueWork(workName)
        Log.d("NotificationScheduler", "Specifieke melding voor dagId $dagId geannuleerd.")
    }

    private fun parseApiDate(apiDate: String?): java.util.Date? {
        if (apiDate == null) return null
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(apiDate)
        } catch (e: Exception) {
            null
        }
    }
}