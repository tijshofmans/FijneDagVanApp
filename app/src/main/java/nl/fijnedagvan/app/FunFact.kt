/*
Haalt een willekeurig feitje op uit de FunFacts-table in de database om in een notificatie weer te geven.
 */

package nl.fijnedagvan.app

import com.google.gson.annotations.SerializedName

data class FunFact(
    @SerializedName("id")
    val id: String?,

    @SerializedName("feitje")
    val feitje: String?
)
