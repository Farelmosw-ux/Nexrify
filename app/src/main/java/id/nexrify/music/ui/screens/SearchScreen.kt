package id.nexrify.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.nexrify.music.i18n.S
import id.nexrify.music.ui.icons.NexIcons
import id.nexrify.music.ui.theme.NexrifyAppTheme
import id.nexrify.music.ui.theme.Typography

/** Kategori jelajah. Nilainya juga dipakai sebagai kata kunci pencarian. */
val BrowseCategories = listOf(
    "Hits Global",
    "Lo-Fi & Santai",
    "Anime & J-Pop",
    "Fokus Mendalam",
    "Hip-Hop & R&B",
    "Akustik & Folk",
    "Rock & Alternatif",
    "Elektronik & EDM"
)

/**
 * Tab Cari: kotak pencarian yang membuka halaman pencarian sendiri, lalu kategori untuk dijelajahi.
 * Pencarian, riwayat, dan hasilnya ada di [SearchPageScreen].
 */
@Composable
fun SearchScreen(
    onOpenSearchPage: (initialQuery: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NexrifyAppTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
    ) {
        Text(
            text = S.searchTitle,
            style = Typography.headlineLarge,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.card)
                .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                .clickable { onOpenSearchPage("") }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = NexIcons.Search,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = S.searchHint,
                color = colors.textMuted,
                style = Typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = S.exploreMusic,
            style = Typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            modifier = Modifier.padding(top = 22.dp, bottom = 12.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(BrowseCategories) { category ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2.25f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.card)
                        .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                        .clickable { onOpenSearchPage(category) }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = S.categoryLabel(category),
                            style = Typography.titleMedium,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = S.categoryDesc(category),
                            style = Typography.bodySmall,
                            color = colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
