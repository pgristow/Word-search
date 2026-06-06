# Word Recognition, Scoring, Coins, Leagues & Casual — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the word-search game recognize bonus/overlapping words and reward long words, then build the coin economy, weekly leagues, baseline casual mode, and a unified online leaderboard.

**Architecture:** Server-authoritative recognition (board persisted with the session; client sends the traced path; server classifies target/bonus/invalid against the stored grid + an in-memory dictionary). Pure-logic services (`ScoringService`, `DictionaryService`, classifier, `EconomyService`, `LeagueService`) are built test-first. Data on Supabase Postgres; one `leaderboard_entries` table serves all boards.

**Tech Stack:** Spring Boot (Kotlin), JPA/Hibernate, Flyway, JUnit5, PostgreSQL/Supabase (H2 for local tests); Android Kotlin + Jetpack Compose + Hilt + Retrofit.

**Build/test commands** (no gradlew wrapper; system `gradle` 8.5, JDK 21):
- Backend tests: `cd backend && gradle test --tests "com.wordsearch.service.<Class>" --console=plain`
- Full backend build: `cd backend && gradle build --console=plain`

---

## Phasing & Coverage Map

| Spec section | Phase / Tasks |
|---|---|
| §3, §4 Recognition | Phase 1: T2 (board persistence), T4–T6 (dictionary, classifier, submit) |
| §5 Scoring | Phase 1: T3 (ScoringService) |
| §10 Data model V6 | Phase 1: T1 (migration + entities) |
| §6 Coins | Phase 2: T7–T9 |
| §7 Leagues | Phase 2: T10–T11 |
| §8 Casual | Phase 1: T3/T6 casual paths; Phase 2: T12 |
| §9 Unified leaderboard | Phase 2: T13 |
| §9.2 Supabase | Phase 3: T14 |
| §11 API | distributed across backend tasks |
| §12 Android UI/UX | Phase 4: T15–T19 |
| §13 Testing | embedded (TDD) in every task |

> Phase 1 is a coherent, shippable deliverable on its own (fixes the two reported bugs). Phases 2–4 build on it.

---

# PHASE 1 — Foundation + Recognition + Scoring (the reported bugs)

## Task 1: Flyway V6 migration + entity/DTO updates

**Files:**
- Create: `backend/src/main/resources/db/migration/V6__Recognition_Scoring_Economy_Leagues.sql`
- Modify: `backend/src/main/kotlin/com/wordsearch/model/GameSession.kt` (add `boardState`)
- Modify: `backend/src/main/kotlin/com/wordsearch/model/UserProgress.kt` (add columns)
- Modify: `backend/src/main/kotlin/com/wordsearch/model/UserFoundWord.kt` (add columns)
- Modify: `backend/src/main/kotlin/com/wordsearch/model/Category.kt` (add `coinUnlockCost`)

- [ ] **Step 1: Write the migration** (only the columns/tables needed across all phases, so we migrate once)

