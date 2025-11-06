# Word Search Game - Backend API

Spring Boot backend for the endless word search game with progressive difficulty and boss levels.

## Tech Stack

- **Framework**: Spring Boot 3.2.0
- **Language**: Kotlin 1.9.21
- **Database**: H2 (in-memory, development) / PostgreSQL (production)
- **Authentication**: JWT
- **Build Tool**: Gradle
- **API Style**: RESTful JSON

## Features Implemented

### Phase 1 ✅
- User authentication (register/login with JWT)
- Database schema with Flyway migrations
- Category management
- Word bank loaded from CSV files (6 categories with 200+ words)

### Phase 2 ✅ COMPLETE
- **Endless board generation algorithm** with progressive difficulty
- Dynamic word placement (horizontal, vertical, diagonal)
- Reverse word mechanics (words backwards)
- Scoring system with combo multipliers
- Level calculation from total score
- **Game session management** (start, submit word, end session)
- **Real-time word submission** with validation
- **User progress tracking** with statistics
- **Streak system** for daily engagement
- **Leaderboard** with global rankings

## Quick Start

### Prerequisites
- JDK 17 or higher
- Gradle 8.x (or use the Gradle wrapper `./gradlew`)

### Run the Application

```bash
cd backend

# Using Gradle wrapper (recommended)
./gradlew bootRun

# Or if you have Gradle installed globally
gradle bootRun
```

The server will start on `http://localhost:8080`

### Access H2 Database Console

While the app is running, visit: `http://localhost:8080/h2-console`

- **JDBC URL**: `jdbc:h2:mem:wordsearch`
- **Username**: `sa`
- **Password**: (leave empty)

## API Endpoints

### Authentication

#### Register User
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "player1",
  "email": "player1@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "userId": "550e8400-...",
  "username": "player1",
  "email": "player1@example.com"
}
```

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "player1",
  "password": "password123"
}
```

### Categories

#### Get All Categories
```http
GET /api/categories
```

**Response:**
```json
[
  {
    "id": "550e8400-...",
    "name": "Animals",
    "description": "Common and exotic animals from around the world",
    "unlockRequirementType": "NONE",
    "unlockRequirementValue": 0,
    "displayOrder": 1,
    "isActive": true
  },
  ...
]
```

### Game Sessions (Endless Mode)

#### Start New Session
```http
POST /api/game/session/start?userId=550e8400-...&categoryId=550e8400-...
```

**Response:**
```json
{
  "sessionId": "650e8400-...",
  "level": 1,
  "gridSize": 10,
  "category": "Animals",
  "grid": [
    "CATBIRDDOG",
    "FISHAEROG",
    "LIONXTREE"
  ],
  "words": [
    {
      "word": "CAT",
      "isReversed": false,
      "startRow": 0,
      "startCol": 0,
      "direction": "HORIZONTAL"
    }
  ],
  "currentScore": 0,
  "currentCombo": 0,
  "totalWordsFound": 0
}
```

#### Submit Found Word
```http
POST /api/game/session/{sessionId}/submit-word?userId=550e8400-...&word=CAT&isReversed=false&isDiagonal=false&timeElapsed=5
```

**Response:**
```json
{
  "valid": true,
  "score": 600,
  "totalScore": 600,
  "currentCombo": 1,
  "leveledUp": false,
  "newLevel": null,
  "wordsFoundInSession": 1
}
```

#### End Game Session
```http
POST /api/game/session/{sessionId}/end?userId=550e8400-...
```

**Response:**
```json
{
  "sessionId": "650e8400-...",
  "startingLevel": 1,
  "endingLevel": 3,
  "totalScore": 5000,
  "wordsFound": 25,
  "highestCombo": 8,
  "duration": 15
}
```

#### Get Active Session
```http
GET /api/game/session/active?userId=550e8400-...
```

### Game Utilities

#### Calculate Score for Word
```http
POST /api/game/calculate-score?word=ELEPHANT&isReversed=true&isDiagonal=true&timeElapsed=10&currentCombo=3
```

#### Get Level from Score
```http
GET /api/game/level-from-score?totalScore=50000
```

### User Progress

#### Get User Progress
```http
GET /api/user/progress?userId=550e8400-...
```

**Response:**
```json
{
  "userId": "550e8400-...",
  "currentLevel": 15,
  "totalScore": 125000,
  "highestLevelReached": 15,
  "highestCombo": 12,
  "totalWordsFound": 450,
  "totalReversedWordsFound": 180,
  "currentStreakDays": 5,
  "longestStreakDays": 12,
  "lastPlayedAt": "2025-11-06T22:30:00"
}
```

#### Get User Statistics
```http
GET /api/user/progress/statistics?userId=550e8400-...
```

