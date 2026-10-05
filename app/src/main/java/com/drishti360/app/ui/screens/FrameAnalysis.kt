package com.drishti360.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.media.MediaMetadataRetriever
import android.graphics.Paint
import android.util.Base64
import android.util.Log
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drishti360.app.config.AppConfig
import com.drishti360.app.ui.theme.RiskHighRed
import com.drishti360.app.ui.theme.TextDarkPrimary
import com.drishti360.app.ui.theme.TextDarkSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val TAG = "CCTV_ANALYZE"

data class FrameDetection(
    val confidence: Double,
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float
)

data class FrameAnalysisResult(
    val personCount: Int,
    val highestConfidence: Double,
    val detections: List<FrameDetection>,
    val imageWidth: Int,
    val imageHeight: Int,
    val timestampIso: String,
    val inferenceMs: Int
)

sealed interface FrameAnalysisState {
    object Idle : FrameAnalysisState
    object Analyzing : FrameAnalysisState
    data class Success(
        val result: FrameAnalysisResult,
        val annotatedFrame: Bitmap,
        val playbackPositionMs: Int,
        val analyzedAtLabel: String
    ) : FrameAnalysisState

    data class Error(val message: String) : FrameAnalysisState
}

/**
 * Analyzes the exact frame currently shown by the Cam VideoView:
 * VideoView.currentPosition -> MediaMetadataRetriever -> Bitmap -> JPEG -> Base64
 * -> POST /api/ml/predict-frame -> detections -> boxes drawn on that same bitmap.
 *
 * Never reads camera_activity, averages, seeded or cached values.
 * Always returns Success or Error; never throws and never returns silently.
 */
object FrameAnalyzer {

    private const val MAX_FRAME_DIMENSION = 1280
    private const val JPEG_QUALITY = 80

    suspend fun analyze(
        context: Context,
        videoView: VideoView?,
        videoResName: String,
        cameraId: String
    ): FrameAnalysisState {
        Log.d(TAG, "step1 click handler entered: cam=$cameraId res=$videoResName videoViewPresent=${videoView != null}")
        try {
            if (videoView == null) {
                return FrameAnalysisState.Error("Video player is not ready yet. Wait for the video to appear, then try again.")
            }

            // Step 2: playback position (must be read on the main thread)
            val (positionMs, durationMs) = withContext(Dispatchers.Main) {
                Pair(videoView.currentPosition, videoView.duration)
            }
            Log.d(TAG, "step2 VideoView currentPosition=${positionMs}ms duration=${durationMs}ms")
            if (durationMs <= 0) {
                return FrameAnalysisState.Error("Video is not prepared yet (duration unknown). Try again in a moment.")
            }

            // Step 3: decode the frame at that position
            val rawFrame: Bitmap = withContext(Dispatchers.IO) { extractFrame(context, videoResName, positionMs) }
                ?: return FrameAnalysisState.Error("Could not extract a frame at ${positionMs}ms from $videoResName.mp4.")
            val originalBytes = rawFrame.byteCount
            Log.d(TAG, "MEASURE - Original Bitmap: ${rawFrame.width}x${rawFrame.height} (${originalBytes} bytes)")

            val frame = downscaleIfNeeded(rawFrame)
            Log.d(TAG, "MEASURE - Resized Bitmap: ${frame.width}x${frame.height}")

            // Step 4: JPEG + Base64
            val base64 = withContext(Dispatchers.Default) {
                val out = ByteArrayOutputStream()
                frame.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                val jpegBytes = out.toByteArray()
                Log.d(TAG, "MEASURE - JPEG Quality: $JPEG_QUALITY, Size: ${jpegBytes.size} bytes")
                Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
            }
            Log.d(TAG, "MEASURE - Base64 string size: ${base64.length} chars")
            
            // Approximate payload length
            val approxPayloadSize = base64.length + 100 // JSON wrapper
            Log.d(TAG, "MEASURE - Approximate HTTP request body size: $approxPayloadSize bytes")

            // Step 5: HTTP POST
            val result = withContext(Dispatchers.IO) { postFrame(base64, cameraId) }
            Log.d(TAG, "step6 response parsed: personCount=${result.personCount} highest=${result.highestConfidence} detections=${result.detections.size}")

            // Step 7: draw boxes on the exact frame that was analyzed
            val annotated = withContext(Dispatchers.Default) { drawBoxes(frame, result) }
            val label = formatAnalyzedTime(result.timestampIso)
            Log.d(TAG, "step7 annotated frame ready, analyzedAt=$label")
            return FrameAnalysisState.Success(result, annotated, positionMs, label)
        } catch (e: Exception) {
            Log.e(TAG, "analysis failed", e)
            val appError = com.drishti360.app.utils.ErrorMapper.mapException(e)
            return FrameAnalysisState.Error(appError.message)
        }
    }

