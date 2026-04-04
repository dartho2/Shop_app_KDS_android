package com.itsorderkds.ui.order

import com.google.android.gms.maps.model.LatLng
import com.itsorderkds.ui.order.OrderStatusEnum // Upewnij się, że import jest poprawny

data class RouteSegment(
    val points: List<LatLng>,
    val status: OrderStatusEnum,
    val orderNumber: String // Dodajmy orderNumber, przyda się do logiki
)

// ✅ NOWA KLASA: Reprezentuje jeden klaster (np. A -> B -> RETURN)
data class RouteCluster(
    val segments: List<RouteSegment>,
    val clusterIndex: Int // Indeks klastra, użyjemy go do wybrania koloru
)