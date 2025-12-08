/*
Type: UI (Container)

Functie: Het venster waar de app in draait.

Taken: Beheert de navigatiebalk onderin, zorgt dat de juiste Fragments worden getoond, initieert het SharedViewModel en regelt de highlighting van de menuknoppen.
 */


package nl.fijnedagvan.app

import android.Manifest
import android.content.Context // Importeer Context
import android.content.pm.PackageManager
import android.content.res.Configuration // Importeer Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var navController: NavController

    // Instantie van het SharedViewModel
    private val sharedViewModel: SharedViewModel by viewModels()

    private var jaarLijstVoorVerrassing: List<DagVan> = emptyList()

    // Launcher voor het aanvragen van notificatie-permissies
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "Notificatie permissie is verleend.")
        } else {
            Log.d("MainActivity", "Notificatie permissie is geweigerd.")
            Toast.makeText(this, "Notificaties zijn uitgeschakeld.", Toast.LENGTH_LONG).show()
        }
    }

    // --- HIER BEGINT DE TOEGEVOEGDE CODE ---
    override fun attachBaseContext(newBase: Context) {
        // Haal de opgeslagen lettergrootte-schaal op uit SharedPreferences.
        // De default is 1.0f (normale grootte).
        val scale = NotificationPrefsManager.getFontScale(newBase)

        // Maak een nieuwe configuratie aan en stel de lettergrootte-schaal in.
        val config = Configuration(newBase.resources.configuration)
        config.fontScale = scale

        // Pas de nieuwe configuratie toe op de context.
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }
    // --- HIER EINDIGT DE TOEGEVOEGDE CODE ---

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottom_navigation)
        setupNavigation()
        observeViewModel()

        // Vraag om notificatie-permissie bij het opstarten van de app
        askNotificationPermission()
    }

    // ... de rest van uw code blijft ongewijzigd ...

    private fun askNotificationPermission() {
        // Dit is alleen nodig voor Android 13 (API 33) en hoger.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Vraag de permissie aan
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                Log.d("MainActivity", "Notificatie permissie is al verleend.")
            }
        }
    }

    private fun observeViewModel() {
        sharedViewModel.jaarLijst.observe(this) { dagen ->
            Log.d("MainActivity", "Data ontvangen van ViewModel: ${dagen.size} items.")
            jaarLijstVoorVerrassing = dagen
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        bottomNavigation.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.nav_about, R.id.nav_contact, R.id.nav_settings -> {
                    bottomNavigation.menu.findItem(R.id.nav_menu).isChecked = true
                }
                R.id.nav_detail -> {
                    val checkedItem = bottomNavigation.menu.findItem(bottomNavigation.selectedItemId)
                    checkedItem?.isChecked = false
                }
            }
        }

        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home, R.id.nav_overzicht -> {
                    navController.navigate(menuItem.itemId)
                    true
                }
                R.id.nav_verrassing -> {
                    handleVerrassingsdag()
                    false
                }
                R.id.nav_menu -> {
                    navController.navigate(R.id.nav_menu)
                    false
                }
                else -> false
            }
        }
    }

    private fun handleVerrassingsdag() {
        if (jaarLijstVoorVerrassing.isNotEmpty()) {
            val geldigeDagen = jaarLijstVoorVerrassing.filter { it.datumCheck == "1" || it.datumCheck == "1.0" }
            if (geldigeDagen.isNotEmpty()) {
                val verrassingsDag = geldigeDagen.random()
                Log.d("MainActivity", "Verrassingsdag gekozen: ${verrassingsDag.naam}")

                val bundle = Bundle().apply { putParcelable("dagVan", verrassingsDag) }
                navController.navigate(R.id.nav_detail, bundle)

            } else {
                Toast.makeText(this, "Geen geldige verrassingsdagen gevonden.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Data voor verrassing wordt nog geladen, probeer het zo opnieuw.", Toast.LENGTH_SHORT).show()
        }
    }
}
