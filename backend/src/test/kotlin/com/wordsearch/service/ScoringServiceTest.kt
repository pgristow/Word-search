package com.wordsearch.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ScoringServiceTest {
    private val s = ScoringService()

    @Test fun `base is 100 per letter for short word`() {
        // CAT len 3: base 300, no length bonus, casual = core only
        assertEquals(300, s.score(ScoreInput("CAT", false, false, 999, 1, ScoreMode.CASUAL)))
    }

    @Test fun `length bonus escalates past four letters`() {
        // GIRAFFE len 7 casual: base 700 + (50+100+200) = 1050
        assertEquals(1050, s.score(ScoreInput("GIRAFFE", false, false, 999, 1, ScoreMode.CASUAL)))
    }

    @Test fun `classic combo multiplies subtotal`() {
        // WORD len 4, combo 5 (mult 3), timeElapsed 999 -> speed 0: base 400 * 3 = 1200
        assertEquals(1200, s.score(ScoreInput("WORD", false, false, 999, 5, ScoreMode.CLASSIC_TARGET)))
    }

    @Test fun `bonus word ignores combo and speed`() {
        // HOUSE len 5 bonus, combo 10: core = 500 + 50 = 550, no mult, no speed
        assertEquals(550, s.score(ScoreInput("HOUSE", false, false, 0, 10, ScoreMode.BONUS)))
    }

    @Test fun `reverse and diagonal add to classic`() {
        // WORD len 4 classic combo1, t=0 -> speed 100; core 400; rev +200(0.5); diag +100(0.25)
        // subtotal 400+200+100+100 = 800 * 1 = 800
        assertEquals(800, s.score(ScoreInput("WORD", true, true, 0, 1, ScoreMode.CLASSIC_TARGET)))
    }

    @Test fun `length bonus for eight letters and beyond`() {
        // length 9: tiers 5..9 = 50+100+200+300+300 = 950
        assertEquals(950, s.lengthBonus(9))
    }
}
