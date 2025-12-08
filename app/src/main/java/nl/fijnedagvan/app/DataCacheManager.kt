/*
Type: Helper (Data Opslag)

Functie: De "kluis". Slaat de JSON-data van het hele jaar lokaal op de telefoon op.

Doel: Zorgt ervoor dat de app bij een tweede keer opstarten niet opnieuw alles van internet hoeft te halen, waardoor de app direct laadt.
 */

package nl.fijnedagvan.app

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit


object DataCacheManager {

    private val client = OkHttpClient()
    private const val BASE_URL = "https://fijnedagvan.nl/jsonscript.php"
    private val API_KEY = BuildConfig.API_KEY

    private val CACHE_DURATION_24_HOURS = TimeUnit.HOURS.toMillis(24)
    private val CACHE_DURATION_4_HOURS = TimeUnit.HOURS.toMillis(4)

    private const val DEBUG_TAG = "DATA_DEBUG"

    // [BESTAANDE FUNCTIES BLIJVEN HETZELFDE]
    suspend fun getAllDagenFromCache(context: Context): List<DagVan> {
        val cacheKey = "all_dagen.json"
        val cachedJson = getData(context, cacheKey, CACHE_DURATION_24_HOURS)
        if (cachedJson != null) {
            Log.d(DEBUG_TAG, "Volledige data geladen uit cache ($cacheKey).")
            return parseDagenJson(cachedJson)
        }

        Log.d(DEBUG_TAG, "Geen volledige cache, data wordt opgehaald van netwerk...")
        val networkJson = fetchDataFromApi("")
        if (networkJson != null) {
            saveData(context, cacheKey, networkJson)
            return parseDagenJson(networkJson)
        }
        return emptyList()
    }

    suspend fun getDagenForYear(context: Context, year: String): List<DagVan> {
        val cacheKey = "year_$year.json"
        val cachedJson = getData(context, cacheKey, CACHE_DURATION_24_HOURS)
        if (cachedJson != null) {
            Log.d(DEBUG_TAG, "Jaar-data geladen uit cache ($cacheKey).")
            return parseDagenJson(cachedJson)
        }

        Log.d(DEBUG_TAG, "Geen cache voor jaar $year, data wordt opgehaald van netwerk...")
        val networkJson = fetchDataFromApi("?year=$year")
        if (networkJson != null) {
            saveData(context, cacheKey, networkJson)
            return parseDagenJson(networkJson)
        }
        return emptyList()
    }

    suspend fun getDagenForDate(context: Context, date: String): List<DagVan> {
        val cacheKey = "date_$date.json"
        val cachedJson = getData(context, cacheKey, CACHE_DURATION_4_HOURS)
        if (cachedJson != null) {
            return parseDagenJson(cachedJson)
        }

        Log.d(DEBUG_TAG, "Geen cache voor datum $date, data wordt opgehaald van netwerk...")
        val networkJson = fetchDataFromApi("?date=$date")
        if (networkJson != null) {
            saveData(context, cacheKey, networkJson)
            return parseDagenJson(networkJson)
        }
        return emptyList()
    }

    // --- AANGEPAST: FUNCTIE VOOR GEFORCEERDE REFRESH ---
    // Deze functie probeert de cache te verversen. Als het mislukt, gooit het een exceptie.
    suspend fun refreshDagenForDate(context: Context, date: String) {
        val cacheKey = "date_$date.json"
        Log.d(DEBUG_TAG, "Geforceerde refresh voor datum $date gestart...")

        val networkJson = fetchDataFromApi("?date=$date")
            ?: throw IOException("Failed to fetch data from API for date: $date")

        saveData(context, cacheKey, networkJson)
        Log.d(DEBUG_TAG, "Cache voor datum $date succesvol ververst.")
    }

