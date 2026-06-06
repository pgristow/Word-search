package com.wordsearch.service

import com.fasterxml.jackson.databind.ObjectMapper

/** A single grid cell coordinate. */
data class Cell(val row: Int = 0, val col: Int = 0)

/** A target word as placed on the board, with its exact cell path. */
data class SolutionWord(
    val word: String = "",
    val path: List<Cell> = emptyList(),
    val isReversed: Boolean = false,
    val direction: String = ""
)

/**
 * The server-side record of a generated board: the letter grid plus the solution paths.
 * Persisted (as JSON) with the game session so the server can authoritatively validate a
 * player's traced path and reveal hints — the client is never trusted for this.
 */
data class BoardState(
    val gridSize: Int = 0,
    /** One string per row; row[col] is the letter at that cell. */
    val grid: List<String> = emptyList(),
    val solution: List<SolutionWord> = emptyList()
) {
    /** Reads the letters at [path], or null if any cell is out of bounds. */
    fun lettersAlong(path: List<Cell>): String? {
        val sb = StringBuilder()
        for (c in path) {
            if (c.row !in 0 until gridSize || c.col !in 0 until gridSize) return null
            if (c.col >= grid[c.row].length) return null
            sb.append(grid[c.row][c.col])
        }
        return sb.toString()
    }

    fun toJson(mapper: ObjectMapper): String = mapper.writeValueAsString(this)

    companion object {
        fun fromJson(json: String, mapper: ObjectMapper): BoardState =
            mapper.readValue(json, BoardState::class.java)
    }
}
