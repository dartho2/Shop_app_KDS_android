package com.itsorderkds.ui.theme.home.components

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.itsorderkds.R
import com.itsorderkds.data.model.Vehicle
import com.itsorderkds.ui.theme.home.AppDestinations
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerContent(
    currentRoute: String,
    userName: String,
    userRole: String?, // Poprawnie zdefiniowany jako nullable
    onLogout: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onCloseDrawer: () -> Unit,
    showHome: Boolean = true,
) {
    val context = LocalContext.current

    ModalDrawerSheet {
        Column(modifier = Modifier.fillMaxSize()) {

            // --- 1. NAGŁÓWEK (Header) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(vertical = 24.dp, horizontal = 16.dp)
            ) {
                Column {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "Logo Aplikacji",
                        modifier = Modifier
                            .size(64.dp)
                            .padding(bottom = 16.dp)
                    )
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    // ✅ POPRAWKA: Użyj .isNullOrEmpty() do sprawdzenia Stringa
                    if (!userRole.isNullOrEmpty()) {
                        Text(
                            text = userRole,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                FilledIconButton(
                    onClick = onCloseDrawer,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Zamknij menu"
                    )
                }
            }

            // --- 2. GŁÓWNA NAWIGACJA ---
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                // ✅ POPRAWKA: Dodaj warunki 'showHome', 'showProducts', 'showSettings'

                if (showHome) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("Główna") },
                        selected = currentRoute == AppDestinations.HOME,
                        onClick = { onNavigateToHome() },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }

            // --- 3. STOPKA (Footer) ---
            Spacer(modifier = Modifier.weight(1f))
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            NavigationDrawerItem(
                icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
                label = { Text("Wyloguj") },
                selected = false,
                onClick = onLogout,
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            Text(
                text = "Wersja ${getAppVersion(context)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}


@Composable
fun AssignmentPrompt(
    modifier: Modifier = Modifier,
    vehicles: List<Vehicle>,
    isLoading: Boolean,
    onStartShiftWithVehicle: (Vehicle) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { vehicles.size })
    var selectedVehicleId by remember { mutableStateOf<String?>(null) }

    val today = LocalDate.now()

    // ✨ POPRAWKA: Użyj nowej, poprawnej logiki dostępności pojazdu.
    val hasAnyAvailableVehicle = remember(vehicles) { vehicles.any { isVehicleAvailable(it) } }
    val selectedVehicle = remember(selectedVehicleId, vehicles) {
        vehicles.find { it.id == selectedVehicleId }
    }

    // ✨ POPRAWKA: Przenieś logikę `enabled` przycisku do zmiennej.

    val isButtonEnabled = selectedVehicle?.let { isVehicleAvailable(it, today) } == true

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ... (sekcja z tekstem na górze - bez zmian) ...
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
//            Image(
//                painter = painterResource(id = R.drawable.ic_launcher_foreground),
//                contentDescription = null,
//                modifier = Modifier.size(80.dp)
//            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(id = R.string.shift_not_started_label),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Wybierz pojazd, aby kontynuować",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Sekcja z Pagerem
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (vehicles.isEmpty()) {
                Text("Brak pojazdów w systemie.")
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(0.dp), // 👈 usunięcie bocznych paddingów
                        pageSpacing = 0.dp // 👈 usunięcie odstępów między kartami
                    ) { page ->
                        val vehicle = vehicles[page]


                        val isAvailableInPager = isVehicleAvailable(vehicle, today)
                        VehiclePage(
                            vehicle = vehicle,
                            isSelected = vehicle.id == selectedVehicleId,
                            onClick = {
                                if (isAvailableInPager) {
                                    selectedVehicleId =
                                        if (selectedVehicleId == vehicle.id) null else vehicle.id
                                }
                            },
                            isAvailable = isAvailableInPager
                        )
                    }
                    Row(
                        Modifier
                            .height(50.dp)
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(pagerState.pageCount) { iteration ->
                            val color by animateColorAsState(
                                targetValue = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                label = "Indicator Color"
                            )
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .size(12.dp)
                            )
                        }
                    }
                }
            }
        }


        // Dolny przycisk akcji
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!isLoading && vehicles.isNotEmpty()) {
                Button(
                    onClick = {
                        vehicles.find { it.id == selectedVehicleId }?.let {
                            onStartShiftWithVehicle(it)
                        }
                    },
                    enabled = isButtonEnabled, // ✨ Użyj nowej zmiennej
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Login, contentDescription = null)
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text(
                        text = stringResource(id = R.string.start_shift_button),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else if (!isLoading && !hasAnyAvailableVehicle) { // ✨ Użyj `hasAnyAvailableVehicle`
                Text(
                    text = "Brak dostępnych pojazdów. Skontaktuj się z administratorem.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

private fun isVehicleAvailable(v: Vehicle, today: LocalDate = LocalDate.now()): Boolean {
    val inUseNow = (v.inUse == true)

    // bezpieczne wyznaczenie daty
    val lastUsedLocalDate = runCatching {
        v.lastUsedAt?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()
    }.getOrNull()

    val wasUsedToday = (lastUsedLocalDate?.isEqual(today) == true)

    return (v.isActive == true) && (!inUseNow || !wasUsedToday)
}

@Composable
private fun VehiclePage(
    vehicle: Vehicle,
    isSelected: Boolean,
    onClick: () -> Unit,
    isAvailable: Boolean // ✨ Nowy parametr
) {
    // Log.e("VehiclePage", "Last used date: ${vehicle.name} ${vehicle.lastUsedAt} isAvailable: ${isAvailable}")


    // Kolor tła dla niedostępnych pojazdów
    val backgroundColor =
        if (!isAvailable) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .then(
                if (isSelected && isAvailable) Modifier.border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(18.dp)
                ) else Modifier
            )
            .clickable(onClick = onClick, enabled = isAvailable),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = if (isAvailable) CardDefaults.cardElevation(8.dp) else CardDefaults.cardElevation(
            0.dp
        ) // ✨ brak cienia jeśli niedostępny
    ) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                AsyncImage(
                    model = vehicle.photoUrl,
                    contentDescription = "Zdjęcie pojazdu: ${vehicle.brand} ${vehicle.name}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop,
                    alpha = if (isAvailable) 1.0f else 0.5f
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                startY = 300f
                            )
                        )
                )
                Text(
                    text = "${vehicle.brand} ${vehicle.name}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                )
            }

            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = vehicle.registrationNumber,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.5f
                    )
                )
                Text(
                    text = if (isAvailable) "Status: Dostępny" else "Status: Niedostępny",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isAvailable) Color(0xFF388E3C) else Color(0xFFD32F2F)
                )
            }
        }
    }
}

// Pomocnicza funkcja do pobierania wersji aplikacji
@Suppress("DEPRECATION")
fun getAppVersion(context: Context): String = try {
    val pm = context.packageManager
    val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        pm.getPackageInfo(context.packageName, 0)
    }
    pkgInfo.versionName ?: "N/A"
} catch (e: Exception) {
    "N/A"
}

@Composable
fun ShiftStatusAction(
    onCheckOut: () -> Unit
) {
    Button(
        onClick = onCheckOut,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFD32F2F), // Zawsze czerwony
            contentColor = Color.White
        )
    ) {
        Icon(
            imageVector = Icons.Default.Logout, // Zawsze ikona wylogowania
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(
            text = stringResource(R.string.end_shift) // Zawsze tekst "Zakończ zmianę"
        )
    }
}

@Composable
fun EmptyPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.SentimentDissatisfied,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Brak wyników",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Brak zamówień do wyświetlenia.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

    }
}

@Composable
fun LockedOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // półprzezroczysty „scrim”
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(0.dp) // dla jasności
                .let { m ->
                    // jeśli chcesz lekko przyciemnić tło:
                    m.then(Modifier)
                }
        )
        CircularProgressIndicator()
    }
}