    // --- NIEUW: FALLBACK FUNCTIE VOOR DE WORKER ---
    // Deze functie haalt data uit de cache, ONGEACHT hoe oud het is.
    // Dit is de 'plan B' als de refresh mislukt.
    fun getDagenFromCacheForDate(context: Context, date: String): List<DagVan> {
        val cacheKey = "date_$date.json"
        Log.d(DEBUG_TAG, "Fallback: Oude cache wordt ingelezen voor datum $date...")
        try {
            val cacheFile = File(context.cacheDir, cacheKey)
            if (cacheFile.exists()) {
                val cachedJson = cacheFile.readText()
                if (cachedJson.isNotBlank()) {
                    Log.d(DEBUG_TAG, "Fallback succesvol: Oude cache gevonden en geparsed voor $date.")
                    return parseDagenJson(cachedJson)
                }
            }
        } catch (e: Exception) {
            Log.e(DEBUG_TAG, "Fallback mislukt: Fout bij lezen van oude cache voor $date", e)
        }
        Log.w(DEBUG_TAG, "Fallback mislukt: Geen bruikbare oude cache gevonden voor $date.")
        return emptyList()
    }


    private suspend fun fetchDataFromApi(queryParams: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$BASE_URL$queryParams"
                Log.d(DEBUG_TAG, "Aanvraag naar URL: $url")
                val request = Request.Builder().url(url).addHeader("X-API-KEY", API_KEY).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(DEBUG_TAG, "API Fout: ${response.code} voor url: $url")
                        return@withContext null
                    }
                    val responseBody = response.body?.string()
                    Log.d(DEBUG_TAG, "RUWE SERVER RESPONSE: $responseBody")
                    responseBody
                }
            } catch (e: Exception) {
                Log.e(DEBUG_TAG, "Netwerk Fout bij ophalen data van API: ${e.message}")
                null
            }
        }
    }

    fun saveData(context: Context, cacheKey: String, jsonData: String) {
        try {
            val cacheFile = File(context.cacheDir, cacheKey)
            cacheFile.writeText(jsonData)
        } catch (e: Exception) {
            Log.e(DEBUG_TAG, "Fout bij opslaan van data in cache voor key: $cacheKey", e)
        }
    }

    fun getData(context: Context, cacheKey: String, maxAgeInMillis: Long): String? {
        try {
            val cacheFile = File(context.cacheDir, cacheKey)
            if (!cacheFile.exists()) return null
            val cacheAge = System.currentTimeMillis() - cacheFile.lastModified()
            if (cacheAge > maxAgeInMillis) {
                cacheFile.delete()
                return null
            }
            return cacheFile.readText()
        } catch (e: Exception) {
            Log.e(DEBUG_TAG, "Fout bij lezen van data uit cache voor key: $cacheKey", e)
            return null
        }
    }

    private fun parseDagenJson(jsonString: String): List<DagVan> {
        if (jsonString.isBlank()) {
            Log.w(DEBUG_TAG, "Parser: JSON string is leeg, kan niet parsen.")
            return emptyList()
        }
        val gson = Gson()

        try {
            val type = object : TypeToken<List<DagVan>>() {}.type
            val simpleList: List<DagVan> = gson.fromJson(jsonString, type)
            if (simpleList.isNotEmpty() && simpleList.first().dagId != null) {
                Log.d(DEBUG_TAG, "Parser: Succesvol geparsed als SIMPELE lijst. ${simpleList.size} dagen gevonden.")
                return simpleList
            }
        } catch (e: Exception) {
            // Ga door
        }

        try {
            val rootElement = JsonParser.parseString(jsonString)
            if (rootElement.isJsonArray) {
                val rootArray = rootElement.asJsonArray
                if (rootArray.size() > 2) {
                    val dagenTableElement = rootArray.get(2)
                    val dagenTable = gson.fromJson(dagenTableElement, DagenTable::class.java)
                    val result = dagenTable?.dagenLijst ?: emptyList()
                    Log.d(DEBUG_TAG, "Parser: Succesvol geparsed als COMPLEXE structuur. ${result.size} dagen gevonden.")
                    return result
                }
            }
        } catch (e: Exception) {
            Log.e(DEBUG_TAG, "Parser: Fout bij parsen van complexe structuur", e)
        }

        Log.e(DEBUG_TAG, "Parser: Beide parsing pogingen zijn mislukt.")
        return emptyList()
    }
}
