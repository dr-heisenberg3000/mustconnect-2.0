package com.must.connect.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.must.connect.ui.theme.BrandBlue
import com.must.connect.ui.theme.BrandNavy
import com.must.connect.ui.theme.BrandRoyalBlue
import com.must.connect.ui.theme.OnBrandWhite
import com.must.connect.ui.theme.OnBrandWhite70
import kotlinx.coroutines.delay

/**
 * Full-screen splash that shows for 2 seconds then navigates to the role
 * selection screen.  Implements the deep-blue gradient aesthetic from the
 * Figma design, stripped of all filler content.
 */
@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {

    // Trigger navigation after 2 seconds
    LaunchedEffect(Unit) {
        delay(2_000)
        onSplashComplete()
    }

    // Subtle pulse animation on the logo badge
    val infiniteTransition = rememberInfiniteTransition(label = "logoScale")
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue  = 1.03f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "logoScaleAnim",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BrandNavy, BrandRoyalBlue, BrandBlue),
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {

            // ── Logo badge ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .scale(logoScale)
                    .size(80.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x33FFFFFF),
                                Color(0x11FFFFFF),
                            )
                        ),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text  = "MC",
                    color = OnBrandWhite,
                    fontSize   = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── App name ─────────────────────────────────────────────────────
            Text(
                text       = "MUST-CONNECT",
                color      = OnBrandWhite,
                fontSize   = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                textAlign  = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text      = "Departmental Communication\n& Classroom Management System",
                color     = OnBrandWhite70,
                fontSize  = 13.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp,
            )

            Spacer(modifier = Modifier.height(60.dp))

            // ── Loading indicator ─────────────────────────────────────────────
            CircularProgressIndicator(
                modifier     = Modifier.size(36.dp),
                color        = OnBrandWhite,
                trackColor   = Color(0x33FFFFFF),
                strokeWidth  = 3.dp,
                strokeCap    = StrokeCap.Round,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text      = "Loading your campus...",
                color     = OnBrandWhite70,
                fontSize  = 12.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.4.sp,
            )

            Spacer(modifier = Modifier.height(56.dp))

            // ── Department footer ─────────────────────────────────────────────
            Text(
                text      = "CS & IT DEPARTMENT",
                color     = Color(0x80FFFFFF),
                fontSize  = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 3.sp,
            )
        }
    }
}
