package com.wordsearch.service

import com.wordsearch.model.LeaderboardEntry
import com.wordsearch.repository.LeaderboardEntryRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class LeaderboardServiceTest {
    private lateinit var repo: LeaderboardEntryRepository
    private lateinit var service: LeaderboardService

    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        repo = mockk(relaxed = true)
        service = LeaderboardService(repo)
        every { repo.save(any()) } answers { firstArg() }
    }

    @Test fun `upsert inserts a new row when none exists`() {
        every { repo.findByUserIdAndBoardTypeAndPeriodKey(userId, "GLOBAL_CLASSIC", "ALL_TIME") } returns null
        val saved = slot<LeaderboardEntry>()
        every { repo.save(capture(saved)) } answers { firstArg() }

        service.upsert(userId, "alice", "GLOBAL_CLASSIC", "ALL_TIME", 500)

        assertEquals(userId, saved.captured.userId)
        assertEquals("alice", saved.captured.username)
        assertEquals(500L, saved.captured.score)
        assertEquals("GLOBAL_CLASSIC", saved.captured.boardType)
        assertEquals("ALL_TIME", saved.captured.periodKey)
    }

    @Test fun `upsert updates score on existing row preserving its id`() {
        val existing = LeaderboardEntry(
            userId = userId, username = "alice", boardType = "GLOBAL_CLASSIC",
            periodKey = "ALL_TIME", score = 500
        )
        every { repo.findByUserIdAndBoardTypeAndPeriodKey(userId, "GLOBAL_CLASSIC", "ALL_TIME") } returns existing
        val saved = slot<LeaderboardEntry>()
        every { repo.save(capture(saved)) } answers { firstArg() }

        service.upsert(userId, "alice", "GLOBAL_CLASSIC", "ALL_TIME", 1200)

        assertEquals(existing.id, saved.captured.id) // same row updated
        assertEquals(1200L, saved.captured.score)
    }

    @Test fun `topEntries returns repo ordering capped at limit`() {
        val entries = (0 until 5).map {
            LeaderboardEntry(
                userId = UUID.randomUUID(), username = "u$it",
                boardType = "GLOBAL_CLASSIC", periodKey = "ALL_TIME",
                score = (500 - it * 10).toLong()
            )
        }
        every { repo.findByBoardTypeAndPeriodKeyOrderByScoreDesc("GLOBAL_CLASSIC", "ALL_TIME") } returns entries

        val top = service.topEntries("GLOBAL_CLASSIC", "ALL_TIME", limit = 3)
        assertEquals(3, top.size)
        assertEquals(listOf(500L, 490L, 480L), top.map { it.score })
    }
}