```sql
-- V6: recognition, scoring, economy, leagues, casual, unified leaderboard

ALTER TABLE game_sessions ADD COLUMN board_state JSONB;

ALTER TABLE user_found_words ADD COLUMN is_bonus BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE user_found_words ADD COLUMN word_length INT NOT NULL DEFAULT 0;
ALTER TABLE user_found_words ADD COLUMN path JSONB;

ALTER TABLE user_progress ADD COLUMN coins BIGINT NOT NULL DEFAULT 0;
ALTER TABLE user_progress ADD COLUMN total_bonus_words_found INT NOT NULL DEFAULT 0;
ALTER TABLE user_progress ADD COLUMN longest_word_found INT NOT NULL DEFAULT 0;
ALTER TABLE user_progress ADD COLUMN weekly_score BIGINT NOT NULL DEFAULT 0;
ALTER TABLE user_progress ADD COLUMN casual_best_score BIGINT NOT NULL DEFAULT 0;

ALTER TABLE categories ADD COLUMN coin_unlock_cost INT NOT NULL DEFAULT 0;

CREATE TABLE coin_transactions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    delta BIGINT NOT NULL,
    reason VARCHAR(40) NOT NULL,
    ref_id UUID,
    balance_after BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_coin_tx_user ON coin_transactions(user_id, created_at);

CREATE TABLE themes (
    id UUID PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    coin_cost INT NOT NULL,
    asset_key VARCHAR(80) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE user_owned_themes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    theme_id UUID NOT NULL,
    acquired_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (user_id, theme_id)
);
CREATE TABLE user_unlocked_categories (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    category_id UUID NOT NULL,
    method VARCHAR(20) NOT NULL,
    unlocked_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (user_id, category_id)
);

CREATE TABLE league_tiers (
    id INT PRIMARY KEY,
    name VARCHAR(40) NOT NULL,
    tier_order INT NOT NULL,
    promote_count INT NOT NULL,
    relegate_count INT NOT NULL,
    promotion_reward INT NOT NULL
);
INSERT INTO league_tiers (id, name, tier_order, promote_count, relegate_count, promotion_reward) VALUES
 (1,'Bronze',1,7,0,50),(2,'Silver',2,7,5,75),(3,'Gold',3,7,5,100),
 (4,'Platinum',4,7,5,150),(5,'Diamond',5,7,5,200),(6,'Master',6,0,5,300);

CREATE TABLE league_cohorts (
    id UUID PRIMARY KEY,
    tier_id INT NOT NULL,
    week_key VARCHAR(10) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_cohort_tier_week ON league_cohorts(tier_id, week_key, status);

CREATE TABLE league_memberships (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    cohort_id UUID NOT NULL,
    weekly_score BIGINT NOT NULL DEFAULT 0,
    final_rank INT,
    result VARCHAR(12),
    UNIQUE (user_id, cohort_id)
);
CREATE INDEX idx_membership_cohort ON league_memberships(cohort_id, weekly_score DESC);

CREATE TABLE leaderboard_entries (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    username VARCHAR(80) NOT NULL,
    board_type VARCHAR(20) NOT NULL,
    period_key VARCHAR(40) NOT NULL,
    score BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (user_id, board_type, period_key)
);
CREATE INDEX idx_lb_board_period_score ON leaderboard_entries(board_type, period_key, score DESC);
```

- [ ] **Step 2: Add the H2-compatible note.** Local tests use H2 in PostgreSQL mode. Confirm `application.yml` H2 url already sets `MODE=PostgreSQL` or add `;MODE=PostgreSQL` and map JSONB→`other`/`varchar`. If H2 rejects `JSONB`, the migration runs only on Postgres; tests use `@DataJpaТest` against H2 with columns typed `CLOB`. Decision: store board/path as `TEXT` columns in JPA via `@Column(columnDefinition = "TEXT")` and keep `JSONB` in the Postgres DDL — Hibernate writes JSON strings either way. Update migration `board_state JSONB` is fine for Postgres; for H2 test DDL Hibernate auto-generates from entities (ddl-auto in test profile), so the Flyway script is Postgres-only and tests rely on Hibernate-generated schema. Verify `application.yml` test profile uses `ddl-auto: create-drop` and disables Flyway for H2.

- [ ] **Step 3: Update entities.** Add to `GameSession`:
```kotlin
    @Column(name = "board_state", columnDefinition = "TEXT")
    val boardState: String? = null,
```
Add to `UserProgress`: `coins: Long = 0`, `totalBonusWordsFound: Int = 0`, `longestWordFound: Int = 0`, `weeklyScore: Long = 0`, `casualBestScore: Long = 0` (each with matching `@Column`).
Add to `UserFoundWord`: `isBonus: Boolean = false`, `wordLength: Int = 0`, `path: String? = null`.
Add to `Category`: `coinUnlockCost: Int = 0`.

- [ ] **Step 4: Compile**

