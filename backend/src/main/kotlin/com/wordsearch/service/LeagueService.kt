package com.wordsearch.service

import com.wordsearch.model.LeagueCohort
import com.wordsearch.model.LeagueMembership
import com.wordsearch.model.LeagueTier
import com.wordsearch.repository.LeagueCohortRepository
import com.wordsearch.repository.LeagueMembershipRepository
import com.wordsearch.repository.LeagueTierRepository
import com.wordsearch.repository.UserProgressRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.IsoFields
import java.util.UUID

@Service
class LeagueService(
    private val leagueTierRepository: LeagueTierRepository,
    private val leagueCohortRepository: LeagueCohortRepository,
    private val leagueMembershipRepository: LeagueMembershipRepository,
    private val userProgressRepository: UserProgressRepository,
    private val economyService: EconomyService,
    private val clock: Clock
) {

    companion object {
        const val COHORT_SIZE = 30
        const val MIN_TIER_ORDER = 1
        const val MAX_TIER_ORDER = 6
    }

    /** ISO week key like "2026-W23". */
    fun weekKey(date: LocalDate): String {
        val week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        val year = date.get(IsoFields.WEEK_BASED_YEAR)
        return "%04d-W%02d".format(year, week)
    }

    /**
     * Returns the user's membership in an OPEN cohort for (tierId, current week),
     * creating the cohort and/or membership as needed. A new cohort is created once
     * an OPEN cohort reaches [COHORT_SIZE] members.
     */
    @Transactional
    fun joinOrGetCurrentCohort(userId: UUID, tierId: Int = 1, today: LocalDate): LeagueMembership {
        val week = weekKey(today)
        val openCohorts = leagueCohortRepository.findByTierIdAndWeekKeyAndStatus(tierId, week, "OPEN")

        // If the user already belongs to one of the open cohorts this week, return it.
        for (cohort in openCohorts) {
            val existing = leagueMembershipRepository.findByUserIdAndCohortId(userId, cohort.id)
            if (existing != null) return existing
        }

        // Find an open cohort with room; otherwise create a new one.
        val cohort = openCohorts.firstOrNull { leagueMembershipRepository.countByCohortId(it.id) < COHORT_SIZE }
            ?: leagueCohortRepository.save(LeagueCohort(tierId = tierId, weekKey = week, status = "OPEN"))

        return leagueMembershipRepository.save(
            LeagueMembership(userId = userId, cohortId = cohort.id)
        )
    }

    /** Standings for a cohort, ranked by weekly score descending. */
    fun currentStandings(cohortId: UUID): List<LeagueMembership> =
        leagueMembershipRepository.findByCohortIdOrderByWeeklyScoreDesc(cohortId)

    /** Adds [delta] to the user's cohort weekly score. */
    @Transactional
    fun addWeeklyScore(userId: UUID, cohortId: UUID, delta: Long) {
        val membership = leagueMembershipRepository.findByUserIdAndCohortId(userId, cohortId) ?: return
        leagueMembershipRepository.save(
            membership.copy(weeklyScore = membership.weeklyScore + delta)
        )
    }

    /**
     * Processes every OPEN cohort whose week has passed: ranks members, assigns
     * final_rank + result (PROMOTED / RELEGATED / STAYED), pays promotion rewards,
     * seeds next-week memberships in the adjusted tier, resets weekly scores, and
     * closes the old cohort.
     *
     * Assignment is well-defined: promotions come from the top first, then
     * relegations from the bottom of the remaining, then everyone else STAYS.
     */
    @Transactional
    fun runWeeklyRollover(asOf: LocalDate) {
        val currentWeek = weekKey(asOf)
        val tiersById = leagueTierRepository.findAll().associateBy { it.id }
        val tiersByOrder = tiersById.values.associateBy { it.tierOrder }

        val staleCohorts = leagueCohortRepository.findByStatus("OPEN")
            .filter { it.weekKey != currentWeek }

        for (cohort in staleCohorts) {
            val tier = tiersById[cohort.tierId] ?: continue
            val ranked = leagueMembershipRepository.findByCohortIdOrderByWeeklyScoreDesc(cohort.id)
            val size = ranked.size

            // Promotions take precedence from the top; relegations from the bottom of the rest.
            val promoteN = minOf(tier.promoteCount, size)
            val relegateN = minOf(tier.relegateCount, size - promoteN)

            ranked.forEachIndexed { index, membership ->
                val rank = index + 1
                val result = when {
                    index < promoteN -> "PROMOTED"
                    index >= size - relegateN -> "RELEGATED"
                    else -> "STAYED"
                }

                leagueMembershipRepository.save(
                    membership.copy(finalRank = rank, result = result)
                )

                if (result == "PROMOTED" && tier.promotionReward > 0) {
                    economyService.earn(
                        membership.userId,
                        tier.promotionReward.toLong(),
                        "LEAGUE_PROMOTION",
                        cohort.id
                    )
                }

                val nextTier = nextTierForResult(tier, result, tiersByOrder)
                joinOrGetCurrentCohort(membership.userId, nextTier.id, asOf)

                // Reset competitive weekly score for the processed user.
                userProgressRepository.findByUserId(membership.userId)?.let { progress ->
                    userProgressRepository.save(progress.copy(weeklyScore = 0))
                }
            }

            leagueCohortRepository.save(cohort.copy(status = "CLOSED"))
        }
    }

    private fun nextTierForResult(
        tier: LeagueTier,
        result: String,
        tiersByOrder: Map<Int, LeagueTier>
    ): LeagueTier {
        val targetOrder = when (result) {
            "PROMOTED" -> minOf(tier.tierOrder + 1, MAX_TIER_ORDER)
            "RELEGATED" -> maxOf(tier.tierOrder - 1, MIN_TIER_ORDER)
            else -> tier.tierOrder
        }
        return tiersByOrder[targetOrder] ?: tier
    }

    @Scheduled(cron = "0 0 0 * * MON", zone = "UTC")
    fun scheduledRollover() = runWeeklyRollover(LocalDate.now(clock))
}
