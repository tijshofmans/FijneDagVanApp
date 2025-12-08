/*
Type: Data Model (Blauwdruk)

Functie: Bevat de definitie van de klasse DagVan. Hierin staat beschreven uit welke velden een Dag bestaat (id, naam, datum, info, afbeelding, etc.).
 */

package nl.fijnedagvan.app

import com.google.gson.annotations.SerializedName
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class DagenTable(
    @SerializedName("name") val tableName: String,
    @SerializedName("data") val dagenLijst: List<DagVan>
)

@Parcelize
data class DagVan(
    @SerializedName("id") val id: String?,
    @SerializedName("dag_id") val dagId: String?,
    @SerializedName("dagnaam") val naam: String?,
    @SerializedName("datum") val datumString: String?,
    @SerializedName("wat") val korteOmschrijving: String?,
    @SerializedName("wanneer") val wanneerOmschrijving: String?,
    @SerializedName("info") val uitgebreideInfo: String?,
    @SerializedName("intro") val intro: String?,
    @SerializedName("website") val websiteUrl: String?,

    // --- GECORRIGEERD: Naam is nu identiek aan JSON ---
    @SerializedName("dagsoort") val dagsoort: String?,

    @SerializedName("schaal") val schaal: String?,
    @SerializedName("datum_check") val datumCheck: String?,
    @SerializedName("onderwerp") val onderwerp: String?,
    @SerializedName("slug") val slug: String?

) : Parcelable {
    val imageUrl: String?
        get() {
            if (!dagId.isNullOrEmpty()) {
                return "https://fijnedagvan.nl/assets/img/dagen/${dagId}.jpg"
            }
            return null
        }
}