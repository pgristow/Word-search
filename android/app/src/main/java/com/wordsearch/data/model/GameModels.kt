package com.wordsearch.data.model

import com.google.gson.annotations.SerializedName

// Game Mode
enum class GameMode {
    CLASSIC,
    CASUAL
}

// Category
data class Category(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String?,
    @SerializedName("unlockLevel")
    val unlockLevel: Int,
    @SerializedName("wordCount")
    val wordCount: Int,
    @SerializedName("iconUrl")
    val iconUrl: String?
)

// Game Session
data class GameSession(
    @SerializedName("sessionId")
    val sessionId: String,
    @SerializedName("level")
    val level: Int,
    @SerializedName("gridSize")
    val gridSize: Int,
    @SerializedName("category")
    val category: String,
    @SerializedName("grid")
    val grid: List<List<Char>>,
    @SerializedName("words")
    val words: List<WordInfo>,
    @SerializedName("targetWordCount")
    val targetWordCount: Int,
    @SerializedName("currentScore")
    val currentScore: Int,
    @SerializedName("wordsFound")
    val wordsFound: Int,
    @SerializedName("currentCombo")
    val currentCombo: Int,
    @SerializedName("gameMode")
    val gameMode: String = "CLASSIC"
)

data class WordInfo(
    @SerializedName("word")
    val word: String,
    @SerializedName("isReversed")
    val isReversed: Boolean
)

data class PlacedWord(
    val word: String,
    val startRow: Int,
    val startCol: Int,
    val endRow: Int,
    val endCol: Int,
    val isReversed: Boolean,
    val isDiagonal: Boolean
)

// Word submission
data class SubmitWordRequest(
    val word: String,
    val isReversed: Boolean,
    val isDiagonal: Boolean,
    val timeElapsed: Int
)

data class WordSubmissionResponse(
    @SerializedName("correct")
    val correct: Boolean,
    @SerializedName("score")
    val score: Int,
    @SerializedName("combo")
    val combo: Int,
    @SerializedName("levelUp")
    val levelUp: Boolean,
    @SerializedName("newLevel")
    val newLevel: Int?,
    @SerializedName("message")
    val message: String
)

// User Progress
data class UserProgress(
    @SerializedName("currentLevel")
    val currentLevel: Int,
    @SerializedName("totalScore")
    val totalScore: Int,
    @SerializedName("totalWordsFound")
    val totalWordsFound: Int,
    @SerializedName("highestCombo")
    val highestCombo: Int,
    @SerializedName("currentStreak")
    val currentStreak: Int,
    @SerializedName("longestStreak")
    val longestStreak: Int,
    @SerializedName("bossLevelsCompleted")
    val bossLevelsCompleted: Int,
    @SerializedName("casualPuzzlesCompleted")
    val casualPuzzlesCompleted: Int = 0
)

// Leaderboard
data class LeaderboardEntry(
    @SerializedName("rank")
    val rank: Int,
    @SerializedName("username")
    val username: String,
    @SerializedName("totalScore")
    val totalScore: Int,
    @SerializedName("currentLevel")
    val currentLevel: Int,
    @SerializedName("isPremium")
    val isPremium: Boolean
)
