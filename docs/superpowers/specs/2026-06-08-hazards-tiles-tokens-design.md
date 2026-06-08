# WordPop — Hazards, Tile Materials & Token Economy (Design Spec)

## v2 — Owner decisions (2026-06-08)
1. **Currency = existing COINS** (no new token currency). Wipe/clean, hint (30), and tile
   upgrades are all priced in coins. **Casual earns 0 coins** (implemented).
2. **10 hazards, not 3**, spread across **7 eras** over levels 1–1000 (acid moved **200 → 450**
   so it sits behind the Teflon→Acid-Resistant gate). New: Fog, Mud, Soot, Frost, Oil, Wind,
   Rust. First-appearance levels: Water 11, Fog 35, Mud 70, Paint 120, Soot 180, Frost 260,
   Oil 350, Acid 450, Wind 560, Rust 680.
3. **Everything recoverable** — a fully-acid-eaten tile is restored by a wash/clean (nothing
   permanent).
4. **Build order: Water → Paint → Acid.** Hazards ship to **Casual mode first**, toggleable in
   Settings, as a free no-coin sandbox; later promoted to competitive with the coin economy.
5. **Level select + replay** (Chapter Map): frontier = `highestLevelReached + 1`; all cleared
   levels replayable; replays pay **bonus-word coins only with a decay** (×max(0.25, 1−0.15×replays))
   and don't advance progress. Session-start gains an optional `requestedLevel`. Needs a small
   `user_level_progress(user_id, level, best_score, clear_count)` table.

Coin costs (v2): cleans 8/12/18/25 by hazard weight; upgrades Wood 2,500 / Teflon 9,000 /
Acid-Resistant 32,000 / Diamond-Glaze 110,000; hint 30. Full-path grind ≈ 185–200 hrs. Full
tables (hazard catalogue, era schedule, era earn-vs-spend, level-select) are in the design
investigation; sections below are the original v1 framing kept for context.

---

> Status: **DRAFT for approval.** No code written yet. Consolidates three specialist
> design passes (tile materials, token economy, hazard progression) plus the earlier
> hazard/physics direction. Implementation is phased (see end).

## 1. Concept

A difficulty arc built on the existing paint mechanic. Letter tiles get splashed by
escalating hazards that hide letters (a memory challenge). Players counter with **tokens**
(wipe a board) or permanent **tile-material upgrades**. Tokens are a scarce hard currency
earned by grinding, ads, or IAP.

**Core invariant:** hazards remove *information*, never *agency*. Every cell stays
selectable; every level is completable with zero spend. Wipes/upgrades buy convenience.

## 2. Hazards (escalation)

| Hazard | Effect | First seen |
|---|---|---|
| **Water** | Ink runs / smudges; hides letters on weak tiles, partially re-fades | Level 11 |
| **Paint** | Opaque blob covers tiles; appears at hazard bosses, grows each tier | Level 50 |
| **Acid** | Slowly eats a tile over ~12–25s; letter vanishes, cell stays usable | Level 200 |

Physics is **simulated** (ballistic throws, gravity-biased drips, cellular erosion) — no
real fluid/rigid-body engine. Tiles **float** over the themed background (drop shadow).

### Schedule (key thresholds)
- Levels **1–10**: hazard-free grace (learn the loop, bank tokens).
- **Water**: first @11; tiers up @26, @51, @151, @301 (capped).
- **Paint**: first @50; recurs every 50, big events every 100; tiers up @100/200/300/500.
- **Acid**: first @200 (boss); enters normal rounds @201; tiers up @300, @500.
- Bosses split: minor (level %5) vs **hazard bosses** (multiples of 50; mega at 100).
- **Visibility floor:** ≥40% of letters legible at any instant, and every target word has
  at least one moment per round where all its letters are simultaneously readable. Caps on
  all hazards so late game tests skill/memory, not pure obscuration.

## 3. Tile materials (upgrade path)

| Tier | Material | Defeats | Water | Paint | Acid |
|---|---|---|---|---|---|
| 0 | Paper & Ink (start) | — | none | none | none |
| 1 | Wood | Water | full | none | none |
| 2 | Teflon | Paint | full | full | partial (2× slower) |
| 3 | Acid-Resistant | Acid | full | full | partial (delay) |
| 4 | Ceramic / Diamond-Glaze (prestige) | all | full | full | capped at pitting (never destroyed) |

**Interaction highlights** (full matrix in agent A output):
- Paper+Water = ink bleeds over 3s → letter unreadable. Paper+Acid = curls/chars,
  destroyed ~10s.
- Teflon+Paint = blob slides off (cosmetic). Teflon+Acid = pits, coating fails ~20s then
  behaves as paper.
- Ceramic+Paint = self-clears (cracks/flakes off ~2.4s). Ceramic+Acid = dims to 70%, never
  destroyed.
- **Wipe** = clear hazard on a board. Acid wipe works only *before* full destruction; a
  fully-acid-destroyed paper/wood tile is permanent (blank but selectable). Ceramic can't
  reach that stage.

