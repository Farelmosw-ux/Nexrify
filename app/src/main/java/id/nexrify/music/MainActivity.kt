package id.nexrify.music

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import id.nexrify.music.data.model.Track
import id.nexrify.music.ui.components.FullPlayerSheet
import id.nexrify.music.ui.components.MiniPlayer
import id.nexrify.music.ui.components.NexrifySplashScreen
import id.nexrify.music.ui.components.QueueScreen
import id.nexrify.music.ui.navigation.Screen
import id.nexrify.music.ui.screens.HomeScreen
import id.nexrify.music.ui.screens.LibraryScreen
import id.nexrify.music.ui.screens.SearchScreen
import id.nexrify.music.ui.theme.NexrifyTheme
import id.nexrify.music.viewmodel.MusicViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import id.nexrify.music.ui.theme.NexrifyAppTheme

class MainActivity : ComponentActivity() {

    // ViewModel dipegang Application agar queue & kontrol tetap hidup saat Activity ditutup.
    private val viewModel: MusicViewModel by lazy {
        (application as NexrifyApp).musicViewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) {}.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            NexrifyTheme(themeMode = themeMode) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

private val tabOrder = listOf(Screen.Home.route, Screen.Search.route, Screen.Library.route)

/** Arah slide mengikuti urutan tab: ke kanan -> masuk dari kanan, ke kiri -> masuk dari kiri. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabDirection(): AnimatedContentTransitionScope.SlideDirection {
    val from = tabOrder.indexOf(initialState.destination.route)
    val to = tabOrder.indexOf(targetState.destination.route)
    return if (to >= from) AnimatedContentTransitionScope.SlideDirection.Left
    else AnimatedContentTransitionScope.SlideDirection.Right
}

@Composable
fun MainAppScreen(viewModel: MusicViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isLoadingTrack by viewModel.isLoadingTrack.collectAsState()
    val isExpanded by viewModel.isPlayerExpanded.collectAsState()
    val isShuffle by viewModel.isShuffle.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val isQueueVisible by viewModel.isQueueVisible.collectAsState()

    // ---- Loading screen (sekali per proses) ----
    var splashVisible by remember { mutableStateOf(!NexrifyApp.splashShown) }
    LaunchedEffect(Unit) {
        if (splashVisible) {
            delay(1400) // beri waktu logo fade-in selesai
            // Tunggu katalog selesai dimuat (maks. 7 detik agar tidak menggantung saat offline)
            withTimeoutOrNull(7000L) { viewModel.isLoadingTrending.first { !it } }
            NexrifyApp.splashShown = true
            splashVisible = false
        }
    }

    // ---- Pesan singkat dari ViewModel (antrean, playlist, dll.) ----
    LaunchedEffect(Unit) {
        viewModel.uiMessages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler(enabled = isExpanded) {
        viewModel.expandPlayer(false)
    }
    BackHandler(enabled = isQueueVisible) {
        viewModel.showQueue(false)
    }

    val colors = NexrifyAppTheme.colors

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Scaffold(
            containerColor = colors.background,
            bottomBar = {
                NavigationBar(
                    containerColor = colors.background,
                    tonalElevation = 0.dp
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = colors.textPrimary,
                                selectedTextColor = colors.textPrimary,
                                unselectedIconColor = colors.textMuted,
                                unselectedTextColor = colors.textMuted,
                                indicatorColor = colors.card
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier.fillMaxSize(),
                    // Transisi fade + slide antar tab
                    enterTransition = {
                        fadeIn(tween(300, easing = FastOutSlowInEasing)) +
                            slideIntoContainer(tabDirection(), tween(300, easing = FastOutSlowInEasing)) { it / 6 }
                    },
                    exitTransition = {
                        fadeOut(tween(200)) +
                            slideOutOfContainer(tabDirection(), tween(300, easing = FastOutSlowInEasing)) { it / 6 }
                    },
                    popEnterTransition = {
                        fadeIn(tween(300, easing = FastOutSlowInEasing)) +
                            slideIntoContainer(tabDirection(), tween(300, easing = FastOutSlowInEasing)) { it / 6 }
                    },
                    popExitTransition = {
                        fadeOut(tween(200)) +
                            slideOutOfContainer(tabDirection(), tween(300, easing = FastOutSlowInEasing)) { it / 6 }
                    }
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(
                            viewModel = viewModel,
                            onOpenPlaylist = { playlistId ->
                                viewModel.openPlaylist(playlistId)
                                navController.navigate(Screen.Library.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                    composable(Screen.Search.route) {
                        SearchScreen(viewModel = viewModel)
                    }
                    composable(Screen.Library.route) {
                        LibraryScreen(viewModel = viewModel)
                    }
                }

                // Mini Player: muncul (slide + fade) saat lagu mulai diputar
                AnimatedVisibility(
                    visible = currentTrack != null && !isExpanded,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                    enter = slideInVertically(tween(350, easing = FastOutSlowInEasing)) { it / 2 } +
                        fadeIn(tween(300)),
                    exit = slideOutVertically(tween(250)) { it / 2 } + fadeOut(tween(200))
                ) {
                    currentTrack?.let { track ->
                        MiniPlayerContainer(
                            viewModel = viewModel,
                            track = track,
                            isPlaying = isPlaying,
                            isLoading = isLoadingTrack
                        )
                    }
                }
            }
        }

        // Now Playing: Mini Player berkembang menjadi layar penuh
        AnimatedVisibility(
            visible = isExpanded && currentTrack != null,
            enter = slideInVertically(tween(400, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(250)),
            exit = slideOutVertically(tween(320, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(250))
        ) {
            currentTrack?.let { track ->
                FullPlayerSheet(
                    track = track,
                    viewModel = viewModel,
                    isPlaying = isPlaying,
                    isLoading = isLoadingTrack,
                    isShuffle = isShuffle,
                    repeatMode = repeatMode,
                    onPlayPauseClick = { viewModel.togglePlayPause() },
                    onSkipNextClick = { viewModel.skipNext() },
                    onSkipPreviousClick = { viewModel.skipPrevious() },
                    onShuffleClick = { viewModel.toggleShuffle() },
                    onRepeatClick = { viewModel.toggleRepeat() },
                    onFavoriteClick = { viewModel.toggleFavorite(track) },
                    onDismiss = { viewModel.expandPlayer(false) }
                )
            }
        }

        // Smart Queue
        AnimatedVisibility(
            visible = isQueueVisible,
            enter = slideInVertically(tween(350, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(250)),
            exit = slideOutVertically(tween(300, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(200))
        ) {
            QueueScreen(
                viewModel = viewModel,
                onClose = { viewModel.showQueue(false) }
            )
        }

        // Loading screen: tampil di paling atas, lalu fade halus menuju Home
        AnimatedVisibility(
            visible = splashVisible,
            enter = EnterTransition.None,
            exit = fadeOut(tween(500)) + scaleOut(tween(500), targetScale = 1.04f)
        ) {
            // progress = null: durasi muat katalog tidak bisa diukur, jadi persentase tidak ditampilkan.
            NexrifySplashScreen(progress = null)
        }
    }
}

@Composable
fun MiniPlayerContainer(
    viewModel: MusicViewModel,
    track: Track,
    isPlaying: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val currentPosMs by viewModel.playbackPosition.collectAsState()
    val durMs by viewModel.duration.collectAsState()
    val effDur = if (durMs > 0) durMs else (track.durationSeconds * 1000L).coerceAtLeast(1L)
    val progressFloat = (currentPosMs.toFloat() / effDur.toFloat()).coerceIn(0f, 1f)

    MiniPlayer(
        track = track,
        isPlaying = isPlaying,
        isLoading = isLoading,
        progress = progressFloat,
        onPlayPauseClick = { viewModel.togglePlayPause() },
        onSkipNextClick = { viewModel.skipNext() },
        onClick = { viewModel.expandPlayer(true) },
        modifier = modifier
    )
}
