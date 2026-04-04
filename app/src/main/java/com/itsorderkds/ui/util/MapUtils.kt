package com.itsorderkds.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.itsorderkds.data.model.OrderTras

fun planRouteOnMap(context: Context, stops: List<OrderTras>) {
    if (stops.isEmpty()) {
        Toast.makeText(context, "Zaznacz przystanki na trasie", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val waypoints = stops.joinToString(separator = "|") { "${it.lat},${it.lng}" }
        val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${stops.last().lat},${stops.last().lng}&waypoints=$waypoints&travelmode=driving")
        val intent = Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.apps.maps")
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Aplikacja Mapy Google nie jest zainstalowana.", Toast.LENGTH_LONG).show()
    }
}
