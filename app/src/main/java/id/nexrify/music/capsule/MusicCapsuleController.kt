package id.nexrify.music.capsule

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import id.nexrify.music.MainActivity
import id.nexrify.music.R
import id.nexrify.music.data.settings.SettingsManager
import id.nexrify.music.service.PlaybackBridge
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.min

/**
 * Panel musik berbentuk kapsul di atas layar (mirip kapsul musik di HP Android modern).
 *
 *  - Ketuk 1x      : buka aplikasi Nexrify
 *  - Tekan lama    : buka panel penuh (judul, artis, seekbar, prev / play / next)
 *  - Geser ke atas, ketuk di luar panel, atau diam beberapa detik : balik jadi kapsul
 *
 * Kapsul ini digambar sendiri oleh Nexrify memakai izin "tampil di atas aplikasi lain" (TYPE_APPLICATION_OVERLAY),
 * jadi jalan di Android 8.0 ke atas, tidak bergantung pada fitur bawaan merek HP tertentu.
 * Hanya muncul saat lagu diputar dan Nexrify sedang tidak dibuka.
 */
class MusicCapsuleController(
    private val context: Context,
    private val player: Player
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private val artworkExecutor = Executors.newSingleThreadExecutor()
    private val density = context.resources.displayMetrics.density

    private var root: FrameLayout? = null
    private var params: WindowManager.LayoutParams? = null
    private var expanded = false
    private var attached = false
    private var destroyed = false
    private var pausedSince = 0L

    // Views
    private lateinit var background: GradientDrawable
    private lateinit var content: LinearLayout
    private lateinit var artView: ImageView
    private lateinit var waveform: WaveformView
    private lateinit var titleView: TextView
    private lateinit var artistView: TextView
    private lateinit var expandedGroup: LinearLayout
    private lateinit var seekBar: SeekBar
    private lateinit var elapsedView: TextView
    private lateinit var totalView: TextView
    private lateinit var playPauseView: ImageView
    private lateinit var heartView: ImageView
    private lateinit var headerRow: LinearLayout
    private lateinit var textColumn: LinearLayout

    private var draggingSeek = false
    @Volatile private var artworkKey: String? = null
    private var lastBitmap: Bitmap? = null
    private var placeholderShown = false
    private var loadingKey: String? = null
    private var failedKey: String? = null
    private var failedAt = 0L
    private var lastTitle: String? = null
    private var lastArtist: String? = null
    private val artworkCache = android.util.LruCache<String, Bitmap>(12)

    private val regular: Typeface? = ResourcesCompat.getFont(context, R.font.outfit_regular)
    private val semibold: Typeface? = ResourcesCompat.getFont(context, R.font.outfit_semibold)

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying) pausedSince = System.currentTimeMillis()
            refresh()
        }
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) = refresh()
        override fun onPlaybackStateChanged(playbackState: Int) = refresh()
        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) = refresh()
    }

    private val ticker = object : Runnable {
        override fun run() {
            if (destroyed) return
            if (attached && root?.isAttachedToWindow == false) {
                refresh()
                if (!attached) handler.postDelayed(this, 1_000L)
                return
            }
            if (!attached) return
            updateHeart()
            updateProgress()
            // Kapsul yang dijeda terlalu lama disembunyikan, panel penuh ikut menutup sendiri.
            if (!player.isPlaying && !expanded && System.currentTimeMillis() - pausedSince > PAUSED_HIDE_MS) {
                refresh()
            }
            handler.postDelayed(this, 500L)
        }
    }

    private val autoCollapse = Runnable { setExpanded(false) }

    // ---------------------------------------------------------------- public

    fun start() {
        player.addListener(playerListener)
        AppVisibility.setListener { handler.post { refresh() } }
        SettingsManager.getInstance().setCapsuleChangeListener { handler.post { refresh() } }
        refresh()
    }

    fun destroy() {
        destroyed = true
        player.removeListener(playerListener)
        AppVisibility.setListener(null)
        SettingsManager.getInstance().setCapsuleChangeListener(null)
        handler.removeCallbacksAndMessages(null)
        detach()
        artworkExecutor.shutdownNow()
    }

    /** Dipanggil ulang kapan pun kondisi berubah (lagu, status putar, aplikasi tampil/tidak, izin, pengaturan). */
    fun refresh() {
        if (destroyed) return
        // Kalau tampilan sempat dilepas sistem, anggap belum terpasang supaya dipasang ulang.
        if (attached && root?.isAttachedToWindow == false) {
            root = null
            params = null
            attached = false
            expanded = false
        }
        if (shouldShow()) {
            handler.removeCallbacks(hideRunnable)
            hidePending = false
            attach()
            bind()
        } else if (mustHideNow()) {
            handler.removeCallbacks(hideRunnable)
            hidePending = false
            detach()
        } else if (attached && !hidePending) {
            // Kondisi sementara (misalnya pergantian lagu): tunggu sebentar sebelum menyembunyikan kapsul.
            hidePending = true
            handler.postDelayed(hideRunnable, HIDE_GRACE_MS)
        }
    }

    private var hidePending = false

    private val hideRunnable = Runnable {
        hidePending = false
        if (!destroyed && !shouldShow()) detach()
    }

    // ---------------------------------------------------------------- visibility rules

    /** Alasan yang pasti, bukan sementara: pengaturan mati, izin dicabut, atau Nexrify sedang dibuka. */
    private fun mustHideNow(): Boolean =
        !SettingsManager.getInstance().isCapsuleEnabled() || !canDrawOverlays(context) || AppVisibility.inForeground

    private fun hasSession(): Boolean =
        player.mediaItemCount > 0 || !player.mediaMetadata.title.isNullOrBlank() || lastTitle != null

    private fun shouldShow(): Boolean {
        if (mustHideNow()) return false
        // Selama musik diputar (termasuk buffering dan pergantian lagu) kapsul tidak boleh hilang.
        if (player.isPlaying || player.playWhenReady) return true
        if (player.playbackState == Player.STATE_BUFFERING) return true
        if (expanded) return true
        if (!hasSession()) return false
        return System.currentTimeMillis() - pausedSince <= PAUSED_HIDE_MS
    }

    // ---------------------------------------------------------------- window

    private fun attach() {
        if (attached) return
        val view = buildView()
        val lp = WindowManager.LayoutParams(
            collapsedWidth(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(COLLAPSED_TOP_MARGIN_DP)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            title = "NexrifyCapsule"
        }
        try {
            windowManager.addView(view, lp)
            root = view
            params = lp
            attached = true
            expanded = false
            view.alpha = 0f
            view.translationY = -dp(16).toFloat()
            view.animate().alpha(1f).translationY(0f).setDuration(220L).start()
            handler.removeCallbacks(ticker)
            handler.post(ticker)
        } catch (_: Exception) {
            // Izin dicabut atau window ditolak sistem: diam saja, coba lagi di refresh berikutnya.
            root = null
            params = null
            attached = false
        }
    }

    private fun detach() {
        handler.removeCallbacks(ticker)
        handler.removeCallbacks(autoCollapse)
        val view = root
        if (view != null && attached) {
            try {
                windowManager.removeViewImmediate(view)
            } catch (_: Exception) {
            }
        }
        root = null
        params = null
        attached = false
        expanded = false
    }

    // ---------------------------------------------------------------- view construction

    private fun buildView(): FrameLayout {
        val container = FrameLayout(context)

        background = GradientDrawable().apply {
            setColor(Color.BLACK)
        }
        setCorners(top = dp(COLLAPSED_HEIGHT_DP / 2).toFloat(), bottom = dp(COLLAPSED_HEIGHT_DP / 2).toFloat())
        container.background = background
        container.elevation = dp(6).toFloat()

        content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(5), dp(5), dp(5), dp(5))
        }
        container.addView(content, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // Baris atas: sampul, teks, gelombang
        headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        artView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(0xFF1E1E1E.toInt())
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, dp(if (expanded) 12 else 9).toFloat())
                }
            }
            clipToOutline = true
        }
        headerRow.addView(artView, LinearLayout.LayoutParams(dp(COLLAPSED_ART_DP), dp(COLLAPSED_ART_DP)))

        textColumn = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        titleView = TextView(context).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            typeface = semibold
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            isSingleLine = true
        }
        artistView = TextView(context).apply {
            setTextColor(0xFFA1A1AA.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = regular
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            isSingleLine = true
        }
        textColumn.addView(titleView)
        textColumn.addView(artistView)
        headerRow.addView(textColumn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = dp(12)
            marginEnd = dp(8)
        })

        // Spacer fleksibel supaya gelombang selalu di kanan saat kapsul.
        val spacer = View(context)
        headerRow.addView(spacer, LinearLayout.LayoutParams(0, 1, 1f))
        waveform = WaveformView(context)
        headerRow.addView(waveform, LinearLayout.LayoutParams(dp(26), dp(18)).apply { marginEnd = dp(9) })
        content.addView(headerRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        this.spacer = spacer

        // Bagian panel penuh
        expandedGroup = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(dp(11), dp(10), dp(11), dp(8))
        }
        seekBar = SeekBar(context).apply {
            max = 1000
            progressTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(0xFF3F3F46.toInt())
            thumbTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            setPadding(dp(8), 0, dp(8), 0)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        val dur = player.duration
                        if (dur > 0 && dur != C.TIME_UNSET) {
                            elapsedView.text = formatTime(dur * progress / 1000L)
                        }
                    }
                }
                override fun onStartTrackingTouch(sb: SeekBar) {
                    draggingSeek = true
                    handler.removeCallbacks(autoCollapse)
                }
                override fun onStopTrackingTouch(sb: SeekBar) {
                    draggingSeek = false
                    val dur = player.duration
                    if (dur > 0 && dur != C.TIME_UNSET) player.seekTo(dur * sb.progress / 1000L)
                    scheduleAutoCollapse()
                }
            })
        }
        expandedGroup.addView(seekBar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(28)))

        val timeRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        elapsedView = timeLabel("0:00")
        totalView = timeLabel("--:--")
        timeRow.addView(elapsedView, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        timeRow.addView(totalView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        expandedGroup.addView(timeRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            marginStart = dp(8); marginEnd = dp(8)
        })

        val controls = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        heartView = controlButton(R.drawable.ic_heart_outline) {
            PlaybackBridge.onToggleFavorite?.invoke()
            handler.postDelayed({ updateHeart() }, 150L)
            scheduleAutoCollapse()
        }
        val prev = controlButton(R.drawable.ic_nexrify_rewind) { player.seekToPrevious(); scheduleAutoCollapse() }
        playPauseView = controlButton(R.drawable.ic_nexrify_pause) {
            if (player.isPlaying) player.pause() else player.play()
            scheduleAutoCollapse()
        }
        val next = controlButton(R.drawable.ic_nexrify_fast_forward) { player.seekToNext(); scheduleAutoCollapse() }
        controls.addView(heartView, LinearLayout.LayoutParams(dp(52), dp(48)))
        controls.addView(prev, LinearLayout.LayoutParams(dp(64), dp(48)))
        controls.addView(playPauseView, LinearLayout.LayoutParams(dp(72), dp(48)))
        controls.addView(next, LinearLayout.LayoutParams(dp(64), dp(48)))
        // Penyeimbang di kanan supaya tombol putar tetap di tengah.
        controls.addView(View(context), LinearLayout.LayoutParams(dp(52), dp(48)))
        expandedGroup.addView(controls, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(4) })
        content.addView(expandedGroup, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // Gestur
        val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                openApp()
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                container.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                if (!expanded) setExpanded(true)
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (e1 != null && expanded && e1.y - e2.y > dp(24) && abs(vy) > abs(vx)) {
                    setExpanded(false)
                    return true
                }
                return false
            }
        })
        container.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                if (expanded) setExpanded(false)
                false
            } else {
                detector.onTouchEvent(event)
            }
        }
        container.isClickable = true
        return container
    }

    private var spacer: View? = null

    private fun timeLabel(text: String) = TextView(context).apply {
        this.text = text
        setTextColor(0xFFA1A1AA.toInt())
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        typeface = regular
    }

    private fun controlButton(iconRes: Int, onClick: () -> Unit) = ImageView(context).apply {
        setImageResource(iconRes)
        scaleType = ImageView.ScaleType.CENTER_INSIDE
        setColorFilter(Color.WHITE)
        setPadding(dp(12), dp(10), dp(12), dp(10))
        isClickable = true
        isFocusable = false
        setOnClickListener { onClick() }
    }

    // ---------------------------------------------------------------- state binding

    private fun bind() {
        val view = root ?: return
        val md = player.mediaMetadata
        val itemMd = player.currentMediaItem?.mediaMetadata

        // Simpan judul dan artis terakhir: saat pergantian lagu metadata bisa kosong sesaat.
        val title = md.title?.toString()?.takeIf { it.isNotBlank() }
            ?: itemMd?.title?.toString()?.takeIf { it.isNotBlank() }
        val artist = md.artist?.toString()?.takeIf { it.isNotBlank() }
            ?: itemMd?.artist?.toString()?.takeIf { it.isNotBlank() }
        if (title != null) {
            lastTitle = title
            lastArtist = artist.orEmpty()
        }
        titleView.text = lastTitle ?: "Nexrify"
        artistView.text = lastArtist.orEmpty()

        waveform.setActive(player.isPlaying || (player.playWhenReady && player.playbackState == Player.STATE_BUFFERING))
        playPauseView.setImageResource(
            if (player.isPlaying || player.playWhenReady) R.drawable.ic_nexrify_pause else R.drawable.ic_nexrify_play
        )
        bindArtwork(md, itemMd)
        updateHeart()
        updateProgress()
        view.requestLayout()
    }

    /**
     * Sampul tidak boleh hilang: gambar lama dipertahankan sampai gambar baru benar benar siap,
     * hasil unduhan disimpan di cache memori, unduhan gagal dicoba ulang, dan ada placeholder kalau memang belum ada.
     */
    private fun bindArtwork(md: MediaMetadata, itemMd: MediaMetadata?) {
        val data = md.artworkData ?: itemMd?.artworkData
        val url = (md.artworkUri ?: itemMd?.artworkUri)?.toString().orEmpty()

        if (data != null) {
            val key = "bytes:${data.size}:${data.contentHashCode()}"
            if (key == artworkKey) return
            artworkKey = key
            decodeSampled(data)?.let { showArtwork(it) }
            return
        }
        if (url.isBlank()) {
            // Metadata belum lengkap (biasanya sesaat saat ganti lagu): biarkan sampul sebelumnya tetap tampil.
            if (lastBitmap == null) showPlaceholder()
            return
        }
        if (url == artworkKey && lastBitmap != null) return

        artworkCache.get(url)?.let {
            artworkKey = url
            showArtwork(it)
            return
        }
        val now = System.currentTimeMillis()
        if (url == failedKey && now - failedAt < RETRY_BACKOFF_MS) {
            if (lastBitmap == null) showPlaceholder()
            return
        }
        if (url == loadingKey) return
        loadingKey = url
        if (lastBitmap == null) showPlaceholder()

        artworkExecutor.execute {
            var bitmap: Bitmap? = null
            var attempt = 0
            while (bitmap == null && attempt < 3 && !destroyed) {
                var conn: HttpURLConnection? = null
                try {
                    conn = (URL(url).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8_000
                        readTimeout = 8_000
                        instanceFollowRedirects = true
                    }
                    val bytes = conn.inputStream.use { it.readBytes() }
                    bitmap = decodeSampled(bytes)
                } catch (_: Exception) {
                } finally {
                    conn?.disconnect()
                }
                if (bitmap == null) {
                    attempt++
                    try {
                        Thread.sleep(700L * attempt)
                    } catch (_: InterruptedException) {
                        return@execute
                    }
                }
            }
            handler.post {
                if (destroyed) return@post
                loadingKey = null
                if (bitmap != null) {
                    artworkCache.put(url, bitmap)
                    // Hanya tampilkan kalau masih lagu yang sama; kalau sudah ganti, bind berikutnya yang memuat.
                    val currentUrl = (player.mediaMetadata.artworkUri ?: player.currentMediaItem?.mediaMetadata?.artworkUri)?.toString().orEmpty()
                    if (currentUrl == url || currentUrl.isBlank()) {
                        artworkKey = url
                        showArtwork(bitmap)
                    }
                } else {
                    failedKey = url
                    failedAt = System.currentTimeMillis()
                }
            }
        }
    }

    private fun showArtwork(bitmap: Bitmap) {
        lastBitmap = bitmap
        if (placeholderShown) {
            artView.clearColorFilter()
            artView.scaleType = ImageView.ScaleType.CENTER_CROP
            placeholderShown = false
        }
        artView.setImageBitmap(bitmap)
    }

    private fun showPlaceholder() {
        artView.setImageResource(R.drawable.ic_notification)
        artView.setColorFilter(0xFF8A8A92.toInt())
        artView.scaleType = ImageView.ScaleType.CENTER_INSIDE
        placeholderShown = true
    }

    private fun decodeSampled(bytes: ByteArray): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sample = 1
            val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
            while (maxSide / sample > 384) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        } catch (_: Throwable) {
            null
        }
    }

    private fun updateHeart() {
        if (!attached) return
        heartView.setImageResource(
            if (PlaybackBridge.isFavorite.value) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
        )
    }

    private fun updateProgress() {
        if (!attached || !expanded || draggingSeek) return
        val dur = player.duration
        val pos = player.currentPosition.coerceAtLeast(0L)
        if (dur > 0 && dur != C.TIME_UNSET) {
            seekBar.isEnabled = true
            seekBar.progress = ((pos * 1000L) / dur).toInt().coerceIn(0, 1000)
            totalView.text = formatTime(dur)
        } else {
            seekBar.isEnabled = false
            seekBar.progress = 0
            totalView.text = "--:--"
        }
        elapsedView.text = formatTime(pos)
    }

    // ---------------------------------------------------------------- expand / collapse

    private fun setExpanded(value: Boolean) {
        val view = root ?: return
        val lp = params ?: return
        if (expanded == value) return
        expanded = value
        handler.removeCallbacks(autoCollapse)

        val metrics = context.resources.displayMetrics
        if (value) {
            lp.width = metrics.widthPixels - dp(EXPANDED_SIDE_MARGIN_DP * 2)
            lp.y = 0
            setCorners(top = 0f, bottom = dp(EXPANDED_RADIUS_DP).toFloat())
            content.setPadding(dp(14), statusBarHeight().coerceAtLeast(dp(24)), dp(14), dp(6))
            artView.layoutParams = LinearLayout.LayoutParams(dp(EXPANDED_ART_DP), dp(EXPANDED_ART_DP))
            textColumn.visibility = View.VISIBLE
            spacer?.visibility = View.GONE
            expandedGroup.visibility = View.VISIBLE
            expandedGroup.alpha = 0f
            expandedGroup.translationY = -dp(8).toFloat()
            expandedGroup.animate().alpha(1f).translationY(0f).setDuration(200L).start()
            scheduleAutoCollapse()
        } else {
            lp.width = collapsedWidth()
            lp.y = dp(COLLAPSED_TOP_MARGIN_DP)
            setCorners(top = dp(COLLAPSED_HEIGHT_DP / 2).toFloat(), bottom = dp(COLLAPSED_HEIGHT_DP / 2).toFloat())
            content.setPadding(dp(5), dp(5), dp(5), dp(5))
            artView.layoutParams = LinearLayout.LayoutParams(dp(COLLAPSED_ART_DP), dp(COLLAPSED_ART_DP))
            textColumn.visibility = View.GONE
            spacer?.visibility = View.VISIBLE
            expandedGroup.visibility = View.GONE
        }
        artView.invalidateOutline()
        try {
            windowManager.updateViewLayout(view, lp)
        } catch (_: Exception) {
        }
        bind()
        if (!value) refresh()
    }

    private fun scheduleAutoCollapse() {
        handler.removeCallbacks(autoCollapse)
        handler.postDelayed(autoCollapse, AUTO_COLLAPSE_MS)
    }

    // ---------------------------------------------------------------- helpers

    private fun openApp() {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
        }
        if (expanded) setExpanded(false)
    }

    private fun collapsedWidth(): Int {
        val screenW = context.resources.displayMetrics.widthPixels
        return min((screenW * 0.62f).toInt(), dp(COLLAPSED_MAX_WIDTH_DP))
    }

    private fun statusBarHeight(): Int {
        val id = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) context.resources.getDimensionPixelSize(id) else 0
    }

    private fun setCorners(top: Float, bottom: Float) {
        background.cornerRadii = floatArrayOf(top, top, top, top, bottom, bottom, bottom, bottom)
    }

    private fun dp(value: Int): Int = (value * density).toInt()

    private fun formatTime(ms: Long): String {
        val total = (ms / 1000L).coerceAtLeast(0L)
        return String.format(Locale.US, "%d:%02d", total / 60, total % 60)
    }

    companion object {
        private const val COLLAPSED_HEIGHT_DP = 38
        private const val COLLAPSED_ART_DP = 28
        private const val COLLAPSED_TOP_MARGIN_DP = 6
        private const val COLLAPSED_MAX_WIDTH_DP = 260
        private const val EXPANDED_SIDE_MARGIN_DP = 0
        private const val EXPANDED_RADIUS_DP = 34
        private const val EXPANDED_ART_DP = 58
        private const val AUTO_COLLAPSE_MS = 7_000L
        private const val PAUSED_HIDE_MS = 60_000L
        private const val HIDE_GRACE_MS = 4_000L
        private const val RETRY_BACKOFF_MS = 8_000L

        fun canDrawOverlays(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
    }
}
