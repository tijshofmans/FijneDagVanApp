/*
Type: Achtergrondtaak (Worker)

Functie: De "speciale koerier". Wordt eenmalig wakker op een specifieke datum.

Taken: Haalt de gegevens van die ene dag op (uit de data die hij meekreeg bij het inplannen) en stuurt een notificatie specifiek voor die dag.
 */



package nl.fijnedagvan.app

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson

class SpecificDayNotificationWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_DAG_VAN_JSON = "dag_van_json_key"
    }

    override suspend fun doWork(): Result {
        Log.d("SpecificDayWorker", "Worker voor specifieke dag is gestart.")

        // 1. Haal de meegegeven JSON-data op
        val dagVanJson = inputData.getString(KEY_DAG_VAN_JSON)
        if (dagVanJson.isNullOrBlank()) {
            Log.e("SpecificDayWorker", "Worker mislukt: JSON-data ontbreekt.")
            return Result.failure()
        }

        // 2. Parse de JSON-data naar een DagVan object
        val dag: DagVan = try {
            Gson().fromJson(dagVanJson, DagVan::class.java)
        } catch (e: Exception) {
            Log.e("SpecificDayWorker", "Worker mislukt: Fout bij parsen van JSON.", e)
            return Result.failure()
        }
        Log.d("SpecificDayWorker", "Data ontvangen voor: ${dag.naam}")


        // 3. Roep de centrale hulpfunctie aan om de notificatie te versturen
        // Deze functie staat nu in je JsonUtils.kt bestand.
        sendDagVanNotification(appContext, dag)

        return Result.success()
    }
}