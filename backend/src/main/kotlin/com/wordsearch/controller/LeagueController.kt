package com.wordsearch.controller

import com.wordsearch.dto.ErrorResponse
import com.wordsearch.repository.LeagueCohortRepository
import com.wordsearch.repository.LeagueTierRepository
import com.wordsearch.repository.UserRepository
import com.wordsearch.service.LeagueService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/leagues")
class LeagueController(
    private val leagueService: LeagueService,
    private val leagueCohortRepository: LeagueCohortRepository,
    private val leagueTierRepository: LeagueTierRepository,
    private val userRepository: UserRepository,
    private val clock: Clock
) {

    @GetMapping("/me")
    fun myLeague(authentication: Authentication): ResponseEntity<Any> {
        return try {
            val userId = UUID.fromString(authentication.principal as String)
            val today = LocalDate.now(clock)
            val membership = leagueService.joinOrGetCurrentCohort(userId, today = today)
            val cohort = leagueCohortRepository.findById(membership.cohortId).orElse(null)
            val tier = cohort?.let { leagueTierRepository.findById(it.tierId).orElse(null) }
            val standings = leagueService.currentStandings(membership.cohortId)

            val usernames = userRepository.findAllById(standings.map { it.userId })
                .associate { it.id to it.username }

            val ranked = standings.mapIndexed { index, m ->
                mapOf(
                    "rank" to index + 1,
                    "userId" to m.userId.toString(),
                    "username" to (usernames[m.userId] ?: ""),
                    "weeklyScore" to m.weeklyScore,
                    "result" to m.result
                )
            }

            // Flat shape matching the Android LeagueMeResponse contract.
            ResponseEntity.ok(
                mapOf(
                    "tierName" to (tier?.name ?: "Bronze"),
                    "tierOrder" to (tier?.tierOrder ?: 1),
                    "weekKey" to leagueService.weekKey(today),
                    "myUserId" to userId.toString(),
                    "standings" to ranked
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(e.message ?: "Failed to load league"))
        } catch (e: Exception) {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse(e.message ?: "Internal server error"))
        }
    }
}
