package com.wordsearch.repository

import com.wordsearch.model.DailyChallenge
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
interface DailyChallengeRepository : JpaRepository<DailyChallenge, UUID> {
    fun findByChallengeDate(challengeDate: LocalDate): DailyChallenge?
    fun findByChallengeDateBetween(startDate: LocalDate, endDate: LocalDate): List<DailyChallenge>
}
