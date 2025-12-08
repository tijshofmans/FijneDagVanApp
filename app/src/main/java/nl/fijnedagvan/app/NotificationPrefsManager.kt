/*
Type: Helper (Instellingen Opslag)

Functie: Beheert alle voorkeuren van de gebruiker.

Wat het opslaat: Wel/geen meldingen, het gekozen tijdstip, de thema-keuze (licht/donker), en de lijst met ID's van specifieke dagen die gevolgd worden.
 */




package nl.fijnedagvan.app

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.util.Log

object NotificationPrefsManager {

    private const val PREFS_NAME = "notification_prefs"
    private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    private const val KEY_NO_DAY_NOTIFICATION_ENABLED = "no_day_notification_enabled"
    private const val KEY_HOUR = "notification_hour"
    private const val KEY_MINUTE = "notification_minute"
    private const val KEY_INDIVIDUAL_SUBSCRIPTIONS = "individual_subscriptions_v2"
    private const val KEY_THEME_MODE = "theme_mode"
    // NIEUWE SLEUTEL VOOR LETTERGROOTTE
    private const val KEY_FONT_SCALE = "font_scale"


    // Data class om een abonnement vast te leggen
    data class Subscription(val dagId: String, val dagNaam: String, val datumString: String)

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // --- Functies voor Thema-instellingen ---
    fun saveThemeMode(context: Context, mode: Int) {
        getPrefs(context).edit().putInt(KEY_THEME_MODE, mode).apply()
    }

    fun getThemeMode(context: Context): Int {
        return getPrefs(context).getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    // --- Functies voor de andere notificaties ---
    fun saveNotificationsEnabled(context: Context, isEnabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, isEnabled).apply()
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun saveNoDayNotificationEnabled(context: Context, isEnabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_NO_DAY_NOTIFICATION_ENABLED, isEnabled).apply()
    }

    fun areNoDayNotificationsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_NO_DAY_NOTIFICATION_ENABLED, false)
    }

    fun saveNotificationTime(context: Context, hour: Int, minute: Int) {
        getPrefs(context).edit().putInt(KEY_HOUR, hour).putInt(KEY_MINUTE, minute).apply()
    }

    fun getNotificationHour(context: Context): Int {
        return getPrefs(context).getInt(KEY_HOUR, 7)
    }

    fun getNotificationMinute(context: Context): Int {
        return getPrefs(context).getInt(KEY_MINUTE, 30)
    }

    fun getIndividualSubscriptions(context: Context): List<Subscription> {
        val json = getPrefs(context).getString(KEY_INDIVIDUAL_SUBSCRIPTIONS, null) ?: return emptyList()
        val type = object : TypeToken<List<Subscription>>() {}.type
        return Gson().fromJson(json, type)
    }

    private fun saveIndividualSubscriptions(context: Context, subscriptions: List<Subscription>) {
        val json = Gson().toJson(subscriptions)
        getPrefs(context).edit().putString(KEY_INDIVIDUAL_SUBSCRIPTIONS, json).apply()
    }

    fun addIndividualSubscription(context: Context, dag: DagVan) {
        if (dag.dagId.isNullOrBlank() || dag.naam.isNullOrBlank() || dag.datumString.isNullOrBlank()) {
            return
        }
        val currentSubs = getIndividualSubscriptions(context).toMutableList()
        if (currentSubs.none { it.dagId == dag.dagId }) {
            currentSubs.add(Subscription(dag.dagId, dag.naam, dag.datumString))
            saveIndividualSubscriptions(context, currentSubs)
        }
    }
    fun removeIndividualSubscription(context: Context, dagId: String) {
        val currentSubs = getIndividualSubscriptions(context).toMutableList()
        currentSubs.removeAll { it.dagId == dagId }
        saveIndividualSubscriptions(context, currentSubs)
    }

    fun isIndividualNotificationEnabled(context: Context, dagId: String): Boolean {
        return getIndividualSubscriptions(context).any { it.dagId == dagId }
    }

    // --- Functies voor Lettergrootte ---
    fun saveFontScale(context: Context, scale: Float) {
        getPrefs(context).edit().putFloat(KEY_FONT_SCALE, scale).apply()
    }

    fun getFontScale(context: Context): Float {
        // Standaard is 1.0f (normale grootte)
        return getPrefs(context).getFloat(KEY_FONT_SCALE, 1.0f)
    }
}