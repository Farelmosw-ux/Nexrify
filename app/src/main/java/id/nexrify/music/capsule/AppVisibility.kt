package id.nexrify.music.capsule

/** Penanda apakah Nexrify sedang tampil di layar. Kapsul hanya muncul saat aplikasi ada di latar belakang. */
object AppVisibility {
    @Volatile
    var inForeground: Boolean = false
        private set

    @Volatile
    private var listener: (() -> Unit)? = null

    fun setListener(l: (() -> Unit)?) {
        listener = l
    }

    fun set(foreground: Boolean) {
        if (inForeground == foreground) return
        inForeground = foreground
        listener?.invoke()
    }
}
