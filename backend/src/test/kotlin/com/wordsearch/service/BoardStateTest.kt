package com.wordsearch.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BoardStateTest {
    private val mapper = jacksonObjectMapper()

    @Test fun `serializes and reads letters along a path`() {
        val board = BoardState(
            gridSize = 3,
            grid = listOf("CAT", "XYZ", "DOG"),
            solution = listOf(
                SolutionWord("CAT", listOf(Cell(0, 0), Cell(0, 1), Cell(0, 2)), false, "HORIZONTAL")
            )
        )
        val json = board.toJson(mapper)
        val back = BoardState.fromJson(json, mapper)
        assertEquals("CAT", back.lettersAlong(listOf(Cell(0, 0), Cell(0, 1), Cell(0, 2))))
        assertEquals("DOG", back.lettersAlong(listOf(Cell(2, 0), Cell(2, 1), Cell(2, 2))))
    }

    @Test fun `lettersAlong returns null for out of bounds`() {
        val board = BoardState(2, listOf("AB", "CD"), emptyList())
        assertNull(board.lettersAlong(listOf(Cell(0, 0), Cell(0, 5))))
        assertNull(board.lettersAlong(listOf(Cell(-1, 0))))
    }
}
