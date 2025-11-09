package com.wordsearch.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordsearch.data.model.GameSession
import com.wordsearch.data.model.SubmitWordRequest
import com.wordsearch.data.model.WordSubmissionResponse
import com.wordsearch.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class GameViewModel @Inject constructor(
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Loading)
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _selectedCells = MutableStateFlow<List<Pair<Int, Int>>>(emptyList())
    val selectedCells: StateFlow<List<Pair<Int, Int>>> = _selectedCells.asStateFlow()

    private val _foundWords = MutableStateFlow<Set<String>>(emptySet())
    val foundWords: StateFlow<Set<String>> = _foundWords.asStateFlow()

    // Track found word paths for drawing lines
    private val _foundWordPaths = MutableStateFlow<List<Pair<String, List<Pair<Int, Int>>>>>(emptyList())
    val foundWordPaths: StateFlow<List<Pair<String, List<Pair<Int, Int>>>>> = _foundWordPaths.asStateFlow()

    private var gameStartTime: Long = 0
    private var currentSession: GameSession? = null

    fun startGame(categoryId: String, gameMode: String = "CLASSIC") {
        viewModelScope.launch {
            _uiState.value = GameUiState.Loading

            try {
                val result = gameRepository.startSession(categoryId, gameMode)
                if (result.isSuccess) {
                    val session = result.getOrNull()!!
                    currentSession = session
                    gameStartTime = System.currentTimeMillis()

                    // Initialize found words from session (important for resumed casual games)
                    _foundWords.value = session.foundWords.map { it.lowercase() }.toSet()

                    _uiState.value = GameUiState.Playing(session)
                } else {
                    _uiState.value = GameUiState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to start game"
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error starting game")
                _uiState.value = GameUiState.Error(e.message ?: "An error occurred")
            }
        }
    }

    // Vector-based selection: select from start position to end position
    fun selectFromStartToEnd(startRow: Int, startCol: Int, endRow: Int, endCol: Int, gridSize: Int) {
        // If start and end are the same, select single cell
        if (startRow == endRow && startCol == endCol) {
            _selectedCells.value = listOf(startRow to startCol)
            return
        }

        // Calculate angle between start and end
        val dy = endRow - startRow
        val dx = endCol - startCol
        val angleRadians = kotlin.math.atan2(dy.toDouble(), dx.toDouble())
        val angleDegrees = Math.toDegrees(angleRadians)

        // Round to nearest 45 degrees (0, 45, 90, 135, 180, -135, -90, -45)
        val roundedAngle = (kotlin.math.round(angleDegrees / 45.0) * 45.0).toInt()

        // Convert angle to direction vector
        val (dirRow, dirCol) = when (roundedAngle) {
            0, -180, 180 -> 0 to 1      // Right
            45 -> -1 to 1               // Up-right
            90 -> -1 to 0               // Up
            135, -135 -> -1 to -1       // Up-left
            -45 -> 1 to 1               // Down-right
            -90 -> 1 to 0               // Down
            -180 -> 0 to -1             // Left
            else -> 0 to 1              // Default to right
        }

        // Build path from start to end following the direction
        val path = mutableListOf<Pair<Int, Int>>()
        var currentRow = startRow
        var currentCol = startCol

        // Add cells until we go out of bounds or reach a reasonable limit
        while (currentRow in 0 until gridSize && currentCol in 0 until gridSize && path.size < gridSize * 2) {
            path.add(currentRow to currentCol)

            // Check if we've gone past the end position
            if (dirRow != 0) {
                if ((dirRow > 0 && currentRow > endRow) || (dirRow < 0 && currentRow < endRow)) {
                    break
                }
            }
            if (dirCol != 0) {
                if ((dirCol > 0 && currentCol > endCol) || (dirCol < 0 && currentCol < endCol)) {
                    break
                }
            }

            // Move to next cell
            currentRow += dirRow
            currentCol += dirCol
        }

        _selectedCells.value = path
    }

    fun startSelection(row: Int, col: Int) {
        _selectedCells.value = listOf(row to col)

        // Clear any previous message when starting a new selection
        val currentState = _uiState.value
        if (currentState is GameUiState.Playing && currentState.message != null) {
            _uiState.value = GameUiState.Playing(
                session = currentState.session,
                message = null,
                isSuccess = null
            )
        }
    }

    fun updateSelection(endRow: Int, endCol: Int, gridSize: Int) {
        val current = _selectedCells.value
        if (current.isEmpty()) return

        val (startRow, startCol) = current.first()
        selectFromStartToEnd(startRow, startCol, endRow, endCol, gridSize)
    }

    fun clearSelection() {
        _selectedCells.value = emptyList()
    }

    fun submitWord() {
        val session = currentSession ?: return
        val selected = _selectedCells.value
        if (selected.isEmpty()) return

        viewModelScope.launch {
            try {
                // Build the word from selected cells
                val word = buildString {
                    selected.forEach { (row, col) ->
                        append(session.grid[row][col])
                    }
                }

                // Check if word is reversed
                val isReversed = selected.first().first > selected.last().first ||
                        (selected.first().first == selected.last().first &&
                                selected.first().second > selected.last().second)

                // Check if diagonal
                val isDiagonal = selected.size > 1 &&
                        abs(selected[0].first - selected[1].first) == 1 &&
                        abs(selected[0].second - selected[1].second) == 1

                // Calculate time elapsed
                val timeElapsed = ((System.currentTimeMillis() - gameStartTime) / 1000).toInt()

                // Submit to backend
                val result = gameRepository.submitWord(
                    sessionId = session.sessionId,
                    word = word,
                    isReversed = isReversed,
                    isDiagonal = isDiagonal,
                    timeElapsed = timeElapsed
                )
                if (result.isSuccess) {
                    val response = result.getOrNull()!!
                    handleWordSubmission(response, word, selected.toList())
                } else {
                    _uiState.value = GameUiState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to submit word"
                    )
                }

                // Clear selection
                clearSelection()
            } catch (e: Exception) {
                Timber.e(e, "Error submitting word")
                _uiState.value = GameUiState.Error(e.message ?: "An error occurred")
            }
        }
    }

    private fun handleWordSubmission(response: WordSubmissionResponse, word: String, path: List<Pair<Int, Int>>) {
        val currentState = _uiState.value
        if (currentState !is GameUiState.Playing) return

        if (response.correct) {
            // Add to found words
            _foundWords.value = _foundWords.value + word.lowercase()

            // Save the path for this word (for drawing lines)
            _foundWordPaths.value = _foundWordPaths.value + (word.lowercase() to path)

            // Update session with new score and combo
            val updatedSession = currentState.session.copy(
                currentScore = response.score,
                currentCombo = response.combo,
                wordsFound = currentState.session.wordsFound + 1
            )

            _uiState.value = GameUiState.Playing(
                session = updatedSession,
                message = response.message,
                isSuccess = true
            )

            // Check if level up
            if (response.levelUp) {
                _uiState.value = GameUiState.LevelUp(
                    oldLevel = currentState.session.level,
                    newLevel = response.newLevel ?: currentState.session.level,
                    session = updatedSession
                )
            }

            // Check if all words found
            if (updatedSession.wordsFound >= updatedSession.targetWordCount) {
                endGame()
            }
        } else {
            _uiState.value = GameUiState.Playing(
                session = currentState.session,
                message = response.message,
                isSuccess = false
            )
        }
    }

    fun continueAfterLevelUp() {
        val currentState = _uiState.value
        if (currentState is GameUiState.LevelUp) {
            _uiState.value = GameUiState.Playing(currentState.session)
        }
    }

    fun endGame() {
        val session = currentSession ?: return

        viewModelScope.launch {
            try {
                val result = gameRepository.endSession(session.sessionId)
                if (result.isSuccess) {
                    val response = result.getOrNull()!!
                    _uiState.value = GameUiState.GameOver(
                        finalScore = response.finalScore,
                        wordsFound = response.wordsFound,
                        sessionDuration = response.sessionDuration
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error ending game")
            }
        }
    }

    fun saveCasualProgress() {
        val session = currentSession ?: return

        viewModelScope.launch {
            try {
                val result = gameRepository.saveCasualProgress(session.sessionId)
                if (result.isSuccess) {
                    val summary = result.getOrNull()!!
                    _uiState.value = GameUiState.CasualSaved(
                        finalScore = summary.totalScore,
                        wordsFound = summary.wordsFound,
                        sessionDuration = summary.duration.toInt()
                    )
                } else {
                    _uiState.value = GameUiState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to save progress"
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error saving casual progress")
                _uiState.value = GameUiState.Error(e.message ?: "An error occurred")
            }
        }
    }
}

sealed class GameUiState {
    object Loading : GameUiState()
    data class Playing(
        val session: GameSession,
        val message: String? = null,
        val isSuccess: Boolean? = null
    ) : GameUiState()
    data class LevelUp(
        val oldLevel: Int,
        val newLevel: Int,
        val session: GameSession
    ) : GameUiState()
    data class GameOver(
        val finalScore: Int,
        val wordsFound: Int,
        val sessionDuration: Int
    ) : GameUiState()
    data class CasualSaved(
        val finalScore: Int,
        val wordsFound: Int,
        val sessionDuration: Int
    ) : GameUiState()
    data class Error(val message: String) : GameUiState()
}
