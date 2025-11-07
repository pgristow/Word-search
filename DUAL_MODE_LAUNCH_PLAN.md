# Dual Mode Launch Plan - Classic + Casual

## Goal
Launch with both **Classic Mode** (competitive) and **Casual Mode** (relaxed) to serve both player types.

---

## Current Status Assessment

### ✅ Already Implemented
- Backend authentication with JWT
- Game session management
- Word validation logic
- Category system
- User progress tracking
- Android UI foundation (Login, Categories, Game screens)
- API endpoints for game sessions

### 🔨 Needs Implementation
- Game mode enum and selection
- Casual mode logic (no timers, no combos)
- Mode selection UI
- Casual mode visual theme
- Save/resume functionality for casual mode

---

## Implementation Checklist

### Phase 1: Backend - Game Mode Support (Day 1)

**1.1 Create Game Mode Enum**
```kotlin
enum class GameMode {
    CLASSIC,  // Competitive with timers and combos
    CASUAL    // Relaxed, no pressure
}
```

**1.2 Update Database Models**
- Add `gameMode` field to `GameSession`
- Add migration script
- Update repositories

**1.3 Update Game Session Service**
- Add mode-specific logic
- Classic: Apply timers, combos, speed bonuses
- Casual: Skip timer logic, all scoring simplified
- Save/resume support for casual mode

**1.4 Update API Endpoints**
- Add `gameMode` parameter to session start
- Return mode-specific responses
- No timer fields for casual mode responses

---

### Phase 2: Android - Mode Selection (Day 2)

**2.1 Create Mode Selection Screen**
```
After Categories Screen:
┌─────────────────────────────┐
│      Choose Game Mode       │
├─────────────────────────────┤
│                             │
│   ┌───────────────────┐    │
│   │  🏆 CLASSIC MODE  │    │
│   │                   │    │
│   │  • Timed gameplay │    │
│   │  • Combos         │    │
│   │  • Leaderboards   │    │
│   │  • Boss levels    │    │
│   └───────────────────┘    │
│                             │
│   ┌───────────────────┐    │
│   │  🧘 CASUAL MODE   │    │
│   │                   │    │
│   │  • No timers      │    │
│   │  • Relaxing       │    │
│   │  • Your pace      │    │
│   │  • Save progress  │    │
│   └───────────────────┘    │
│                             │
└─────────────────────────────┘
```

**2.2 Update Navigation**
- Add mode selection between Categories and Game
- Pass mode parameter to GameScreen

---

### Phase 3: Casual Mode UI (Day 3)

**3.1 Create CasualGameScreen**
Based on existing GameScreen but with changes:
- ❌ Remove timer display
- ❌ Remove combo indicator
- ❌ Remove speed bonus text
- ✅ Show word list with checkboxes
- ✅ Soft color palette
- ✅ "Take your time" messaging
- ✅ Save button (save and exit)
- ✅ Gentle animations

**3.2 Visual Theme**
```kotlin
// Casual mode colors
val CasualBackground = Color(0xFFF5F9FC) // Soft blue-gray
val CasualPrimary = Color(0xFF7FB3D5)    // Gentle blue
val CasualWordFound = Color(0xFFA8D5BA) // Soft green
val CasualText = Color(0xFF5A5A5A)       // Muted text
```

**3.3 Update GameViewModel**
- Add mode-aware logic
- Skip combo tracking for casual
- Simplified scoring for casual
- Save/resume state management

---

### Phase 4: Integration & Testing (Day 4)

**4.1 End-to-End Testing**
- [ ] User can select mode
- [ ] Classic mode works as before (timed, combos)
- [ ] Casual mode has no timer
- [ ] Casual mode can save/resume
- [ ] Words cross off correctly in casual
- [ ] Scoring works differently per mode

**4.2 Mode-Specific Features**
- Classic: Timer countdown works
- Classic: Combo multiplier applies
- Casual: No time pressure
- Casual: Save button functional
- Both: Word validation works

---

## Implementation Details

### Backend Changes

