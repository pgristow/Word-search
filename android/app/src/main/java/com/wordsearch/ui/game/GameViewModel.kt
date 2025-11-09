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

    fun onCellSelected(row: Int, col: Int) {
        val current = _selectedCells.value.toMutableList()

        // If this is the first cell, just add it
        if (current.isEmpty()) {
            current.add(row to col)
            _selectedCells.value = current
            return
        }

        // Check if cell is already selected
        val cellIndex = current.indexOf(row to col)
        if (cellIndex != -1) {
            // If it's the last cell, remove it (backtrack)
            if (cellIndex == current.size - 1) {
                current.removeAt(cellIndex)
                _selectedCells.value = current
            }
            // If it's not the last cell, ignore (can't skip cells)
            return
        }

        // Check if this cell is valid to add (must be adjacent and in line)
        val lastCell = current.last()
        if (isValidNextCell(lastCell, row to col, current)) {
            current.add(row to col)
            _selectedCells.value = current
        }
    }

    private fun isValidNextCell(
        lastCell: Pair<Int, Int>,
        newCell: Pair<Int, Int>,
        currentPath: List<Pair<Int, Int>>
    ): Boolean {
        val (lastRow, lastCol) = lastCell
        val (newRow, newCol) = newCell

        val rowDiff = newRow - lastRow
        val colDiff = newCol - lastCol

        // Check if adjacent (including diagonal)
        if (abs(rowDiff) > 1 || abs(colDiff) > 1) return false
        if (rowDiff == 0 && colDiff == 0) return false

        // If this is the second cell, any adjacent cell is valid
        if (currentPath.size == 1) return true

        // For subsequent cells, check if direction is consistent
        // Allow the direction to be established by first 2 cells
        val firstCell = currentPath[0]
        val secondCell = currentPath[1]

        val directionRow = secondCell.first - firstCell.first
        val directionCol = secondCell.second - firstCell.second

        // Normalize direction (-1, 0, or 1)
        val normalizedDirRow = when {
            directionRow > 0 -> 1
            directionRow < 0 -> -1
            else -> 0
        }
        val normalizedDirCol = when {
            directionCol > 0 -> 1
            directionCol < 0 -> -1
            else -> 0
        }

        // Check if new cell continues in the same direction
        val newDirRow = when {
            rowDiff > 0 -> 1
            rowDiff < 0 -> -1
            else -> 0
        }
        val newDirCol = when {
            colDiff > 0 -> 1
            colDiff < 0 -> -1
            else -> 0
        }

        return newDirRow == normalizedDirRow && newDirCol == normalizedDirCol
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
                    handleWordSubmission(response, word)
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

    private fun handleWordSubmission(response: WordSubmissionResponse, word: String) {
        val currentState = _uiState.value
        if (currentState !is GameUiState.Playing) return

        if (response.correct) {
            // Add to found words
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
