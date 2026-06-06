package com.wordsearch.repository

import com.wordsearch.model.LeagueCohort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface LeagueCohortRepository : JpaRepository<LeagueCohort, UUID> {
    fun findByTierIdAndWeekKeyAndStatus(tierId: Int, weekKey: String, status: String): List<LeagueCohort>
    fun findByStatus(status: String): List<LeagueCohort>
}
