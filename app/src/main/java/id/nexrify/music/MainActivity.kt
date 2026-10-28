package id.nexrify.music

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.runtime.CompositionLocalProvider
import id.nexrify.music.capsule.AppVisibility
import id.nexrify.music.i18n.LocalStrings
import id.nexrify.music.i18n.S
import id.nexrify.music.i18n.Strings
import id.nexrify.music.ui.components.NexLoading
import id.nexrify.music.ui.components.rememberEstimatedProgress
import id.nexrify.music.ui.screens.SearchPageScreen
import id.nexrify.music.ui.theme.FredokaFontFamily
import id.nexrify.music.data.model.Track
import id.nexrify.music.ui.components.FullPlayerSheet
import id.nexrify.music.ui.components.MiniPlayer
import id.nexrify.music.ui.navigation.Screen
import id.nexrify.music.ui.screens.HomeScreen
import id.nexrify.music.ui.screens.LibraryScreen
import id.nexrify.music.ui.screens.SearchScreen
import id.nexrify.music.ui.theme.DarkBorder
import id.nexrify.music.ui.theme.DarkCard
import id.nexrify.music.ui.theme.NexrifyTheme
import id.nexrify.music.ui.theme.MutedText
import id.nexrify.music.ui.theme.PureBlack
import id.nexrify.music.ui.theme.PureWhite
import id.nexrify.music.viewmodel.MusicViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by viewModels()

    override fun onStart() {
        super.onStart()
        AppVisibility.set(true)
    }

    override fun onStop() {
        AppVisibility.set(false)
        super.onStop()
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
            val language by viewModel.settingsManager.language.collectAsState()
            val strings = remember(language) { Strings(viewModel.settingsManager.isEnglish()) }
            val initialLoadDone by viewModel.initialLoadDone.collectAsState()

            var showLaunch by remember { mutableStateOf(true) }
            val loadProgress = rememberEstimatedProgress(active = !initialLoadDone)
            LaunchedEffect(initialLoadDone) {
                if (initialLoadDone) {
                    delay(520) // beri waktu progress mencapai 100% sebelum layar loading hilang
                    showLaunch = false
                }
            }
            LaunchedEffect(Unit) {
                delay(15_000) // jaring pengaman kalau jaringan macet
                showLaunch = false
            }

            CompositionLocalProvider(LocalStrings provides strings) {
                NexrifyTheme(themeMode = themeMode) {
                    Box(Modifier.fillMaxSize()) {
                        MainAppScreen(viewModel = viewModel)
                        AnimatedVisibility(
                            visible = showLaunch,
                            enter = fadeIn(tween(120)),
                            exit = fadeOut(tween(300))
                        ) {
                            val colors = id.nexrify.music.ui.theme.NexrifyAppTheme.colors
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(colors.background)
                                    .padding(horizontal = 44.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                            ) {
                                Text(
                                    text = "Nexrify",
                                    color = colors.textPrimary,
                                    fontFamily = FredokaFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 38.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = S.loadingSub,
                                    color = colors.textMuted,
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                                )
                                Spacer(Modifier.height(36.dp))
                                NexLoading(progress = loadProgress)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: MusicViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isLoadingTrack by viewModel.isLoadingTrack.collectAsState()
    val isExpanded by viewModel.isPlayerExpanded.collectAsState()
    val isShuffle by viewModel.isShuffle.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()

    BackHandler(enabled = isExpanded) {
        viewModel.expandPlayer(false)
    }

    val colors = id.nexrify.music.ui.theme.NexrifyAppTheme.colors

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Scaffold(
            containerColor = colors.background,
            bottomBar = {
                if (currentRoute?.startsWith(Screen.SearchPage.BASE) != true) NavigationBar(
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
                                    contentDescription = screen.title(S)
                                )
                            },
                            label = { Text(screen.title(S)) },
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
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(viewModel = viewModel)
                    }
                    composable(Screen.Search.route) {
                        SearchScreen(onOpenSearchPage = { q -> navController.navigate(Screen.SearchPage.route(q)) })
                    }
                    composable(
                        route = Screen.SearchPage.ROUTE,
                        arguments = listOf(navArgument("q") { type = NavType.StringType; defaultValue = "" })
                    ) { entry ->
                        SearchPageScreen(
                            viewModel = viewModel,
                            initialQuery = entry.arguments?.getString("q").orEmpty(),
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Library.route) {
                        LibraryScreen(viewModel = viewModel)
                    }
                }

                // Floating MiniPlayer above bottom nav bar
                if (currentTrack != null && !isExpanded) {
                    MiniPlayerContainer(
                        viewModel = viewModel,
                        track = currentTrack!!,
                        isPlaying = isPlaying,
                        isLoading = isLoadingTrack,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                    )
                }
            }
        }

        // Full-Screen Player overlay
        AnimatedVisibility(
            visible = isExpanded && currentTrack != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
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
