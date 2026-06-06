package com.wordsearch.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DictionaryServiceTest {
    private val dict = DictionaryService(resourcePath = "/data/words_test.txt", minLength = 3)

    @Test fun `recognizes real word case-insensitively`() {
        assertTrue(dict.isWord("HOUSE"))
        assertTrue(dict.isWord("house"))
        assertTrue(dict.isWord("Giraffe"))
    }

    @Test fun `rejects non-word`() {
        assertFalse(dict.isWord("ZZZZ"))
    }

    @Test fun `rejects below min length`() {
        assertFalse(dict.isWord("at"))
    }

    @Test fun `missing resource yields empty dictionary, not crash`() {
        val empty = DictionaryService(resourcePath = "/data/does_not_exist.txt", minLength = 3)
        assertFalse(empty.isWord("cat"))
    }
}
