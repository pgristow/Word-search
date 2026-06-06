# Design Spec: Word Recognition, Scoring, Coins, Leagues & Casual Mode

**Date:** 2026-06-06
**Status:** Draft for review
**Scope:** One combined spec, delivered as modules. Next Flyway migration is `V6`.

---

## 1. Background & Problem

The Word Search game (Android/Compose + Spring Boot + PostgreSQL) is feature-rich
(dual mode, daily challenges, achievements, boss levels, premium, ads) but has two
gameplay gaps the owner flagged, plus a thin progression "reason to play":

1. **Words-within-words not recognized.** `GameSessionService.submitWord` only accepts
   words in the category's predefined list (`validWords.contains(word)` →
   `GameSessionService.kt:111`). Any real word a player traces on the grid that isn't a
   target word is rejected ("Word not in list"). Overlapping target words can't be
   credited either because the server can't verify the traced path.
2. **No reward for large words.** Score is flat `100 × length`
   (`GameBoardGenerator.calculateWordScore` and `calculateCasualScore`). A 7-letter word
   feels barely better than a 4-letter one.
3. **Thin progression.** Level is derived purely from cumulative `totalScore`; difficulty
   rises automatically; the leaderboard is computed ad-hoc from `user_progress`
   (no leaderboard table). There is no recurring goal, no economy, no "destination."

**Root architectural constraint:** the backend does **not** persist the generated grid,
so it cannot verify a traced path or detect off-list words. The client builds the word
string locally and submits only the word + flags. This must change for any secure
recognition/scoring work.

### Research grounding (top word games)
- **Bonus words**: Wordscapes / Word Cookies reward real words not in the puzzle list
  (coins + score). This is the #1 missing "popular aspect."
- **Length-rewarded scoring**: both games give super-linear points for longer words.
- **Recurring competition**: weekly leagues/divisions (Duolingo-style) drive retention.
- **Coins**: the lubricant that connects bonus words → hints/unlocks/cosmetics.

---

## 2. Goals / Non-Goals

### Goals
- Recognize **both** overlapping target words **and** real off-list dictionary words
  ("bonus words"), validated server-side against the stored board.
- Reward long words via a **length × difficulty** scoring model.
- Add a **coin economy** (earn from bonus words & wins; spend on hints, early category
  unlock, cosmetic themes).
- Add a **competitive league** progression spine (weekly tiers, promotion/relegation).
- Refine **casual mode** to a fixed baseline difficulty with its **own leaderboard**.
- Host data on **Supabase Postgres** so leaderboards are reliably **online**, with a
  **unified leaderboard schema** covering competitive, league, and casual boards.

### Non-Goals (YAGNI for this spec)
- Real-money store / IAP changes beyond existing premium.
- Friend graph / social invites.
- Multiplayer real-time play.
- Rewriting existing auth (stays Spring Boot JWT).

---

## 3. Architecture Decision: Server-Authoritative Recognition

**Chosen:** Approach A — the server owns the board and validates traced paths.

- On session start, persist the grid + solution (placed-word paths) with the session.
- The client sends the **traced path** (ordered list of cells) it already computes
  (`GameViewModel._selectedCells`) alongside the word.
- The server reconstructs the letters from the stored grid along the path, confirms the
  path is a straight line of contiguous cells, confirms the letters spell the claimed
  word, then classifies it: **target** / **bonus** / **invalid**.

Rejected: client-side dictionary with trusted submissions (cheatable — fatal once bonus
words feed score/leagues) and a hybrid dual-dictionary (more than needed now).

**Dictionary:** a bundled English word list loaded into an in-memory structure
(`DictionaryService`) at startup; min length 3; uppercased. ~170k words (~2 MB resource).

---

## 4. Module A — Word Recognition

### 4.1 Board persistence
Add a `board_state` column to `game_sessions` (JSON/JSONB) storing:
```json
{
  "gridSize": 8,
  "grid": [["C","A","T",...], ...],
  "solution": [
    {"word":"CAT","path":[[0,0],[0,1],[0,2]],"isReversed":false,"direction":"HORIZONTAL"}
  ]
}
```
This also fixes the existing bug where `getActiveSession` returns an empty grid
(`GameSessionService.kt:299-304`): resume can now rebuild the real board.

