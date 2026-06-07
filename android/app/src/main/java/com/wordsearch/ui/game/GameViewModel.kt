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

    // Drives the celebratory word popup over the grid. New id => re-trigger animation.
    private val _wordPopup = MutableStateFlow<WordPopup?>(null)
    val wordPopup: StateFlow<WordPopup?> = _wordPopup.asStateFlow()
    private var popupCounter = 0L
    fun clearWordPopup() { _wordPopup.value = null }

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
                val path = selected.toList()

                // OPTIMISTIC: if the traced word is one of this board's target words and
                // not yet found, confirm it instantly on-device (the server will agree),
                // so there's no ~1s wait. The server call below just syncs the score.
                val wordUpper = word.uppercase()
                val targets = session.words.map { it.word.uppercase() }.toSet()
                val optimistic = wordUpper in targets && wordUpper !in _foundWords.value.map { it.uppercase() }
                if (optimistic) {
                    _foundWords.value = _foundWords.value + word.lowercase()
                    _foundWordPaths.value = _foundWordPaths.value + (word.lowercase() to path)
                    _uiState.value = GameUiState.Playing(
                        session = session.copy(wordsFound = session.wordsFound + 1),
                        message = null, isSuccess = true, isBonus = false
                    )
                    showPopup(word, isBonus = false, score = null, coins = 0)
                }
                clearSelection()

                // Submit to backend (authoritative score/coins/leaderboard)
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
                    handleWordSubmission(response, word, path, optimisticallyApplied = optimistic)
                } else if (!optimistic) {
                    _uiState.value = GameUiState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to submit word"
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error submitting word")
                _uiState.value = GameUiState.Error(e.message ?: "An error occurred")
            }
        }
    }

    private fun showPopup(word: String, isBonus: Boolean, score: Int?, coins: Long) {
        popupCounter += 1
        _wordPopup.value = WordPopup(popupCounter, word.uppercase(), isBonus, score, coins)
    }

    private fun handleWordSubmission(
        response: WordSubmissionResponse,
        word: String,
        path: List<Pair<Int, Int>>,
        optimisticallyApplied: Boolean = false
    ) {
        val currentState = _uiState.value
        if (currentState !is GameUiState.Playing) return

        if (response.coinBalance > 0L) _coinBalance.value = response.coinBalance

        if (response.correct) {
            _lastWordResult.value = LastWordResult(
                word, response.score, response.isBonus, response.coinsEarned, response.scoreBreakdown
            )

            if (response.isBonus) {
                _bonusWords.value = _bonusWords.value + word.lowercase()
                _foundWordPaths.value = _foundWordPaths.value + (word.lowercase() to path)
                _uiState.value = GameUiState.Playing(
                    session = currentState.session.copy(currentScore = response.score, currentCombo = response.combo),
                    message = null, isSuccess = true, isBonus = true
                )
                showPopup(word, true, response.score, response.coinsEarned)
            } else {
                if (!optimisticallyApplied) {
                    _foundWords.value = _foundWords.value + word.lowercase()
                    _foundWordPaths.value = _foundWordPaths.value + (word.lowercase() to path)
                }
                val wordsFound =
                    if (optimisticallyApplied) currentState.session.wordsFound
                    else currentState.session.wordsFound + 1
                val updatedSession = currentState.session.copy(
                    currentScore = response.score, currentCombo = response.combo, wordsFound = wordsFound
                )
                _uiState.value = GameUiState.Playing(updatedSession, null, true, false)
                // Fill in the score on the already-shown popup, or show a fresh one.
                if (optimisticallyApplied) {
                    _wordPopup.value = _wordPopup.value?.copy(score = response.score, coins = response.coinsEarned)
                } else {
                    showPopup(word, false, response.score, response.coinsEarned)
                }
                if (response.levelUp) {
                    _uiState.value = GameUiState.LevelUp(
                        currentState.session.level, response.newLevel ?: currentState.session.level, updatedSession
                    )
                }
                if (updatedSession.wordsFound >= updatedSession.targetWordCount) endGame()
            }
        } else if (optimisticallyApplied) {
            // Server disagreed with our optimistic accept — roll it back.
            _foundWords.value = _foundWords.value - word.lowercase()
            _foundWordPaths.value = _foundWordPaths.value.filterNot { it.first == word.lowercase() }
            _wordPopup.value = null
            _uiState.value = GameUiState.Playing(
                session = currentState.session.copy(wordsFound = maxOf(0, currentState.session.wordsFound - 1)),
                message = null, isSuccess = false
            )
            _hintMessage.value = response.message
        } else {
            _lastWordResult.value = null
            _uiState.value = GameUiState.Playing(currentState.session, message = null, isSuccess = false)
            _hintMessage.value = response.message
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

// Drives the celebratory popup over the grid. A new [id] re-triggers the pop animation.
data class WordPopup(
    val id: Long,
    val word: String,
    val isBonus: Boolean,
    val score: Int?,
    val coins: Long
)

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
