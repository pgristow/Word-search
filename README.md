# Word Search Game

A modern Android word search game with category-based progression, backend-driven content, and engaging gameplay mechanics.

## Overview

Players solve word search puzzles across various categories, unlocking new content as they progress. The game features:

- Modern Android UI built with Jetpack Compose
- Backend API for content management and user progression
- Multiple categories (Animals, Food, Sports, Science, etc.)
- Progressive unlocking system based on completion and word count
- Words in horizontal, vertical, and diagonal orientations
- Star rating system and leaderboards
- Offline play capability

## Project Status

🚧 **Planning Phase** - Ready to begin implementation

## Documentation

- [**PROJECT_PLAN.md**](./PROJECT_PLAN.md) - Comprehensive project plan including:
  - Technology stack and architecture
  - Database schema and data models
  - API endpoint specifications
  - Development workflow and phases
  - UI/UX features and game mechanics
  - Testing strategy
  - Timeline and milestones

## Architecture

### System Components

```
┌─────────────────────┐
│  Android App        │
│  (Kotlin + Compose) │
└──────────┬──────────┘
           │ REST API
           │
┌──────────▼──────────┐
│  Backend API        │
│  (Spring Boot)      │
└──────────┬──────────┘
           │
┌──────────▼──────────┐
│  PostgreSQL         │
│  Database           │
└─────────────────────┘
```

## Tech Stack

### Frontend (Android)
- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt
- **Networking**: Retrofit
- **Database**: Room

### Backend
- **Framework**: Spring Boot (Kotlin)
- **Database**: PostgreSQL
- **Auth**: JWT
- **Caching**: Redis

## Getting Started

### Prerequisites

**For Android Development:**
- Android Studio Hedgehog or later
- JDK 17 or later
- Android SDK 24+ (API level 24)

**For Backend Development:**
- JDK 17 or later
- PostgreSQL 14+
- Redis (optional, for caching)

### Project Structure

```
Word-search/
├── android/          # Android application (to be created)
├── backend/          # Backend API server (to be created)
├── docs/             # Additional documentation
├── PROJECT_PLAN.md   # Detailed project plan
└── README.md         # This file
```

## Development Phases

| Phase | Timeline | Focus |
|-------|----------|-------|
| Phase 1 | Weeks 1-2 | Foundation (Auth, DB, Basic UI) |
| Phase 2 | Weeks 3-4 | Core Game Engine |
| Phase 3 | Weeks 5-6 | Progression & Categories |
| Phase 4 | Weeks 7-8 | Polish & Features |
| Phase 5 | Weeks 9-10 | Testing & Deployment |

## Key Features

### Gameplay
- Dynamic word search grid generation
- Touch-and-drag word selection
- Instant validation and feedback
- Multiple difficulty levels
- Hint system

### Progression
- Category-based content organization
- Unlock system based on achievements
- Star rating for puzzle completion
- Level progression
- Statistics tracking

### Social
- Global leaderboards
- Friend competitions
- Achievement system
- Daily challenges

## API Endpoints (Planned)

```
POST   /api/auth/register          - User registration
POST   /api/auth/login             - User login
GET    /api/categories             - Get all categories
GET    /api/puzzles/{id}           - Get puzzle details
POST   /api/puzzles/{id}/submit    - Submit found word
GET    /api/user/progress          - Get user progress
GET    /api/leaderboard/global     - Get leaderboard
```

## Database Schema

### Core Tables
- `users` - User accounts and profiles
- `categories` - Puzzle categories
- `puzzles` - Word search puzzles
- `words` - Words within puzzles
- `user_progress` - User's puzzle completion
- `user_found_words` - Tracking found words
- `leaderboard` - User rankings

See [PROJECT_PLAN.md](./PROJECT_PLAN.md) for detailed schema.

## Development Setup (Coming Soon)

Instructions for setting up the development environment will be added as we initialize the Android and backend projects.

## Testing

### Backend
- Unit tests with JUnit5
- Integration tests for APIs
- Load testing for performance

### Android
- Unit tests for ViewModels
- Instrumentation tests with Compose Testing
- UI tests for critical flows

## Contributing

This project is currently in the planning phase. Contribution guidelines will be established once development begins.

## License

TBD

## Contact & Support

For questions or issues, please refer to the project repository.

---

**Next Steps**: Review PROJECT_PLAN.md and begin Phase 1 implementation
