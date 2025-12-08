/*
Type: UI (Scherm)

Functie: De instellingenpagina.

Logica: Toont en wijzigt de instellingen (tijd, thema, algemene meldingen) en toont de lijst met specifieke meldingen (via SettingsAdapter).
 */



package nl.fijnedagvan.app

import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import nl.fijnedagvan.app.databinding.FragmentSettingsBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var settingsAdapter: SettingsAdapter
    private val workName = "daily_dagvan_notification"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupThemeSelector()
        setupFontSizeSelector()
        setupTimePicker()
        setupGeneralSwitches()
        setupRecyclerView()
    }

    private fun setupThemeSelector() {
        fun updateThemeValueText() {
            binding.tvThemeValue.text = when (NotificationPrefsManager.getThemeMode(requireContext())) {
                AppCompatDelegate.MODE_NIGHT_NO -> "Licht"
                AppCompatDelegate.MODE_NIGHT_YES -> "Donker"
                else -> "Systeemstandaard"
            }
        }
        updateThemeValueText()
        binding.themeSelector.setOnClickListener {
            val themes = arrayOf("Licht", "Donker", "Systeemstandaard")
            val themeValues = arrayOf(
                AppCompatDelegate.MODE_NIGHT_NO,
                AppCompatDelegate.MODE_NIGHT_YES,
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            )
            val currentMode = NotificationPrefsManager.getThemeMode(requireContext())
            val checkedItem = themeValues.indexOf(currentMode).coerceAtLeast(0)
            AlertDialog.Builder(requireContext())
                .setTitle("Kies een thema")
                .setSingleChoiceItems(themes, checkedItem) { dialog, which ->
                    val selectedMode = themeValues[which]
                    NotificationPrefsManager.saveThemeMode(requireContext(), selectedMode)
                    AppCompatDelegate.setDefaultNightMode(selectedMode)
                    updateThemeValueText()
                    dialog.dismiss()
                }
                .setNegativeButton("Annuleren", null)
                .show()
        }
    }

    private fun setupFontSizeSelector() {
        fun updateFontSizeValueText() {
            binding.tvFontSizeValue.text = when (NotificationPrefsManager.getFontScale(requireContext())) {
                0.85f -> "Klein"
                1.15f -> "Groot"
                else -> "Normaal"
            }
        }
        updateFontSizeValueText()
        binding.fontSizeSelector.setOnClickListener {
            val fontSizes = arrayOf("Klein", "Normaal", "Groot")
            val fontScaleValues = floatArrayOf(0.85f, 1.0f, 1.15f)
            val currentScale = NotificationPrefsManager.getFontScale(requireContext())
            val checkedItem = fontScaleValues.asList().indexOf(currentScale).coerceAtLeast(0)
            AlertDialog.Builder(requireContext())
                .setTitle("Kies een tekstgrootte")
                .setSingleChoiceItems(fontSizes, checkedItem) { dialog, which ->
                    val selectedScale = fontScaleValues[which]
                    NotificationPrefsManager.saveFontScale(requireContext(), selectedScale)
                    activity?.recreate()
                    dialog.dismiss()
                }
                .setNegativeButton("Annuleren", null)
                .show()
        }
    }

    private fun setupTimePicker() {
        fun updateTimeText() {
            val hour = NotificationPrefsManager.getNotificationHour(requireContext())
            val minute = NotificationPrefsManager.getNotificationMinute(requireContext())
            binding.tvTimeSetting.text = String.format("Dagelijkse melding om %02d:%02d", hour, minute)
        }
        updateTimeText()
        binding.tvTimeSetting.setOnClickListener {
            val currentHour = NotificationPrefsManager.getNotificationHour(requireContext())
            val currentMinute = NotificationPrefsManager.getNotificationMinute(requireContext())
            TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                NotificationPrefsManager.saveNotificationTime(requireContext(), hourOfDay, minute)
                updateTimeText()
                if (NotificationPrefsManager.areNotificationsEnabled(requireContext())) {
                    scheduleDailyWorker()
                }
            }, currentHour, currentMinute, true).show()
        }
    }

    private fun setupGeneralSwitches() {
        binding.switchGeneralNotifications.isChecked = NotificationPrefsManager.areNotificationsEnabled(requireContext())
        binding.switchNoDayNotification.isChecked = NotificationPrefsManager.areNoDayNotificationsEnabled(requireContext())
        binding.switchGeneralNotifications.setOnCheckedChangeListener { _, isChecked ->
            NotificationPrefsManager.saveNotificationsEnabled(requireContext(), isChecked)
            if (isChecked) {
                scheduleDailyWorker()
            } else {
                cancelDailyWorker()
            }
        }
        binding.switchNoDayNotification.setOnCheckedChangeListener { _, isChecked ->
            NotificationPrefsManager.saveNoDayNotificationEnabled(requireContext(), isChecked)
        }
    }

    private fun setupRecyclerView() {
        val subscriptions = NotificationPrefsManager.getIndividualSubscriptions(requireContext())
        settingsAdapter = SettingsAdapter(subscriptions.toMutableList()) { subscription, isEnabled ->
            val dagId = subscription.dagId
            if (isEnabled) {
                val dagToSchedule = DagVan(
                    id = null, dagId = dagId, naam = subscription.dagNaam,
                    datumString = subscription.datumString, korteOmschrijving = null,
                    wanneerOmschrijving = null, uitgebreideInfo = null, intro = null,
                    websiteUrl = null, dagsoort = null, schaal = null,
                    onderwerp = null, datumCheck = null, slug = null
                )
                NotificationPrefsManager.addIndividualSubscription(requireContext(), dagToSchedule)
                IndividualNotificationScheduler.scheduleNotification(requireContext(), dagToSchedule)
            } else {
                NotificationPrefsManager.removeIndividualSubscription(requireContext(), dagId)
                IndividualNotificationScheduler.cancelNotification(requireContext(), dagId)
            }
        }
        binding.rvSpecificNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSpecificNotifications.adapter = settingsAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun scheduleDailyWorker() {
        lifecycleScope.launch {
            val context = requireContext().applicationContext
            val todayApiDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
            val dagenVanVandaag = DataCacheManager.getDagenForDate(context, todayApiDate)
            val dagenJson = Gson().toJson(dagenVanVandaag)
            val inputData = Data.Builder().putString("KEY_DAGEN_JSON", dagenJson).build()
            val workManager = WorkManager.getInstance(context)
            val hour = NotificationPrefsManager.getNotificationHour(context)
            val minute = NotificationPrefsManager.getNotificationMinute(context)

            val targetTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
            }
            if (targetTime.before(Calendar.getInstance())) {
                targetTime.add(Calendar.DAY_OF_YEAR, 1)
            }

            // --- DEZE REGEL IS GECORRIGEERD ---
            val initialDelay = targetTime.timeInMillis - System.currentTimeMillis()

            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val dailyWorkRequest = OneTimeWorkRequestBuilder<DailyNotificationWorker>()
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()
            workManager.enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, dailyWorkRequest)
            Log.d("SettingsFragment", "Dagelijkse worker ingepland/vervangen. Draait over ${TimeUnit.MILLISECONDS.toMinutes(initialDelay)} minuten.")
        }
    }

    private fun cancelDailyWorker() {
        val context = requireContext().applicationContext
        WorkManager.getInstance(context).cancelUniqueWork(workName)
        Log.d("SettingsFragment", "Dagelijkse worker geannuleerd.")
    }
}