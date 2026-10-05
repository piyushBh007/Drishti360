package com.drishti360.app.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.drishti360.app.data.models.NGO
import com.drishti360.app.data.models.RiskLevel
import com.drishti360.app.ui.theme.LightBackground
import com.drishti360.app.ui.theme.LightBorder
import com.drishti360.app.ui.theme.LightSurface
import com.drishti360.app.ui.theme.PrimaryBlue
import com.drishti360.app.ui.theme.RiskHighBg
import com.drishti360.app.ui.theme.RiskHighRed
import com.drishti360.app.ui.theme.RiskLowBg
import com.drishti360.app.ui.theme.RiskLowGreen
import com.drishti360.app.ui.theme.RiskMediumAmber
import com.drishti360.app.ui.theme.RiskMediumBg
import com.drishti360.app.ui.theme.TextDarkPrimary
import com.drishti360.app.ui.theme.TextDarkSecondary
import com.drishti360.app.ui.theme.TextMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Top App Header matching Reference Screen 1
 */
@Composable
fun MainHeaderBar(
    title: String = "Drishti 360",
    subtitle: String = "Transparency. Accountability. Impact.",
    onProfileClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextDarkSecondary
            )
        }
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .clickable { onProfileClick() }
                .padding(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Profile",
                tint = TextDarkSecondary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * Compact Stat Box matching Reference Screen 1 (e.g. 08 NGOs Monitored)
 */
@Composable
fun StatCard(
    number: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(84.dp)
            .border(1.dp, LightBorder, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = LightSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = number,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = TextDarkSecondary,
                fontWeight = FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 2,
                lineHeight = 13.sp
            )
        }
    }
}

/**
 * Risk Score Tag Pill matching Reference UI (e.g. "87  High Risk" or "62  Medium Risk")
 */
