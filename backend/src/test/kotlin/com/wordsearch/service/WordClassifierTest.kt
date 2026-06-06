package com.wordsearch.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WordClassifierTest {
    private val dict = DictionaryService("/data/words_test.txt", 3)
    private val c = WordClassifier(dict)
    private val board = BoardState(
        gridSize = 3,
        grid = listOf("CAT", "XYZ", "DOG"),
        solution = listOf(
            SolutionWord("CAT", listOf(Cell(0, 0), Cell(0, 1), Cell(0, 2)), false, "HORIZONTAL")
        )
    )

    @Test fun `target word on its path classifies TARGET`() {
        val r = c.classify(board, listOf(Cell(0, 0), Cell(0, 1), Cell(0, 2)), setOf("CAT"), emptySet())
        assertEquals(WordClass.TARGET, r.wordClass)
        assertEquals("CAT", r.word)
    }

    @Test fun `real off-list word classifies BONUS`() {
        val r = c.classify(board, listOf(Cell(2, 0), Cell(2, 1), Cell(2, 2)), setOf("CAT"), emptySet())
        assertEquals(WordClass.BONUS, r.wordClass)
        assertEquals("DOG", r.word)
    }

    @Test fun `non-straight path is INVALID`() {
        val r = c.classify(board, listOf(Cell(0, 0), Cell(2, 2)), setOf("CAT"), emptySet())
        assertEquals(WordClass.INVALID, r.wordClass)
    }

    @Test fun `already found is INVALID`() {
        val r = c.classify(board, listOf(Cell(0, 0), Cell(0, 1), Cell(0, 2)), setOf("CAT"), setOf("CAT"))
        assertEquals(WordClass.INVALID, r.wordClass)
    }

    @Test fun `gibberish path is INVALID`() {
        // column 2 top-to-bottom: T, Z, G -> not a word
        val r = c.classify(board, listOf(Cell(0, 2), Cell(1, 2), Cell(2, 2)), setOf("CAT"), emptySet())
        assertEquals(WordClass.INVALID, r.wordClass)
    }

    @Test fun `two-letter path is INVALID (too short)`() {
        val r = c.classify(board, listOf(Cell(0, 0), Cell(0, 1)), setOf("CAT"), emptySet())
        assertEquals(WordClass.INVALID, r.wordClass)
    }
}
