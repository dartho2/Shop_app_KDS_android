package com.itsorderkds.ui.loading

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itsorderkds.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

// ─── Kolory lokalne do splash ──────────────────────────────────────────────
private val SplashBg       = Color(0xFF0D0F12)
private val SplashCard     = Color(0xFF13171D)
private val SplashBorder   = Color(0xFF2C3340)
private val SplashAccent   = Color(0xFFFF8C42)
private val SplashAccent2  = Color(0xFFFFCA28)
private val SplashText     = Color(0xFFF0F2F5)
private val SplashMuted    = Color(0xFF8B95A1)

@Composable
fun AppLoadingScreen(modifier: Modifier = Modifier) {

    val infiniteTransition = rememberInfiniteTransition(label = "kds_splash")

    // Obrót pierścienia
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 360f,
        animationSpec = infiniteRepeatable(
            animation  = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring"
    )

    // Pulsowanie alpha zewnętrznego pierścienia
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue  = 0.45f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringAlpha"
    )

    // Progress bar shimmer
    val shimmer by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue  = 2f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    // Fade-in przy starcie
    val fadeIn by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label         = "fadeIn"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SplashBg)
            .alpha(fadeIn),
        contentAlignment = Alignment.Center
    ) {

        // ── Dekoracyjne kółka w tle ──────────────────────────────────────
        BackgroundDecor(rotation = ringRotation, ringAlpha = ringAlpha)

        // ── Główna zawartość ─────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier            = Modifier.padding(horizontal = 32.dp)
        ) {

            // ── Ikona KDS ────────────────────────────────────────────────
            KdsIconBadge(rotation = ringRotation)

            Spacer(Modifier.height(40.dp))

            // ── Nazwa aplikacji ──────────────────────────────────────────
            Text(
                text       = "Kitchen Display",
                style      = androidx.compose.material3.MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color      = SplashText,
                textAlign  = TextAlign.Center
            )
            Text(
                text       = "System",
                style      = androidx.compose.material3.MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color      = SplashAccent,
                textAlign  = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            // ── Tagline ──────────────────────────────────────────────────
            Text(
                text      = "Profesjonalne zarządzanie zamówieniami kuchennymi",
                style     = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color     = SplashMuted,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(56.dp))

            // ── Pasek ładowania ──────────────────────────────────────────
            KdsLoadingBar(shimmer = shimmer)

            Spacer(Modifier.height(16.dp))

            Text(
                text  = "Łączenie z systemem...",
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = SplashMuted.copy(alpha = 0.7f),
                letterSpacing = 1.5.sp
            )

            Spacer(Modifier.height(60.dp))

            // ── Dolne info ───────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                StatusDot()
                Text(
                    text  = "itsorder.pl",
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    color = SplashMuted.copy(alpha = 0.5f),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

// ─── Duża ikona KDS z pierścieniem ───────────────────────────────────────────
@Composable
private fun KdsIconBadge(rotation: Float) {
    Box(
        contentAlignment = Alignment.Center,
        modifier         = Modifier.size(160.dp)
    ) {
        // Obracający się pierścień przerywany
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r  = size.width * 0.46f

            // Zewnętrzny pierścień — przerywany, obracający się
            rotate(degrees = rotation, pivot = Offset(cx, cy)) {
                val segmentCount = 8
                for (i in 0 until segmentCount) {
                    val startAngle = (i * (360f / segmentCount)) - 90f
                    val sweepAngle = 360f / segmentCount * 0.6f
                    val alpha = if (i % 2 == 0) 0.6f else 0.15f
                    drawArc(
                        color      = SplashAccent.copy(alpha = alpha),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter  = false,
                        style      = Stroke(width = 2.5.dp.toPx())
                    )
                }
            }

            // Wewnętrzny stały pierścień
            drawCircle(
                color  = SplashBorder,
                radius = r * 0.78f,
                center = Offset(cx, cy),
                style  = Stroke(width = 1.dp.toPx())
            )
        }

        // Karta z emoji kuchni
        Surface(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = SplashCard,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Delikatny gradient wewnątrz karty
                    drawRect(
                        brush = Brush.radialGradient(
                            colors  = listOf(
                                SplashAccent.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center  = Offset(size.width / 2f, size.height / 2f),
                            radius  = size.width * 0.7f
                        )
                    )
                }
                Text(
                    text     = "🍳",
                    fontSize = 44.sp
                )
            }
        }
    }
}

// ─── Dekoracyjne kółka w tle ─────────────────────────────────────────────────
@Composable
private fun BackgroundDecor(rotation: Float, ringAlpha: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        // Duże kółka dekoracyjne
        drawCircle(
            color  = SplashAccent.copy(alpha = ringAlpha * 0.12f),
            radius = size.width * 0.72f,
            center = Offset(cx, cy),
            style  = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color  = SplashAccent2.copy(alpha = ringAlpha * 0.07f),
            radius = size.width * 0.9f,
            center = Offset(cx, cy),
            style  = Stroke(width = 1.dp.toPx())
        )

        // Małe cząsteczki na orbicie
        val particleCount = 6
        for (i in 0 until particleCount) {
            val angle = Math.toRadians(
                ((i.toFloat() / particleCount) * 360f + rotation * 0.3f).toDouble()
            )
            val r = size.width * 0.42f
            val px = cx + cos(angle).toFloat() * r
            val py = cy + sin(angle).toFloat() * r
            drawCircle(
                color  = SplashAccent.copy(alpha = 0.20f),
                radius = 3.dp.toPx(),
                center = Offset(px, py)
            )
        }
    }
}

// ─── Pasek ładowania ─────────────────────────────────────────────────────────
@Composable
private fun KdsLoadingBar(shimmer: Float) {
    Box(
        modifier = Modifier
            .width(220.dp)
            .height(3.dp)
            .clip(RoundedCornerShape(50))
            .background(SplashBorder)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val barW = size.width
            // Przesuwający się blask
            val gradStart = shimmer * barW
            drawRect(
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to Color.Transparent,
                        0.4f to SplashAccent.copy(alpha = 0.9f),
                        0.6f to SplashAccent2,
                        1.0f to Color.Transparent
                    ),
                    start = Offset(gradStart - barW * 0.5f, 0f),
                    end   = Offset(gradStart + barW * 0.5f, 0f)
                )
            )
        }
    }
}

// ─── Zielona kropka "online" ─────────────────────────────────────────────────
@Composable
private fun StatusDot() {
    val pulse by rememberInfiniteTransition(label = "dot").animateFloat(
        initialValue  = 0.5f,
        targetValue   = 1.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    Canvas(modifier = Modifier.size(6.dp)) {
        drawCircle(color = KdsSlaGreen.copy(alpha = pulse))
    }
}

