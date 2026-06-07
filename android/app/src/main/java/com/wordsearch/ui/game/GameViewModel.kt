package com.wordsearch.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordsearch.data.model.CellDto
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

    // Bonus words found (not part of the target word list)
    private val _bonusWords = MutableStateFlow<Set<String>>(emptySet())
    val bonusWords: StateFlow<Set<String>> = _bonusWords.asStateFlow()

    // Last word submission result, used to drive score-breakdown UI
    private val _lastWordResult = MutableStateFlow<LastWordResult?>(null)
    val lastWordResult: StateFlow<LastWordResult?> = _lastWordResult.asStateFlow()

    // Track found word paths for drawing lines
    private val _foundWordPaths = MutableStateFlow<List<Pair<String, List<Pair<Int, Int>>>>>(emptyList())
    val foundWordPaths: StateFlow<List<Pair<String, List<Pair<Int, Int>>>>> = _foundWordPaths.asStateFlow()

    // Hint: cells of the most-recently hinted word (cleared when a new selection starts)
    private val _hintedCells = MutableStateFlow<List<Pair<Int, Int>>>(emptyList())
    val hintedCells: StateFlow<List<Pair<Int, Int>>> = _hintedCells.asStateFlow()

    // Live coin balance (updated after word submissions and hint use)
    private val _coinBalance = MutableStateFlow<Long>(0L)
    val coinBalance: StateFlow<Long> = _coinBalance.asStateFlow()

    // Transient hint feedback message (error or confirmation)
    private val _hintMessage = MutableStateFlow<String?>(null)
    val hintMessage: StateFlow<String?> = _hintMessage.asStateFlow()

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
                    _bonusWords.value = emptySet()
                    _lastWordResult.value = null

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

    // Vector-based selection: select from start position to end position.
    // Snaps the drag to one of the 8 directions in GRID coordinates (row increases
    // downward), so all directions work — not just left-to-right.
    fun selectFromStartToEnd(startRow: Int, startCol: Int, endRow: Int, endCol: Int, gridSize: Int) {
        if (startRow == endRow && startCol == endCol) {
            _selectedCells.value = listOf(startRow to startCol)
            return
        }

        val dRow = endRow - startRow
        val dCol = endCol - startCol
        val aRow = abs(dRow)
        val aCol = abs(dCol)

        // Pick horizontal, vertical, or diagonal based on which axis dominates.
        val stepRow: Int
        val stepCol: Int
        val steps: Int
        when {
            aCol > aRow * 2 -> { stepRow = 0; stepCol = dCol.coerceIn(-1, 1); steps = aCol }            // horizontal
            aRow > aCol * 2 -> { stepRow = dRow.coerceIn(-1, 1); stepCol = 0; steps = aRow }            // vertical
            else -> { stepRow = dRow.coerceIn(-1, 1); stepCol = dCol.coerceIn(-1, 1); steps = maxOf(aRow, aCol) } // diagonal
        }

        _selectedCells.value = (0..steps)
            .map { i -> (startRow + stepRow * i) to (startCol + stepCol * i) }
            .filter { (r, c) -> r in 0 until gridSize && c in 0 until gridSize }
        return
    }

    fun startSelection(row: Int, col: Int) {
        _selectedCells.value = listOf(row to col)
        // Clear hint highlight when the user starts a new drag
        _hintedCells.value = emptyList()

        // Clear any previous message and score breakdown when starting a new selection
        val currentState = _uiState.value
        if (currentState is GameUiState.Playing && currentState.message != null) {
            _uiState.value = GameUiState.Playing(
                session = currentState.session,
                message = null,
                isSuccess = null,
                isBonus = false
            )
        }
        _lastWordResult.value = null
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

                // Map selected cells to CellDto path for the backend
                val cellPath = selected.map { (row, col) -> CellDto(row, col) }

                // Submit to backend
                val result = gameRepository.submitWord(
                    sessionId = session.sessionId,
                    word = word,
                    isReversed = isReversed,
                    isDiagonal = isDiagonal,
                    timeElapsed = timeElapsed,
                    path = cellPath
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

        // Keep coin balance in sync
        if (response.coinBalance > 0L) {
            _coinBalance.value = response.coinBalance
        }

        if (response.correct) {
            // Save the path for this word (for drawing lines)
            _foundWordPaths.value = _foundWordPaths.value + (word.lowercase() to path)

            // Emit the score breakdown result for UI feedback
            _lastWordResult.value = LastWordResult(
                word = word,
                score = response.score,
                isBonus = response.isBonus,
                coinsEarned = response.coinsEarned,
                scoreBreakdown = response.scoreBreakdown
            )

            if (response.isBonus) {
                // Bonus words are NOT target words — track them separately
                _bonusWords.value = _bonusWords.value + word.lowercase()

                // Update session score/combo without incrementing wordsFound target counter
                val updatedSession = currentState.session.copy(
                    currentScore = response.score,
                    currentCombo = response.combo
                )
                _uiState.value = GameUiState.Playing(
                    session = updatedSession,
                    message = response.message,
                    isSuccess = true,
                    isBonus = true
                )
            } else {
                // Regular target word
                _foundWords.value = _foundWords.value + word.lowercase()

                // Update session with new score and combo
                val updatedSession = currentState.session.copy(
                    currentScore = response.score,
                    currentCombo = response.combo,
                    wordsFound = currentState.session.wordsFound + 1
                )

                _uiState.value = GameUiState.Playing(
                    session = updatedSession,
                    message = response.message,
                    isSuccess = true,
                    isBonus = false
                )

                // Check if level up
                if (response.levelUp) {
                    _uiState.value = GameUiState.LevelUp(
                        oldLevel = currentState.session.level,
                        newLevel = response.newLevel ?: currentState.session.level,
                        session = updatedSession
                    )
                }

                // Check if all target words found
                if (updatedSession.wordsFound >= updatedSession.targetWordCount) {
                    endGame()
                }
            }
        } else {
            _lastWordResult.value = null
            _uiState.value = GameUiState.Playing(
                session = currentState.session,
                message = response.message,
                isSuccess = false
            )
        }
    }

    fun useHint() {
        val session = currentSession ?: return

        viewModelScope.launch {
            try {
                val result = gameRepository.useHint(session.sessionId)
                if (result.isSuccess) {
                    val hintResponse = result.getOrNull()!!
                    // Expose the hinted cells for distinct highlight in the grid
                    _hintedCells.value = hintResponse.cells.map { it.row to it.col }
                    // Update coin balance
                    _coinBalance.value = hintResponse.coinBalance
                    _hintMessage.value = "Hint: ${hintResponse.word}"
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Could not use hint"
                    _hintMessage.value = errorMessage
                }
            } catch (e: Exception) {
                Timber.e(e, "Error using hint")
                _hintMessage.value = e.message ?: "An error occurred"
            }
        }
    }

    fun clearHintMessage() {
        _hintMessage.value = null
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
                    val summary = result.getOrNull()!!
                    _uiState.value = GameUiState.GameOver(
                        finalScore = summary.totalScore,
                        wordsFound = summary.wordsFound,
                        sessionDuration = summary.duration.toInt(),
                        casualBest = summary.casualBestScore,
                        casualGames = summary.casualGamesPlayed,
                        casualWeekly = summary.casualWeeklyScore
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

// Holds the breakdown of the last correctly-submitted word for transient UI display
data class LastWordResult(
    val word: String,
    val score: Int,
    val isBonus: Boolean,
    val coinsEarned: Long,
    val scoreBreakdown: Map<String, Int>
)

sealed class GameUiState {
    object Loading : GameUiState()
    data class Playing(
        val session: GameSession,
        val message: String? = null,
        val isSuccess: Boolean? = null,
        val isBonus: Boolean = false
    ) : GameUiState()
    data class LevelUp(
        val oldLevel: Int,
        val newLevel: Int,
        val session: GameSession
    ) : GameUiState()
    data class GameOver(
        val finalScore: Int,
        val wordsFound: Int,
        val sessionDuration: Int,
        val casualBest: Long = 0,
        val casualGames: Int = 0,
        val casualWeekly: Long = 0
    ) : GameUiState()
    data class CasualSaved(
        val finalScore: Int,
        val wordsFound: Int,
        val sessionDuration: Int
    ) : GameUiState()
    data class Error(val message: String) : GameUiState()
}
