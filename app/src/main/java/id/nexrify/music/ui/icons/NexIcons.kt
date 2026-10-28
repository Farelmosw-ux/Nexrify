package id.nexrify.music.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Ikon gaya Iconsax (linear, garis 1.5, ujung membulat, grid 24).
 * Digambar ulang sebagai vector Compose supaya tidak butuh dependency tambahan.
 * Mau pakai paket resmi Iconsax? Cukup ganti isi object ini, nama propertinya sudah sama dengan yang dipakai di UI.
 */
object NexIcons {

    private fun icon(
        name: String,
        vararg paths: String,
        strokeWidth: Float = 1.5f,
        filled: Boolean = false
    ): ImageVector {
        val builder = ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        paths.forEach { d ->
            builder.addPath(
                pathData = addPathNodes(d),
                fill = if (filled) SolidColor(Color.Black) else null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = strokeWidth,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }
        return builder.build()
    }

    private fun circle(cx: Float, cy: Float, r: Float): String =
        "M${cx - r} $cy A$r $r 0 1 0 ${cx + r} $cy A$r $r 0 1 0 ${cx - r} $cy Z"

    private const val HEART =
        "M12 20 C5 15.5 3 12 3 8.8 C3 6.2 5 4.5 7.3 4.5 C9.3 4.5 11 5.6 12 7.2 C13 5.6 14.7 4.5 16.7 4.5 C19 4.5 21 6.2 21 8.8 C21 12 19 15.5 12 20 Z"

    val Home by lazy {
        icon(
            "Home",
            "M3 10.5 L12 3 L21 10.5 V19 A2 2 0 0 1 19 21 H5 A2 2 0 0 1 3 19 Z",
            "M9.5 21 V15.5 A1 1 0 0 1 10.5 14.5 H13.5 A1 1 0 0 1 14.5 15.5 V21"
        )
    }
    val Search by lazy { icon("Search", circle(11f, 11f, 8f), "M21 21 L16.8 16.8") }
    val LibraryMusic by lazy {
        icon(
            "LibraryMusic",
            "M5 3 H19 A2 2 0 0 1 21 5 V19 A2 2 0 0 1 19 21 H5 A2 2 0 0 1 3 19 V5 A2 2 0 0 1 5 3 Z",
            "M10 16 V8.5 L16 7 V14.5",
            circle(8.5f, 16f, 1.5f),
            circle(14.5f, 14.5f, 1.5f)
        )
    }
    val QueueMusic by lazy {
        icon(
            "QueueMusic",
            "M3 6 H14", "M3 11 H11", "M3 16 H9",
            "M17.5 16 V4.5 L21 6",
            circle(15.5f, 16f, 2f)
        )
    }
    val Translate by lazy {
        icon(
            "Translate",
            "M3 6 H12", "M7.5 4 V6",
            "M5 6 C5.5 9.5 8 12 11 13.5", "M10 6 C9.5 9.5 7 12 4 13.5",
            "M12.5 20 L16.5 10 L20.5 20", "M14 16.5 H19"
        )
    }
    val Check by lazy { icon("Check", "M4.5 12.5 L9.5 17.5 L19.5 7") }
    val Add by lazy { icon("Add", "M12 5 V19", "M5 12 H19") }
    val Tune by lazy {
        icon(
            "Tune",
            "M4 7 H12", "M16 7 H20", "M12 5 V9", "M16 5 V9",
            "M4 17 H8", "M12 17 H20", "M8 15 V19", "M12 15 V19"
        )
    }
    val History by lazy {
        icon(
            "History",
            "M3.5 12 A8.5 8.5 0 1 0 6 6", "M3.5 4 V8.5 H8", "M12 7.5 V12 L15 14"
        )
    }
    val FavoriteBorder by lazy { icon("FavoriteBorder", HEART) }
    val Favorite by lazy { icon("Favorite", HEART, filled = true) }
    val BarChart by lazy { icon("BarChart", "M5 20 V13", "M12 20 V4", "M19 20 V9", strokeWidth = 2.4f) }
    val AccountCircle by lazy {
        icon(
            "AccountCircle",
            circle(12f, 8f, 3.5f),
            "M4.5 20.5 C5.5 16.5 8.5 15 12 15 C15.5 15 18.5 16.5 19.5 20.5"
        )
    }
    val Shuffle by lazy {
        icon(
            "Shuffle",
            "M3 7 H6 C12 7 12 17 18 17 H21", "M18 14 L21 17 L18 20",
            "M3 17 H6 C8 17 9.2 16 10 14.8", "M14 9.2 C14.8 8 16 7 18 7 H21", "M18 4 L21 7 L18 10"
        )
    }
    val MoreVert by lazy {
        icon("MoreVert", "M12 5 V5.1", "M12 12 V12.1", "M12 19 V19.1", strokeWidth = 2.6f)
    }
    val KeyboardArrowDown by lazy { icon("KeyboardArrowDown", "M5 9 L12 16 L19 9") }
    val KeyboardArrowUp by lazy { icon("KeyboardArrowUp", "M5 15 L12 8 L19 15") }
    val Close by lazy { icon("Close", "M6 6 L18 18", "M18 6 L6 18") }
    val Clear by lazy { icon("Clear", "M6 6 L18 18", "M18 6 L6 18") }
    val CloseCircle by lazy { icon("CloseCircle", circle(12f, 12f, 9f), "M9 9 L15 15", "M15 9 L9 15") }
    val VolumeUp by lazy {
        icon(
            "VolumeUp",
            "M3 10 V14 H7 L12 18 V6 L7 10 Z",
            "M15.5 9 C16.8 10.5 16.8 13.5 15.5 15",
            "M18 6.5 C20.7 9.5 20.7 14.5 18 17.5"
        )
    }
    val Share by lazy {
        icon(
            "Share",
            circle(18f, 5.5f, 2.5f), circle(6f, 12f, 2.5f), circle(18f, 18.5f, 2.5f),
            "M8.2 10.8 L15.8 6.7", "M8.2 13.2 L15.8 17.3"
        )
    }
    val Settings by lazy {
        icon(
            "Settings",
            "M12 2.8 L19.9 7.4 V16.6 L12 21.2 L4.1 16.6 V7.4 Z",
            circle(12f, 12f, 3f)
        )
    }
    val Palette by lazy {
        icon(
            "Palette",
            "M12 3 C7 3 3 7 3 12 C3 17 7 21 12 21 C13.5 21 14 20 14 19 C14 18 13 17.5 13 16.5 C13 15.5 14 15 15 15 H17 C19.2 15 21 13.2 21 11 C21 6.5 17 3 12 3 Z",
            "M7.5 11 V11.1", "M10 7.5 V7.6", "M14.5 7.5 V7.6",
            strokeWidth = 1.8f
        )
    }
    val Logout by lazy {
        icon("Logout", "M9 4 H5 A2 2 0 0 0 3 6 V18 A2 2 0 0 0 5 20 H9", "M16 8 L20 12 L16 16", "M20 12 H9")
    }
    val Info by lazy { icon("Info", circle(12f, 12f, 9f), "M12 11 V16", "M12 7.8 V7.9") }
    val Headphones by lazy {
        icon(
            "Headphones",
            "M4 15 V12 A8 8 0 0 1 20 12 V15",
            "M4 14 H6.5 A1 1 0 0 1 7.5 15 V18 A1 1 0 0 1 6.5 19 H5 A1 1 0 0 1 4 18 Z",
            "M20 14 H17.5 A1 1 0 0 0 16.5 15 V18 A1 1 0 0 0 17.5 19 H19 A1 1 0 0 0 20 18 Z"
        )
    }
    val GraphicEq by lazy {
        icon("GraphicEq", "M4 10 V14", "M8 6 V18", "M12 3 V21", "M16 7 V17", "M20 10 V14")
    }
    val DeleteOutline by lazy {
        icon(
            "DeleteOutline",
            "M3 6 H21",
            "M8 6 V4 A1 1 0 0 1 9 3 H15 A1 1 0 0 1 16 4 V6",
            "M5.5 6 L6.5 19.5 A2 2 0 0 0 8.5 21 H15.5 A2 2 0 0 0 17.5 19.5 L18.5 6",
            "M10 11 V17", "M14 11 V17"
        )
    }
    val ChatBubbleOutline by lazy {
        icon("ChatBubbleOutline", "M4 5 A2 2 0 0 1 6 3 H18 A2 2 0 0 1 20 5 V14 A2 2 0 0 1 18 16 H11 L6 20.5 V16 A2 2 0 0 1 4 14 Z")
    }
    val BluetoothAudio by lazy { icon("BluetoothAudio", "M7 7.5 L17 16.5 L12 21 V3 L17 7.5 L7 16.5") }
    val Sync by lazy {
        icon(
            "Sync",
            "M20 11 A8 8 0 0 0 5.5 7.5", "M20 4 V11 H13",
            "M4 13 A8 8 0 0 0 18.5 16.5", "M4 20 V13 H11"
        )
    }
    val Repeat by lazy {
        icon(
            "Repeat",
            "M3 11 V9 A3 3 0 0 1 6 6 H20", "M17 3 L20 6 L17 9",
            "M21 13 V15 A3 3 0 0 1 18 18 H4", "M7 21 L4 18 L7 15"
        )
    }
    val RepeatOne by lazy {
        icon(
            "RepeatOne",
            "M3 11 V9 A3 3 0 0 1 6 6 H20", "M17 3 L20 6 L17 9",
            "M21 13 V15 A3 3 0 0 1 18 18 H4", "M7 21 L4 18 L7 15",
            "M10.5 10.5 L12 9.5 V14.5"
        )
    }
    val Refresh by lazy { icon("Refresh", "M20 12 A8 8 0 1 1 17.6 6.3", "M20 4 V9 H15") }
    val PlayArrow by lazy {
        icon(
            "PlayArrow",
            "M7 4.8 V19.2 C7 20 7.9 20.5 8.6 20.1 L20 13 C20.6 12.6 20.6 11.4 20 11 L8.6 3.9 C7.9 3.5 7 4 7 4.8 Z"
        )
    }
    val NorthWest by lazy { icon("NorthWest", "M17 17 L7 7", "M7 15 V7 H15") }
    val ArrowBack by lazy { icon("ArrowBack", "M20 12 H4", "M10 6 L4 12 L10 18") }
}
