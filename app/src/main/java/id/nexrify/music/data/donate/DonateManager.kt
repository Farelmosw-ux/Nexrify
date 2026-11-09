package id.nexrify.music.data.donate

import id.nexrify.music.data.account.AccountError
import id.nexrify.music.data.account.AccountManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Status donasi dan papan peringkat. Semua panggilan jaringan bersifat suspend dan melempar AccountError kalau gagal. */
object DonateManager {

    private val _home = MutableStateFlow<DonateHome?>(null)
    val home: StateFlow<DonateHome?> = _home.asStateFlow()
    private val _top = MutableStateFlow<List<DonorTop>>(emptyList())
    val top: StateFlow<List<DonorTop>> = _top.asStateFlow()
    /** username (huruf kecil) → peringkat, hanya untuk donatur yang memasang title-nya. Dipakai untuk tag di chat. */
    private val _ranks = MutableStateFlow<Map<String, Int>>(emptyMap())
    val ranks: StateFlow<Map<String, Int>> = _ranks.asStateFlow()
    private val _history = MutableStateFlow<List<Donation>>(emptyList())
    val history: StateFlow<List<Donation>> = _history.asStateFlow()
    private val _historyLoaded = MutableStateFlow(false)
    val historyLoaded: StateFlow<Boolean> = _historyLoaded.asStateFlow()

    private fun token(): String = AccountManager.getInstance().token() ?: throw AccountError("auth", "Belum masuk")

    private fun applyTop(list: List<DonorTop>) {
        _top.value = list
        _ranks.value = list.filter { it.titleOn }.associate { it.username.lowercase() to it.rank }
    }

    suspend fun refreshHome() {
        val h = DonateApi.home(token())
        _home.value = h
        applyTop(h.top)
    }

    /** Ringan: hanya tiga teratas, untuk tag di chat. Gagal diam-diam. */
    suspend fun refreshTop() {
        try { applyTop(DonateApi.top(token())) } catch (e: CancellationException) { throw e } catch (_: Throwable) {}
    }

    suspend fun refreshHistory() {
        _history.value = DonateApi.history(token())
        _historyLoaded.value = true
    }

    private fun upsert(d: Donation) {
        _history.value = (listOf(d) + _history.value.filter { it.id != d.id }).sortedByDescending { it.createdAt }
    }

    suspend fun create(method: String, amount: Long, sender: String, note: String): Donation =
        DonateApi.create(token(), method, amount, sender, note).also { upsert(it) }

    suspend fun submit(id: String, file: NexProofFile, onProgress: (Int) -> Unit = {}): Donation =
        DonateApi.submit(token(), id, file, onProgress).also { upsert(it) }

    suspend fun cancel(id: String): Donation = DonateApi.cancel(token(), id).also { upsert(it) }

    suspend fun delete(id: String) {
        DonateApi.delete(token(), id)
        _history.value = _history.value.filter { it.id != id }
    }

    suspend fun deleteFinished() {
        DonateApi.deleteFinished(token())
        _history.value = _history.value.filter { it.isActive }
    }

    suspend fun setTitle(on: Boolean) {
        DonateApi.setTitle(token(), on)
        refreshHome()
    }
}