**1. GameMode Enum**
```kotlin
// backend/src/main/kotlin/com/wordsearch/model/GameMode.kt
package com.wordsearch.model

enum class GameMode {
    CLASSIC,
    CASUAL;

    fun isCasual() = this == CASUAL
    fun isClassic() = this == CLASSIC
}
```

**2. Updated GameSession Model**
```kotlin
@Entity
@Table(name = "game_sessions")
data class GameSession(
    // ... existing fields ...

    @Enumerated(EnumType.STRING)
    @Column(name = "game_mode")
    val gameMode: GameMode = GameMode.CLASSIC,

    @Column(name = "is_paused")
    val isPaused: Boolean = false  // For casual mode save/resume
)
```

**3. Database Migration**
```sql
-- V2__Add_Game_Mode.sql
ALTER TABLE game_sessions
ADD COLUMN game_mode VARCHAR(20) DEFAULT 'CLASSIC';

ALTER TABLE game_sessions
ADD COLUMN is_paused BOOLEAN DEFAULT false;
```

**4. Updated Service Logic**
```kotlin
fun submitWord(
    sessionId: UUID,
    userId: UUID,
    word: String,
    isReversed: Boolean,
    isDiagonal: Boolean,
    timeElapsed: Int
): WordSubmissionResponse {
    val session = getSession(sessionId)

    // Mode-specific scoring
    val score = when (session.gameMode) {
        GameMode.CLASSIC -> calculateClassicScore(
            word, isReversed, isDiagonal, timeElapsed, session.currentCombo
        )
        GameMode.CASUAL -> calculateCasualScore(word) // Simple scoring
    }

    // Mode-specific combo
    val combo = when (session.gameMode) {
        GameMode.CLASSIC -> updateCombo(session, timeElapsed)
        GameMode.CASUAL -> 0 // No combos in casual
    }

    return WordSubmissionResponse(
        correct = true,
        score = score,
        combo = combo,
        levelUp = false,
        newLevel = null,
        message = "Great job!"
    )
}

private fun calculateCasualScore(word: String): Int {
    // Simple scoring: 100 points per word, no bonuses
    return 100 * word.length
}
```

**5. New API Endpoints**
```kotlin
// Save casual game progress
@PostMapping("/casual/{sessionId}/save")
fun saveCasualProgress(
    @PathVariable sessionId: String,
    authentication: Authentication
): ResponseEntity<Any>

// Resume casual game
@GetMapping("/casual/{sessionId}/resume")
fun resumeCasualGame(
    @PathVariable sessionId: String,
    authentication: Authentication
): ResponseEntity<GameSessionResponse>
```

---

### Android Changes

**1. Mode Selection Screen**
```kotlin
// ModeSelectionScreen.kt
@Composable
fun ModeSelectionScreen(
    categoryId: String,
    onModeSelected: (GameMode) -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Choose Game Mode",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Classic Mode Card
        ModeCard(
            title = "Classic Mode",
            icon = Icons.Default.Star,
            description = "Competitive gameplay with timers and combos",
            features = listOf(
                "⏱️ Timed challenges",
                "🔥 Combo multipliers",
                "🏆 Leaderboards",
                "👑 Boss levels"
            ),
            onClick = { onModeSelected(GameMode.CLASSIC) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Casual Mode Card
        ModeCard(
            title = "Casual Mode",
            icon = Icons.Default.Favorite,
            description = "Relaxing gameplay at your own pace",
            features = listOf(
                "🧘 No timers",
                "😌 No pressure",
                "💾 Save anytime",
                "☮️ Peaceful experience"
            ),
            onClick = { onModeSelected(GameMode.CASUAL) }
        )
    }
}
```