@Composable
fun RiskTag(
    score: Int,
    level: RiskLevel,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, labelText) = when (level) {
        RiskLevel.HIGH -> Triple(RiskHighBg, RiskHighRed, "High Risk")
        RiskLevel.MEDIUM -> Triple(RiskMediumBg, RiskMediumAmber, "Medium Risk")
        RiskLevel.LOW -> Triple(RiskLowBg, RiskLowGreen, "Low Risk")
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$score",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = labelText,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

/**
 * NGO Card Item with clean neutral Facility placeholder icon and compact layout
 */
@Composable
fun NgoCardItem(
    ngo: NGO,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
            .border(1.dp, LightBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = LightSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Neutral Facility placeholder icon box (48x48 dp)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9))
                        .border(1.dp, LightBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = "Facility",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Facility",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDarkSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ngo.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "📍 ${ngo.location.city}, ${ngo.location.state}",
                        fontSize = 12.sp,
                        color = TextDarkSecondary
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open",
                    tint = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (ngo.riskScore != null) {
                    RiskTag(score = ngo.riskScore.score, level = ngo.riskScore.level)
                } else {
                    Text(
                        text = "Not Assessed",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(LightBackground)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // CCTV Dot Status
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (ngo.cctvOnlineCount == null) Color.Gray else if (ngo.cctvOnlineCount > 0) RiskLowGreen else RiskHighRed
                                )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (ngo.cctvOnlineCount == null) "No CCTV Data" else if (ngo.cctvOnlineCount > 0) "CCTV Online" else "CCTV Offline",
                            fontSize = 11.sp,
                            color = TextDarkSecondary
                        )
                    }

                    // Attendance Status Tag
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (ngo.attendanceRate == null) Icons.Default.Info else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (ngo.attendanceRate == null) Color.Gray else if (ngo.attendanceRate < 70) RiskHighRed else RiskLowGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (ngo.attendanceRate == null) "No Data" else if (ngo.attendanceRate < 70) "Attendance Anomaly" else "Attendance Normal",
                            fontSize = 11.sp,
                            color = if (ngo.attendanceRate == null) Color.Gray else if (ngo.attendanceRate < 70) RiskHighRed else TextDarkSecondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Standard Primary Action Button matching Reference UI
 */
@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

/**
 * Real Video Player for CCTV Monitoring Screen
 * Plays local cctv_demo.mp4 bundled in res/raw.
 * If cctv_demo.mp4 is missing or unplayable, explicitly reports "cctv_demo.mp4 is missing."
 */
/**
 * Real Video Player for CCTV Monitoring Screen
 * Plays local bundled MP4 video file from res/raw (e.g. cctv_demo.mp4 or cctv_demo_2.mp4).
 */
@Composable
fun VideoPlayerCanvas(
    videoResName: String = "cctv_demo",
    rawResIdInput: Int = 0,
    modifier: Modifier = Modifier,
    onVideoViewCreated: (VideoView) -> Unit = {}
) {
    val context = LocalContext.current
    var isPlaybackError by remember { mutableStateOf(false) }
    var rawResId by remember(videoResName, rawResIdInput) { mutableStateOf(rawResIdInput) }
    var isPlaceholderTextFile by remember { mutableStateOf(false) }

    LaunchedEffect(videoResName, rawResIdInput) {
        val targetResId = if (rawResIdInput != 0) {
            rawResIdInput
        } else {
            context.resources.getIdentifier(videoResName, "raw", context.packageName)
        }
        rawResId = targetResId
        if (targetResId != 0) {
            try {
                val pfd = context.resources.openRawResourceFd(targetResId)
                if (pfd != null) {
                    val length = pfd.length
                    pfd.close()
                    if (length < 1000) {
                        isPlaceholderTextFile = true
                    }
                }
            } catch (e: Exception) {
                isPlaceholderTextFile = true
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "cctvAnim")
    val scanY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan"
    )

    if (rawResId == 0 || isPlaceholderTextFile || isPlaybackError) {
        // Explicitly report missing mp4
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(230.dp)
                .border(1.dp, LightBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = LightSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VideocamOff,
                    contentDescription = "Missing File",
                    tint = RiskHighRed,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$videoResName.mp4 is missing.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = RiskHighRed
                )
                Text(
                    text = "Please place actual $videoResName.mp4 video file in res/raw/.",
                    fontSize = 11.sp,
                    color = TextDarkSecondary
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(230.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E293B))
        ) {
            // Native Android VideoView centered inside FrameLayout to preserve original aspect ratio
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val frameLayout = FrameLayout(ctx)
                    val videoView = VideoView(ctx).apply {
                        val rawUri = Uri.parse("android.resource://${ctx.packageName}/$rawResId")
                        setVideoURI(rawUri)
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            mp.start()
                        }
                        setOnErrorListener { _, _, _ ->
                            isPlaybackError = true
                            true
                        }
                    }
                    val params = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER
                    )
                    frameLayout.addView(videoView, params)
                    onVideoViewCreated(videoView)
                    frameLayout
                },
                update = { frameLayout ->
                    val videoView = frameLayout.getChildAt(0) as? VideoView
                    val expectedUri = Uri.parse("android.resource://${frameLayout.context.packageName}/$rawResId")
                    if (videoView?.tag != rawResId) {
                        videoView?.tag = rawResId
                        videoView?.setVideoURI(expectedUri)
                        videoView?.setOnPreparedListener { mp ->
                            mp.isLooping = true
                            mp.start()
                        }
                    }
                }
            )

            // Overlay Canvas for AI Scanlines
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawLine(
                    color = PrimaryBlue.copy(alpha = 0.3f),
                    start = Offset(0f, h * scanY),
                    end = Offset(w, h * scanY),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Top Overlay: RECORDED FEED Badge (Not falsely labeled LIVE)
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFD97706))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "RECORDED FEED",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Bottom Overlay Bar: Info & Expand Icon
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REC | 1080p | 30 FPS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Icon(
                    imageVector = Icons.Default.Fullscreen,
                    contentDescription = "Expand",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
