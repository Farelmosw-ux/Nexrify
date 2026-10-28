package id.nexrify.music.data.search

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

/** Riwayat pencarian: terbaru di atas, tanpa duplikat, maksimal 30 item, disimpan di SharedPreferences. */
class SearchHistoryManager(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("nexrify_search_history", Context.MODE_PRIVATE)

    private val _history = MutableStateFlow(load())
    val history: StateFlow<List<String>> = _history.asStateFlow()

    fun add(query: String) {
        val clean = query.trim()
        if (clean.isEmpty()) return
        val updated = buildList {
            add(clean)
            addAll(_history.value.filterNot { it.equals(clean, ignoreCase = true) })
        }.take(MAX_ITEMS)
        _history.value = updated
        save(updated)
    }

    fun remove(query: String) {
        val updated = _history.value.filterNot { it == query }
        _history.value = updated
        save(updated)
    }

    fun clear() {
        _history.value = emptyList()
        prefs.edit().remove(KEY).apply()
    }

    private fun load(): List<String> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            List(arr.length()) { arr.getString(it) }.filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
    }

    private fun save(list: List<String>) {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    private companion object {
        const val KEY = "items"
        const val MAX_ITEMS = 30
    }
}