### 4.2 Submission flow (`submitWord` rewrite)
Input gains `path: List<Cell>` (Cell = `{row, col}`). Algorithm:
1. Load session + `board_state`.
2. **Path validation:** non-empty; all cells in-bounds; equal step deltas between
   consecutive cells (one of the 8 directions); letters read off `grid` equal the
   submitted word (case-insensitive). Fail → `invalid` (reason "Not a straight line" /
   "Letters don't match").
3. **Classification:**
   - In category target list & not already found → **target**. Counts toward
     `targetWordCount`; updates combo (Classic).
   - Else in dictionary, length ≥ 3, not already found, not a target → **bonus**.
     Awards score + coins; does **not** count toward puzzle completion; does **not**
     extend/break the target combo chain.
   - Already found (target or bonus) → `invalid` ("Already found"), combo reset rules
     unchanged.
   - Otherwise → `invalid` ("Not a word").
4. Persist `UserFoundWord` with `is_bonus`, `word_length`, and `path`.

Overlapping target words are handled for free: the player traces them, the path
validates, classification credits them.

### 4.3 Anti-abuse
- All classification server-side; client never asserts validity or score.
- Dedup by `(session_id, word)` (existing `existsBySessionIdAndWord`), extended to bonus.
- Per-session bonus cap (config, e.g. 50) to bound coin farming on lucky boards.

---

## 5. Module B — Scoring Overhaul

Replace `calculateWordScore` / `calculateCasualScore` with one tunable model
(`ScoringService`), so Classic and Casual share the curve:

```
base        = 100 * length
lengthBonus = Σ tier(i) for the 5th..Nth letter
              5th:+50, 6th:+100, 7th:+200, 8th+:+300 each
reverseBonus  = (isReversed)  ? 0.5 * (base + lengthBonus) : 0
diagonalBonus = (isDiagonal)  ? 0.25 * (base + lengthBonus) : 0
speedBonus    = min(100, max(0, (60 - timeElapsed) * 2))     // Classic only
subtotal      = base + lengthBonus + reverseBonus + diagonalBonus + speedBonus
comboMult     = 1 / 2 / 3 / 4   (combo 1 / 2–4 / 5–9 / 10+)  // Classic target words only

Classic target word: final = subtotal * comboMult
Casual word:         final = base + lengthBonus               (no speed/combo)
Bonus word (any mode): final = base + lengthBonus + reverse/diagonal bonuses
                               (NO combo multiplier, NO speed bonus)
```
Length tiers and tier values live as constants in `ScoringService` for easy tuning.
`scoreBreakdown` (base, length, reverse, diagonal, speed, comboMult) is returned to the
client so the UI can show *why* a word scored what it did.

**Coins from a word:** `bonusCoins = 5 + 2 * max(0, length - 3)` for bonus words
(tunable in `EconomyService`); target words earn no coins directly (their reward is score
+ progression). Numbers are starting points.

---

## 6. Module C — Coin Economy

### 6.1 Wallet & ledger
- `coins` balance on `user_progress` (add column).
- `coin_transactions` ledger: `id, user_id, delta, reason (enum), ref_id, balance_after,
  created_at`. Every earn/spend writes a row; balance is derived/checked from the ledger
  to prevent drift and enable audit.

### 6.2 Earning (all server-side, via `EconomyService`)
| Source | Default reward |
|---|---|
| Bonus word | `5 + 2*(len-3)` coins |
| Puzzle completion (all targets) | 25 |
| Daily challenge win | 50 |
| Boss level win | 75 |
| Daily streak tick | 10 * min(streakDays, 7) |
| League promotion | 100 (Bronze→…) scaling by tier |

### 6.3 Spending — the store
- **Hint** — `POST /sessions/{id}/hint`: deduct coins (default 30), return one unfound
  target word's location from `board_state` (server-authoritative). Wires the
  long-promised hint system into the loop.
- **Category early-unlock** — pay coins to unlock a category before its milestone.
  Reconciliation: **Animals free**; others unlock by existing milestone (free path) **or**
  coins. Add `user_unlocked_categories` (user_id, category_id, unlocked_at, method).
  Add `coin_unlock_cost` to `categories`.
- **Cosmetic themes** — `themes` table + `user_owned_themes`; purely visual board skins.

