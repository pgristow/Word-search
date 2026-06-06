package com.wordsearch.repository

import com.wordsearch.model.CoinTransaction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CoinTransactionRepository : JpaRepository<CoinTransaction, UUID> {
    fun findByUserIdOrderByCreatedAtDesc(userId: UUID): List<CoinTransaction>
}
