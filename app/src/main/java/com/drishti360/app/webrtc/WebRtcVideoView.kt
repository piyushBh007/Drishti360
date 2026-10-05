package com.drishti360.app.webrtc

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

/**
 * A Compose wrapper around WebRTC's [SurfaceViewRenderer].
 *
 * @param onViewReady Invoked once the native view is created and available.
 * @param isOverlay When true, configures [SurfaceViewRenderer.setZOrderMediaOverlay] so the PIP view
 *                  properly renders on top of underlying SurfaceViews without being clipped or blacked out.
 * @param mirror When true, mirrors the video feed (standard for front-facing selfie preview).
 */
@Composable
fun WebRtcVideoView(
    onViewReady: (SurfaceViewRenderer) -> Unit,
    modifier: Modifier = Modifier,
    isOverlay: Boolean = false,
    mirror: Boolean = false
) {
    AndroidView(
        factory = { ctx ->
            Log.d("Drishti360-Renderer", "Creating SurfaceViewRenderer (isOverlay=$isOverlay, mirror=$mirror)")
            SurfaceViewRenderer(ctx).apply {
                if (isOverlay) {
                    setZOrderMediaOverlay(true)
                }
                if (mirror) {
                    setMirror(true)
                }
                setEnableHardwareScaler(true)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                Log.d("Drishti360-Renderer", "SurfaceViewRenderer created: $this — invoking onViewReady")
                onViewReady(this)
            }
        },
        modifier = modifier
    )
}