### 6.4 API
`GET /economy/wallet`, `GET /economy/store`, `POST /economy/purchase` (themes/unlocks),
`POST /sessions/{id}/hint`. All deductions validated against ledger balance.

---

## 7. Module D — Competitive Leagues (progression spine)

### 7.1 Model
- **Tiers (static):** Bronze, Silver, Gold, Platinum, Diamond, Master (`league_tiers`).
- **Weekly cycle:** Monday 00:00 UTC → Sunday. Players grouped into a **cohort**
  (~30) within their tier.
- **Metric:** `weekly_score` — a separate accumulator added to alongside lifetime
  `totalScore` whenever score is earned; **reset to 0** each Monday. Fair for newcomers,
  and neutralizes bonus-word board-luck skew (everyone restarts weekly).
- **Rollover (scheduled):** `LeagueService.runWeeklyRollover()` (`@Scheduled`, Monday
  00:00 UTC) closes cohorts, ranks by `weekly_score`, **promotes top ~7 / relegates
  bottom ~5 / middle holds**, pays promotion coins, records final ranks, and seeds next
  week's cohorts (snake/seeded fill to keep cohorts full).

### 7.2 Tables
- `league_tiers` — static tier definitions (order, name, promote_count, relegate_count,
  promotion_reward).
- `league_cohorts` — one row per (tier, week_key); status open/closed.
- `league_memberships` — `user_id, cohort_id, weekly_score, final_rank, result
  (PROMOTED/STAYED/RELEGATED)`. Current membership = cohort with status open.

### 7.3 Android
New **League** screen: current division badge, live cohort standings (rank, name,
weekly_score), promotion/relegation zone shading, countdown to rollover, and an
end-of-week results moment (promoted/relegated animation).

---

## 8. Casual Mode Refinement

Refine existing `GameMode.CASUAL` (do not add a new mode):
1. **Baseline difficulty:** casual board generation uses a fixed `DifficultyConfig`
   (level-1-equivalent: small grid, H/V only, no reversed, min length 3), **independent**
   of `userProgress.currentLevel`. Add a dedicated `getCasualConfig()` in
   `GameBoardGenerator`.
2. **Separate leaderboard:** casual scores never touch league `weekly_score`, lifetime
   competitive `totalScore`, or level. Casual has its own board.
3. **Metric (default):** best single-game casual score (`casual_best_score` on
   `user_progress`, updated on session end). *Confirm in review — alternatives: total
   casual score, words found.*
4. Bonus words & length scoring **do** apply in casual (Module A/B), earning coins; just
   no combo/speed and no progression.

---

## 9. Unified Leaderboard Schema (Supabase, online)

There is currently **no leaderboard table** — boards are computed from `user_progress`.
We introduce one aligned model serving all three board types.

### 9.1 `leaderboard_entries`
```
id            uuid pk
user_id       uuid           -- denormalized username for display
username      text
board_type    text           -- GLOBAL_CLASSIC | LEAGUE_WEEKLY | CASUAL_BEST
period_key    text           -- 'ALL_TIME' | week_key '2026-W23' | cohort_id
score         bigint
updated_at    timestamptz
unique (user_id, board_type, period_key)
index (board_type, period_key, score desc)
```
- **GLOBAL_CLASSIC** / `ALL_TIME` ← lifetime `totalScore`.
- **LEAGUE_WEEKLY** / `cohort_id` ← `weekly_score` (rank within cohort).
- **CASUAL_BEST** / `ALL_TIME` ← `casual_best_score`.

One table, one set of endpoints (`GET /leaderboards/{type}?period=...`), uniform Android
rendering. League cohort membership/promotion still lives in `league_memberships`; this
table is the **scoreboard projection**, updated by the services that change scores.

### 9.2 Supabase / online
- **Host:** provision a Supabase project; point Spring Boot `spring.datasource.url` at the
  Supabase Postgres **transaction pooler** (port 6543) with SSL; run Flyway migrations
  against it. No application-logic rewrite — Supabase is the managed, always-online
  Postgres. (See supabase skill at implementation time for pooler/SSL specifics.)
- **Online reach:** leaderboards are served by the deployed Spring Boot API backed by
  Supabase, so all clients see shared, live data.
- **Optional (phase 2 nicety):** expose `leaderboard_entries` read-only via Supabase
  Realtime/PostgREST with RLS for a live-updating board, without moving game logic off
  Spring Boot. Not required for the unified-schema goal.

