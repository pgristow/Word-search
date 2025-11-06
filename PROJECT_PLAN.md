# Word Search Game - Project Plan

## Executive Summary

A modern Android word search game with **endless gameplay**, progressive difficulty, and engaging visual effects. Players experience continuous word-finding action with ever-increasing challenges, special "boss levels" with timed board shuffles, and satisfying animations. The game features category-based themes, spectacular visual feedback, and a competitive leaderboard system.

---

## 1. Technology Stack

### Frontend (Android)
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (modern, declarative UI)
- **Architecture**: MVVM (Model-View-ViewModel) with Clean Architecture
- **Dependency Injection**: Hilt (Dagger for Android)
- **Networking**: Retrofit + OkHttp
- **Local Database**: Room (SQLite wrapper)
- **Async Operations**: Kotlin Coroutines + Flow
- **Image Loading**: Coil
- **Navigation**: Jetpack Navigation Compose
- **State Management**: ViewModel + StateFlow
- **Ads**: Google AdMob (interstitial ads)
- **In-App Purchases**: Google Play Billing Library
- **Animations**: Jetpack Compose Animations + Lottie (for complex effects)
- **Particle Effects**: Custom Canvas drawing or Particle Emitter library
- **Sound Effects**: SoundPool (low-latency audio)

### Backend
- **Framework**: Spring Boot (Kotlin) or Node.js (TypeScript)
  - **Recommendation**: Spring Boot for type safety and Java ecosystem
- **Database**: PostgreSQL (production) + H2 (development)
- **ORM**: Spring Data JPA or TypeORM
- **API Style**: RESTful JSON
- **Authentication**: JWT (JSON Web Tokens)
- **Caching**: Redis (for leaderboards, frequent queries)
- **File Storage**: AWS S3 or local storage for assets

### DevOps & Tools
- **Version Control**: Git + GitHub
- **CI/CD**: GitHub Actions
- **Backend Deployment**: Docker + Kubernetes or Railway/Render
- **Monitoring**: Sentry (error tracking), Prometheus + Grafana
- **Testing**: JUnit5, Mockk (Android), Jest/Mocha (Node) or JUnit (Spring)

---

## 2. Architecture Overview

### System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Android Application                      │
│  ┌────────────┐  ┌────────────┐  ┌──────────────────────┐  │
│  │     UI     │  │  ViewModel │  │    Repository        │  │
│  │  (Compose) │◄─┤  (State)   │◄─┤  (Data Layer)        │  │
│  └────────────┘  └────────────┘  └──────────────────────┘  │
│                                    ▲            ▲            │
│                                    │            │            │
│                          ┌─────────┴──┐   ┌────┴─────┐      │
│                          │ Remote API │   │ Room DB  │      │
│                          │   Client   │   │ (Cache)  │      │
│                          └─────────────┘   └──────────┘      │
└───────────────────────────────┬─────────────────────────────┘
                                │ HTTPS/REST
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                     Backend API Server                       │
│  ┌────────────┐  ┌────────────┐  ┌──────────────────────┐  │
│  │ Controller │  │  Service   │  │    Repository        │  │
│  │  (REST)    │─►│  (Logic)   │─►│  (Data Access)       │  │
│  └────────────┘  └────────────┘  └──────────────────────┘  │
│                                              ▲               │
│                                              │               │
│                                    ┌─────────┴──────────┐    │
│                                    │   PostgreSQL       │    │
│                                    │   (User, Puzzles,  │    │
│                                    │    Progress)       │    │
│                                    └────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### Android App Architecture (Clean Architecture)

```
┌─────────────────────────────────────────────────────────────┐
│ Presentation Layer (UI)                                      │
│  - Jetpack Compose screens                                   │
│  - ViewModels (state management)                             │
│  - UI State classes                                          │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│ Domain Layer (Business Logic)                                │
│  - Use Cases (GetPuzzleUseCase, SubmitWordUseCase)          │
│  - Domain Models (Puzzle, Word, Category)                   │
│  - Repository Interfaces                                     │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│ Data Layer                                                    │
│  - Repository Implementations                                 │
│  - Remote Data Source (API)                                  │
│  - Local Data Source (Room)                                  │
│  - DTOs and Mappers                                          │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Data Models

### Database Schema

#### User Table
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    total_words_found INTEGER DEFAULT 0,
    total_puzzles_completed INTEGER DEFAULT 0,
    current_level INTEGER DEFAULT 1,
    is_premium BOOLEAN DEFAULT false,
    premium_expires_at TIMESTAMP, -- NULL for lifetime, future date for subscription
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### Category Table
```sql
CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    icon_url VARCHAR(255),
    unlock_requirement_type VARCHAR(50), -- 'WORDS', 'PUZZLES', 'LEVEL', 'NONE'
    unlock_requirement_value INTEGER,
    display_order INTEGER,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### Puzzle Table
```sql
CREATE TABLE puzzles (
    id UUID PRIMARY KEY,
    category_id UUID REFERENCES categories(id),
    title VARCHAR(100),
    difficulty_level INTEGER, -- 1-5
    grid_size INTEGER, -- 10, 12, 15, etc.
    grid_data TEXT, -- JSON: [['A','B',...], ['C','D',...]]
    time_limit_seconds INTEGER,
    unlock_requirement_type VARCHAR(50),
    unlock_requirement_value INTEGER,
    display_order INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### Word Table
```sql
CREATE TABLE words (
    id UUID PRIMARY KEY,
    puzzle_id UUID REFERENCES puzzles(id),
    word VARCHAR(50) NOT NULL,
    start_row INTEGER NOT NULL,
    start_col INTEGER NOT NULL,
    direction VARCHAR(20) NOT NULL, -- 'HORIZONTAL', 'VERTICAL', 'DIAGONAL_DOWN', 'DIAGONAL_UP'
    points INTEGER DEFAULT 10
);
```

#### User Progress Table (Endless Mode)
```sql
CREATE TABLE user_progress (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    current_level INTEGER DEFAULT 1,
    total_score BIGINT DEFAULT 0,
    current_session_score INTEGER DEFAULT 0,
    highest_level_reached INTEGER DEFAULT 1,
    highest_combo INTEGER DEFAULT 0,
    total_words_found INTEGER DEFAULT 0,
    total_reversed_words_found INTEGER DEFAULT 0,
    last_played_at TIMESTAMP,
    current_streak_days INTEGER DEFAULT 0,
    longest_streak_days INTEGER DEFAULT 0,
    last_streak_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id)
);
```

#### Game Session Table
```sql
CREATE TABLE game_sessions (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    session_start TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    session_end TIMESTAMP,
    starting_level INTEGER,
    ending_level INTEGER,
    total_score INTEGER DEFAULT 0,
    words_found INTEGER DEFAULT 0,
    highest_combo INTEGER DEFAULT 0,
    boss_levels_completed INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true
);