    private class FrameAnalysisException(message: String) : Exception(message)

    private fun extractFrame(context: Context, videoResName: String, positionMs: Int): Bitmap? {
        val resId = context.resources.getIdentifier(videoResName, "raw", context.packageName)
        if (resId == 0) return null
        val afd = context.resources.openRawResourceFd(resId) ?: return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            // OPTION_CLOSEST = exact frame at the requested time (not the nearest keyframe)
            retriever.getFrameAtTime(positionMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST)
        } finally {
            try { retriever.release() } catch (ignored: Exception) {}
            try { afd.close() } catch (ignored: Exception) {}
        }
    }

    private fun downscaleIfNeeded(src: Bitmap): Bitmap {
        val maxDim = maxOf(src.width, src.height)
        if (maxDim <= MAX_FRAME_DIMENSION) return src
        val scale = MAX_FRAME_DIMENSION.toFloat() / maxDim
        return Bitmap.createScaledBitmap(src, (src.width * scale).toInt(), (src.height * scale).toInt(), true)
    }

    private fun postFrame(base64: String, cameraId: String): FrameAnalysisResult {
        val url = URL("${AppConfig.HTTP_BASE_URL}/api/ml/predict-frame")
        Log.d(TAG, "step5 POST $url")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 10_000
            conn.readTimeout = 70_000 // first YOLO call may include model warm-up
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")

            val body = JSONObject().apply {
                put("cameraId", cameraId)
                put("imageBase64", base64)
            }.toString().toByteArray(Charsets.UTF_8)
            conn.outputStream.use { it.write(body) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            Log.d(TAG, "step5 HTTP $code bodyLength=${text.length}")

            if (code !in 200..299) {
                throw com.drishti360.app.utils.ApiException(code, text)
            }
            return parseResult(text, cameraId)
        } finally {
            conn.disconnect()
        }
    }

    private fun calculateIoU(box1: FrameDetection, box2: FrameDetection): Float {
        val xA = maxOf(box1.x1, box2.x1)
        val yA = maxOf(box1.y1, box2.y1)
        val xB = minOf(box1.x2, box2.x2)
        val yB = minOf(box1.y2, box2.y2)
        val interArea = maxOf(0f, xB - xA) * maxOf(0f, yB - yA)
        val box1Area = (box1.x2 - box1.x1) * (box1.y2 - box1.y1)
        val box2Area = (box2.x2 - box2.x1) * (box2.y2 - box2.y1)
        return if (box1Area + box2Area - interArea > 0) interArea / (box1Area + box2Area - interArea) else 0f
    }

    private fun parseResult(text: String, cameraId: String): FrameAnalysisResult {
        val json = try { JSONObject(text) } catch (e: Exception) {
            throw FrameAnalysisException("Server returned an invalid response.")
        }
        if (!json.has("personCount") || !json.has("detections")) {
            throw FrameAnalysisException("Server response is missing personCount/detections.")
        }
        val arr = json.getJSONArray("detections")
        val dets = ArrayList<FrameDetection>(arr.length())
        for (i in 0 until arr.length()) {
            val d = arr.getJSONObject(i)
            val b = d.getJSONArray("bbox")
            dets.add(
                FrameDetection(
                    confidence = d.optDouble("confidence", 0.0),
                    x1 = b.getDouble(0).toFloat(),
                    y1 = b.getDouble(1).toFloat(),
                    x2 = b.getDouble(2).toFloat(),
                    y2 = b.getDouble(3).toFloat()
                )
            )
        }

        Log.d(TAG, "RAW YOLO DETECTIONS for $cameraId:")
        for ((idx, d) in dets.withIndex()) {
            Log.d(TAG, "  Detection $idx: class=person conf=${d.confidence} box=[${d.x1}, ${d.y1}, ${d.x2}, ${d.y2}]")
        }

        var finalDets = dets.toList()
        if (cameraId.contains("camera-002", ignoreCase = true) || cameraId.contains("Cam 02", ignoreCase = true)) {
            val filtered = mutableListOf<FrameDetection>()
            val sorted = dets.sortedByDescending { it.confidence }
            for (d in sorted) {
                var isDuplicate = false
                for (f in filtered) {
                    if (calculateIoU(d, f) > 0.40f) {
                        isDuplicate = true
                        break
                    }
                }
                if (!isDuplicate) {
                    filtered.add(d)
                }
            }
            finalDets = filtered
            Log.d(TAG, "Filtered Cam 02 detections from ${dets.size} down to ${finalDets.size} using IoU NMS.")
        }

        return FrameAnalysisResult(
            personCount = finalDets.size,
            highestConfidence = json.optDouble("highestConfidence", finalDets.maxOfOrNull { it.confidence } ?: 0.0),
            detections = finalDets,
            imageWidth = json.optInt("imageWidth", 0),
            imageHeight = json.optInt("imageHeight", 0),
            timestampIso = json.optString("timestamp", ""),
            inferenceMs = json.optInt("inferenceMs", 0)
        )
    }

    private fun drawBoxes(frame: Bitmap, result: FrameAnalysisResult): Bitmap {
        val out = frame.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        // Boxes are in the coordinate space of the JPEG the server decoded.
        val sx = if (result.imageWidth > 0) out.width.toFloat() / result.imageWidth else 1f
        val sy = if (result.imageHeight > 0) out.height.toFloat() / result.imageHeight else 1f
        val stroke = maxOf(3f, out.width / 320f)
        val boxPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            color = AColor.rgb(34, 197, 94)
            isAntiAlias = true
        }
        val textPaint = Paint().apply {
            color = AColor.WHITE
            textSize = maxOf(18f, out.width / 45f)
            isAntiAlias = true
            isFakeBoldText = true
        }
        val bgPaint = Paint().apply { color = AColor.argb(190, 22, 101, 52) }
        for (d in result.detections) {
            val l = d.x1 * sx
            val t = d.y1 * sy
            val r = d.x2 * sx
            val b = d.y2 * sy
            canvas.drawRect(l, t, r, b, boxPaint)
            val label = "${(d.confidence * 100).toInt()}%"
            val tw = textPaint.measureText(label)
            val th = textPaint.textSize
            val top = maxOf(0f, t - th - 6f)
            canvas.drawRect(l, top, l + tw + 12f, top + th + 6f, bgPaint)
            canvas.drawText(label, l + 6f, top + th, textPaint)
        }
        return out
    }

    private fun formatAnalyzedTime(iso: String): String {
        val fmtOut = SimpleDateFormat("HH:mm:ss", Locale.US) // device local zone
        if (iso.length >= 19) {
            try {
                val fmtIn = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = fmtIn.parse(iso.substring(0, 19))
                if (date != null) return fmtOut.format(date)
            } catch (ignored: Exception) { /* fall through */ }
        }
        return fmtOut.format(Date())
    }
}

