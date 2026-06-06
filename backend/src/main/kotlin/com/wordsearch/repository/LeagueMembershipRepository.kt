package com.wordsearch.repository

import com.wordsearch.model.LeagueMembership
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface LeagueMembershipRepository : JpaRepository<LeagueMembership, UUID> {
    fun findByCohortIdOrderByWeeklyScoreDesc(cohortId: UUID): List<LeagueMembership>
    fun findByUserIdAndCohortId(userId: UUID, cohortId: UUID): LeagueMembership?
    fun countByCohortId(cohortId: UUID): Long
    fun findByCohortId(cohortId: UUID): List<LeagueMembership>
}
