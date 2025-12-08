package nl.fijnedagvan.app

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object DataRepository {

    private val client = OkHttpClient()
    private const val BASE_URL = "https://fijnedagvan.nl/jsonscript.php"
    private val API_KEY = BuildConfig.API_KEY

    // Cache-duur: 24 uur in milliseconden
    private val CACHE_DURATION_24_HOURS = TimeUnit.HOURS.toMillis(24)

    /**
     * Haalt de lijst van Dagen voor een specifiek jaar op.
     * Kijkt eerst in de cache, en haalt anders data op van het netwerk.
     */
    suspend fun getDagenForYear(context: Context, year: String): List<DagVan> {
        val cacheKey = "year_$year.json"

        // 1. Probeer data uit de cache te halen
        val cachedJson = DataCacheManager.getData(context, cacheKey, CACHE_DURATION_24_HOURS)
        if (cachedJson != null) {
            return parseDagenJson(cachedJson) // Gebruikt jouw functie uit JsonUtils.kt
        }

        // 2. Geen geldige cache, haal op van het netwerk
        Log.d("DataRepository", "Geen cache voor jaar $year, data wordt opgehaald van netwerk...")
        val networkJson = fetchDataFromApi("?year=$year")
        if (networkJson != null) {
            // Sla de nieuwe data op in de cache voor de volgende keer
            DataCacheManager.saveData(context, cacheKey, networkJson)
            return parseDagenJson(networkJson) // Gebruikt jouw functie uit JsonUtils.kt
        }

        // 3. Fout bij netwerk, geef lege lijst terug
        return emptyList()
    }

    /**
     * Haalt de lijst van Dagen voor een specifieke DATUM op.
     * Deze kunnen we straks in de DailyNotificationWorker gebruiken.
     */
    suspend fun getDagenForDate(context: Context, date: String): List<DagVan> {
        val cacheKey = "date_$date.json"

        // 1. Probeer data uit de cache te halen (data voor een specifieke dag is korter geldig)
        val cachedJson = DataCacheManager.getData(context, cacheKey, TimeUnit.HOURS.toMillis(4))
        if (cachedJson != null) {
            return parseDagenJson(cachedJson)
        }

        // 2. Geen geldige cache, haal op van het netwerk
        Log.d("DataRepository", "Geen cache voor datum $date, data wordt opgehaald van netwerk...")
        val networkJson = fetchDataFromApi("?date=$date")
        if (networkJson != null) {
            DataCacheManager.saveData(context, cacheKey, networkJson)
            return parseDagenJson(networkJson)
        }

        return emptyList()
    }

    /**
     * De daadwerkelijke netwerk-aanroep.
     */
    private suspend fun fetchDataFromApi(queryParams: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$BASE_URL$queryParams"
                val request = Request.Builder().url(url).addHeader("X-API-KEY", API_KEY).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    response.body?.string()
                }
            } catch (e: Exception) {
                Log.e("DataRepository", "Fout bij ophalen data van API: ${e.message}")
                null
            }
        }
    }
}