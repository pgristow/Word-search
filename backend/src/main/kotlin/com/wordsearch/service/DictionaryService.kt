package com.wordsearch.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * In-memory English dictionary used to recognize "bonus" words — real words a player
 * traces on the grid that are not in the puzzle's target list. Loaded once, lazily, from
 * a bundled resource (one lowercase word per line). Words shorter than [minLength] are
 * never recognized.
 */
@Service
class DictionaryService(
    @Value("\${app.dictionary.path:/data/words_en.txt}") private val resourcePath: String,
    @Value("\${app.dictionary.min-length:3}") private val minLength: Int
) {
    private val words: Set<String> by lazy { load() }

    private fun load(): Set<String> {
        val stream = javaClass.getResourceAsStream(resourcePath) ?: return emptySet()
        return stream.bufferedReader().useLines { lines ->
            lines.map { it.trim().uppercase() }
                .filter { it.length >= minLength && it.all { ch -> ch in 'A'..'Z' } }
                .toHashSet()
        }
    }

    fun isWord(candidate: String): Boolean {
        val w = candidate.uppercase()
        return w.length >= minLength && words.contains(w)
    }
}
