package com.wordsearch.service

import com.wordsearch.model.DailyChallenge
import com.wordsearch.model.UserDailyAttempt
import com.wordsearch.repository.DailyChallengeRepository
import com.wordsearch.repository.UserDailyAttemptRepository
import com.wordsearch.repository.CategoryRepository
import com.wordsearch.repository.UserProgressRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

data class DailyChallengeResponse(
    val challengeId: UUID,
    val challengeDate: LocalDate,
    val category: String,
    val categoryId: UUID,
    val difficultyLevel: Int,
    val targetScore: Int,
    val targetWords: Int,
    val timeLimitSeconds: Int,
    val bonusMultiplier: Float,
    val userAttempt: UserAttemptResponse?
)

data class UserAttemptResponse(
    val attemptId: UUID,
    val scoreAchieved: Int,
    val wordsFound: Int,
    val timeTakenSeconds: Int?,
    val completed: Boolean,
    val rewardClaimed: Boolean,
    val attemptedAt: LocalDateTime
)

data class DailyChallengeStartResponse(
    val attemptId: UUID,
    val challenge: DailyChallengeResponse,
    val gameBoard: GameBoard,
    val message: String
)

data class DailyChallengeCompletionResponse(
    val attemptId: UUID,
    val challengeId: UUID,
    val scoreAchieved: Int,
    val wordsFound: Int,
    val targetScore: Int,
    val targetWords: Int,
    val timeTakenSeconds: Int,
    val completed: Boolean,
    val bonusPoints: Int,
    val totalReward: Int,
    val message: String
)

data class DailyChallengeHistory(
    val totalAttempts: Int,
    val completedChallenges: Int,
    val totalRewardsEarned: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val recentAttempts: List<DailyChallengeResponse>
)

