/*
Type: ViewModel (Het Brein)

Functie: De centrale hub voor data.

Werking: Haalt data op (van cache of server) en houdt deze vast in het geheugen. Alle schermen (Home, Overzicht, Settings) vragen data aan dit bestand in plaats van zelf het internet op te gaan.
 */


package nl.fijnedagvan.app

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.time.YearMonth

class SharedViewModel(application: Application) : AndroidViewModel(application) {

    private val _jaarLijst = MutableLiveData<List<DagVan>>()
    val jaarLijst: LiveData<List<DagVan>> get() = _jaarLijst

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _allDagsoorten = MutableLiveData<List<String>>()
    val allDagsoorten: LiveData<List<String>> get() = _allDagsoorten

    private val _allSchalen = MutableLiveData<List<String>>()
    val allSchalen: LiveData<List<String>> get() = _allSchalen

    private val _allOnderwerpen = MutableLiveData<List<String>>()
    val allOnderwerpen: LiveData<List<String>> get() = _allOnderwerpen

    init {
        fetchDataForYear(YearMonth.now().year)
    }

    fun fetchDataForYear(year: Int) {
        _isLoading.value = true
        Log.d("SharedViewModel", "Ophalen data voor jaar $year...")

        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val dagen = DataCacheManager.getDagenForYear(context, year.toString())

            _jaarLijst.postValue(dagen)
            Log.d("SharedViewModel", "Data voor jaar $year verwerkt: ${dagen.size} items.")

            if (dagen.isNotEmpty()) {
                Log.d("SharedViewModel", "Bouwen van filterlijsten op basis van data voor jaar $year.")

                // --- GECORRIGEERD: .lowercase() toegevoegd ---
                _allDagsoorten.postValue(
                    dagen.mapNotNull { it.dagsoort }
                        .flatMap { it.split(',') }
                        .map { it.trim().lowercase() } // Converteer naar kleine letters
                        .filter { it.isNotEmpty() }
                        .distinct()
                        .sorted()
                )
                _allSchalen.postValue(
                    dagen.mapNotNull { it.schaal }
                        .flatMap { it.split(',') }
                        .map { it.trim().lowercase() } // Converteer naar kleine letters
                        .filter { it.isNotEmpty() }
                        .distinct()
                        .sorted()
                )
                _allOnderwerpen.postValue(
                    dagen.mapNotNull { it.onderwerp }
                        .flatMap { it.split(',') }
                        .map { it.trim().lowercase() } // Converteer naar kleine letters
                        .filter { it.isNotEmpty() }
                        .distinct()
                        .sorted()
                )
            } else {
                _allDagsoorten.postValue(emptyList())
                _allSchalen.postValue(emptyList())
                _allOnderwerpen.postValue(emptyList())
            }
            _isLoading.postValue(false)
        }
    }
}