package id.nexrify.music.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import id.nexrify.music.i18n.Strings
import id.nexrify.music.ui.icons.NexIcons

sealed class Screen(val route: String, val icon: ImageVector) {
    object Home : Screen("home", NexIcons.Home)
    object Search : Screen("search", NexIcons.Search)
    object Library : Screen("library", NexIcons.LibraryMusic)

    /** Halaman pencarian tersendiri (riwayat, saran, hasil). Bukan item di bottom bar. */
    object SearchPage {
        const val ROUTE = "search_page?q={q}"
        const val BASE = "search_page"
        fun route(query: String): String = "$BASE?q=${android.net.Uri.encode(query)}"
    }

    fun title(s: Strings): String = when (this) {
        Home -> s.navHome
        Search -> s.navSearch
        Library -> s.navLibrary
    }

    companion object {
        val bottomNavItems = listOf(Home, Search, Library)
    }
}
