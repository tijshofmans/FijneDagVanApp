package nl.fijnedagvan.app

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val scale = NotificationPrefsManager.getFontScale(newBase)
        // --- DEZE LOG-REGEL IS NIEUW ---
        Log.d("BaseActivity_DEBUG", "attachBaseContext wordt uitgevoerd met fontScale: $scale")

        val newConfig = Configuration(newBase.resources.configuration)
        newConfig.fontScale = scale

        val newContext = newBase.createConfigurationContext(newConfig)

        super.attachBaseContext(newContext)
    }
}