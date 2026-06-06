package com.wordsearch.service

import com.wordsearch.model.CoinTransaction
import com.wordsearch.model.UserProgress
import com.wordsearch.repository.CoinTransactionRepository
import com.wordsearch.repository.UserProgressRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class EconomyServiceTest {
    private lateinit var userProgressRepository: UserProgressRepository
    private lateinit var coinTransactionRepository: CoinTransactionRepository
    private lateinit var service: EconomyService

    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        userProgressRepository = mockk(relaxed = true)
        coinTransactionRepository = mockk(relaxed = true)
        service = EconomyService(userProgressRepository, coinTransactionRepository)
        // Generic save(S):S returns Object under relaxed mockk; echo the argument back.
        every { userProgressRepository.save(any()) } answers { firstArg() }
        every { coinTransactionRepository.save(any()) } answers { firstArg() }
    }

    @Test fun `earn increments balance and writes a ledger row with correct balance_after`() {
        every { userProgressRepository.findByUserId(userId) } returns
            UserProgress(userId = userId, coins = 10L)

        val txSlot = slot<CoinTransaction>()
        val progressSlot = slot<UserProgress>()
        every { coinTransactionRepository.save(capture(txSlot)) } answers { firstArg() }
        every { userProgressRepository.save(capture(progressSlot)) } answers { firstArg() }

        val newBalance = service.earn(userId, 7L, "BONUS_WORD")

        assertEquals(17L, newBalance)
        assertEquals(17L, progressSlot.captured.coins)
        assertEquals(7L, txSlot.captured.delta)
        assertEquals(17L, txSlot.captured.balanceAfter)
        assertEquals("BONUS_WORD", txSlot.captured.reason)
    }

    @Test fun `spend with insufficient funds throws and does not change balance`() {
        every { userProgressRepository.findByUserId(userId) } returns
            UserProgress(userId = userId, coins = 3L)

        assertThrows(InsufficientCoinsException::class.java) {
            service.spend(userId, 10L, "PURCHASE")
        }
        verify(exactly = 0) { userProgressRepository.save(any()) }
        verify(exactly = 0) { coinTransactionRepository.save(any()) }
    }

    @Test fun `spend deducts and records a negative delta`() {
        every { userProgressRepository.findByUserId(userId) } returns
            UserProgress(userId = userId, coins = 20L)

        val txSlot = slot<CoinTransaction>()
        every { coinTransactionRepository.save(capture(txSlot)) } answers { firstArg() }

        val newBalance = service.spend(userId, 8L, "PURCHASE")

        assertEquals(12L, newBalance)
        assertEquals(-8L, txSlot.captured.delta)
        assertEquals(12L, txSlot.captured.balanceAfter)
    }

    @Test fun `bonusWordCoins scales with length`() {
        assertEquals(5L, service.bonusWordCoins(3))
        assertEquals(13L, service.bonusWordCoins(7))
    }
}
