package id.nexrify.music.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp

/** Pull-to-refresh sederhana yang bekerja dengan LazyColumn/LazyRow nested scroll. */
@Composable
fun PullToRefreshBox(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val refreshCallback by rememberUpdatedState(onRefresh)
    val refreshingState by rememberUpdatedState(refreshing)
    var dragDistance by remember { mutableFloatStateOf(0f) }
    val threshold = 84.dp.value * androidx.compose.ui.platform.LocalDensity.current.density
    val connection = remember(threshold) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (!refreshingState && available.y > 0f) {
                    dragDistance = (dragDistance + available.y).coerceAtMost(threshold * 1.5f)
                } else if (available.y < 0f) {
                    dragDistance = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!refreshingState && dragDistance >= threshold) refreshCallback()
                dragDistance = 0f
                return Velocity.Zero
            }
        }
    }

    Box(modifier = modifier.nestedScroll(connection)) {
        content()
        if (refreshing) {
            Box(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            }
        }
    }
}
