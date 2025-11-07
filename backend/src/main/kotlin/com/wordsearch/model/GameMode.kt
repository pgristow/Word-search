package com.wordsearch.model

/**
 * Game mode enum defining different gameplay styles
 */
enum class GameMode {
    /**
     * Classic mode: Competitive gameplay with timers, combos, and boss levels
     */
    CLASSIC,

    /**
     * Casual mode: Relaxed gameplay with no timers or pressure
     */
    CASUAL;

    fun isCasual() = this == CASUAL
    fun isClassic() = this == CLASSIC
}
