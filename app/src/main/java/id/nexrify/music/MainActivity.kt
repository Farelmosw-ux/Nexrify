package id.nexrify.music

import id.nexrify.music.ui.components.CrashReportDialog

import id.nexrify.music.util.CrashReporter

import id.nexrify.music.ui.screens.AuthScreen

import id.nexrify.music.data.account.AccountSync

import id.nexrify.music.data.account.AccountManager

import id.nexrify.music.ui.screens.MailboxScreen

import id.nexrify.music.ui.components.UpdateDialog

import id.nexrify.music.data.mailbox.MailboxManager

import id.nexrify.music.data.update.UpdateManager

import android.net.Uri

import android.content.Intent

import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.border
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset

import androidx.compose.foundation.layout.Row

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
import androidx.compose.animation.core.animateDpAsState
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
import kotlin.math.abs
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
import id.nexrify.music.ui.screens.ChatScreen
import id.nexrify.music.ui.screens.DonateScreen
import id.nexrify.music.ui.screens.FeedbackScreen
import id.nexrify.music.viewmodel.MusicViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by lazy {
        androidx.lifecycle.ViewModelProvider(
            application as androidx.lifecycle.ViewModelStoreOwner,
            androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[MusicViewModel::class.java]
    }

    override fun onStart() {
        super.onStart()
        AppVisibility.set(true)
    }

    override fun onStop() {
        AppVisibility.set(false)
        super.onStop()
    }

    private fun handleChatIntent(intent: android.content.Intent?) {
        if (intent?.getBooleanExtra(id.nexrify.music.data.chat.ChatNotifier.EXTRA_OPEN_CHAT, false) == true) {
            id.nexrify.music.data.chat.ChatNav.request.value = intent.getLongExtra(id.nexrify.music.data.chat.ChatNotifier.EXTRA_SEQ, 0L)
            intent.removeExtra(id.nexrify.music.data.chat.ChatNotifier.EXTRA_OPEN_CHAT)
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleChatIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleChatIntent(intent)

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

            val appContext = androidx.compose.ui.platform.LocalContext.current
            val updateManager = remember { UpdateManager.getInstance() }
            val availableUpdate by updateManager.available.collectAsState()
            // Naik setiap aplikasi kembali ke depan, supaya rilis baru (terutama yang wajib) terdeteksi lagi.
            var resumeTick by remember { mutableStateOf(0) }
            androidx.compose.runtime.DisposableEffect(appContext) {
                val owner = appContext as? androidx.lifecycle.LifecycleOwner
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) resumeTick++
                }
                owner?.lifecycle?.addObserver(observer)
                onDispose { owner?.lifecycle?.removeObserver(observer) }
            }
            LaunchedEffect(resumeTick) {
                // Deteksi rilis baru dan ambil kotak surat lewat website Nexrify.
                updateManager.checkOnLaunch()
                MailboxManager.getInstance().refresh()
            }

            var crashReport by remember { mutableStateOf(CrashReporter.pending(appContext)) }

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
                NexrifyTheme(themeMode = themeMode, themeStyle = viewModel.themeStyle.collectAsState().value) {
                    Box(Modifier.fillMaxSize()) {
                        crashReport?.let { report ->
                            CrashReportDialog(report = report, onClose = { CrashReporter.clear(appContext); crashReport = null })
                        }
                        val nexUser by AccountManager.getInstance().user.collectAsState()
                        LaunchedEffect(nexUser?.id) {
                            if (nexUser != null) {
                                // Segarkan profil dan tarik data akun (tidak mengganggu kalau offline).
                                AccountManager.getInstance().refresh()
                                AccountSync.getInstance().initialSync(preferLocal = false)
                                MailboxManager.getInstance().refresh()
                            }
                        }
                        if (nexUser == null) AuthScreen() else MainAppScreen(viewModel = viewModel)
                        if (nexUser != null) availableUpdate?.let { release ->
                            UpdateDialog(
                                release = release,
                                onLater = { updateManager.later(release) },
                                onUpdate = {
                                    runCatching {
                                        appContext.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(release.downloadUrl))
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    }
                                    if (!release.mandatory) updateManager.dismissWithoutSnooze()
                                }
                            )
                        }
                        if (nexUser != null && !showLaunch) {
                            // Wajib izin lokasi untuk Maghrib, lalu pantau waktu Maghrib secara real-time.
                            id.nexrify.music.ui.components.LocationPermissionPrompt()
                            id.nexrify.music.ui.components.MaghribReminderHost(viewModel)
                        }
                        // Popup pemberitahuan bergaya untuk semua fitur.
                        id.nexrify.music.ui.components.NexNotificationHost()
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

    // Dibuka dari notifikasi Global Chat: pindah ke layar chat dan sorot pesannya.
    val chatRequest by id.nexrify.music.data.chat.ChatNav.request.collectAsState()
    LaunchedEffect(chatRequest) {
        val seq = chatRequest ?: return@LaunchedEffect
        id.nexrify.music.data.chat.ChatNav.request.value = null
        if (id.nexrify.music.data.account.AccountManager.getInstance().isLoggedIn()) navController.navigate("chat?seq=$seq") { launchSingleTop = true }
    }

    val currentTrack by viewModel.currentTrack.collectAsState()
    // Mini player yang ditutup pengguna: disembunyikan untuk lagu ini, muncul lagi saat lagu berganti.
    var dismissedTrackId by remember { mutableStateOf<String?>(null) }
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
                val themeStyle by id.nexrify.music.data.settings.SettingsManager.getInstance().themeStyle.collectAsState()
                val glassNavigation = themeStyle == id.nexrify.music.data.settings.ThemeStyle.LIQUID_GLASS || themeStyle == id.nexrify.music.data.settings.ThemeStyle.LIQUID_GLASS_V2
                val glassNavigationV2 = themeStyle == id.nexrify.music.data.settings.ThemeStyle.LIQUID_GLASS_V2
                if (currentRoute?.startsWith(Screen.SearchPage.BASE) != true && currentRoute != "mailbox" && currentRoute != "donate" && currentRoute != "feedback" && currentRoute?.startsWith("chat") != true) {
                    if (glassNavigation) {
                        val navItems = Screen.bottomNavItems
                        val selectedIndex = navItems.indexOfFirst { currentRoute == it.route }.coerceAtLeast(0)
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .height(if (glassNavigationV2) 74.dp else 68.dp)
                                .clip(RoundedCornerShape(if (glassNavigationV2) 30.dp else 24.dp))
                                .background(if (glassNavigationV2) Brush.linearGradient(listOf(Color(0xE63B4D68), Color(0xD51A2A40), Color(0xE6445572))) else Brush.linearGradient(listOf(Color(0xD5263548), Color(0xBC172536), Color(0xD5334357))))
                                .border(1.dp, if (glassNavigationV2) Color(0x99B9D4FF) else Color(0x668BA7C8), RoundedCornerShape(if (glassNavigationV2) 30.dp else 24.dp))
                                .pointerInput(currentRoute, navItems.size) {
                                    var dragDistance = 0f
                                    detectHorizontalDragGestures(
                                        onHorizontalDrag = { _, amount -> dragDistance += amount },
                                        onDragEnd = {
                                            if (kotlin.math.abs(dragDistance) > 55f) {
                                                val next = (selectedIndex + if (dragDistance < 0f) 1 else -1).coerceIn(0, navItems.lastIndex)
                                                navItems.getOrNull(next)?.let { target ->
                                                    navController.navigate(target.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            }
                                            dragDistance = 0f
                                        }
                                    )
                                }
                        ) {
                            val itemWidth = maxWidth / navItems.size
                            val indicatorOffset by animateDpAsState(
                                targetValue = itemWidth * selectedIndex,
                                label = "glass-navigation-indicator"
                            )
                            Box(
                                modifier = Modifier
                                    .offset(x = indicatorOffset)
                                    .width(itemWidth)
                                    .fillMaxSize()
                                    .padding(if (glassNavigationV2) 4.dp else 5.dp)
                                    .clip(RoundedCornerShape(if (glassNavigationV2) 25.dp else 19.dp))
                                    .blur(if (Build.VERSION.SDK_INT >= 33) if (glassNavigationV2) 13.dp else 8.dp else 0.dp)
                                    .background(
                                        Brush.linearGradient(
                                            if (glassNavigationV2) listOf(Color(0x88B9D4FF), Color(0x555D83B5), Color(0x88D7E8FF))
                                            else listOf(Color(0x668BA7C8), Color(0x443B526A), Color(0x668BA7C8))
                                        )
                                    )
                                    .border(1.dp, if (glassNavigationV2) Color(0x88D8E8FF) else Color(0x668BA7C8), RoundedCornerShape(if (glassNavigationV2) 25.dp else 19.dp))
                            )
                            Row(Modifier.fillMaxSize()) {
                                navItems.forEach { screen ->
                                    val selected = currentRoute == screen.route
                                    Column(
                                        modifier = Modifier.weight(1f).fillMaxSize()
                                            .clickable {
                                                navController.navigate(screen.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = screen.icon,
                                            contentDescription = screen.title(S),
                                            tint = if (selected) colors.textPrimary else colors.textMuted
                                        )
                                        Spacer(Modifier.height(3.dp))
                                        Text(
                                            screen.title(S),
                                            color = if (selected) colors.textPrimary else colors.textMuted,
                                            fontSize = 10.sp,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        NavigationBar(containerColor = colors.background, tonalElevation = 0.dp) {
                            Screen.bottomNavItems.forEach { screen ->
                                val selected = currentRoute == screen.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(imageVector = screen.icon, contentDescription = screen.title(S)) },
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
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    val online by viewModel.isOnline.collectAsState()
                    if (!online) {
                        val bannerColors = id.nexrify.music.ui.theme.NexrifyAppTheme.colors
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bannerColors.card)
                                .clickable {
                                    viewModel.requestOfflineTab()
                                    navController.navigate(Screen.Library.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = S.offlineBanner,
                                color = bannerColors.textSecondary,
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = S.offlineBannerAction,
                                color = bannerColors.textPrimary,
                                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(
                        bottom = if (currentRoute?.startsWith("chat") == true && currentTrack != null && !isExpanded && currentTrack!!.id != dismissedTrackId) 84.dp else 0.dp
                    )
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(
                            viewModel = viewModel,
                            onOpenMailbox = { navController.navigate("mailbox") },
                            onOpenChat = { navController.navigate("chat?seq=0") },
                            onOpenDonate = { navController.navigate("donate") },
                            onOpenFeedback = { navController.navigate("feedback") }
                        )
                    }
                    composable("donate") {
                        DonateScreen(onBack = { navController.popBackStack() })
                    }
                    composable("mailbox") {
                        MailboxScreen(onBack = { navController.popBackStack() })
                    }
                    composable(
                        route = "chat?seq={seq}",
                        arguments = listOf(navArgument("seq") { type = NavType.LongType; defaultValue = 0L })
                    ) { entry ->
                        ChatScreen(
                            viewModel = viewModel,
                            highlightSeq = entry.arguments?.getLong("seq") ?: 0L,
                            onBack = { navController.popBackStack() },
                            onOpenFeedback = { navController.navigate("feedback") }
                        )
                    }
                    composable("feedback") {
                        FeedbackScreen(onBack = { navController.popBackStack() })
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
                }

                // Floating MiniPlayer above bottom nav bar
                if (currentTrack != null && !isExpanded && currentTrack!!.id != dismissedTrackId) {
                    MiniPlayerContainer(
                        viewModel = viewModel,
                        track = currentTrack!!,
                        isPlaying = isPlaying,
                        isLoading = isLoadingTrack,
                        onDismiss = { dismissedTrackId = currentTrack?.id },
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
    onDismiss: (() -> Unit)? = null,
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
        onDismiss = onDismiss,
        modifier = modifier
    )
}