---

## 10. Data Model Summary (Flyway `V6`)

- `game_sessions`: + `board_state` JSONB.
- `user_found_words`: + `is_bonus` bool, + `word_length` int, + `path` JSONB.
- `user_progress`: + `coins` bigint, + `total_bonus_words_found` int,
  + `longest_word_found` int, + `weekly_score` bigint, + `casual_best_score` bigint.
- New: `coin_transactions`, `themes`, `user_owned_themes`, `user_unlocked_categories`,
  `league_tiers`, `league_cohorts`, `league_memberships`, `leaderboard_entries`.
- `categories`: + `coin_unlock_cost` int.
- Seed: `league_tiers` (6), a few starter `themes`.

---

## 11. API Summary (new/changed)

- `POST /sessions/{id}/submit` — body gains `path`; response gains `isBonus`,
  `wordLength`, `scoreBreakdown`, `coinsEarned`, `coinBalance`.
- `POST /sessions/{id}/hint` — spend coins, reveal a target word location.
- `GET /economy/wallet`, `GET /economy/store`, `POST /economy/purchase`.
- `GET /leaderboards/{type}` — unified board (GLOBAL_CLASSIC, LEAGUE_WEEKLY, CASUAL_BEST).
- `GET /leagues/me` — current division, cohort standings, rollover time.

---

## 12. Android / UI-UX Summary

- **DTOs/repo:** `SubmitWordRequest.path`; new economy/league/leaderboard models & APIs.
- **Game screen:** send traced path; **bonus-word feedback** (distinct color + "BONUS!
  +N ⨀coins" popup, separate "Bonus words" counter/list); **score-breakdown popup**
  showing length & multipliers; **hint button** (shows cost, disabled if broke).
- **League screen** (new): division, standings, promo/relegation zones, countdown,
  weekly results moment.
- **Store screen** (new): coin balance, hints, category unlocks, themes.
- **Leaderboard screen:** tabbed — Global / League / Casual — off the unified endpoint.
- **Casual:** baseline difficulty, casual leaderboard tab, no level/progression UI.

---

## 13. Testing Strategy

Pure-logic core is built **test-first (TDD)**:
- `ScoringService` — length tiers, multipliers, casual vs classic vs bonus paths.
- Recognition classifier — path validation (straight-line, bounds, letter match),
  target/bonus/invalid, dedup, overlapping target words, bonus cap.
- `DictionaryService` — membership, min length, case.
- `EconomyService` — earn/spend, ledger balance integrity, insufficient funds.
- `LeagueService.runWeeklyRollover` — promotion/relegation math, cohort seeding,
  reward payout (deterministic with injected clock).
- Integration: submit endpoint with path (target, bonus, invalid, spoofed path),
  hint endpoint, leaderboard projections.
- Android: ViewModel tests for bonus feedback & path submission.

---

## 14. Delivery Plan (fanned-out specialist agents)

Sequenced so prerequisites land first; independent work parallelized.

1. **Foundation (sequential):** Flyway `V6` migration + entities/repositories +
   `DictionaryService` resource. Everything else depends on this.
2. **Parallel after foundation:**
   - *Backend recognition+scoring agent* — board persistence, `submitWord` classifier,
     `ScoringService` (TDD).
   - *Backend economy+leagues agent* — `EconomyService`, ledger, store, hints,
     `LeagueService` + rollover, unified leaderboard projection (TDD).
   - *Android data agent* — DTOs, APIs, repositories (path, economy, leagues, boards).
3. **Parallel after data agent:**
   - *Compose UI/UX agent (gameplay)* — bonus feedback, score breakdown, hint button.
   - *Compose UI/UX agent (meta)* — League, Store, unified Leaderboard screens.
4. **Infra:** Supabase provisioning + datasource config + migration run (uses supabase
   skill).
5. **Integration & verification:** end-to-end tests, manual verification per
   verification-before-completion.

---

## 15. Open Items to Confirm in Review
- Casual leaderboard metric (default: best single-game score).
- Starting economy numbers (coin values, hint cost, unlock costs).
- League constants (cohort 30, promote 7 / relegate 5, 6 tiers, UTC week).
- Whether to include the optional Supabase Realtime live board now or defer to phase 2.
