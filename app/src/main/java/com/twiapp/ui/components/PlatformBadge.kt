package com.twiapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twiapp.R
import com.twiapp.ui.theme.PlatformColors
import com.twiapp.utils.Platform

/**
 * A colored badge showing the detected platform with its real logo.
 */
@Composable
fun PlatformBadge(
    platform: Platform?,
    modifier: Modifier = Modifier
) {
    if (platform == null) return

    val (color, iconRes, label) = getPlatformVisual(platform)

    val animatedColor by animateColorAsState(
        targetValue = color,
        label = "platform_color"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = animatedColor.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                tint = animatedColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                color = animatedColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Grid item showing a supported platform with its real logo and name.
 */
@Composable
fun PlatformGridItem(
    platform: Platform,
    modifier: Modifier = Modifier
) {
    val (color, iconRes, label) = getPlatformVisual(platform)

    Column(
        modifier = modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = color.copy(alpha = 0.1f),
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

private data class PlatformVisual(val color: Color, val iconRes: Int, val label: String)

private fun getPlatformVisual(platform: Platform): PlatformVisual {
    return when (platform) {
        Platform.TIKTOK -> PlatformVisual(
            PlatformColors.TikTok, R.drawable.ic_tiktok, "TikTok"
        )
        Platform.INSTAGRAM -> PlatformVisual(
            PlatformColors.Instagram, R.drawable.ic_instagram, "Instagram"
        )
        Platform.YOUTUBE -> PlatformVisual(
            PlatformColors.YouTube, R.drawable.ic_youtube, "YouTube"
        )
        Platform.TWITTER -> PlatformVisual(
            PlatformColors.Twitter, R.drawable.ic_twitter_x, "Twitter / X"
        )
        Platform.FACEBOOK -> PlatformVisual(
            PlatformColors.Facebook, R.drawable.ic_facebook, "Facebook"
        )
    }
}