Run: `cd backend && gradle compileKotlin --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**
```bash
git add backend/src/main/resources/db/migration/V6__*.sql backend/src/main/kotlin/com/wordsearch/model/
git commit -m "feat(db): V6 migration + entities for recognition/scoring/economy/leagues"
```

---

## Task 2: Board model + persistence (BoardState serialization)

**Files:**
- Create: `backend/src/main/kotlin/com/wordsearch/service/BoardState.kt`
- Test: `backend/src/test/kotlin/com/wordsearch/service/BoardStateTest.kt`
- Modify: `GameBoardGenerator.kt` (emit cell paths in `PlacedWord`), `GameSessionService.kt` (persist on start)

- [ ] **Step 1: Write the failing test**
```kotlin
package com.wordsearch.service
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BoardStateTest {
    private val mapper = ObjectMapper()

    @Test fun `serializes and reads letters along a path`() {
        val board = BoardState(
            gridSize = 3,
            grid = listOf(listOf('C','A','T'), listOf('X','Y','Z'), listOf('D','O','G')),
            solution = listOf(SolutionWord("CAT", listOf(Cell(0,0),Cell(0,1),Cell(0,2)), false, "HORIZONTAL"))
        )
        val json = board.toJson(mapper)
        val back = BoardState.fromJson(json, mapper)
        assertEquals("CAT", back.lettersAlong(listOf(Cell(0,0),Cell(0,1),Cell(0,2))))
        assertEquals("DOG", back.lettersAlong(listOf(Cell(2,0),Cell(2,1),Cell(2,2))))
    }

    @Test fun `lettersAlong returns null for out of bounds`() {
        val board = BoardState(2, listOf(listOf('A','B'), listOf('C','D')), emptyList())
        assertNull(board.lettersAlong(listOf(Cell(0,0), Cell(0,5))))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && gradle test --tests "com.wordsearch.service.BoardStateTest" --console=plain`
Expected: FAIL (BoardState not defined)

- [ ] **Step 3: Implement**
```kotlin
package com.wordsearch.service
import com.fasterxml.jackson.databind.ObjectMapper

data class Cell(val row: Int, val col: Int)
data class SolutionWord(val word: String, val path: List<Cell>, val isReversed: Boolean, val direction: String)

data class BoardState(
    val gridSize: Int,
    val grid: List<List<Char>>,
    val solution: List<SolutionWord>
) {
    fun lettersAlong(path: List<Cell>): String? {
        val sb = StringBuilder()
        for (c in path) {
            if (c.row !in 0 until gridSize || c.col !in 0 until gridSize) return null
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && gradle test --tests "com.wordsearch.service.BoardStateTest" --console=plain`
Expected: PASS

- [ ] **Step 5: Wire generation + persistence.** In `GameBoardGenerator`, compute the cell path for each placed word (start + direction deltas × length) and expose it on `PlacedWord` (add `path: List<Cell>`). In `GameSessionService.startNewSession`, build a `BoardState` from `gameBoard` and save `boardState = it.toJson(mapper)` on the session (inject `ObjectMapper`). Also return the full grid in `getActiveSession` by reading `boardState`.

- [ ] **Step 6: Commit**
```bash
git add backend/src/main/kotlin/com/wordsearch/service/BoardState.kt backend/src/test/kotlin/com/wordsearch/service/BoardStateTest.kt backend/src/main/kotlin/com/wordsearch/service/GameBoardGenerator.kt backend/src/main/kotlin/com/wordsearch/service/GameSessionService.kt
git commit -m "feat(game): persist board state with sessions for path validation"
```

---

## Task 3: ScoringService (length × difficulty)

**Files:**
- Create: `backend/src/main/kotlin/com/wordsearch/service/ScoringService.kt`
- Test: `backend/src/test/kotlin/com/wordsearch/service/ScoringServiceTest.kt`

- [ ] **Step 1: Write the failing tests**
```kotlin
package com.wordsearch.service
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ScoringServiceTest {
    private val s = ScoringService()

    @Test fun `base is 100 per letter for short word`() {
        // CAT len 3: base 300, no length bonus, no mults
        assertEquals(300, s.score(ScoreInput("CAT", false, false, 999, 1, ScoreMode.CASUAL)))
    }
    @Test fun `length bonus escalates past four letters`() {
        // 7-letter casual: base 700 + (50+100+200) = 1050
        assertEquals(1050, s.score(ScoreInput("GIRAFFE", false, false, 999, 1, ScoreMode.CASUAL)))
    }
    @Test fun `classic combo multiplies subtotal`() {
        // 4-letter, combo 5 (mult 3), no speed (timeElapsed 999): base 400 * 3 = 1200
        assertEquals(1200, s.score(ScoreInput("WORD", false, false, 999, 5, ScoreMode.CLASSIC_TARGET)))
    }
    @Test fun `bonus word ignores combo and speed`() {
        // 5-letter bonus, combo 10: base 500 + length 50 = 550 (no mult)
        assertEquals(550, s.score(ScoreInput("HOUSE", false, false, 0, 10, ScoreMode.BONUS)))
    }
    @Test fun `reverse and diagonal add to classic`() {
        // 4-letter classic combo1: base400; reverse +200(0.5); diag +100(0.25); speed: t=0 ->100
        // subtotal 400+200+100+100=800 * 1 = 800
        assertEquals(800, s.score(ScoreInput("WORD", true, true, 0, 1, ScoreMode.CLASSIC_TARGET)))
    }
}
```

- [ ] **Step 2: Run to verify fail**

Run: `cd backend && gradle test --tests "com.wordsearch.service.ScoringServiceTest" --console=plain`
Expected: FAIL

- [ ] **Step 3: Implement**
```kotlin
package com.wordsearch.service
import org.springframework.stereotype.Service

enum class ScoreMode { CASUAL, CLASSIC_TARGET, BONUS }
data class ScoreInput(
    val word: String,
    val isReversed: Boolean,
    val isDiagonal: Boolean,
    val timeElapsed: Int,
    val combo: Int,
    val mode: ScoreMode
)

@Service
class ScoringService {
    fun lengthBonus(length: Int): Int {
        var bonus = 0
        for (i in 5..length) bonus += when (i) { 5 -> 50; 6 -> 100; 7 -> 200; else -> 300 }
        return bonus
    }
    fun comboMultiplier(combo: Int): Int = when (combo) {
        in 2..4 -> 2; in 5..9 -> 3; in 10..Int.MAX_VALUE -> 4; else -> 1
    }
    fun score(input: ScoreInput): Int {
        val len = input.word.length
        val base = 100 * len
        val lenBonus = lengthBonus(len)
        val core = base + lenBonus
        return when (input.mode) {
            ScoreMode.CASUAL -> core
            ScoreMode.BONUS -> {
                val rev = if (input.isReversed) (core * 0.5).toInt() else 0
                val diag = if (input.isDiagonal) (core * 0.25).toInt() else 0
                core + rev + diag
            }
            ScoreMode.CLASSIC_TARGET -> {
                val rev = if (input.isReversed) (core * 0.5).toInt() else 0
                val diag = if (input.isDiagonal) (core * 0.25).toInt() else 0
                val speed = minOf(100, maxOf(0, (60 - input.timeElapsed) * 2))
                (core + rev + diag + speed) * comboMultiplier(input.combo)
            }
        }
    }
}
```

- [ ] **Step 4: Run to verify pass**

Run: `cd backend && gradle test --tests "com.wordsearch.service.ScoringServiceTest" --console=plain`
Expected: PASS (5 tests)

- [ ] **Step 5: Commit**
```bash
git add backend/src/main/kotlin/com/wordsearch/service/ScoringService.kt backend/src/test/kotlin/com/wordsearch/service/ScoringServiceTest.kt
git commit -m "feat(scoring): length x difficulty ScoringService with TDD"
```

---

## Task 4: DictionaryService

**Files:**
- Create: `backend/src/main/resources/data/words_en.txt` (bundled word list, lowercase, one per line)
- Create: `backend/src/main/kotlin/com/wordsearch/service/DictionaryService.kt`
- Test: `backend/src/test/kotlin/com/wordsearch/service/DictionaryServiceTest.kt`

- [ ] **Step 1: Add a small test-friendly word resource.** For tests, create `backend/src/test/resources/data/words_test.txt` containing: `cat`, `dog`, `house`, `giraffe`, `word`. The production list (full English, e.g. dwyl/english-words `words_alpha.txt`, min length filtered) is dropped at `backend/src/main/resources/data/words_en.txt` during execution.

- [ ] **Step 2: Write the failing test**
```kotlin
package com.wordsearch.service
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DictionaryServiceTest {
    private val dict = DictionaryService(resourcePath = "/data/words_test.txt", minLength = 3)

    @Test fun `recognizes real word case-insensitively`() {
        assertTrue(dict.isWord("HOUSE")); assertTrue(dict.isWord("house"))
    }
    @Test fun `rejects non-word`() = assertFalse(dict.isWord("ZZZZ"))
    @Test fun `rejects below min length`() = assertFalse(dict.isWord("at"))
}
```

- [ ] **Step 3: Run to verify fail.** Run: `cd backend && gradle test --tests "com.wordsearch.service.DictionaryServiceTest" --console=plain` — Expected FAIL.

- [ ] **Step 4: Implement**
```kotlin
package com.wordsearch.service
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class DictionaryService(
    @Value("\${app.dictionary.path:/data/words_en.txt}") private val resourcePath: String,
    @Value("\${app.dictionary.min-length:3}") private val minLength: Int
) {
    private val words: Set<String> by lazy { load() }
    private fun load(): Set<String> {
        val stream = javaClass.getResourceAsStream(resourcePath)
            ?: return emptySet()
        return stream.bufferedReader().useLines { lines ->
            lines.map { it.trim().uppercase() }.filter { it.length >= minLength }.toHashSet()
        }
    }
    fun isWord(candidate: String): Boolean {
        val w = candidate.uppercase()
        return w.length >= minLength && words.contains(w)
    }
}
```

- [ ] **Step 5: Run to verify pass.** Expected PASS (3 tests).

- [ ] **Step 6: Commit**
```bash
git add backend/src/main/kotlin/com/wordsearch/service/DictionaryService.kt backend/src/test/kotlin/com/wordsearch/service/DictionaryServiceTest.kt backend/src/test/resources/data/words_test.txt
git commit -m "feat(dict): in-memory DictionaryService with TDD"
```

---

## Task 5: WordClassifier (target / bonus / invalid)

**Files:**
- Create: `backend/src/main/kotlin/com/wordsearch/service/WordClassifier.kt`
- Test: `backend/src/test/kotlin/com/wordsearch/service/WordClassifierTest.kt`

- [ ] **Step 1: Write the failing tests**
```kotlin
package com.wordsearch.service
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WordClassifierTest {
    private val dict = DictionaryService("/data/words_test.txt", 3)
    private val c = WordClassifier(dict)
    private val board = BoardState(3,
        listOf(listOf('C','A','T'), listOf('X','Y','Z'), listOf('D','O','G')),
        listOf(SolutionWord("CAT", listOf(Cell(0,0),Cell(0,1),Cell(0,2)), false, "HORIZONTAL")))

    @Test fun `target word on its path classifies TARGET`() {
        val r = c.classify(board, listOf(Cell(0,0),Cell(0,1),Cell(0,2)), setOf("CAT"), emptySet())
        assertEquals(WordClass.TARGET, r.wordClass); assertEquals("CAT", r.word)
    }
    @Test fun `real off-list word classifies BONUS`() {
        val r = c.classify(board, listOf(Cell(2,0),Cell(2,1),Cell(2,2)), setOf("CAT"), emptySet())
        assertEquals(WordClass.BONUS, r.wordClass); assertEquals("DOG", r.word)
    }
    @Test fun `non-straight path is INVALID`() {
        val r = c.classify(board, listOf(Cell(0,0),Cell(2,2)), setOf("CAT"), emptySet())
        assertEquals(WordClass.INVALID, r.wordClass)
    }
    @Test fun `already found is INVALID`() {
        val r = c.classify(board, listOf(Cell(0,0),Cell(0,1),Cell(0,2)), setOf("CAT"), setOf("CAT"))
        assertEquals(WordClass.INVALID, r.wordClass)
    }
    @Test fun `gibberish path is INVALID`() {
        val r = c.classify(board, listOf(Cell(0,2),Cell(1,2),Cell(2,2)), setOf("CAT"), emptySet())
        // T,Z,G not a word
        assertEquals(WordClass.INVALID, r.wordClass)
    }
}
```

- [ ] **Step 2: Run to verify fail.**

- [ ] **Step 3: Implement**
```kotlin
package com.wordsearch.service
import org.springframework.stereotype.Service

enum class WordClass { TARGET, BONUS, INVALID }
data class Classification(val wordClass: WordClass, val word: String, val reason: String = "")

@Service
class WordClassifier(private val dictionary: DictionaryService) {
    fun isStraightLine(path: List<Cell>): Boolean {
        if (path.size < 2) return path.size == 1
        val dr = path[1].row - path[0].row
        val dc = path[1].col - path[0].col
        if (dr == 0 && dc == 0) return false
        if (dr !in -1..1 || dc !in -1..1) return false
        for (i in 1 until path.size) {
            if (path[i].row - path[i-1].row != dr) return false
            if (path[i].col - path[i-1].col != dc) return false
        }
        return true
    }
    fun classify(board: BoardState, path: List<Cell>, targets: Set<String>, alreadyFound: Set<String>): Classification {
        if (path.size < 3) return Classification(WordClass.INVALID, "", "Too short")
        if (!isStraightLine(path)) return Classification(WordClass.INVALID, "", "Not a straight line")
        val letters = board.lettersAlong(path) ?: return Classification(WordClass.INVALID, "", "Out of bounds")
        val word = letters.uppercase()
        if (alreadyFound.contains(word)) return Classification(WordClass.INVALID, word, "Already found")
        if (targets.contains(word)) return Classification(WordClass.TARGET, word)
        if (dictionary.isWord(word)) return Classification(WordClass.BONUS, word)
        return Classification(WordClass.INVALID, word, "Not a word")
    }
}
```

- [ ] **Step 4: Run to verify pass** (5 tests).

- [ ] **Step 5: Commit**
```bash
git add backend/src/main/kotlin/com/wordsearch/service/WordClassifier.kt backend/src/test/kotlin/com/wordsearch/service/WordClassifierTest.kt
git commit -m "feat(game): WordClassifier target/bonus/invalid with TDD"
```

---

## Task 6: Rewrite `submitWord` to use path + classifier + scoring

**Files:**
- Modify: `backend/src/main/kotlin/com/wordsearch/service/GameSessionService.kt`
- Modify: `backend/src/main/kotlin/com/wordsearch/dto/GameDto.kt` (request `path`, response fields)
- Modify: `backend/src/main/kotlin/com/wordsearch/controller/GameController.kt`
- Test: `backend/src/test/kotlin/com/wordsearch/service/GameSessionServiceSubmitTest.kt` (mock repos)

- [ ] **Step 1: Extend DTOs.** Add to `SubmitWordRequest`: `val path: List<CellDto> = emptyList()` with `data class CellDto(val row: Int, val col: Int)`. Add to `WordSubmissionResponse`: `isBonus: Boolean = false`, `wordLength: Int = 0`, `coinsEarned: Long = 0`, `coinBalance: Long = 0`, and `scoreBreakdown: Map<String,Int> = emptyMap()`.

- [ ] **Step 2: Write the failing test** (a focused unit test with mocked repositories asserting a bonus word is accepted and scored, and an off-path spoof is rejected). Use Mockito-Kotlin (already a Spring Boot test dep) to stub `gameSessionRepository.findById`, `userFoundWordRepository.existsBySessionIdAndWord`, etc. Construct a session whose `boardState` JSON contains DOG at row 2; submit the DOG path with `word="DOG"`; assert `response.correct && response.isBonus`. Submit `word="CAT"` with DOG's path; assert `!response.correct` (letters mismatch).

- [ ] **Step 3: Run to verify fail.**

- [ ] **Step 4: Implement the rewrite.** Replace the list-membership check with: load `boardState`, build `targets` from category words, build `alreadyFound` from `userFoundWordRepository.findBySessionId`, call `classifier.classify(...)`. Branch:
  - INVALID → reset combo (Classic), return `correct=false` with reason as message.
  - TARGET → `ScoringService.score(CLASSIC_TARGET or CASUAL)`; count toward target; combo logic as today; award no coins.
  - BONUS → `ScoringService.score(BONUS)`; do not touch target combo; award coins (`EconomyService` in Phase 2 — for Phase 1 award coins inline via a simple `+= bonusCoins` on `user_progress.coins` and a `coin_transactions` row; or stub coins=0 until Phase 2 and add a TODO test). **Phase-1 decision:** award score now, set `coinsEarned=0`, wire real coins in Task 8. Persist `UserFoundWord(isBonus, wordLength, path=json)`. Update `weekly_score` and `casual_best` accumulators where relevant (weekly handled in Phase 2 Task 11 — for now also increment `weeklyScore` so leagues have data).
  - Validate the submitted `word` equals classifier's reconstructed word (anti-spoof) — if the client word disagrees with grid letters, INVALID.

- [ ] **Step 5: Run to verify pass.**

- [ ] **Step 6: Run full backend test suite.** Run: `cd backend && gradle test --console=plain` — Expected: existing + new tests PASS. Fix `GameBoardGeneratorTest` if `PlacedWord` signature changed.

- [ ] **Step 7: Commit**
```bash
git add backend/src/main/kotlin/com/wordsearch/ backend/src/test/kotlin/com/wordsearch/service/GameSessionServiceSubmitTest.kt
git commit -m "feat(game): path-based submit with bonus/overlap recognition + new scoring"
```

**End of Phase 1 — the two reported bugs are fixed and shippable.**

---

# PHASE 2 — Economy, Leagues, Casual, Unified Leaderboard (backend)

## Task 7: EconomyService + ledger (TDD)
Create `EconomyService` (`backend/.../service/EconomyService.kt`) + `CoinTransaction` entity + `CoinTransactionRepository`. Methods: `earn(userId, amount, reason, refId)`, `spend(userId, amount, reason, refId)` (throws `InsufficientCoinsException`), `balance(userId)`. Each writes a ledger row with `balance_after`. TDD: earn increments balance + writes row; spend with insufficient funds throws and writes nothing; balance derived equals ledger sum. Centralize reward constants here (bonus word `5 + 2*(len-3)`, completion 25, etc.).

## Task 8: Wire coins into submit + store endpoints
Inject `EconomyService` into `GameSessionService`; bonus words call `earn(BONUS_WORD)`. Add `EconomyController`: `GET /economy/wallet`, `GET /economy/store`, `POST /economy/purchase` (theme/category-unlock with `spend`). Add `POST /sessions/{id}/hint` → `spend(HINT_COST)` then read one unfound target's path from `boardState` and return it. Add `Theme`, `UserOwnedTheme`, `UserUnlockedCategory` entities + repositories. Update category gating to also pass if `user_unlocked_categories` row exists. TDD each endpoint via `@WebMvcTest`/service tests.

## Task 9: Category unlock reconciliation
Modify the existing category-availability logic (find where `unlockRequirementType` is evaluated — `CategoriesViewModel`/category endpoint) so a category is available if (milestone met) OR (coin-unlocked). Seed `coin_unlock_cost` per category in V6 (e.g. 200–1000 scaled by display order). TDD the availability resolver.

## Task 10: LeagueService — cohorts + standings (TDD)
Create `LeagueService` + entities (`LeagueTier`, `LeagueCohort`, `LeagueMembership`) + repos. `joinOrGetCurrentCohort(userId)` assigns a user to an OPEN cohort in their tier (create cohort if none has room < 30). `currentStandings(userId)` returns ranked memberships. `addWeeklyScore(userId, delta)` increments membership + `user_progress.weekly_score`. TDD cohort fill (31st player → new cohort), ranking order.

## Task 11: Weekly rollover (TDD with injected clock)
`LeagueService.runWeeklyRollover()` (`@Scheduled(cron = "0 0 0 * * MON", zone="UTC")`): for each OPEN cohort, rank by `weekly_score`, set `final_rank`/`result`, promote top `promote_count`/relegate bottom `relegate_count` by writing next-week memberships in adjusted tiers, pay `promotion_reward` via `EconomyService.earn`, mark cohort CLOSED, reset each user's `user_progress.weekly_score=0`. Inject a `Clock` so tests are deterministic. TDD: a cohort of 10 → correct promote/relegate/stay split + rewards paid + scores reset.

## Task 12: Casual baseline difficulty + best-score
Add `GameBoardGenerator.getCasualConfig()` (fixed: grid 8, H/V only, no reverse, minLen 3, targetCount 8). In `startNewSession`, when `gameMode == CASUAL`, use `getCasualConfig()` instead of level-based config. On `endSession`/`saveCasualProgress` for casual, update `user_progress.casual_best_score = max(...)`. TDD config is level-independent; best-score updates only upward.

## Task 13: Unified leaderboard projection + endpoint
Create `LeaderboardEntry` entity + repo + `LeaderboardService.upsert(userId, username, boardType, periodKey, score)`. Call it from: score updates (GLOBAL_CLASSIC/ALL_TIME), `addWeeklyScore` (LEAGUE_WEEKLY/cohortId), casual best (CASUAL_BEST/ALL_TIME). Add `GET /leaderboards/{type}` returning ranked entries (and `GET /leagues/me`). Replace the ad-hoc `UserProgressService.getLeaderboard` to read from the projection. TDD upsert + ranking.

---

# PHASE 3 — Supabase / online

## Task 14: Supabase Postgres hosting
**Uses the supabase skill.** Provision a Supabase project; set `SPRING_DATASOURCE_URL` to the Supabase transaction pooler (`...pooler.supabase.com:6543/postgres?sslmode=require`), username/password from project settings; ensure Flyway runs `V1..V6` against it; verify `/leaderboards/*` served from Supabase. Document env vars in `backend/DEPLOYMENT.md` and `.env.example`. (Optional, deferred: Supabase Realtime read-only on `leaderboard_entries` with RLS — separate follow-up.)

---

# PHASE 4 — Android / Compose

## Task 15: Data layer
Add `path` to `SubmitWordRequest`; new models for economy/leagues/leaderboard; new Retrofit APIs (`EconomyApi`, `LeagueApi`, `LeaderboardApi`); repository methods. `GameViewModel.submitWord` sends `selected` as `path`.

## Task 16: Gameplay UI
Bonus-word feedback (distinct color + "BONUS! +N ⨀" popup), separate "Bonus words" counter/list, score-breakdown popup (from `scoreBreakdown`), hint button (shows cost, calls `/hint`, highlights revealed word).

## Task 17: League screen
New `LeagueScreen` + `LeagueViewModel`: division badge, cohort standings, promo/relegation zones, countdown, end-of-week results.

## Task 18: Store screen
New `StoreScreen` + `StoreViewModel`: coin balance, hints, category unlocks, themes; purchase flow → `/economy/purchase`.

## Task 19: Unified leaderboard screen
Tabbed Global / League / Casual reading `GET /leaderboards/{type}`; casual tab uses CASUAL_BEST.

---

## Self-Review Notes
- Spec coverage: every §3–§13 item maps to a task (see map above). ✓
- Phase 1 is independently shippable and fixes both reported bugs. ✓
- Types consistent across tasks: `Cell`/`CellDto`, `BoardState`/`SolutionWord`, `ScoreMode`/`ScoreInput`, `WordClass`/`Classification`, `EconomyService.earn/spend`. ✓
- Known follow-ups flagged inline: Phase-1 coins stubbed→wired in Task 8; weekly_score written early so leagues have data; Supabase Realtime deferred.
