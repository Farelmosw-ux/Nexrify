package id.nexrify.music.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.core.content.ContextCompat
import id.nexrify.music.data.account.NexUser
import id.nexrify.music.i18n.S
import id.nexrify.music.ui.icons.NexIcons

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Foto profil layar penuh dengan jepit untuk zoom. Jendelanya memakai FLAG_SECURE, jadi
 * screenshot dan rekam layar diblokir sistem. Di Android 14 ke atas, percobaan tangkap layar
 * juga memunculkan pesan dari app.
 */
@Composable
fun AvatarViewerDialog(user: NexUser?, onDismiss: () -> Unit) {
    val bitmap = remember(user?.avatar) { decodeAvatar(user?.avatar) }
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val activity = context.findActivity()
        var callback: Activity.ScreenCaptureCallback? = null
        if (Build.VERSION.SDK_INT >= 34 && activity != null) {
            val cb = Activity.ScreenCaptureCallback {
                Toast.makeText(context, S.screenshotBlocked, Toast.LENGTH_LONG).show()
            }
            try {
                activity.registerScreenCaptureCallback(ContextCompat.getMainExecutor(context), cb)
                callback = cb
            } catch (_: Throwable) {
            }
        }
        onDispose {
            val cb = callback
            if (Build.VERSION.SDK_INT >= 34 && activity != null && cb != null) {
                try {
                    activity.unregisterScreenCaptureCallback(cb)
                } catch (_: Throwable) {
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            securePolicy = SecureFlagPolicy.SecureOn
        )
    ) {
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.96f))
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = user?.display,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 4f)
                                offset = if (scale <= 1f) Offset.Zero else offset + pan
                            }
                        }
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(NexIcons.CloseCircle, contentDescription = S.cd("Back"), tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Text(
                text = S.avatarProtectedNote,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 32.dp, vertical = 28.dp)
            )
        }
    }
}
