package com.wordsearch.service

import com.wordsearch.model.LeagueCohort
import com.wordsearch.model.LeagueMembership
import com.wordsearch.model.LeagueTier
import com.wordsearch.model.UserProgress
import com.wordsearch.repository.LeagueCohortRepository
import com.wordsearch.repository.LeagueMembershipRepository
import com.wordsearch.repository.LeagueTierRepository
import com.wordsearch.repository.UserProgressRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

class LeagueServiceTest {
    private lateinit var tierRepo: LeagueTierRepository
    private lateinit var cohortRepo: LeagueCohortRepository
    private lateinit var membershipRepo: LeagueMembershipRepository
    private lateinit var userProgressRepo: UserProgressRepository
    private lateinit var economyService: EconomyService
    private lateinit var service: LeagueService

    private val today = LocalDate.of(2026, 6, 1) // ISO week 23 of 2026

    private val tiers = listOf(
        LeagueTier(1, "Bronze", 1, 7, 0, 50),
        LeagueTier(2, "Silver", 2, 7, 5, 75),
        LeagueTier(3, "Gold", 3, 7, 5, 100),
        LeagueTier(4, "Platinum", 4, 7, 5, 150),
        LeagueTier(5, "Diamond", 5, 7, 5, 200),
        LeagueTier(6, "Master", 6, 0, 5, 300)
    )

    @BeforeEach
    fun setup() {
        tierRepo = mockk(relaxed = true)
        cohortRepo = mockk(relaxed = true)
        membershipRepo = mockk(relaxed = true)
        userProgressRepo = mockk(relaxed = true)
        economyService = mockk(relaxed = true)
        service = LeagueService(tierRepo, cohortRepo, membershipRepo, userProgressRepo, economyService, Clock.systemUTC())

        every { cohortRepo.save(any()) } answers { firstArg() }
        every { membershipRepo.save(any()) } answers { firstArg() }
        every { userProgressRepo.save(any()) } answers { firstArg() }
        every { tierRepo.findAll() } returns tiers
        every { tierRepo.findById(any()) } answers {
            java.util.Optional.ofNullable(tiers.find { it.id == firstArg<Int>() })
        }
    }

    @Test fun `weekKey formats ISO week`() {
        assertEquals("2026-W23", service.weekKey(today))
    }

    @Test fun `joins existing open cohort with room`() {
        val cohort = LeagueCohort(tierId = 1, weekKey = "2026-W23", status = "OPEN")
        every { cohortRepo.findByTierIdAndWeekKeyAndStatus(1, "2026-W23", "OPEN") } returns listOf(cohort)
        every { membershipRepo.findByUserIdAndCohortId(any(), cohort.id) } returns null
        every { membershipRepo.countByCohortId(cohort.id) } returns 5L

        val userId = UUID.randomUUID()
        val m = service.joinOrGetCurrentCohort(userId, today = today)
        assertEquals(cohort.id, m.cohortId)
        assertEquals(userId, m.userId)
    }

    @Test fun `31st joiner creates a second cohort`() {
        val full = LeagueCohort(tierId = 1, weekKey = "2026-W23", status = "OPEN")
        every { cohortRepo.findByTierIdAndWeekKeyAndStatus(1, "2026-W23", "OPEN") } returns listOf(full)
        every { membershipRepo.findByUserIdAndCohortId(any(), full.id) } returns null
        every { membershipRepo.countByCohortId(full.id) } returns 30L // full

        val savedCohort = slot<LeagueCohort>()
        every { cohortRepo.save(capture(savedCohort)) } answers { firstArg() }

        val m = service.joinOrGetCurrentCohort(UUID.randomUUID(), today = today)
        // A brand new cohort was created (not the full one).
        verify { cohortRepo.save(any()) }
        assertNotEquals(full.id, m.cohortId)
        assertEquals(savedCohort.captured.id, m.cohortId)
    }

    @Test fun `creates first cohort when none exist`() {
        every { cohortRepo.findByTierIdAndWeekKeyAndStatus(1, "2026-W23", "OPEN") } returns emptyList()
        val savedCohort = slot<LeagueCohort>()
        every { cohortRepo.save(capture(savedCohort)) } answers { firstArg() }

        val m = service.joinOrGetCurrentCohort(UUID.randomUUID(), today = today)
        assertEquals(savedCohort.captured.id, m.cohortId)
        assertEquals("2026-W23", savedCohort.captured.weekKey)
    }

    @Test fun `returns existing membership if user already in an open cohort`() {
        val cohort = LeagueCohort(tierId = 1, weekKey = "2026-W23", status = "OPEN")
        val userId = UUID.randomUUID()
        val existing = LeagueMembership(userId = userId, cohortId = cohort.id, weeklyScore = 42)
        every { cohortRepo.findByTierIdAndWeekKeyAndStatus(1, "2026-W23", "OPEN") } returns listOf(cohort)
        every { membershipRepo.findByUserIdAndCohortId(userId, cohort.id) } returns existing

        val m = service.joinOrGetCurrentCohort(userId, today = today)
        assertEquals(42L, m.weeklyScore)
        verify(exactly = 0) { membershipRepo.save(any()) }
    }

