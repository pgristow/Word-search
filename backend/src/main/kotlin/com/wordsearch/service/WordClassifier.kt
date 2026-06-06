package com.wordsearch.service

import org.springframework.stereotype.Service

enum class WordClass { TARGET, BONUS, INVALID }

data class Classification(
    val wordClass: WordClass,
    val word: String,
    val reason: String = ""
)

/**
 * Server-authoritative classification of a traced path into a target word, a real
 * off-list bonus word, or invalid. The grid is read from the persisted [BoardState] so
 * the client cannot spoof a word it did not actually trace.
 */
@Service
class WordClassifier(private val dictionary: DictionaryService) {

    /** True if cells form a contiguous straight line in one of the 8 directions. */
    fun isStraightLine(path: List<Cell>): Boolean {
        if (path.size < 2) return path.size == 1
        val dr = path[1].row - path[0].row
        val dc = path[1].col - path[0].col
        if (dr == 0 && dc == 0) return false
        if (dr !in -1..1 || dc !in -1..1) return false
        for (i in 1 until path.size) {
            if (path[i].row - path[i - 1].row != dr) return false
            if (path[i].col - path[i - 1].col != dc) return false
        }
        return true
    }

    fun classify(
        board: BoardState,
        path: List<Cell>,
        targets: Set<String>,
        alreadyFound: Set<String>
    ): Classification {
        if (path.size < 3) return Classification(WordClass.INVALID, "", "Too short")
        if (!isStraightLine(path)) return Classification(WordClass.INVALID, "", "Not a straight line")
        val letters = board.lettersAlong(path)
            ?: return Classification(WordClass.INVALID, "", "Out of bounds")
        val word = letters.uppercase()
        if (alreadyFound.contains(word)) return Classification(WordClass.INVALID, word, "Already found")
        if (targets.contains(word)) return Classification(WordClass.TARGET, word)
        if (dictionary.isWord(word)) return Classification(WordClass.BONUS, word)
        return Classification(WordClass.INVALID, word, "Not a word")
    }
}
