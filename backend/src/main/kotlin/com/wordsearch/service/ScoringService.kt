package com.wordsearch.service

import org.springframework.stereotype.Service

/**
 * How a found word is scored.
 * - CASUAL: base + length bonus only (no speed/combo/orientation multipliers).
 * - CLASSIC_TARGET: full competitive scoring (orientation, speed, combo multiplier).
 * - BONUS: a real off-list word — length + orientation, but NO combo and NO speed
 *   (board luck must not inflate the competitive combo chain).
 */
enum class ScoreMode { CASUAL, CLASSIC_TARGET, BONUS }

data class ScoreInput(
    val word: String,
    val isReversed: Boolean,
    val isDiagonal: Boolean,
    val timeElapsed: Int,
    val combo: Int,
    val mode: ScoreMode
)

/**
 * Central, tunable scoring model. Length is rewarded super-linearly via escalating
 * tiers past the 4th letter, then compounded with orientation/speed/combo per mode.
 */
@Service
class ScoringService {

    /** Escalating bonus for each letter from the 5th onward. */
    fun lengthBonus(length: Int): Int {
        var bonus = 0
        for (i in 5..length) {
            bonus += when (i) {
                5 -> 50
                6 -> 100
                7 -> 200
                else -> 300
            }
        }
        return bonus
    }

    fun comboMultiplier(combo: Int): Int = when (combo) {
        in 2..4 -> 2
        in 5..9 -> 3
        in 10..Int.MAX_VALUE -> 4
        else -> 1
    }

    fun score(input: ScoreInput): Int {
        val len = input.word.length
        val core = 100 * len + lengthBonus(len)
        return when (input.mode) {
            ScoreMode.CASUAL -> core
            ScoreMode.BONUS -> core + orientationBonus(core, input)
            ScoreMode.CLASSIC_TARGET -> {
                val speed = minOf(100, maxOf(0, (60 - input.timeElapsed) * 2))
                (core + orientationBonus(core, input) + speed) * comboMultiplier(input.combo)
            }
        }
    }

    private fun orientationBonus(core: Int, input: ScoreInput): Int {
        val rev = if (input.isReversed) (core * 0.5).toInt() else 0
        val diag = if (input.isDiagonal) (core * 0.25).toInt() else 0
        return rev + diag
    }
}
