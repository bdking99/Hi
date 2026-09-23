package com.example

import com.example.data.model.*
import com.example.data.repository.WalletRepository
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WalletAndGiftUnitTest {

    private val walletRepository = WalletRepository()

    @Test
    fun `default gifts catalog contains valid active gifts`() {
        val gifts = walletRepository.getDefaultGifts()
        assertTrue("Gifts catalog should not be empty", gifts.isNotEmpty())
        assertTrue("All default gifts should be active", gifts.all { it.isActive })
        assertTrue("All gifts should have positive price", gifts.all { it.coinPrice > 0 })
        assertNotNull(gifts.find { it.giftId == "gift_rose" })
        assertNotNull(gifts.find { it.giftId == "gift_crown" })
    }

    @Test
    fun `default frames catalog contains valid VIP frames`() {
        val frames = walletRepository.getDefaultFrames()
        assertTrue("Frames catalog should not be empty", frames.isNotEmpty())
        assertTrue("All default frames should be active", frames.all { it.isActive })
        assertTrue("All frames should have valid colors", frames.all { it.glowColorHex != 0L })
        assertNotNull(frames.find { it.frameId == "frame_gold_crown" })
        assertNotNull(frames.find { it.frameId == "frame_neon_cyber" })
    }

    @Test
    fun `wallet transaction data class integrity`() {
        val tx = WalletTransaction(
            transactionId = "tx_1001",
            uid = "user_123",
            publicUserId = "1000001",
            type = "COIN_RECHARGE",
            amount = 1200L,
            balanceBefore = 500L,
            balanceAfter = 1700L,
            status = "SUCCESS",
            description = "Top up: 1,200 Coins"
        )

        assertEquals("tx_1001", tx.transactionId)
        assertEquals(1200L, tx.amount)
        assertEquals(1700L, tx.balanceAfter)
        assertEquals("COIN_RECHARGE", tx.type)
    }

    @Test
    fun `user wallet data class integrity`() {
        val wallet = UserWallet(
            uid = "user_123",
            publicUserId = "1000001",
            coinBalance = 5000L,
            lifetimePurchasedCoins = 10000L,
            lifetimeSpentCoins = 5000L,
            lifetimeReceivedCoins = 2500L
        )

        assertEquals("user_123", wallet.uid)
        assertEquals(5000L, wallet.coinBalance)
        assertEquals(10000L, wallet.lifetimePurchasedCoins)
    }
}
