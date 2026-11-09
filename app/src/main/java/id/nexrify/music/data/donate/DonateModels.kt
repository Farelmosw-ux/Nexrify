package id.nexrify.music.data.donate

/** Satu donatur di papan peringkat. rank 1..3. */
data class DonorTop(
    val rank: Int,
    val uid: String,
    val username: String,
    val display: String,
    val av: String,
    val total: Long,
    val titleOn: Boolean
)

/** Status: pending (menunggu bukti), review (bukti dikirim), paid, rejected, cancelled. */
data class Donation(
    val id: String,
    val method: String,
    val amount: Long,
    val status: String,
    val sender: String,
    val note: String,
    val createdAt: Long,
    val submittedAt: Long,
    val reviewedAt: Long
) {
    val isActive get() = status == "pending" || status == "review"
}

data class DonateConfig(
    val enabled: Boolean,
    val min: Long,
    val max: Long,
    val presets: List<Long>,
    val thanks: String,
    val danaName: String,
    val danaNumber: String,
    val danaReady: Boolean,
    val qrisPage: String
)

data class DonateHome(val config: DonateConfig, val top: List<DonorTop>, val myRank: Int, val titleOn: Boolean)
