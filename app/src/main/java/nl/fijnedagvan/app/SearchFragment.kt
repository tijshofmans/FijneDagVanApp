/*
Type: UI (Scherm)

Functie: De zoekpagina.

Logica: Stuurt zoekopdrachten naar de API en toont de resultaten in een lijst.
 */



package nl.fijnedagvan.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class SearchFragment : Fragment() {

    // --- Variabelen ---
    private val client = OkHttpClient()
    private val baseUrl = "https://fijnedagvan.nl/jsonscript.php"
    private val API_KEY = BuildConfig.API_KEY

    // --- UI Elementen en Adapter ---
    private lateinit var etSearchQuery: TextInputEditText
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var tvNoResults: TextView
    private lateinit var searchAdapter: DagVanAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate de layout voor dit fragment
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Koppel de UI elementen
        etSearchQuery = view.findViewById(R.id.etSearchQuery)
        rvSearchResults = view.findViewById(R.id.rvSearchResults)
        tvNoResults = view.findViewById(R.id.tvNoResults)

        setupRecyclerView()
        setupSearchListener()
    }

    private fun setupRecyclerView() {
        // We hergebruiken de DagVanAdapter met de VOLLEDIGE weergave
        searchAdapter = DagVanAdapter(emptyList(), DisplayMode.FULL) { geklikteDag ->
            navigateToDetail(geklikteDag)
        }
        rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        rvSearchResults.adapter = searchAdapter
    }

    private fun setupSearchListener() {
        etSearchQuery.setOnEditorActionListener { textView, actionId, _ ->
            // Controleer of de gebruiker op de "Search" knop van het toetsenbord drukt
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val searchQuery = textView.text.toString().trim()
                if (searchQuery.isNotEmpty()) {
                    // Voer de zoekopdracht uit
                    performSearch(searchQuery)
                    // Verberg het toetsenbord
                    hideKeyboard()
                }
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
    }

    private fun performSearch(query: String) {
        Log.d("SearchFragment", "Zoeken naar: $query")
        // Toon een (nog te implementeren) laad-indicator
        tvNoResults.visibility = View.GONE
        rvSearchResults.visibility = View.GONE

        CoroutineScope(Dispatchers.Main).launch {
            val searchResults = fetchDataForSearch(query)
            Log.d("SearchFragment", "${searchResults.size} resultaten gevonden voor '$query'")

            if (searchResults.isNotEmpty()) {
                rvSearchResults.visibility = View.VISIBLE
                searchAdapter.updateDagen(searchResults)
            } else {
                tvNoResults.visibility = View.VISIBLE
            }
        }
    }

    private suspend fun fetchDataForSearch(query: String): List<DagVan> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$baseUrl?search=$query"
                val request = Request.Builder().url(url).addHeader("X-API-KEY", API_KEY).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext emptyList()
                    val responseBody = response.body?.string()
                    if (responseBody != null) parseDagenJson(responseBody) else emptyList()
                }
            } catch (e: Exception) {
                Log.e("SearchFragment", "Fout bij ophalen zoekresultaten: ${e.message}")
                emptyList()
            }
        }
    }

    private fun navigateToDetail(dag: DagVan) {
        // Navigeer naar de detailpagina vanuit het zoekscherm
        val action = SearchFragmentDirections.actionSearchFragmentToDetailFragment(dag)
        findNavController().navigate(action)
    }

    private fun hideKeyboard() {
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }
}