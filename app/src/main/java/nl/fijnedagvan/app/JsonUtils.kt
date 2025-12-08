/*
Type: Helper (Vertaler)

Functie: Bevat de functie parseDagenJson.

Doel: Vertaalt de ruwe tekst (JSON) die van je server komt naar een begrijpelijke lijst van DagVan-objecten die Kotlin kan gebruiken.
 */



package nl.fijnedagvan.app

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavDeepLinkBuilder
import coil.ImageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken

// --- JOUW BESTAANDE FUNCTIE ---
fun parseDagenJson(jsonString: String): List<DagVan> {
    val gson = Gson()
    val listType = object : TypeToken<List<DagVan>>() {}.type

    return try {
        val dagen: List<DagVan> = gson.fromJson(jsonString, listType)
        Log.d("JsonUtils", "Succesvol ${dagen.size} items geparsed.")
        dagen
    } catch (e: JsonSyntaxException) {
        Log.e("JsonUtils", "Fout bij parsen van JSON string: ${e.message}")
        emptyList()
    } catch (e: Exception) {
        Log.e("JsonUtils", "Onverwachte fout tijdens JSON parsen: ${e.message}")
        emptyList()
    }
}


// --- HIERONDER DE CODE DIE EERDER IN NOTIFICATIONHELPER ZAT ---

/**
 * De centrale functie voor het versturen van een rijke 'Dag Van'-notificatie.
 */
suspend fun sendDagVanNotification(context: Context, dag: DagVan) {
    val dagId = dag.dagId
    val dagNaam = dag.naam

    if (dagId.isNullOrBlank() || dagNaam.isNullOrBlank()) {
        Log.e("NotificationUtils", "Kan notificatie niet maken: ID of naam ontbreekt.")
        return
    }

    val titel = "Vandaag is het ${getLidwoord(dagNaam)}$dagNaam"
    val tekst = "Lees meer over deze dag in de app!"

    val bitmap = fetchImageBitmap(context, dag.imageUrl)

    val pendingIntent = NavDeepLinkBuilder(context)
        .setGraph(R.navigation.nav_graph)
        .setDestination(R.id.nav_detail)
        .setArguments(Bundle().apply { putParcelable("dagVan", dag) })
        .createPendingIntent()

    buildAndSendNotification(context, titel, tekst, bitmap, pendingIntent, dagId.hashCode())
}

/**
 * Helper-functie om een afbeelding van een URL te downloaden met Coil.
 */
suspend fun fetchImageBitmap(context: Context, url: String?): Bitmap? {
    if (url.isNullOrEmpty()) {
        Log.d("NotificationUtils", "Geen afbeeldings-URL beschikbaar.")
        return null
    }

    Log.d("NotificationUtils", "Poging tot downloaden afbeelding: $url")
    val loader = ImageLoader(context)
    val request = ImageRequest.Builder(context)
        .data(url)
        .allowHardware(false)
        .build()

    when (val result = loader.execute(request)) {
        is SuccessResult -> {
            Log.d("NotificationUtils", "Afbeelding succesvol gedownload.")
            return result.drawable.toBitmap()
        }
        is ErrorResult -> {
            Log.e("NotificationUtils", "Afbeelding downloaden mislukt.", result.throwable)
            return null
        }
        else -> return null
    }
}

/**
 * Centrale functie voor het bouwen en versturen van de notificatie.
 */
fun buildAndSendNotification(context: Context, title: String, message: String, bitmap: Bitmap?, pendingIntent: PendingIntent, notificationId: Int) {
    val builder = NotificationCompat.Builder(context, "DAGVAN_CHANNEL_ID")
        .setSmallIcon(R.drawable.ic_notificatie)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)

    if (bitmap != null) {
        builder.setLargeIcon(bitmap)
        builder.setStyle(NotificationCompat.BigPictureStyle()
            .bigPicture(bitmap)
            .bigLargeIcon(null as Bitmap?))
    } else {
        builder.setStyle(NotificationCompat.BigTextStyle().bigText(message))
    }

    with(NotificationManagerCompat.from(context)) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.e("NotificationUtils", "Kan notificatie niet tonen: permissie ontbreekt.")
            return
        }
        notify(notificationId, builder.build())
        Log.d("NotificationUtils", "Notificatie verstuurd (ID: $notificationId): $title")
    }
}

/**
 * Bepaalt of het lidwoord 'de' nodig is.
 */
fun getLidwoord(dagNaam: String): String {
    return if (dagNaam.startsWith("Internationale Dag van", ignoreCase = true) || dagNaam.startsWith("Dag van", ignoreCase = true)) {
        "de "
    } else {
        ""
    }
}