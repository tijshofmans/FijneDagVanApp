/*
Type: Application (Startpunt)

Functie: Dit bestand draait één keer zodra de app opstart (of de telefoon herstart).

Taken: Maakt het notificatiekanaal aan (verplicht voor Android) en plant de allereerste dagelijkse notificatie in bij de WorkManager.
 */

package nl.fijnedagvan.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class FijneDagVanApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val themeMode = NotificationPrefsManager.getThemeMode(this)
        AppCompatDelegate.setDefaultNightMode(themeMode)

        createNotificationChannel()
        scheduleInitialDailyNotification()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Dagelijkse Herinneringen"
            val descriptionText = "Kanaal voor de dagelijkse 'Dag Van' melding."
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("DAGVAN_CHANNEL_ID", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun scheduleInitialDailyNotification() {
        if (!NotificationPrefsManager.areNotificationsEnabled(this)) {
            Log.d("FijneDagVanApp", "Notificaties staan uit, er wordt geen taak ingepland.")
            return
        }

        // Gebruik een Coroutine om de netwerk-aanroep te doen
        CoroutineScope(Dispatchers.IO).launch {
            // Haal eerst de data voor vandaag op
            val todayApiDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
            val dagenVanVandaag = DataCacheManager.getDagenForDate(this@FijneDagVanApplication, todayApiDate)

            // Converteer de lijst naar een JSON-string
            val dagenJson = Gson().toJson(dagenVanVandaag)

            // Bouw de input data voor de worker
            val inputData = Data.Builder()
                .putString("KEY_DAGEN_JSON", dagenJson)
                .build()

            // Plan de worker in met de data
            val workManager = WorkManager.getInstance(this@FijneDagVanApplication)
            val workName = "daily_dagvan_notification"
            val hour = NotificationPrefsManager.getNotificationHour(this@FijneDagVanApplication)
            val minute = NotificationPrefsManager.getNotificationMinute(this@FijneDagVanApplication)

            val currentTime = Calendar.getInstance()
            val targetTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
            }

            if (targetTime.before(currentTime)) {
                targetTime.add(Calendar.DAY_OF_YEAR, 1)
            }
            val initialDelay = targetTime.timeInMillis - currentTime.timeInMillis

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val dailyWorkRequest = OneTimeWorkRequestBuilder<DailyNotificationWorker>()
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setInputData(inputData) // Geef de data mee
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniqueWork(workName, ExistingWorkPolicy.KEEP, dailyWorkRequest)
            Log.d("FijneDagVanApp", "Dagelijkse worker ingepland met data.")
        }
    }
}