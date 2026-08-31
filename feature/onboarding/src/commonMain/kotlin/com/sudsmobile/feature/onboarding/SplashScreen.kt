package com.sudsmobile.feature.onboarding

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sudsmobile.shared.theme.LocalSudsMotionPreferences
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import sudsandshine.feature.onboarding.generated.resources.Res
import sudsandshine.feature.onboarding.generated.resources.suds_splash_mark

internal const val SplashMinimumDurationMillis = 1_400L

private val SplashNavy = Color(0xFF142539)
private val SplashBlue = Color(0xFF36B6E5)
private val SplashWhite = Color(0xFFF7FBFF)

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
) {
    LaunchedEffect(Unit) {
        awaitMinimumSplashDuration()
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashNavy)
            .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-24).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(Res.drawable.suds_splash_mark),
                contentDescription = null,
                modifier = Modifier.size(246.dp),
                contentScale = ContentScale.Fit,
            )

            Text(
                text = "Suds & Shine",
                color = SplashWhite,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.7).sp,
                lineHeight = 36.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "SOLUTIONS",
                color = SplashBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Surface(
                modifier = Modifier
                    .width(40.dp)
                    .height(2.dp),
                color = SplashBlue,
                shape = RoundedCornerShape(1.dp),
            ) {}
            Spacer(Modifier.height(14.dp))
            Text(
                text = "LAVAGEM AUTOMÓVEL  ·  DETAILING",
                modifier = Modifier.alpha(0.68f),
                color = SplashWhite,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.35.sp,
                lineHeight = 15.sp,
                textAlign = TextAlign.Center,
            )
        }

        SplashLoadingIndicator(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-48).dp),
        )
    }
}

internal suspend fun awaitMinimumSplashDuration(
    wait: suspend (Long) -> Unit = { delay(it) },
) {
    wait(SplashMinimumDurationMillis)
}

@Composable
private fun SplashLoadingIndicator(modifier: Modifier = Modifier) {
    val reduceMotion = LocalSudsMotionPreferences.current.reduceMotion
    if (reduceMotion) {
        SplashLoadingIndicatorContent(modifier = modifier, pulse = 1f)
    } else {
        val transition = rememberInfiniteTransition(label = "splash-loading")
        val pulse by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 700),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "splash-loading-pulse",
        )
        SplashLoadingIndicatorContent(modifier = modifier, pulse = pulse)
    }
}

@Composable
private fun SplashLoadingIndicatorContent(
    modifier: Modifier,
    pulse: Float,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier
                .size(4.dp)
                .alpha(pulse * 0.7f),
            color = SplashBlue,
            shape = CircleShape,
        ) {}
        Surface(
            modifier = Modifier
                .width(34.dp)
                .height(3.dp)
                .alpha(pulse),
            color = SplashBlue,
            shape = RoundedCornerShape(2.dp),
        ) {}
        Surface(
            modifier = Modifier
                .size(4.dp)
                .alpha(pulse * 0.7f),
            color = SplashBlue,
            shape = CircleShape,
        ) {}
    }
}
