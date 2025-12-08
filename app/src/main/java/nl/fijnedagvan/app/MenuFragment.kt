/*
Type: UI (Pop-up)

Functie: Het menu dat van onderen omhoog schuift (BottomSheet). Bevat de knoppen naar Zoeken, Over Ons, Contact en Instellingen.
 */


package nl.fijnedagvan.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MenuFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_menu_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val optionSearch = view.findViewById<TextView>(R.id.option_search)
        val optionSettings = view.findViewById<TextView>(R.id.option_settings)
        val optionAbout = view.findViewById<TextView>(R.id.option_about)
        val optionContact = view.findViewById<TextView>(R.id.option_contact)

        optionSearch.setOnClickListener {
            // Navigeer nu naar het SearchFragment
            findNavController().navigate(R.id.action_menuFragment_to_searchFragment)
        }

        optionSettings.setOnClickListener {
            findNavController().navigate(R.id.action_menuFragment_to_settingsFragment)
        }

        optionAbout.setOnClickListener {
            findNavController().navigate(R.id.action_menuFragment_to_aboutFragment)
        }

        optionContact.setOnClickListener {
            findNavController().navigate(R.id.action_menuFragment_to_contactFragment)
        }
    }
}