**Response:**
```json
{
  "totalScore": 125000,
  "currentLevel": 15,
  "highestLevelReached": 15,
  "totalWordsFound": 450,
  "totalReversedWordsFound": 180,
  "reversedWordPercentage": 40.0,
  "highestCombo": 12,
  "totalSessionsPlayed": 25,
  "averageScorePerSession": 5000,
  "averageWordsPerSession": 18,
  "currentStreakDays": 5,
  "longestStreakDays": 12,
  "totalBossLevelsCompleted": 3
}
```

#### Update Daily Streak
```http
POST /api/user/progress/update-streak?userId=550e8400-...
```

#### Get Leaderboard
```http
GET /api/user/progress/leaderboard?limit=100
```

**Response:**
```json
[
  {
    "rank": 1,
    "userId": "550e8400-...",
    "totalScore": 500000,
    "currentLevel": 45,
    "highestLevelReached": 45
  },
  {
    "rank": 2,
    "userId": "650e8400-...",
    "totalScore": 350000,
    "currentLevel": 32,
    "highestLevelReached": 35
  }
]
```

## Progressive Difficulty

The game scales difficulty based on level:

| Level  | Grid Size | Directions       | Reverse % | Words |
|--------|-----------|------------------|-----------|-------|
| 1-5    | 10×10     | H + V            | 0%        | 8     |
| 6-10   | 12×12     | H + V + Diag     | 0%        | 10    |
| 11-15  | 12×12     | All 8 directions | 30%       | 10    |
| 16-20  | 15×15     | All 8 directions | 50%       | 12    |
| 21-30  | 15×15     | All 8 directions | 60%       | 12    |
| 31-40  | 18×18     | All 8 directions | 70%       | 15    |
| 41-50+ | 20×20     | All 8 directions | 80%       | 15    |

## Word Categories

The game includes 6 categories with CSV word banks:

1. **Animals** (41 words) - CAT, DOG, ELEPHANT, HIPPOPOTAMUS...
2. **Food** (42 words) - APPLE, PIZZA, STRAWBERRY, CAPPUCCINO...
3. **Sports** (42 words) - SOCCER, BASKETBALL, GYMNASTICS...
4. **Science** (37 words) - ATOM, GRAVITY, PHOTOSYNTHESIS...
5. **Nature** (41 words) - TREE, RAINBOW, VOLCANO, HURRICANE...
6. **Technology** (32 words) - COMPUTER, SMARTPHONE, BLOCKCHAIN...

## Database Schema

Main tables:
- `users` - User accounts
- `categories` - Game categories
- `words` - Word bank per category
- `user_progress` - Endless mode progression
- `game_sessions` - Active/completed sessions
- `boss_level_attempts` - Boss level tracking
- `leaderboard` - Global rankings

## Scoring System

```
Base Score: 100 × word length

Bonuses:
+ 50% for reversed words
+ 25% for diagonal words
+ 10 points per second saved (max 60s)

Combo Multipliers:
× 2 (combo 2-4)
× 3 (combo 5-9)
× 4 (combo 10+)
```

## Project Structure

```
backend/
├── src/main/kotlin/com/wordsearch/
│   ├── config/          # Security, CORS configuration
│   ├── controller/      # REST API controllers
│   ├── model/           # JPA entities
│   ├── repository/      # Spring Data JPA repositories
│   ├── service/         # Business logic
│   ├── dto/             # Data transfer objects
│   ├── util/            # JWT utilities
│   └── WordSearchApplication.kt
├── src/main/resources/
│   ├── db/migration/    # Flyway database migrations
│   ├── data/            # CSV word banks
│   └── application.yml  # Configuration
├── build.gradle.kts
└── README.md
```

## Next Steps (Not Yet Implemented)

### Phase 3:
- Boss level generation
- Boss level APIs with shuffle mechanics
- Leaderboard system

### Phase 4:
- Ad tracking APIs
- Premium subscription management
- Google Play purchase verification

### Phase 5:
- Testing
- Production database configuration
- Deployment setup

## Development Notes

- Uses H2 in-memory database for rapid development
- JWT tokens expire after 24 hours
- CSV word lists are loaded automatically on startup
- CORS configured for localhost:3000 and localhost:8080
- Flyway handles database migrations

## Configuration

Edit `src/main/resources/application.yml` to change:
- Server port (default: 8080)
- Database connection
- JWT secret and expiration
- Logging levels

## Building for Production

```bash
# Create executable JAR
./gradlew build

# Run the JAR
java -jar build/libs/word-search-backend-0.0.1-SNAPSHOT.jar
```

For production, switch to PostgreSQL in `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/wordsearch
    username: your_username
    password: your_password
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
```

## License

TBD

---

**Status**: Phase 1 Complete ✅ | Phase 2 In Progress ⚡