    @Test fun `currentStandings returns sorted desc`() {
        val cohortId = UUID.randomUUID()
        val sorted = listOf(
            LeagueMembership(userId = UUID.randomUUID(), cohortId = cohortId, weeklyScore = 300),
            LeagueMembership(userId = UUID.randomUUID(), cohortId = cohortId, weeklyScore = 200),
            LeagueMembership(userId = UUID.randomUUID(), cohortId = cohortId, weeklyScore = 100)
        )
        every { membershipRepo.findByCohortIdOrderByWeeklyScoreDesc(cohortId) } returns sorted

        val standings = service.currentStandings(cohortId)
        assertEquals(listOf(300L, 200L, 100L), standings.map { it.weeklyScore })
    }

    @Test fun `addWeeklyScore increments membership score`() {
        val userId = UUID.randomUUID()
        val cohortId = UUID.randomUUID()
        val membership = LeagueMembership(userId = userId, cohortId = cohortId, weeklyScore = 50)
        every { membershipRepo.findByUserIdAndCohortId(userId, cohortId) } returns membership
        val saved = slot<LeagueMembership>()
        every { membershipRepo.save(capture(saved)) } answers { firstArg() }

        service.addWeeklyScore(userId, cohortId, 25)
        assertEquals(75L, saved.captured.weeklyScore)
    }

    // ---- Task 11: weekly rollover ----

    @Test fun `rollover of Gold cohort of 10 promotes top 7 and relegates rest`() {
        val goldCohort = LeagueCohort(tierId = 3, weekKey = "2026-W22", status = "OPEN")
        // asOf is week 23; cohort was week 22 -> stale.
        val asOf = today // 2026-W23

        every { cohortRepo.findByStatus("OPEN") } returns listOf(goldCohort)
        // After rollover, joinOrGetCurrentCohort will look up open cohorts for the new tiers.
        every { cohortRepo.findByTierIdAndWeekKeyAndStatus(any(), "2026-W23", "OPEN") } returns emptyList()
        every { membershipRepo.findByUserIdAndCohortId(any(), any()) } returns null

        // 10 members, descending scores already.
        val members = (0 until 10).map {
            LeagueMembership(userId = UUID.randomUUID(), cohortId = goldCohort.id, weeklyScore = (1000 - it * 10).toLong())
        }
        every { membershipRepo.findByCohortIdOrderByWeeklyScoreDesc(goldCohort.id) } returns members
        every { userProgressRepo.findByUserId(any()) } answers {
            UserProgress(userId = firstArg(), weeklyScore = 999)
        }

        val savedMemberships = mutableListOf<LeagueMembership>()
        every { membershipRepo.save(capture(savedMemberships)) } answers { firstArg() }
        val savedProgress = mutableListOf<UserProgress>()
        every { userProgressRepo.save(capture(savedProgress)) } answers { firstArg() }

        service.runWeeklyRollover(asOf)

        // The final_rank/result updates are the ones with a non-null result.
        val results = savedMemberships.filter { it.result != null }
        assertEquals(10, results.size)
        assertEquals(7, results.count { it.result == "PROMOTED" })
        // Gold relegate_count=5, but only 3 remain after 7 promoted -> 3 relegated, 0 stayed.
        assertEquals(3, results.count { it.result == "RELEGATED" })
        assertEquals(0, results.count { it.result == "STAYED" })

        // Each promoted user paid 100 coins.
        verify(exactly = 7) { economyService.earn(any(), 100L, "LEAGUE_PROMOTION", goldCohort.id) }

        // Old cohort closed.
        assertTrue(savedMemberships.isNotEmpty())
        // Weekly scores reset to 0 for all 10 processed users.
        assertEquals(10, savedProgress.count { it.weeklyScore == 0L })

        // Cohort closed.
        // (cohortRepo.save called with CLOSED status)
        verify { cohortRepo.save(match { it is LeagueCohort && it.id == goldCohort.id && it.status == "CLOSED" }) }
    }

    @Test fun `rollover skips cohorts in the current week`() {
        val currentCohort = LeagueCohort(tierId = 1, weekKey = "2026-W23", status = "OPEN")
        every { cohortRepo.findByStatus("OPEN") } returns listOf(currentCohort)

        service.runWeeklyRollover(today) // also 2026-W23

        verify(exactly = 0) { membershipRepo.findByCohortIdOrderByWeeklyScoreDesc(any()) }
        verify(exactly = 0) { cohortRepo.save(any()) }
    }
}
