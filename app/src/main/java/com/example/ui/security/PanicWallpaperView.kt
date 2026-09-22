package com.example.ui.security

import android.graphics.BitmapFactory
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.util.AppStorageHelper
import java.io.File
import kotlin.random.Random

@Composable
fun PanicWallpaperView(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customWallpaperFile = remember { AppStorageHelper.getPanicWallpaperFile(context) }
    val customBitmap = remember(customWallpaperFile) {
        if (customWallpaperFile.exists() && customWallpaperFile.length() > 0) {
            try {
                BitmapFactory.decodeFile(customWallpaperFile.absolutePath)
            } catch (_: Throwable) {
                null
            }
        } else null
    }

    if (customBitmap != null) {
        Image(
            bitmap = customBitmap.asImageBitmap(),
            contentDescription = "Panic Wallpaper",
            modifier = modifier
                .fillMaxSize()
                .testTag("panic_wallpaper_image"),
            contentScale = ContentScale.Crop
        )
    } else {
        // High-definition Procedural Bioluminescent Glowing Green Tree Wallpaper
        ProceduralBioluminescentTreeWallpaper(modifier = modifier)
    }
}

@Composable
fun ProceduralBioluminescentTreeWallpaper(
    modifier: Modifier = Modifier
) {
    // Subtle pulsating glow animation for the fireflies and emerald canopy
    val transition = rememberInfiniteTransition(label = "bioluminescence")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Pre-calculate deterministic star positions & firefly particles
    val starPoints = remember {
        val rand = Random(42)
        List(120) {
            Triple(rand.nextFloat(), rand.nextFloat(), rand.nextFloat() * 1.8f + 0.6f)
        }
    }

    val fireflyNodes = remember {
        val rand = Random(1337)
        List(180) {
            // x between 0.05 and 0.95, y between 0.02 and 0.85
            val x = rand.nextFloat() * 0.90f + 0.05f
            val y = rand.nextFloat() * 0.82f + 0.02f
            val radius = rand.nextFloat() * 3.5f + 1.5f
            val colorType = rand.nextInt(3)
            Triple(Offset(x, y), radius, colorType)
        }
    }

    val deepNightBg = listOf(
        Color(0xFF010402),
        Color(0xFF030D06),
        Color(0xFF020703)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(deepNightBg))
            .testTag("panic_wallpaper_canvas_view")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Draw Starfield in deep space
            starPoints.forEach { (relX, relY, radius) ->
                drawCircle(
                    color = Color(0xFFC0EAC8).copy(alpha = 0.45f * (relX + 0.5f).coerceIn(0.2f, 0.8f)),
                    radius = radius,
                    center = Offset(relX * width, relY * height)
                )
            }

            // 2. Draw Giant Glowing Emerald Canopy Halos (Ambient Bioluminescent Lights)
            val canopyCenters = listOf(
                Offset(width * 0.50f, height * 0.20f) to width * 0.55f,
                Offset(width * 0.25f, height * 0.28f) to width * 0.45f,
                Offset(width * 0.75f, height * 0.26f) to width * 0.45f,
                Offset(width * 0.15f, height * 0.45f) to width * 0.38f,
                Offset(width * 0.85f, height * 0.42f) to width * 0.38f,
                Offset(width * 0.50f, height * 0.45f) to width * 0.40f
            )

            canopyCenters.forEach { (center, radius) ->
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00FF66).copy(alpha = 0.22f * pulseAlpha),
                            Color(0xFF10B981).copy(alpha = 0.12f * pulseAlpha),
                            Color(0xFF064E3B).copy(alpha = 0.04f * pulseAlpha),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius
                    ),
                    center = center,
                    radius = radius
                )
            }

            // 3. Draw Ancient Tree Trunk & Sprawling Branches Silhouette (looking up from base)
            val trunkPath = Path().apply {
                moveTo(width * 0.22f, height)
                // Left trunk curve upward towards center
                cubicTo(
                    width * 0.30f, height * 0.78f,
                    width * 0.42f, height * 0.65f,
                    width * 0.46f, height * 0.52f
                )
                // Main left major bough
                cubicTo(
                    width * 0.32f, height * 0.42f,
                    width * 0.12f, height * 0.38f,
                    width * 0.02f, height * 0.25f
                )
                // Outer left branch top
                lineTo(width * 0.05f, height * 0.22f)
                cubicTo(
                    width * 0.20f, height * 0.34f,
                    width * 0.38f, height * 0.38f,
                    width * 0.47f, height * 0.48f
                )
                // Left-center canopy fork
                cubicTo(
                    width * 0.40f, height * 0.32f,
                    width * 0.35f, height * 0.18f,
                    width * 0.30f, height * 0.02f
                )
                lineTo(width * 0.36f, height * 0.02f)
                cubicTo(
                    width * 0.42f, height * 0.20f,
                    width * 0.46f, height * 0.30f,
                    width * 0.50f, height * 0.42f
                )
                // Center-right vertical fork
                cubicTo(
                    width * 0.52f, height * 0.28f,
                    width * 0.58f, height * 0.14f,
                    width * 0.62f, height * 0.01f
                )
                lineTo(width * 0.68f, height * 0.01f)
                cubicTo(
                    width * 0.62f, height * 0.20f,
                    width * 0.56f, height * 0.32f,
                    width * 0.53f, height * 0.46f
                )
                // Main right major bough
                cubicTo(
                    width * 0.65f, height * 0.38f,
                    width * 0.85f, height * 0.34f,
                    width * 0.98f, height * 0.22f
                )
                lineTo(width * 0.99f, height * 0.26f)
                cubicTo(
                    width * 0.84f, height * 0.40f,
                    width * 0.64f, height * 0.46f,
                    width * 0.55f, height * 0.53f
                )
                // Right trunk curve downward
                cubicTo(
                    width * 0.58f, height * 0.65f,
                    width * 0.70f, height * 0.78f,
                    width * 0.78f, height
                )
                close()
            }

            // Draw deep solid ancient silhouette
            drawPath(
                path = trunkPath,
                color = Color(0xFF040B05)
            )

            // 4. Draw Dense Foliage Clusters with Emerald Bioluminescence
            val foliageClusters = listOf(
                Offset(width * 0.50f, height * 0.12f),
                Offset(width * 0.34f, height * 0.15f),
                Offset(width * 0.66f, height * 0.14f),
                Offset(width * 0.20f, height * 0.26f),
                Offset(width * 0.80f, height * 0.24f),
                Offset(width * 0.10f, height * 0.38f),
                Offset(width * 0.90f, height * 0.36f),
                Offset(width * 0.38f, height * 0.30f),
                Offset(width * 0.62f, height * 0.28f),
                Offset(width * 0.48f, height * 0.35f),
                Offset(width * 0.28f, height * 0.48f),
                Offset(width * 0.72f, height * 0.46f)
            )

            foliageClusters.forEach { pt ->
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF22C55E).copy(alpha = 0.55f * pulseAlpha),
                            Color(0xFF15803D).copy(alpha = 0.35f * pulseAlpha),
                            Color.Transparent
                        ),
                        center = pt,
                        radius = width * 0.24f
                    ),
                    center = pt,
                    radius = width * 0.24f
                )
            }

            // 5. Draw Hundreds of Sparkling Green Fireflies & Light Motes
            fireflyNodes.forEach { (relPos, baseRadius, colorType) ->
                val center = Offset(relPos.x * width, relPos.y * height)
                val primaryColor = when (colorType) {
                    0 -> Color(0xFF00FF66)
                    1 -> Color(0xFF4ADE80)
                    else -> Color(0xFFA3E635)
                }

                // Firefly soft outer halo
                drawCircle(
                    color = primaryColor.copy(alpha = 0.35f * pulseAlpha),
                    radius = baseRadius * 2.8f,
                    center = center
                )
                // Firefly bright sharp core
                drawCircle(
                    color = Color.White.copy(alpha = 0.90f * pulseAlpha),
                    radius = baseRadius * 0.85f,
                    center = center
                )
            }
        }
    }
}