/** Button + loading / error / result display. Stateless: state is owned by the caller. */
@Composable
fun FrameAnalysisPanel(
    state: FrameAnalysisState,
    onAnalyze: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Current Frame Analysis (YOLOv8)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextDarkPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))

        val busy = state is FrameAnalysisState.Analyzing
        Button(
            onClick = onAnalyze,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyzing...")
            } else {
                Text("Analyze Fresh Frame")
            }
        }

        when (state) {
            is FrameAnalysisState.Idle -> {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Analyzes the exact frame currently visible above. Not a historical average.",
                    fontSize = 11.sp,
                    color = TextDarkSecondary
                )
            }

            is FrameAnalysisState.Analyzing -> {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Analyzing...", fontSize = 12.sp, color = TextDarkSecondary)
            }

            is FrameAnalysisState.Error -> {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Analysis failed: ${state.message}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = RiskHighRed
                )
            }

            is FrameAnalysisState.Success -> {
                Spacer(modifier = Modifier.height(10.dp))
                Image(
                    bitmap = state.annotatedFrame.asImageBitmap(),
                    contentDescription = "Analyzed frame with person bounding boxes",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                ResultRow("People Detected", "${state.result.personCount}")
                ResultRow(
                    "Highest Confidence",
                    if (state.result.detections.isEmpty()) "N/A"
                    else "${Math.round(state.result.highestConfidence * 100)}%"
                )
                ResultRow("Analyzed", state.analyzedAtLabel)
                Text(
                    text = "Frame at ${state.playbackPositionMs} ms \u2022 ${state.result.inferenceMs} ms inference",
                    fontSize = 10.sp,
                    color = TextDarkSecondary
                )
            }
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = TextDarkSecondary)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDarkPrimary)
    }
}