**2. Casual Game Screen**
```kotlin
// CasualGameScreen.kt
@Composable
fun CasualGameScreen(
    categoryId: String,
    onNavigateBack: () -> Unit,
    viewModel: GameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Casual-specific theme
    val casualColors = remember {
        lightColorScheme(
            primary = Color(0xFF7FB3D5),
            secondary = Color(0xFFA8D5BA),
            background = Color(0xFFF5F9FC)
        )
    }

    MaterialTheme(colorScheme = casualColors) {
        Scaffold(
            topBar = {
                CasualTopBar(
                    onSave = { viewModel.saveCasualProgress() },
                    onBack = onNavigateBack
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Encouraging message
                Text(
                    text = "Take your time and enjoy! 🌸",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )

                // Word grid (same as classic but different theme)
                WordGrid(...)

                // Word list with checkboxes
                CasualWordList(
                    words = words,
                    foundWords = foundWords
                )
            }
        }
    }
}
```

**3. Updated Navigation**
```kotlin
// NavGraph.kt
composable(
    route = "mode_selection/{categoryId}",
    arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
) { backStackEntry ->
    val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
    ModeSelectionScreen(
        categoryId = categoryId,
        onModeSelected = { mode ->
            navController.navigate("game/${categoryId}/${mode.name}")
        },
        onNavigateBack = { navController.popBackStack() }
    )
}

composable(
    route = "game/{categoryId}/{mode}",
    arguments = listOf(
        navArgument("categoryId") { type = NavType.StringType },
        navArgument("mode") { type = NavType.StringType }
    )
) { backStackEntry ->
    val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
    val mode = backStackEntry.arguments?.getString("mode") ?: "CLASSIC"

    when (mode) {
        "CASUAL" -> CasualGameScreen(categoryId, onNavigateBack = { ... })
        else -> GameScreen(categoryId, onNavigateBack = { ... })
    }
}
```

---

## Timeline

**Day 1 (Backend):**
- ✅ Create GameMode enum
- ✅ Update GameSession model
- ✅ Database migration
- ✅ Update service logic for casual mode
- ✅ Test backend endpoints

**Day 2 (Mode Selection):**
- ✅ Create ModeSelectionScreen UI
- ✅ Update navigation
- ✅ Test mode selection flow

**Day 3 (Casual UI):**
- ✅ Create CasualGameScreen
- ✅ Implement casual theme
- ✅ Word list with checkboxes
- ✅ Save/resume functionality

**Day 4 (Testing & Polish):**
- ✅ End-to-end testing both modes
- ✅ Fix bugs
- ✅ Polish UI
- ✅ Ready for launch

---

## Success Criteria

### Classic Mode
- [x] Timer counts down
- [x] Combos apply
- [x] Speed bonuses work
- [x] Boss levels appear at intervals
- [x] Leaderboard integration

### Casual Mode
- [ ] No timer displayed
- [ ] No combo mechanics
- [ ] Simple scoring (100 per word)
- [ ] Save/resume works
- [ ] Relaxing visual theme
- [ ] Word list with checkboxes
- [ ] "Take your time" messaging

---

## Marketing Copy

### App Store Description
```
Two Ways to Play:

🏆 CLASSIC MODE
Fast-paced word finding action! Race against the clock, build combos,
and climb the leaderboards. Face challenging boss levels and prove
you're the ultimate word finder.

🧘 CASUAL MODE
Relax and unwind with stress-free word search. No timers, no pressure -
just you and the puzzle. Perfect for bedtime, commutes, or whenever you
need a peaceful break. Save your progress anytime.

Choose your style. Play your way.
```

---

## Risk Mitigation

**Risk:** Casual players find Classic mode accidentally
**Mitigation:** Clear mode selection screen with descriptions

**Risk:** Mode switching confusion
**Mitigation:** Mode is set at game start, can't switch mid-game

**Risk:** Casual mode feels incomplete
**Mitigation:** Full-featured with save/resume, progress tracking, achievements

---

## Post-Launch Analytics

Track per mode:
- Selection rate (Classic vs Casual)
- Session length
- Completion rate
- User retention
- User satisfaction scores

**Hypothesis:** Casual mode will have:
- Longer average sessions
- Higher completion rate
- Better retention for 35+ age group
- More bedtime usage (evening sessions)

---

Ready to implement! 🚀
