package com.waheed.artificerx.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AiProviderConfig.isOverQuota / isNearQuota must roll over to a fresh
 * count on a new calendar day. Before this fix, usageTodayCallCount
 * only ever grew — a provider that hit its daily free-tier quota once
 * would stay marked exhausted forever, permanently removing it from
 * the agent's provider fallback rotation (Section 199).
 */
class AiProviderConfigQuotaTest {
    private val oneDayMillis = 86_400_000L

    private fun configWith(
        usageTodayCallCount: Int,
        lastResetEpochDay: Long,
        knownDailyQuota: Int?,
    ) = AiProviderConfig(
        id = "test",
        type = AiProviderType.GROQ,
        displayName = "Test Provider",
        baseUrl = "https://example.com",
        keyAlias = "alias",
        maskedKeyPreview = "****",
        usageTodayCallCount = usageTodayCallCount,
        lastResetEpochDay = lastResetEpochDay,
        knownDailyQuota = knownDailyQuota,
    )

    @Test
    fun `provider at quota today is over quota`() {
        val today = System.currentTimeMillis() / oneDayMillis
        val provider = configWith(usageTodayCallCount = 100, lastResetEpochDay = today, knownDailyQuota = 100)
        assertTrue(provider.isOverQuota)
    }

    @Test
    fun `provider under quota today is not over quota`() {
        val today = System.currentTimeMillis() / oneDayMillis
        val provider = configWith(usageTodayCallCount = 50, lastResetEpochDay = today, knownDailyQuota = 100)
        assertFalse(provider.isOverQuota)
    }

    @Test
    fun `provider exhausted yesterday is not over quota today — the actual bug this fix addresses`() {
        val today = System.currentTimeMillis() / oneDayMillis
        val yesterday = today - 1
        // Exhausted its quota yesterday, but the stored count is stale —
        // isOverQuota must recognize the day rolled over and treat the
        // provider as fresh, not permanently exhausted.
        val provider = configWith(usageTodayCallCount = 100, lastResetEpochDay = yesterday, knownDailyQuota = 100)
        assertFalse(provider.isOverQuota)
    }

    @Test
    fun `provider with no known quota is never over quota regardless of usage`() {
        val today = System.currentTimeMillis() / oneDayMillis
        val provider = configWith(usageTodayCallCount = 999_999, lastResetEpochDay = today, knownDailyQuota = null)
        assertFalse(provider.isOverQuota)
    }

    @Test
    fun `isNearQuota triggers at 85 percent threshold for today's usage`() {
        val today = System.currentTimeMillis() / oneDayMillis
        val provider = configWith(usageTodayCallCount = 85, lastResetEpochDay = today, knownDailyQuota = 100)
        assertTrue(provider.isNearQuota)
        assertFalse(provider.isOverQuota)
    }

    @Test
    fun `isNearQuota does not trigger from a stale prior-day count`() {
        val today = System.currentTimeMillis() / oneDayMillis
        val yesterday = today - 1
        val provider = configWith(usageTodayCallCount = 85, lastResetEpochDay = yesterday, knownDailyQuota = 100)
        assertFalse(provider.isNearQuota)
    }
}
