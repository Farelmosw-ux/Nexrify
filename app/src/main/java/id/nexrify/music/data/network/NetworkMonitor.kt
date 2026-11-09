package id.nexrify.music.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Memantau apakah perangkat benar benar punya internet. Dipakai untuk mode offline. */
class NetworkMonitor private constructor(context: Context) {

    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _online = MutableStateFlow(check())
    val online: StateFlow<Boolean> = _online.asStateFlow()

    init {
        try {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _online.value = check()
                }

                override fun onLost(network: Network) {
                    _online.value = check()
                }

                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    _online.value = check()
                }
            })
        } catch (_: Exception) {
        }
    }

    /** Pemeriksaan langsung saat ini (bukan nilai lama). */
    fun isOnline(): Boolean = check().also { _online.value = it }

    private fun check(): Boolean {
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    companion object {
        @Volatile
        private var instance: NetworkMonitor? = null

        fun init(context: Context): NetworkMonitor =
            instance ?: synchronized(this) {
                instance ?: NetworkMonitor(context.applicationContext).also { instance = it }
            }

        fun getInstance(): NetworkMonitor =
            instance ?: throw IllegalStateException("NetworkMonitor belum di inisialisasi")
    }
}
