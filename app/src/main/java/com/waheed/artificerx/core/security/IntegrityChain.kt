package com.waheed.artificerx.core.security

data class IntegrityRecord(
    val sequence: Long,
    val type: String,
    val payloadDigest: String,
    val digest: String
)

class IntegrityChain {
    private var previous = "GENESIS"
    private var sequence = 0L
    private val records = mutableListOf<IntegrityRecord>()

    @Synchronized
    fun append(type: String, payload: String): IntegrityRecord {
        require(type.isNotBlank())
        val payloadDigest = Checksum.sha256(payload)
        val digest = Checksum.sha256("$previous|$type|$payloadDigest")
        val record = IntegrityRecord(++sequence, type, payloadDigest, digest)
        records += record
        previous = digest
        return record
    }

    @Synchronized
    fun verify(): Boolean {
        var prior = "GENESIS"
        var expectedSequence = 1L
        for (record in records) {
            if (record.sequence != expectedSequence++) return false
            val expected = Checksum.sha256("$prior|${record.type}|${record.payloadDigest}")
            if (!Checksum.constantTimeEquals(record.digest, expected)) return false
            prior = record.digest
        }
        return true
    }

    @Synchronized fun all(): List<IntegrityRecord> = records.toList()
}
