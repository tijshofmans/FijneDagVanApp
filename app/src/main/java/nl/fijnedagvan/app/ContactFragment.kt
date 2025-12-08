/*
Type: UI (Scherm)

Functie: Toont de contactinformatie. Dit is een vrij simpele pagina die voornamelijk statische tekst weergeeft.
 */

package nl.fijnedagvan.app

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class ContactFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate de layout voor dit fragment
        return inflater.inflate(R.layout.fragment_contact, container, false)
    }
}