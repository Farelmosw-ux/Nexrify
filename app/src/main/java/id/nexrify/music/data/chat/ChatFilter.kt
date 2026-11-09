package id.nexrify.music.data.chat

/**
 * Sensor kata kasar (padanan _chatfilter.js di server). Kata kasar diganti # sepanjang kata aslinya,
 * mis. "babi" menjadi "####". Server tetap menyensor ulang, ini supaya gelembung chat langsung tampil bersih.
 */
object ChatFilter {

    private val STRICT = listOf(
        "asu", "tai", "babi", "bego", "goblok", "tolol", "idiot", "bodoh", "dungu", "kampret", "kimak", "puki", "peler", "pepek",
        "titit", "perek", "jablay", "sialan", "brengsek", "keparat", "bacot", "dick", "cock", "cunt", "slut", "whore", "fag", "twat",
        "ass", "asshole", "shit", "bitch", "bastard", "pussy", "cum", "porn", "bokep", "anjir", "anjay", "ngocok", "colmek", "coli"
    )
    private val LOOSE = listOf(
        "anjing", "anjg", "bangsat", "bajingan", "kontol", "memek", "ngentot", "entot", "jancok", "jancuk", "jembut", "pantek", "bencong",
        "cukimai", "cukimay", "fuck", "motherfucker", "nigger", "nigga", "faggot", "dickhead", "bullshit", "sundal", "bispak", "lonte",
        "pelacur", "kampungan"
    )

    private val LEET = mapOf('0' to 'o', '1' to 'i', '3' to 'e', '4' to 'a', '5' to 's', '7' to 't', '8' to 'b', '@' to 'a', '$' to 's')
    private const val CC = "A-Za-z0-9@\$"
    private val SPACED = Regex("(?<![$CC])(?:[$CC][ .\\-_*]{1,2}){2,}[$CC](?![$CC])")
    private val WORD = Regex("[$CC]+")
    private val SUFFIX = Regex("(nya|lah|kah|mu|ku)$")

    private fun norm(s: String): String {
        val letters = StringBuilder()
        for (raw in s.lowercase()) {
            val c = LEET[raw] ?: raw
            if (c in 'a'..'z') letters.append(c)
        }
        val out = StringBuilder()
        for (c in letters) if (out.isEmpty() || out.last() != c) out.append(c)
        return out.toString()
    }

    private val strictSet = STRICT.map { norm(it) }.toSet()
    private val looseList = LOOSE.map { norm(it) }

    private fun isBad(token: String): Boolean {
        val n = norm(token)
        if (n.length < 2) return false
        if (n in strictSet) return true
        val stem = n.replace(SUFFIX, "")
        if (stem != n && stem.length >= 2 && stem in strictSet) return true
        return looseList.any { n.contains(it) }
    }

    /** Mengembalikan teks tersensor. */
    fun censor(input: String): String {
        val chars = input.toCharArray()
        fun mask(start: Int, len: Int) { for (i in start until start + len) chars[i] = '#' }
        for (m in SPACED.findAll(input)) if (isBad(m.value)) mask(m.range.first, m.value.length)
        for (m in WORD.findAll(input)) {
            val tok = m.value
            if (tok.all { it.isDigit() }) continue
            if (isBad(tok)) mask(m.range.first, tok.length)
        }
        return String(chars)
    }
}
