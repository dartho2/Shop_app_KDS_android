package com.itsorderkds.ui.loading

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itsorderkds.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Nowoczesny ekran ładowania z animowanym gradientem, pulsującymi kółkami i cząsteczkami
 */
@Composable
fun AppLoadingScreen(
    modifier: Modifier = Modifier,
    title: String = "Ładowanie danych",
    subtitle: String = "Przygotowujemy wszystko dla Ciebie",
    @DrawableRes logoResId: Int = R.drawable.splash,
    logoSize: Dp = 120.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")

    // Logo scale pulse - wolniejsza dla płynności
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),  // Zwiększono z 1500 do 2000
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo-scale"
    )

    // Rings rotation - wolniejsza dla płynności
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),  // Zwiększono z 4000 do 6000
            repeatMode = RepeatMode.Restart
        ),
        label = "ring-rotation"
    )

    // Dots animation
    val dotsProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dots"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                // Statyczny gradient - lepiej dla wydajności niż animowany
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Animated particles in background (zmniejszono dla wydajności)
        AnimatedParticles(
            modifier = Modifier.fillMaxSize(),
            particleCount = 8,  // Zmniejszono z 20 do 8 dla lepszej wydajności
            progress = dotsProgress
        )

        // Main content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated rings + logo
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                // Outer pulsating rings
                PulsatingRings(
                    rotation = ringRotation,
                    modifier = Modifier.fillMaxSize()
                )

                // Logo with scale animation
                Image(
                    painter = painterResource(id = logoResId),
                    contentDescription = "Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(logoSize)
                        .scale(logoScale)
                )
            }

            Spacer(Modifier.height(48.dp))

            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            // Subtitle with animated dots
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.width(4.dp))
                AnimatedDots(progress = dotsProgress)
            }

            Spacer(Modifier.height(24.dp))

            // Modern loading bar
            ModernLoadingBar(
                modifier = Modifier
                    .width(200.dp)
                    .height(4.dp),
                progress = dotsProgress
            )
        }
    }
}

/**
 * Pulsujące kółka wokół logo - ZOPTYMALIZOWANE dla wydajności
 */
@Composable
private fun PulsatingRings(
    rotation: Float,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        // Jedno obracające się kółko - lepiej niż trzy dla wydajności
        rotate(rotation, pivot = Offset(centerX, centerY)) {
            drawCircle(
                color = primaryColor.copy(alpha = 0.25f),
                radius = size.width * 0.4f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2.5.dp.toPx())
            )
        }
    }
}

/**
 * Animowane cząsteczki w tle - ZOPTYMALIZOWANE
 */
@Composable
private fun AnimatedParticles(
    modifier: Modifier = Modifier,
    particleCount: Int = 8,
    progress: Float
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Uproszczona animacja - mniej obliczeń
        repeat(particleCount) { index ->
            val baseAngle = (index.toFloat() / particleCount) * 360f
            val angle = Math.toRadians((baseAngle + progress * 360f).toDouble())
            val radiusMultiplier = 0.25f + (index % 2) * 0.1f

            val x = (width / 2f + cos(angle).toFloat() * width * radiusMultiplier).toFloat()
            val y = (height / 2f + sin(angle).toFloat() * height * radiusMultiplier).toFloat()

            val particleSize = 3.dp.toPx()
            val alpha = 0.15f + ((progress + index * 0.1f) % 1f) * 0.15f

            drawCircle(
                color = primaryColor.copy(alpha = alpha),
                radius = particleSize,
                center = Offset(x, y)
            )
        }
    }
}

/**
 * Animowane kropki "..."
 */
@Composable
private fun AnimatedDots(progress: Float) {
    val dotCount = ((progress * 3) % 3).toInt() + 1

    Text(
        text = ".".repeat(dotCount).padEnd(3, ' '),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    )
}

/**
 * Nowoczesny pasek ładowania - ZOPTYMALIZOWANY
 */
@Composable
private fun ModernLoadingBar(
    modifier: Modifier = Modifier,
    progress: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bar")

    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),  // Zwiększono z 1500
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    Canvas(modifier = modifier) {
        // Background track
        drawRoundRect(
            color = Color.Gray.copy(alpha = 0.2f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
        )

        // Uproszczony pasek - stała szerokość zamiast sinusoidalnej
        val barWidth = size.width * (0.4f + shimmer * 0.5f)

        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF2196F3),
                    Color(0xFF9C27B0),
                    Color(0xFF2196F3)
                ),
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f)
            ),
            size = androidx.compose.ui.geometry.Size(barWidth, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
        )
    }
}