@Service
class DailyChallengeService(
    private val dailyChallengeRepository: DailyChallengeRepository,
    private val userDailyAttemptRepository: UserDailyAttemptRepository,
    private val categoryRepository: CategoryRepository,
    private val userProgressRepository: UserProgressRepository,
    private val gameBoardGenerator: GameBoardGenerator
) {

    /**
     * Generate today's daily challenge if it doesn't exist
     */
    @Transactional
    fun generateTodaysChallenge(): DailyChallenge {
        val today = LocalDate.now()
        val existingChallenge = dailyChallengeRepository.findByChallengeDate(today)

        if (existingChallenge != null) {
            return existingChallenge
        }

        // Select a random category
        val categories = categoryRepository.findAll()
        if (categories.isEmpty()) {
            throw IllegalStateException("No categories available for daily challenge")
        }

        val randomCategory = categories.random()

        // Generate challenge parameters based on a difficulty level (1-5)
        val difficultyLevel = Random.nextInt(1, 6)
        val (targetScore, targetWords, timeLimitSeconds) = when (difficultyLevel) {
            1 -> Triple(5000, 5, 300)    // 5 minutes, 5 words, 5000 score
            2 -> Triple(10000, 8, 360)   // 6 minutes, 8 words, 10000 score
            3 -> Triple(20000, 10, 420)  // 7 minutes, 10 words, 20000 score
            4 -> Triple(35000, 15, 480)  // 8 minutes, 15 words, 35000 score
            5 -> Triple(50000, 20, 540)  // 9 minutes, 20 words, 50000 score
            else -> Triple(10000, 8, 360)
        }

        val bonusMultiplier = 1.5f + (difficultyLevel * 0.1f) // 1.6x to 2.0x

        val dailyChallenge = DailyChallenge(
            challengeDate = today,
            categoryId = randomCategory.id,
            difficultyLevel = difficultyLevel,
            targetScore = targetScore,
            targetWords = targetWords,
            timeLimitSeconds = timeLimitSeconds,
            bonusMultiplier = bonusMultiplier
        )

        return dailyChallengeRepository.save(dailyChallenge)
    }

    /**
     * Get today's daily challenge with user's attempt if exists
     */
    fun getTodaysChallenge(userId: UUID): DailyChallengeResponse {
        val today = LocalDate.now()
        val challenge = dailyChallengeRepository.findByChallengeDate(today)
            ?: generateTodaysChallenge()

        val category = categoryRepository.findById(challenge.categoryId)
            .orElseThrow { IllegalStateException("Category not found") }

        val userAttempt = userDailyAttemptRepository.findByUserIdAndChallengeId(
            userId,
            challenge.id
        )

        val attemptResponse = userAttempt?.let {
            UserAttemptResponse(
                attemptId = it.id,
                scoreAchieved = it.scoreAchieved,
                wordsFound = it.wordsFound,
                timeTakenSeconds = it.timeTakenSeconds,
                completed = it.completed,
                rewardClaimed = it.rewardClaimed,
                attemptedAt = it.attemptedAt
            )
        }

        return DailyChallengeResponse(
            challengeId = challenge.id,
            challengeDate = challenge.challengeDate,
            category = category.name,
            categoryId = category.id,
            difficultyLevel = challenge.difficultyLevel,
            targetScore = challenge.targetScore,
            targetWords = challenge.targetWords,
            timeLimitSeconds = challenge.timeLimitSeconds,
            bonusMultiplier = challenge.bonusMultiplier,
            userAttempt = attemptResponse
        )
    }

    /**
     * Start a daily challenge attempt
     */
    @Transactional
    fun startDailyChallenge(userId: UUID): DailyChallengeStartResponse {
        val challenge = generateTodaysChallenge()

        // Check if user already has an attempt for today
        val existingAttempt = userDailyAttemptRepository.findByUserIdAndChallengeId(
            userId,
            challenge.id
        )

        if (existingAttempt != null) {
            throw IllegalStateException("You have already attempted today's daily challenge")
        }

        // Create new attempt
        val attempt = UserDailyAttempt(
            userId = userId,
            challengeId = challenge.id,
            scoreAchieved = 0,
            wordsFound = 0,
            completed = false,
            rewardClaimed = false
        )

        val savedAttempt = userDailyAttemptRepository.save(attempt)

        // Generate game board based on challenge difficulty
        val category = categoryRepository.findById(challenge.categoryId)
            .orElseThrow { IllegalStateException("Category not found") }

        val gameBoard = gameBoardGenerator.generateBoardForCategory(
            challenge.categoryId,
            challenge.difficultyLevel,
            challenge.targetWords
        )

        val challengeResponse = DailyChallengeResponse(
            challengeId = challenge.id,
            challengeDate = challenge.challengeDate,
            category = category.name,
            categoryId = category.id,
            difficultyLevel = challenge.difficultyLevel,
            targetScore = challenge.targetScore,
            targetWords = challenge.targetWords,
            timeLimitSeconds = challenge.timeLimitSeconds,
            bonusMultiplier = challenge.bonusMultiplier,
            userAttempt = UserAttemptResponse(
                attemptId = savedAttempt.id,
                scoreAchieved = 0,
                wordsFound = 0,
                timeTakenSeconds = null,
                completed = false,
                rewardClaimed = false,
                attemptedAt = savedAttempt.attemptedAt
            )
        )

        return DailyChallengeStartResponse(
            attemptId = savedAttempt.id,
            challenge = challengeResponse,
            gameBoard = gameBoard,
            message = "Daily challenge started! You have ${challenge.timeLimitSeconds / 60} minutes."
        )
    }

    /**
     * Complete a daily challenge attempt
     */
    @Transactional
    fun completeDailyChallenge(
        attemptId: UUID,
        userId: UUID,
        scoreAchieved: Int,
        wordsFound: Int,
        timeTakenSeconds: Int
    ): DailyChallengeCompletionResponse {
        val attempt = userDailyAttemptRepository.findById(attemptId)
            .orElseThrow { IllegalArgumentException("Attempt not found") }

        if (attempt.userId != userId) {
            throw IllegalArgumentException("This attempt does not belong to you")
        }

        if (attempt.completed) {
            throw IllegalStateException("This challenge has already been completed")
        }

        val challenge = dailyChallengeRepository.findById(attempt.challengeId)
            .orElseThrow { IllegalStateException("Challenge not found") }

        // Check if challenge is completed successfully
        val isCompleted = scoreAchieved >= challenge.targetScore &&
                         wordsFound >= challenge.targetWords &&
                         timeTakenSeconds <= challenge.timeLimitSeconds

        // Calculate rewards
        val bonusPoints = if (isCompleted) {
            (challenge.targetScore * (challenge.bonusMultiplier - 1)).toInt()
        } else {
            0
        }

        val totalReward = if (isCompleted) {
            scoreAchieved + bonusPoints
        } else {
            scoreAchieved / 2 // Half points for incomplete attempts
        }

        // Update attempt
        val updatedAttempt = attempt.copy(
            scoreAchieved = scoreAchieved,
            wordsFound = wordsFound,
            timeTakenSeconds = timeTakenSeconds,
            completed = isCompleted,
            rewardClaimed = true
        )

        userDailyAttemptRepository.save(updatedAttempt)

        // Award points to user
        val userProgress = userProgressRepository.findByUserId(userId)
            ?: throw IllegalStateException("User progress not found")

        userProgress.totalScore += totalReward
        userProgressRepository.save(userProgress)

        val message = if (isCompleted) {
            "Congratulations! You completed the daily challenge and earned ${totalReward} points!"
        } else {
            "Challenge incomplete. You earned ${totalReward} points for your effort."
        }

        return DailyChallengeCompletionResponse(
            attemptId = attemptId,
            challengeId = challenge.id,
            scoreAchieved = scoreAchieved,
            wordsFound = wordsFound,
            targetScore = challenge.targetScore,
            targetWords = challenge.targetWords,
            timeTakenSeconds = timeTakenSeconds,
            completed = isCompleted,
            bonusPoints = bonusPoints,
            totalReward = totalReward,
            message = message
        )
    }

    /**
     * Get user's daily challenge history
     */
    fun getDailyChallengeHistory(userId: UUID, limit: Int = 30): DailyChallengeHistory {
        val attempts = userDailyAttemptRepository.findByUserId(userId)
        val challengeIds = attempts.map { it.challengeId }
        val challenges = dailyChallengeRepository.findAllById(challengeIds)
            .associateBy { it.id }

        val categoryIds = challenges.values.map { it.categoryId }.toSet()
        val categories = categoryRepository.findAllById(categoryIds)
            .associateBy { it.id }

        val recentAttempts = attempts
            .sortedByDescending { it.attemptedAt }
            .take(limit)
            .mapNotNull { attempt ->
                val challenge = challenges[attempt.challengeId] ?: return@mapNotNull null
                val category = categories[challenge.categoryId] ?: return@mapNotNull null

                DailyChallengeResponse(
                    challengeId = challenge.id,
                    challengeDate = challenge.challengeDate,
                    category = category.name,
                    categoryId = category.id,
                    difficultyLevel = challenge.difficultyLevel,
                    targetScore = challenge.targetScore,
                    targetWords = challenge.targetWords,
                    timeLimitSeconds = challenge.timeLimitSeconds,
                    bonusMultiplier = challenge.bonusMultiplier,
                    userAttempt = UserAttemptResponse(
                        attemptId = attempt.id,
                        scoreAchieved = attempt.scoreAchieved,
                        wordsFound = attempt.wordsFound,
                        timeTakenSeconds = attempt.timeTakenSeconds,
                        completed = attempt.completed,
                        rewardClaimed = attempt.rewardClaimed,
                        attemptedAt = attempt.attemptedAt
                    )
                )
            }

        val completedCount = attempts.count { it.completed }
        val totalRewards = attempts
            .filter { it.rewardClaimed }
            .sumOf { it.scoreAchieved }

        // Calculate streaks
        val sortedAttempts = attempts
            .filter { it.completed }
            .sortedByDescending { challenges[it.challengeId]?.challengeDate }

        var currentStreak = 0
        var longestStreak = 0
        var tempStreak = 0
        var previousDate: LocalDate? = null

        for (attempt in sortedAttempts) {
            val challengeDate = challenges[attempt.challengeId]?.challengeDate ?: continue

            if (previousDate == null) {
                tempStreak = 1
                currentStreak = 1
            } else {
                val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(challengeDate, previousDate)
                if (daysDiff == 1L) {
                    tempStreak++
                    if (previousDate == LocalDate.now() || previousDate == LocalDate.now().minusDays(1)) {
                        currentStreak = tempStreak
                    }
                } else {
                    tempStreak = 1
                }
            }

            longestStreak = maxOf(longestStreak, tempStreak)
            previousDate = challengeDate
        }

        return DailyChallengeHistory(
            totalAttempts = attempts.size,
            completedChallenges = completedCount,
            totalRewardsEarned = totalRewards,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            recentAttempts = recentAttempts
        )
    }
}