### Rendering notes (the hard parts)
- Acid hole: an irregular alpha-mask `Path` of 6–8 gravity-biased seed points grown by a
  `corrosionProgress` float, punched via `saveLayer` + `BlendMode.Clear` (reveals
  background), soft edge via blur. Letter alpha = `1 - corrosionProgress*1.5`.
- Performance on 15×15: tick corrosion at 10Hz (not per-frame), only dirty tiles recompute,
  **cap ≤4 concurrently corroding tiles**, cache grain/seed/texture, ≤4 `saveLayer` calls,
  skip off-screen ticks.

## 4. Token economy

**New hard currency `tokens`, separate from existing `coins`.** (Coins stay the easy
bonus-word soft currency; rebranding them would break the "hard to get" premise.)

- **1 WIPE = 10 tokens. 1 × 30s rewarded ad = 10 tokens = 1 wipe.**
- **Hint = 15 tokens** (shares the wipe pool) **OR 30 coins** (fallback, never hard-block).
- Wipes are **tokens-only** (the pure sink that drives ads/IAP).

### Upgrade costs (×~2.5 escalation)
| Stage | Material | Tokens | = wipes/ads |
|---|---|---|---|
| 1 | Wood | 1,000 | 100 |
| 2 | Teflon | 2,500 | 250 |
| 3 | Acid-Resistant | 6,250 | 625 |
| 4 | Diamond-Glaze | 15,000 | 1,500 |

Stage 1 = the user's "100 ads" anchor exactly. Upgrades = permanent hazard immunity.

### Earn rates & time-to-afford
- Grind ≈ **80 tokens/hour** (1/level + perf bonus + boss drips 5/10/20 + daily/streak/league).
- Ad = 10 tokens, **capped 20 ads/day** (200/day) so ads don't trivialize IAP.
- IAP bundles $0.99→$49.99 at ~111→160 tokens/$ (values only; wiring later).

| Milestone | Grind | Ads | IAP |
|---|---|---|---|
| 1 wipe | ~8 min | 1 ad | ~$0.09 |
| Wood (1st upgrade) | ~12.5 hrs | 100 ads (~5 days @cap) | ~$8 |
| Full path | ~310 hrs | ~124 days | ~$155 |

### Fairness guardrails
- 10-level grace; **3 free wipes/day**; emergency pity wipe at 0 tokens; hint coin-fallback;
  no wipe charged on a failed/abandoned board; first-upgrade nudge at 1,000 tokens / level 50.

## 5. Onboarding (one-time, dismissible modal per hazard; "?" to re-read)
- **Water (L11):** "Splash! Water makes the ink run… wipe for {N} tokens, or upgrade to Wood."
- **Paint (L50):** "Paint bombs! Covers your tiles… wipe it off; paint grows each big boss."
- **Acid (L200):** "Acid eats the letter — the tile still works, remember what was on it!"
- **First tile destroyed:** "Poof — letter's gone! Tap it if you remember; wipe for fresh letters."
- Mid-board: a persistent ghosted **🧹 Wipe · {N}** button (bottom-right), pulses once when
  tiles are obscured; if short on tokens a bottom sheet offers *watch ad / buy / keep playing*.
  Upgrade nudges only at round-end, capped ~1 per 10 levels. No mid-round popups after onboarding.

## 6. Data / backend hooks (numbers only)
- `UserProgress`: add `tokens: Long`, `tileTier: Int (0..4)`. Generalize `EconomyService`
  earn/spend/balance to tokens (reuse the ledger with a currency discriminator).
- Constants: `WIPE_COST_TOKENS=10`, `HINT_COST_TOKENS=15`, `TOKENS_PER_AD=10`,
  `DAILY_AD_CAP=20`, `FREE_WIPES_PER_DAY=3`, upgrades `[1000,2500,6250,15000]`,
  boss token rewards `5/10/20`, level token `base=1 +perf 1..2`, `DAILY_FIRST_WIN=5`,
  `STREAK_7=25`, `LEAGUE_PROMO=50`.
- Board/session carries the active hazard set + intensity (derive from level), checked
  against `tileTier` before a hazard applies / a wipe is charged.

## 7. Phased implementation plan
1. **Foundation:** hazard system refactor (pluggable, replaces ad-hoc paint) + floating tiles.
2. **Water + tokens(min):** water balloon hazard, `tokens` currency, wipe action, 10-level grace.
3. **Materials + store:** tile-tier upgrades in the store; water→Wood loop end-to-end.
4. **Paint at bosses:** scale paint with boss tier; Teflon.
5. **Acid + memory:** erosion rendering, acid hazard, Acid-Resistant/Ceramic; onboarding flows.
6. **Economy polish:** ads/IAP wiring, balance tuning, pity/free-wipe faucets.

## 8. Open questions for the owner
- Tokens a **new currency** (recommended) vs reuse coins?
- 4 upgrade tiers or 5 (include Diamond-Glaze prestige)?
- Acid fully-destroyed tile = permanent blank (recommended) vs always recoverable?
- Build order: prototype **water + floating tiles + wipe** first (fastest to feel), then layer up?