-- Index for active sessions
CREATE INDEX idx_active_sessions ON game_sessions(user_id, is_active);
```

#### Boss Level Progress Table
```sql
CREATE TABLE boss_level_attempts (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    session_id UUID REFERENCES game_sessions(id),
    boss_level INTEGER NOT NULL, -- 5, 10, 15, etc.
    boss_type VARCHAR(50), -- 'SPEED', 'MEGA', 'REVERSE', 'CHAOS'
    words_required INTEGER,
    words_found INTEGER,
    time_limit_seconds INTEGER,
    time_taken_seconds INTEGER,
    shuffles_occurred INTEGER DEFAULT 0,
    completed BOOLEAN DEFAULT false,
    score_earned INTEGER DEFAULT 0,
    attempted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### User Found Words Table
```sql
CREATE TABLE user_found_words (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    puzzle_id UUID REFERENCES puzzles(id),
    word_id UUID REFERENCES words(id),
    found_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, puzzle_id, word_id)
);
```

#### Leaderboard Table
```sql
CREATE TABLE leaderboard (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    total_score INTEGER DEFAULT 0,
    rank INTEGER,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id)
);
```

#### Premium Subscription Table
```sql
CREATE TABLE premium_subscriptions (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    is_premium BOOLEAN DEFAULT false,
    purchase_date TIMESTAMP,
    expiry_date TIMESTAMP, -- NULL for lifetime purchase
    purchase_platform VARCHAR(50), -- 'GOOGLE_PLAY', 'PROMO', 'ADMIN'
    purchase_token VARCHAR(255), -- For Google Play verification
    auto_renew BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id)
);
```

#### Ad View Tracking Table
```sql
CREATE TABLE ad_views (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    viewed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ad_type VARCHAR(50), -- 'INTERSTITIAL', 'REWARDED'
    ad_unit_id VARCHAR(100),
    session_id VARCHAR(100) -- To group ads by session
);

-- Index for efficient hourly ad counting
CREATE INDEX idx_ad_views_user_time ON ad_views(user_id, viewed_at);
```

#### Ad Session State Table
```sql
CREATE TABLE ad_session_state (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    session_start_time TIMESTAMP NOT NULL,
    ads_watched_count INTEGER DEFAULT 0,
    ads_remaining INTEGER DEFAULT 5,
    session_expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, session_start_time)
);
```

---

## 4. API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login and get JWT token
- `POST /api/auth/refresh` - Refresh JWT token
- `GET /api/auth/profile` - Get current user profile

### Categories
- `GET /api/categories` - Get all available categories (with unlock status)
- `GET /api/categories/{id}` - Get category details
- `GET /api/categories/{id}/puzzles` - Get puzzles in category

### Endless Game Sessions
- `POST /api/game/start` - Start new endless game session
- `GET /api/game/board` - Get current board state (grid + active words)
- `POST /api/game/submit-word` - Submit found word, get new word + score
- `POST /api/game/level-up` - Advance to next level
- `GET /api/game/current-state` - Get current level, score, combo, etc.
- `POST /api/game/end-session` - End current session, save final score

### Boss Levels
- `POST /api/boss/start` - Start boss level challenge
- `POST /api/boss/shuffle` - Manually trigger board shuffle (or auto on timer)
- `POST /api/boss/complete` - Complete boss level, claim rewards
- `GET /api/boss/leaderboard` - Get boss level completion times

### User Progress
- `GET /api/user/progress` - Get overall user progress
- `GET /api/user/stats` - Get user statistics
- `GET /api/user/unlocked-content` - Get unlocked categories/puzzles

### Leaderboard
- `GET /api/leaderboard/global` - Get global leaderboard
- `GET /api/leaderboard/friends` - Get friends leaderboard
- `GET /api/leaderboard/category/{id}` - Get category-specific leaderboard

### Monetization & Ads
- `GET /api/ads/should-show` - Check if user should see an ad
- `POST /api/ads/view` - Record ad view
- `GET /api/ads/session-status` - Get current ad session status (ads remaining, time until reset)
- `POST /api/premium/purchase` - Verify and activate premium subscription
- `GET /api/premium/status` - Get user's premium subscription status
- `POST /api/premium/restore` - Restore previous purchase (for device changes)
- `DELETE /api/premium/cancel` - Cancel auto-renewal (handle via Google Play)

---

## 5. Game Features & Mechanics

### Core Gameplay: Endless Mode

#### The Game Never Ends - Progressive Difficulty
The game uses an **endless runner** philosophy where players continuously find words with escalating difficulty. There are no discrete "puzzles" - just continuous, increasingly challenging gameplay.

**1. Endless Board Generation**
   - Game starts with a 10x10 grid
   - Words continuously populate the board
   - When a word is found, it's replaced with a new word
   - Board dynamically updates without interrupting gameplay
   - Grid size increases as player levels up

**2. Progressive Difficulty System**

| Level Range | Grid Size | Word Directions | Special Mechanics |
|-------------|-----------|-----------------|-------------------|
| 1-5 | 10x10 | Horizontal, Vertical | Forward only |
| 6-10 | 12x12 | + Diagonal Down | Forward only |
| 11-15 | 12x12 | + Diagonal Up | **Words can be reversed** |
| 16-20 | 15x15 | All directions | 50% reversed words |
| 21-30 | 15x15 | All directions | Words overlap more |
| 31-40 | 18x18 | All directions | 70% reversed words |
| 41-50 | 20x20 | All directions | Maximum difficulty |
| 51+ | 20x20 | All directions | Speed increases |

**Difficulty Progression Mechanics:**
- **Reverse Words**: Words can appear backwards (e.g., CAT → TAC)
- **Board Size Growth**: Grid expands from 10x10 → 20x20
- **More Directions**: Start with 2 directions, unlock all 8
- **Word Overlap**: Higher levels have more intersecting words
- **Speed Increase**: Time pressure increases at higher levels
- **Vocabulary Complexity**: Longer, more obscure words

**3. Word Discovery & Visual Effects**

**Finding Words:**
- Touch and drag to select letters
- Real-time visual feedback during selection
- Word validation on release
- Haptic feedback (vibration) on success

**Spectacular Visual Effects When Word Found:**
```
✓ Word Selection Highlight (glow effect)
✓ Success Animation:
  - Particle explosion (stars/sparkles)
  - Color splash effect radiating from word
  - Flare burst animation
  - Word "pops" with scale animation
✓ Satisfying Sound Effect
✓ Score popup animation (+100 points!)
✓ Combo multiplier display (if applicable)
```

**Animation Sequence:**
1. Word selected → Glow effect
2. Validation success → Particle burst
3. Flash/flare from center of word
4. Word briefly pulses/scales up
5. Fade out with sparkles
6. New word slots in smoothly

**4. Boss Levels - Timed Shuffle Challenges**

Every **5 levels**, players face a special **Boss Level** with unique mechanics:

**Boss Level Mechanics:**
- Appears at levels 5, 10, 15, 20, 25, etc.
- **Countdown Timer**: 60 seconds to find as many words as possible
- **Board Shuffle**: When timer hits 0, the entire board reshuffles
  - All highlighted words disappear
  - New random letter positions
  - Same word list, different layout
  - Fresh challenge
- **Pressure Mechanic**: Find X words before shuffle to advance
- **Bonus Rewards**: Extra points, hints, or power-ups
- **Visual Distinction**:
  - Special background/theme
  - Glowing border around grid
  - Dramatic timer display
  - Epic music/sound effects

**Boss Level Inspiration** (from other games):
- Tetris: Time pressure + clearing mechanics
- Candy Crush: Special levels with unique objectives
- Subway Surfers: Boss runs with different rules
- Temple Run: Periodic difficulty spikes

**Boss Level Variations:**
- **Speed Boss** (Level 5, 15, 25): Find 10 words in 45 seconds
- **Mega Boss** (Level 10, 20, 30): Larger grid, find 15 words
- **Reverse Boss** (Level 20+): All words are backwards
- **Chaos Boss** (Level 30+): Board shuffles every 30 seconds

**5. Scoring & Progression**

**Points System:**
- Base word: 100 points × word length
- Reversed word: +50% bonus
- Diagonal word: +25% bonus
- Speed bonus: +10 points per second remaining
- Combo multiplier: 2x, 3x, 4x (consecutive finds under 5 seconds)
- Boss level completion: 500-2000 bonus points

**Level Up System:**
- Reach score threshold to advance level
- Level 1→2: 1,000 points
- Level 2→3: 2,000 points
- Level N→N+1: N × 1,000 points
- Visual celebration on level up (confetti, fanfare)

**Continuous Engagement:**
- Daily challenges with unique boards
- Streak bonuses for consecutive days
- Category themes rotate (Animals → Food → Sports)
- Special weekend events with bonus multipliers

**6. Hints System**
   - Reveal first letter of unfound word
   - Highlight a random word location (glow effect)
   - Shuffle one word to easier position
   - Free users: 5 hints per day
   - Premium users: 10 hints per day
   - Earn hints by watching rewarded ads

### Visual Effects & Animations Implementation

#### Word Discovery Effects

**1. Selection Glow Effect**
```kotlin
// Real-time glow as user drags finger
Modifier.drawBehind {
    drawPath(
        path = selectionPath,
        brush = Brush.radialGradient(
            colors = listOf(Color.Yellow.copy(alpha = 0.6f), Color.Transparent)
        ),
        style = Stroke(width = 8.dp.toPx())
    )
}
```

**2. Success Particle Explosion**
- Library: Custom Canvas or ParticleEmitter
- 20-30 particles emit from word center
- Particles: stars, sparkles, confetti shapes
- Physics: Velocity, gravity, fade out
- Duration: 800ms
- Colors: Dynamic based on category theme

**3. Flash/Flare Burst**
```kotlin
// Center flash animation
AnimatedVisibility(
    visible = showFlash,
    enter = scaleIn(animationSpec = tween(200)) + fadeIn(),
    exit = scaleOut(animationSpec = tween(300)) + fadeOut()
) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .background(Color.White.copy(alpha = 0.8f), CircleShape)
            .blur(20.dp)
    )
}
```

**4. Word Pulse Animation**
```kotlin
val scale by animateFloatAsState(
    targetValue = if (wordFound) 1.2f else 1.0f,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
)
```

**5. Score Popup**
```kotlin
// Animated text that floats upward
AnimatedContent(
    targetState = score,
    transitionSpec = {
        slideInVertically { it } + fadeIn() with
        slideOutVertically { -it } + fadeOut()
    }
) { score ->
    Text(
        text = "+$score",
        style = MaterialTheme.typography.h4,
        color = Color.Green,
        modifier = Modifier.offset(y = animatedOffset)
    )
}
```

#### Board Shuffle Animation (Boss Levels)

**Shuffle Sequence:**
1. **Warning Phase** (5 seconds before):
   - Timer turns red
   - Board border pulses
   - Countdown beep sounds

2. **Shuffle Initiation**:
   - All highlighted words flash
   - Grid cells "shake" animation
   - Screen shake effect (subtle)

3. **Transition**:
   - Letters scramble with rotation + position animation
   - Blur effect during transition
   - Duration: 1200ms

4. **Reveal**:
   - Letters settle into place
   - Bounce-in animation
   - New board ready sound

```kotlin
// Grid shuffle animation
val offsetX by animateFloatAsState(
    targetValue = if (shuffling) Random.nextFloat() * 10f else 0f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy)
)

LaunchedEffect(shuffleTriggered) {
    // Scramble animation
    cells.forEach { cell ->
        launch {
            cell.animateTo(
                targetPosition = newPosition,
                animationSpec = tween(1200, easing = FastOutSlowInEasing)
            )
        }
    }
}
```

#### Level Up Celebration

**Effects:**
- Full-screen confetti explosion
- "LEVEL UP!" text with scale + fade animation
- New level number display
- Achievement sound effect
- Haptic feedback (vibration pattern)
- Optional Lottie animation for major milestones

#### Combo Multiplier Visual

**Display:**
- Small indicator in corner
- Pulses/glows when active
- Shows multiplier (2x, 3x, 4x)
- Countdown bar for maintaining combo
- Reset animation when combo breaks

#### Boss Level Visuals

**Special Effects:**
- Animated background gradient
- Glowing grid border (animated)
- Particle effects around timer
- Dramatic color scheme (reds, purples)
- Screen edge glow when time running low
- Boss icon/badge display

### UI/UX Features
- **Dark mode support** with smooth theme transitions
- **Smooth animations** for all interactions (60 FPS target)
- **Offline play capability** (sync when online)
- **Tutorial for first-time users** with interactive guidance
- **Achievement badges** with unlock animations
- **Daily challenges** with special themes
- **Streak tracking** with calendar visualization
- **Haptic feedback** for all major interactions
- **Sound effects** for word found, level up, boss level, etc.
- **Accessibility**: Color-blind modes, text scaling, reduced motion option

---

## 6. Development Workflow

### Phase 1: Foundation (Weeks 1-2)
**Backend:**
- [ ] Set up Spring Boot project structure
- [ ] Configure PostgreSQL database
- [ ] Implement authentication (JWT)
- [ ] Create database migrations
- [ ] Implement User and Category APIs

**Android:**
- [ ] Set up Android project with Jetpack Compose
- [ ] Configure Hilt for dependency injection
- [ ] Set up navigation structure
- [ ] Implement authentication UI (login/register)
- [ ] Set up Room database for caching

### Phase 2: Core Game Engine (Weeks 3-4)
**Backend:**
- [ ] Implement endless game board generation algorithm
- [ ] Create dynamic word placement with difficulty scaling
- [ ] Implement reverse word logic
- [ ] Add word validation APIs
- [ ] Create game session management
- [ ] Implement scoring calculation logic
- [ ] Add combo multiplier tracking

**Android:**
- [ ] Build word search grid UI component (Canvas-based)
- [ ] Implement touch gesture handling (drag word selection)
- [ ] Create word validation with visual feedback
- [ ] Build game HUD (score, level, combo display)
- [ ] Implement game state management (endless flow)
- [ ] Add basic animations (selection glow)
- [ ] Create level progression logic

### Phase 3: Visual Effects & Boss Levels (Weeks 5-6)
**Backend:**
- [ ] Implement boss level generation logic
- [ ] Create boss level timer and shuffle mechanics
- [ ] Add boss level completion tracking
- [ ] Implement leaderboard system (by level, score)
- [ ] Create daily challenge system
- [ ] Add streak tracking logic

**Android:**
- [ ] Implement particle effect system (Canvas-based)
- [ ] Create word found animations (flare, splash, particles)
- [ ] Build score popup animations
- [ ] Add level up celebration effects
- [ ] Implement combo multiplier visual
- [ ] Create boss level UI with timer
- [ ] Build board shuffle animation
- [ ] Add haptic feedback integration
- [ ] Implement sound effects (SoundPool)

### Phase 4: Polish & Features (Weeks 7-8)
**Backend:**
- [ ] Implement ad tracking APIs
- [ ] Create premium subscription verification
- [ ] Add Google Play purchase validation
- [ ] Build ad session management logic

**Android:**
- [ ] Integrate Google AdMob SDK
- [ ] Implement interstitial ad loading and display
- [ ] Create ad session tracking UI (ads remaining indicator)
- [ ] Implement Google Play Billing
- [ ] Build premium purchase flow
- [ ] Add restore purchases functionality
- [ ] Create premium benefits UI

**Both:**
- [ ] Implement hints system
- [ ] Add daily challenges
- [ ] Create achievement system
- [ ] Implement offline sync
- [ ] Add sound effects and haptics
- [ ] Create tutorial flow
- [ ] Add dark mode
- [ ] Performance optimization

### Phase 5: Testing & Deployment (Weeks 9-10)
**Backend:**
- [ ] Unit tests for all services
- [ ] Integration tests for APIs
- [ ] Load testing
- [ ] Deploy to staging environment
- [ ] Deploy to production

**Android:**
- [ ] Unit tests for ViewModels and UseCases
- [ ] UI tests with Compose Testing
- [ ] Beta testing with TestFlight/Internal Track
- [ ] Performance profiling
- [ ] Release to Google Play Store

---

## 7. Project Structure

### Backend (Spring Boot + Kotlin)
```
backend/
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── com/wordsearch/
│   │   │       ├── WordSearchApplication.kt
│   │   │       ├── config/
│   │   │       │   ├── SecurityConfig.kt
│   │   │       │   ├── JwtConfig.kt
│   │   │       │   └── DatabaseConfig.kt
│   │   │       ├── controller/
│   │   │       │   ├── AuthController.kt
│   │   │       │   ├── CategoryController.kt
│   │   │       │   ├── PuzzleController.kt
│   │   │       │   └── UserController.kt
│   │   │       ├── service/
│   │   │       │   ├── AuthService.kt
│   │   │       │   ├── PuzzleService.kt
│   │   │       │   ├── ProgressService.kt
│   │   │       │   └── PuzzleGeneratorService.kt
│   │   │       ├── repository/
│   │   │       │   ├── UserRepository.kt
│   │   │       │   ├── CategoryRepository.kt
│   │   │       │   ├── PuzzleRepository.kt
│   │   │       │   └── ProgressRepository.kt
│   │   │       ├── model/
│   │   │       │   ├── User.kt
│   │   │       │   ├── Category.kt
│   │   │       │   ├── Puzzle.kt
│   │   │       │   ├── Word.kt
│   │   │       │   └── UserProgress.kt
│   │   │       ├── dto/
│   │   │       │   ├── request/
│   │   │       │   └── response/
│   │   │       └── util/
│   │   │           ├── JwtUtil.kt
│   │   │           └── PasswordEncoder.kt
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/
│   │           └── V1__Initial_Schema.sql
│   └── test/
│       └── kotlin/
├── build.gradle.kts
└── README.md
```

### Android App
```
android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/wordsearch/
│   │   │   │   ├── WordSearchApp.kt
│   │   │   │   ├── di/
│   │   │   │   │   ├── AppModule.kt
│   │   │   │   │   ├── NetworkModule.kt
│   │   │   │   │   └── DatabaseModule.kt
│   │   │   │   ├── presentation/
│   │   │   │   │   ├── MainActivity.kt
│   │   │   │   │   ├── navigation/
│   │   │   │   │   │   └── NavGraph.kt
│   │   │   │   │   ├── auth/
│   │   │   │   │   │   ├── LoginScreen.kt
│   │   │   │   │   │   ├── RegisterScreen.kt
│   │   │   │   │   │   └── AuthViewModel.kt
│   │   │   │   │   ├── categories/
│   │   │   │   │   │   ├── CategoryListScreen.kt
│   │   │   │   │   │   └── CategoryViewModel.kt
│   │   │   │   │   ├── game/
│   │   │   │   │   │   ├── GameScreen.kt (main endless game screen)
│   │   │   │   │   │   ├── GameViewModel.kt
│   │   │   │   │   │   ├── BossLevelScreen.kt
│   │   │   │   │   │   ├── BossViewModel.kt
│   │   │   │   │   │   └── components/
│   │   │   │   │   │       ├── WordSearchGrid.kt
│   │   │   │   │   │       ├── WordList.kt
│   │   │   │   │   │       ├── GameHUD.kt (score, level, combo)
│   │   │   │   │   │       ├── BossTimer.kt
│   │   │   │   │   │       └── effects/
│   │   │   │   │   │           ├── ParticleEffect.kt
│   │   │   │   │   │           ├── WordFoundAnimation.kt
│   │   │   │   │   │           ├── ShuffleAnimation.kt
│   │   │   │   │   │           ├── LevelUpEffect.kt
│   │   │   │   │   │           └── ComboMultiplierEffect.kt
│   │   │   │   │   ├── profile/
│   │   │   │   │   └── theme/
│   │   │   │   │       ├── Color.kt
│   │   │   │   │       ├── Theme.kt
│   │   │   │   │       └── Type.kt
│   │   │   │   ├── domain/
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── GameBoard.kt
│   │   │   │   │   │   ├── Word.kt
│   │   │   │   │   │   ├── Category.kt
│   │   │   │   │   │   ├── GameSession.kt
│   │   │   │   │   │   ├── BossLevel.kt
│   │   │   │   │   │   └── UserProgress.kt
│   │   │   │   │   ├── repository/
│   │   │   │   │   │   ├── AuthRepository.kt
│   │   │   │   │   │   ├── GameRepository.kt
│   │   │   │   │   │   ├── BossLevelRepository.kt
│   │   │   │   │   │   └── UserRepository.kt
│   │   │   │   │   └── usecase/
│   │   │   │   │       ├── StartGameSessionUseCase.kt
│   │   │   │   │       ├── SubmitWordUseCase.kt
│   │   │   │   │       ├── LevelUpUseCase.kt
│   │   │   │   │       ├── StartBossLevelUseCase.kt
│   │   │   │   │       ├── ShuffleBoardUseCase.kt
│   │   │   │   │       └── GetCategoriesUseCase.kt
│   │   │   │   └── data/
│   │   │   │       ├── remote/
│   │   │   │       │   ├── ApiService.kt
│   │   │   │       │   └── dto/
│   │   │   │       ├── local/
│   │   │   │       │   ├── AppDatabase.kt
│   │   │   │       │   ├── dao/
│   │   │   │       │   └── entity/
│   │   │   │       └── repository/
│   │   │   │           └── (implementations)
│   │   │   ├── res/
│   │   │   │   ├── drawable/
│   │   │   │   ├── values/
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   └── themes.xml
│   │   │   │   └── xml/
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

---

## 8. Key Algorithms

### Endless Board Generation Algorithm

```kotlin
/**
 * Generates an endless word search board that scales with level
 */
fun generateEndlessBoard(
    level: Int,
    category: Category,
    currentWords: List<PlacedWord> = emptyList()
): GameBoard {
    val config = getDifficultyConfig(level)
    val grid = Array(config.gridSize) { CharArray(config.gridSize) { ' ' } }

    // Keep existing words on board
    val placedWords = currentWords.toMutableList()

    // Place existing words back on grid
    placedWords.forEach { placeWord(grid, it) }

    // Add new words to fill board (target: 10-15 words active)
    val targetWordCount = when {
        level < 10 -> 8
        level < 20 -> 10
        level < 30 -> 12
        else -> 15
    }

    val wordsToAdd = targetWordCount - placedWords.size
    val newWords = getWordsByCategory(category, wordsToAdd, config.minWordLength)

    for (word in newWords) {
        val shouldReverse = Random.nextFloat() < config.reverseWordProbability
        val wordToPlace = if (shouldReverse) word.reversed() else word

        val placement = findPlacement(grid, wordToPlace, config)
        if (placement != null) {
            placeWord(grid, wordToPlace, placement)
            placedWords.add(PlacedWord(
                word = word,
                displayWord = wordToPlace,
                isReversed = shouldReverse,
                placement = placement
            ))
        }
    }

    // Fill empty cells with random letters
    fillEmptyCells(grid, config.distractorLetters)

    return GameBoard(grid, placedWords, level, config)
}

/**
 * Gets difficulty configuration based on current level
 */
data class DifficultyConfig(
    val gridSize: Int,
    val allowedDirections: List<Direction>,
    val reverseWordProbability: Float,
    val minWordLength: Int,
    val distractorLetters: String
)

fun getDifficultyConfig(level: Int): DifficultyConfig {
    return when {
        level in 1..5 -> DifficultyConfig(
            gridSize = 10,
            allowedDirections = listOf(Direction.HORIZONTAL, Direction.VERTICAL),
            reverseWordProbability = 0f,
            minWordLength = 3,
            distractorLetters = "ETAOINSHRDLU" // Common letters
        )
        level in 6..10 -> DifficultyConfig(
            gridSize = 12,
            allowedDirections = listOf(
                Direction.HORIZONTAL,
                Direction.VERTICAL,
                Direction.DIAGONAL_DOWN
            ),
            reverseWordProbability = 0f,
            minWordLength = 4,
            distractorLetters = "ETAOINSHRDLUCMFWYPVBGKJQXZ"
        )
        level in 11..15 -> DifficultyConfig(
            gridSize = 12,
            allowedDirections = Direction.values().toList(),
            reverseWordProbability = 0.3f, // 30% reversed
            minWordLength = 4,
            distractorLetters = "ETAOINSHRDLUCMFWYPVBGKJQXZ"
        )
        level in 16..20 -> DifficultyConfig(
            gridSize = 15,
            allowedDirections = Direction.values().toList(),
            reverseWordProbability = 0.5f, // 50% reversed
            minWordLength = 5,
            distractorLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        )
        level in 21..30 -> DifficultyConfig(
            gridSize = 15,
            allowedDirections = Direction.values().toList(),
            reverseWordProbability = 0.6f,
            minWordLength = 5,
            distractorLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        )
        level in 31..40 -> DifficultyConfig(
            gridSize = 18,
            allowedDirections = Direction.values().toList(),
            reverseWordProbability = 0.7f,
            minWordLength = 6,
            distractorLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        )
        else -> DifficultyConfig(
            gridSize = 20,
            allowedDirections = Direction.values().toList(),
            reverseWordProbability = 0.8f,
            minWordLength = 6,
            distractorLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        )
    }
}

/**
 * Replaces a found word with a new one
 */
fun replaceFoundWord(
    gameBoard: GameBoard,
    foundWord: PlacedWord,
    newWord: String
): GameBoard {
    // Remove found word from board
    val updatedWords = gameBoard.placedWords.filter { it != foundWord }

    // Generate new board with replacement word
    return generateEndlessBoard(
        level = gameBoard.level,
        category = gameBoard.category,
        currentWords = updatedWords
    )
}
```

### Unlock Logic Algorithm

```kotlin
/**
 * Determines if content is unlocked for user
 */
fun isUnlocked(
    content: Unlockable,
    userProgress: UserProgress
): Boolean {
    return when (content.unlockType) {
        UnlockType.NONE -> true
        UnlockType.PUZZLES_COMPLETED ->
            userProgress.totalPuzzlesCompleted >= content.unlockValue
        UnlockType.WORDS_FOUND ->
            userProgress.totalWordsFound >= content.unlockValue
        UnlockType.LEVEL ->
            userProgress.currentLevel >= content.unlockValue
        UnlockType.STARS_EARNED ->
            userProgress.totalStars >= content.unlockValue
    }
}

/**
 * Calculates user's current level based on score
 */
fun calculateLevel(totalScore: Long): Int {
    // Level N requires N * 1000 points
    // Total points for level N: sum of 1..N * 1000 = N*(N+1)*500
    // Solve: totalScore = N*(N+1)*500
    // N^2 + N - (totalScore/500) = 0
    val a = 1.0
    val b = 1.0
    val c = -(totalScore / 500.0)
    val level = ((-b + sqrt(b * b - 4 * a * c)) / (2 * a)).toInt()
    return maxOf(1, level)
}

/**
 * Calculates score for finding a word
 */
fun calculateWordScore(
    word: String,
    isReversed: Boolean,
    isDiagonal: Boolean,
    timeElapsed: Int,
    currentCombo: Int
): Int {
    val baseScore = 100 * word.length
    val reverseBonus = if (isReversed) (baseScore * 0.5).toInt() else 0
    val diagonalBonus = if (isDiagonal) (baseScore * 0.25).toInt() else 0
    val speedBonus = maxOf(0, (60 - timeElapsed) * 10)
    val comboMultiplier = when (currentCombo) {
        in 2..4 -> 2
        in 5..9 -> 3
        in 10..Int.MAX_VALUE -> 4
        else -> 1
    }

    return (baseScore + reverseBonus + diagonalBonus + speedBonus) * comboMultiplier
}
```

### Boss Level Algorithm

```kotlin
/**
 * Generates a boss level challenge
 */
data class BossLevel(
    val level: Int,
    val type: BossType,
    val board: GameBoard,
    val wordsRequired: Int,
    val timeLimitSeconds: Int,
    val shuffleIntervalSeconds: Int?
)

enum class BossType {
    SPEED,      // Fast time limit
    MEGA,       // Larger grid, more words
    REVERSE,    // All words reversed
    CHAOS       // Periodic shuffles
}

fun generateBossLevel(level: Int): BossLevel {
    val bossType = when {
        level % 30 == 0 -> BossType.CHAOS
        level % 20 == 0 -> BossType.REVERSE
        level % 10 == 0 -> BossType.MEGA
        else -> BossType.SPEED
    }

    return when (bossType) {
        BossType.SPEED -> BossLevel(
            level = level,
            type = BossType.SPEED,
            board = generateEndlessBoard(level, category),
            wordsRequired = 10,
            timeLimitSeconds = 45,
            shuffleIntervalSeconds = null
        )
        BossType.MEGA -> BossLevel(
            level = level,
            type = BossType.MEGA,
            board = generateEndlessBoard(
                level = level,
                category = category
            ).copy(gridSize = gridSize + 5), // Larger grid
            wordsRequired = 15,
            timeLimitSeconds = 90,
            shuffleIntervalSeconds = null
        )
        BossType.REVERSE -> BossLevel(
            level = level,
            type = BossType.REVERSE,
            board = generateEndlessBoard(level, category).copy(
                reverseWordProbability = 1.0f // All reversed
            ),
            wordsRequired = 12,
            timeLimitSeconds = 60,
            shuffleIntervalSeconds = null
        )
        BossType.CHAOS -> BossLevel(
            level = level,
            type = BossType.CHAOS,
            board = generateEndlessBoard(level, category),
            wordsRequired = 15,
            timeLimitSeconds = 120,
            shuffleIntervalSeconds = 30 // Shuffle every 30 seconds
        )
    }
}

/**
 * Handles board shuffle for boss levels
 */
fun shuffleBoard(bossLevel: BossLevel, foundWords: List<PlacedWord>): GameBoard {
    // Remove found words (they disappear on shuffle)
    val remainingWords = bossLevel.board.placedWords.filter { it !in foundWords }

    // Regenerate board with same words, different positions
    return generateEndlessBoard(
        level = bossLevel.level,
        category = bossLevel.board.category,
        currentWords = emptyList() // Force new positions
    ).copy(
        placedWords = remainingWords.map { word ->
            // Find new placement for each remaining word
            val newPlacement = findPlacement(grid, word.displayWord, config)
            word.copy(placement = newPlacement)
        }
    )
}
```

### Ad Management Algorithm

```kotlin
/**
 * Determines if an ad should be shown to the user
 */
data class AdSessionStatus(
    val shouldShowAd: Boolean,
    val adsRemaining: Int,
    val minutesUntilReset: Int,
    val isInAdFreeWindow: Boolean
)

fun shouldShowAd(userId: UUID, isPremium: Boolean): AdSessionStatus {
    // Premium users never see ads
    if (isPremium) {
        return AdSessionStatus(
            shouldShowAd = false,
            adsRemaining = 0,
            minutesUntilReset = 0,
            isInAdFreeWindow = true
        )
    }

    val now = Instant.now()
    val oneHourAgo = now.minus(60, ChronoUnit.MINUTES)

    // Get ad views in the last hour
    val recentAdViews = adViewRepository.findByUserIdAndViewedAtAfter(userId, oneHourAgo)
    val adsWatchedInLastHour = recentAdViews.size

    // Check if user is in ad-free window
    if (adsWatchedInLastHour >= 5) {
        val firstAdTime = recentAdViews.minByOrNull { it.viewedAt }?.viewedAt
        val adFreeWindowEnd = firstAdTime?.plus(60, ChronoUnit.MINUTES)

        if (now.isBefore(adFreeWindowEnd)) {
            val minutesRemaining = ChronoUnit.MINUTES.between(now, adFreeWindowEnd)
            return AdSessionStatus(
                shouldShowAd = false,
                adsRemaining = 0,
                minutesUntilReset = minutesRemaining.toInt(),
                isInAdFreeWindow = true
            )
        }
    }

    // User should see an ad
    val adsRemaining = 5 - adsWatchedInLastHour
    return AdSessionStatus(
        shouldShowAd = adsRemaining > 0,
        adsRemaining = maxOf(0, adsRemaining),
        minutesUntilReset = 60,
        isInAdFreeWindow = false
    )
}

/**
 * Records an ad view
 */
fun recordAdView(userId: UUID, adType: AdType, adUnitId: String): Boolean {
    val adView = AdView(
        userId = userId,
        viewedAt = Instant.now(),
        adType = adType,
        adUnitId = adUnitId,
        sessionId = UUID.randomUUID().toString()
    )

    adViewRepository.save(adView)

    // Check if this completes the ad requirement for the hour
    val status = shouldShowAd(userId, isPremium = false)
    return status.isInAdFreeWindow
}
```

---

## 9. Security Considerations

1. **Authentication**
   - JWT tokens with expiration
   - Refresh token rotation
   - Password hashing (BCrypt)
   - Rate limiting on auth endpoints

2. **API Security**
   - HTTPS only
   - Request validation
   - SQL injection prevention (parameterized queries)
   - CORS configuration
   - API rate limiting

3. **Data Protection**
   - Encrypted storage for sensitive data on device
   - No sensitive data in logs
   - Secure token storage (Android Keystore)

4. **Cheating Prevention**
   - Server-side validation for all game actions
   - Timestamp verification for puzzle completion
   - Anomaly detection for suspicious patterns

---

## 10. Performance Optimization

### Backend
- Database indexing on frequently queried fields
- Redis caching for:
  - User sessions
  - Leaderboards
  - Frequently accessed puzzles
- Connection pooling
- Lazy loading for related entities
- Pagination for list endpoints

### Android
- Image optimization and caching
- Lazy loading in lists (LazyColumn)
- State hoisting and remember for recomposition
- Background sync with WorkManager
- Efficient database queries (only fetch needed data)
- ProGuard/R8 for code shrinking

---

## 11. Testing Strategy

### Backend Testing
```kotlin
// Unit Tests
@Test
fun `should generate valid puzzle grid`() {
    val words = listOf("CAT", "DOG", "BIRD")
    val puzzle = puzzleGenerator.generatePuzzle(words, 10, Difficulty.EASY)

    assertEquals(10, puzzle.grid.size)
    assertTrue(puzzle.placedWords.isNotEmpty())
}

// Integration Tests
@Test
fun `should submit word and update progress`() {
    val response = mockMvc.post("/api/puzzles/123/submit-word") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"word": "CAT"}"""
        header("Authorization", "Bearer $token")
    }.andExpect {
        status { isOk() }
    }
}
```

### Android Testing
```kotlin
// ViewModel Tests
@Test
fun `when word is found should update game state`() = runTest {
    val viewModel = PuzzleViewModel(mockRepository)
    viewModel.submitWord("CAT")

    val state = viewModel.gameState.value
    assertTrue(state.foundWords.contains("CAT"))
}

// UI Tests
@Test
fun `should display puzzle grid`() {
    composeTestRule.setContent {
        PuzzleScreen(puzzleId = "123")
    }

    composeTestRule.onNodeWithTag("word-search-grid").assertExists()
}
```

---

## 12. Future Enhancements

### Phase 2 Features
- Multiplayer mode (compete in real-time)
- Custom puzzle creation
- Social features (share puzzles, challenge friends)
- Themed events (holidays, seasons)
- Power-ups and boosters
- Animation and visual effects upgrades

### Monetization Strategy (IMPLEMENTED)

#### Free Tier with Smart Ad System
**Hourly Ad Cap System:**
- Users see a **maximum of 5 ads per hour**
- After watching 5 ads, users get **1 hour of uninterrupted gameplay**
- Ads are interstitial (shown between puzzles, not during gameplay)
- Timer resets exactly 60 minutes after the first ad in the session
- Users can see their ad status: "X ads remaining until ad-free hour"

**Ad Placement Strategy:**
- After completing a puzzle (before returning to puzzle list)
- When starting a new category
- **Never** interrupt active puzzle gameplay
- **Never** shown to premium users

**User Experience:**
```
Puzzle Completed → Ad 1/5 → Puzzle Selection → Puzzle → Ad 2/5 → ...
→ Ad 5/5 → ✓ Ad-Free for 60 minutes → No Ads → No Ads → ...
→ (After 60 min) → Ad 1/5 → ...
```

#### Premium Version ($2.99 one-time purchase)
**Benefits:**
- ✓ Completely ad-free experience forever
- ✓ 2x daily hints (10 hints vs 5 hints)
- ✓ Exclusive premium categories
- ✓ Early access to new content
- ✓ Premium badge on leaderboard
- ✓ Cloud save backup (auto-sync progress)

**Purchase Options:**
- One-time purchase: $2.99 (lifetime)
- Alternative: $0.99/month subscription (optional, configurable)

#### Rewarded Video Ads (Optional Income Boost)
- Users can **choose** to watch a rewarded ad for:
  - +1 hint for current puzzle
  - +50 bonus points
  - Skip the hourly ad countdown (reset ad counter early)
- Completely optional, never forced
- Available even for premium users (for hints/points, not to remove ads)

#### Revenue Projections
**Assumptions:**
- 10,000 active users
- 60% free tier, 40% premium conversion over time
- Average 3 ads per session, 2 sessions per day
- $5 CPM (cost per thousand impressions)

**Estimated Monthly Revenue:**
- Free tier ads: 6,000 users × 3 ads × 2 sessions × 30 days × $5/1000 = $5,400
- Premium purchases: 4,000 users × $2.99 × 10% monthly = $1,196
- **Total: ~$6,600/month** (grows with user base)

#### Implementation Notes
- Use Google AdMob for ad serving
- Implement Google Play Billing Library v5+ for purchases
- Server-side purchase verification (prevent fraud)
- Graceful handling of ad load failures (never block gameplay)
- A/B testing capability for ad frequency (can adjust 5 ads per hour)

---

## 13. Success Metrics

### User Engagement
- Daily Active Users (DAU)
- Session length
- Puzzle completion rate
- Retention (Day 1, Day 7, Day 30)

### Game Metrics
- Average puzzles per user
- Average words found per puzzle
- Most popular categories
- Drop-off points

### Monetization Metrics
- **Ad Performance:**
  - Ad impressions per user per day
  - Ad fill rate (successful ad loads)
  - Average ads watched per session
  - Ad-free window usage (% of users reaching 5 ads)
  - CPM (Cost Per Thousand impressions)
  - eCPM (Effective CPM)

- **Premium Conversion:**
  - Free-to-Premium conversion rate (target: 3-5%)
  - Time to conversion (days before purchase)
  - Purchase drop-off rate
  - Premium user retention rate
  - Revenue per user (RPU)
  - Average Revenue Per User (ARPU)
  - Average Revenue Per Paying User (ARPPU)

- **Revenue Tracking:**
  - Daily/Monthly ad revenue
  - Daily/Monthly premium revenue
  - Lifetime Value (LTV) per user
  - Cost per acquisition (CPA) vs LTV ratio

### Technical Metrics
- API response time (< 200ms)
- App crash rate (< 1%)
- App startup time (< 2s)
- API uptime (99.9%)
- Ad load time (< 3s)
- Purchase verification success rate (> 99%)

---

## 14. Timeline Summary

| Phase | Duration | Key Deliverables |
|-------|----------|------------------|
| Phase 1: Foundation | 2 weeks | Auth system, basic UI, database setup |
| Phase 2: Core Game | 2 weeks | Puzzle generation, game UI, word validation |
| Phase 3: Progression | 2 weeks | Unlock system, categories, leaderboard |
| Phase 4: Polish | 2 weeks | Hints, achievements, offline mode |
| Phase 5: Testing & Launch | 2 weeks | Testing, optimization, deployment |
| **Total** | **10 weeks** | **Production-ready app** |

---

## 15. Getting Started

### Immediate Next Steps
1. Review and approve this plan
2. Set up development environment (Android Studio, IntelliJ/VSCode)
3. Create GitHub repository structure (monorepo or separate repos)
4. Initialize backend project (Spring Boot)
5. Initialize Android project (Kotlin + Jetpack Compose)
6. Set up CI/CD pipelines
7. Begin Phase 1 development

### Questions to Resolve
- [ ] Preferred backend framework (Spring Boot vs Node.js)?
- [ ] Deployment platform preference (AWS, GCP, Railway, Render)?
- [ ] Monorepo vs separate repositories?
- [ ] Analytics platform (Firebase, Mixpanel, Amplitude)?
- [ ] Error tracking service preference?
- [ ] Initial word database source (API, manual entry, CSV import)?

---

## Conclusion

This comprehensive plan provides a roadmap for building a modern, scalable word search game with engaging progression mechanics. The architecture supports future growth, the tech stack uses industry best practices, and the phased approach allows for iterative development and testing.

**Ready to start building? Let's begin with Phase 1!**
