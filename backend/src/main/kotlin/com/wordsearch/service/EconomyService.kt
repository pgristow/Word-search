package com.wordsearch.service

import com.wordsearch.model.CoinTransaction
import com.wordsearch.model.UserProgress
import com.wordsearch.repository.CoinTransactionRepository
import com.wordsearch.repository.UserProgressRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

class InsufficientCoinsException(message: String) : RuntimeException(message)

@Service
class EconomyService(
    private val userProgressRepository: UserProgressRepository,
    private val coinTransactionRepository: CoinTransactionRepository
) {

    companion object {
        /** Base coins awarded for a minimum-length (3-letter) bonus word. */
        const val BONUS_WORD_BASE: Long = 5L
        /** Extra coins per letter beyond the 3-letter minimum. */
        const val BONUS_WORD_PER_EXTRA_LETTER: Long = 2L
        /** Coins charged to reveal a single target word via a hint. */
        const val HINT_COST: Long = 30L
    }

    /** Coins awarded for a bonus word of the given length. */
    fun bonusWordCoins(wordLength: Int): Long =
        BONUS_WORD_BASE + BONUS_WORD_PER_EXTRA_LETTER * maxOf(0, wordLength - 3)

    /** Current coin balance for the user (0 if no progress row exists). */
    fun balance(userId: UUID): Long =
        userProgressRepository.findByUserId(userId)?.coins ?: 0L

    /** Adds [amount] coins, persists progress + a ledger row, returns the new balance. */
    @Transactional
    fun earn(userId: UUID, amount: Long, reason: String, refId: UUID? = null): Long =
        adjust(userId, amount, reason, refId)

    /**
     * Removes [amount] coins. Throws [InsufficientCoinsException] (leaving the
     * balance untouched) if the user cannot afford it. Returns the new balance.
     */
    @Transactional
    fun spend(userId: UUID, amount: Long, reason: String, refId: UUID? = null): Long {
        val progress = userProgressRepository.findByUserId(userId)
            ?: throw IllegalArgumentException("User progress not found")
        if (progress.coins < amount) {
            throw InsufficientCoinsException(
                "Insufficient coins: balance ${progress.coins}, required $amount"
            )
        }
        return persist(progress, -amount, reason, refId)
    }

    private fun adjust(userId: UUID, delta: Long, reason: String, refId: UUID?): Long {
        val progress = userProgressRepository.findByUserId(userId)
            ?: userProgressRepository.save(UserProgress(userId = userId))
        return persist(progress, delta, reason, refId)
    }

    private fun persist(progress: UserProgress, delta: Long, reason: String, refId: UUID?): Long {
        val newBalance = progress.coins + delta
        userProgressRepository.save(progress.copy(coins = newBalance))
        coinTransactionRepository.save(
            CoinTransaction(
                userId = progress.userId,
                delta = delta,
                reason = reason,
                refId = refId,
                balanceAfter = newBalance
            )
        )
        return newBalance
    }
}
