# Dual Mode Testing Plan - Classic vs Casual

## Overview
This document outlines comprehensive testing for the dual-mode Word Search game implementation. Both Classic (competitive) and Casual (relaxed) modes have been implemented across backend and Android app.

---

## Backend Testing

### 1. Game Mode API Endpoints

#### POST /api/game/session/start
**Classic Mode Test:**
```json
Request:
{
  "categoryId": "uuid-here",
  "gameMode": "CLASSIC"
}

Expected Response:
{
  "sessionId": "uuid",
  "gameMode": "CLASSIC",
  "currentScore": 0,
  "currentCombo": 0,
  ...
}
```

**Casual Mode Test:**
```json
Request:
{
  "categoryId": "uuid-here",
  "gameMode": "CASUAL"
}

Expected Response:
{
  "sessionId": "uuid",
  "gameMode": "CASUAL",
  "currentScore": 0,
  "currentCombo": 0,
  ...
}
```

**Test Cases:**
- ✓ Classic mode starts with gameMode="CLASSIC"
- ✓ Casual mode starts with gameMode="CASUAL"
- ✓ Default mode is CLASSIC when gameMode not specified
- ✓ Invalid gameMode defaults to CLASSIC
- ✓ Classic mode ends previous active session
- ✓ Casual mode allows multiple concurrent sessions

---

#### POST /api/game/session/{id}/submit-word

**Classic Mode Scoring:**
- ✓ Applies combo multiplier (2x, 3x, 4x)
- ✓ Applies speed bonus
- ✓ Applies diagonal/reversed bonuses
- ✓ Increments highestCombo on consecutive finds
- ✓ Resets combo on incorrect word

**Casual Mode Scoring:**
- ✓ Simple scoring: 100 × word.length
- ✓ No combo multiplier (always 0)
- ✓ No speed bonus
- ✓ No time tracking penalties
- ✓ Consistent scoring regardless of timing

---

#### POST /api/game/session/{id}/save (Casual Only)

**Test Cases:**
- ✓ Successfully saves casual game progress
- ✓ Sets isPaused=true, isActive=false
- ✓ Returns SessionSummary with current stats
- ✓ Rejects save for CLASSIC mode (error response)
- ✓ Unauthorized if userId doesn't match session

**Expected Response:**
```json
{
  "sessionId": "uuid",
  "totalScore": 1500,
  "wordsFound": 15,
  "highestCombo": 0,
  "duration": 25
}
```

---

#### POST /api/game/session/{id}/resume (Casual Only)

**Test Cases:**
- ✓ Resumes paused casual game
- ✓ Sets isPaused=false, isActive=true
- ✓ Returns full GameSession with grid and words
- ✓ Rejects resume for CLASSIC mode
- ✓ Rejects resume if session not paused
- ✓ Unauthorized if userId doesn't match session

---

#### POST /api/game/session/{id}/end

**Classic Mode:**
- ✓ Updates user progress (totalScore, highestCombo, etc.)
- ✓ Does NOT increment casualPuzzlesCompleted

**Casual Mode:**
- ✓ Updates user progress
- ✓ Increments casualPuzzlesCompleted by 1
- ✓ Updates lastPlayedAt timestamp

---

### 2. Database Verification

**game_sessions table:**
- ✓ game_mode column exists (VARCHAR, default 'CLASSIC')
- ✓ is_paused column exists (BOOLEAN, default false)
- ✓ Index on game_mode exists

**user_progress table:**
- ✓ casual_puzzles_completed column exists (INT, default 0)
- ✓ Column increments correctly on casual game completion

---

## Android App Testing

### 3. Navigation Flow

#### Categories → Mode Selection → Game

**Test Cases:**
- ✓ Clicking category navigates to ModeSelectionScreen
- ✓ Category ID and name passed correctly via navigation
- ✓ Back button returns to categories
- ✓ ModeSelectionScreen displays both mode cards

#### Mode Selection Screen

**Classic Mode Card:**
- ✓ Timer icon displayed
- ✓ Shows 5 features: Timed challenges, Combo multipliers, Speed bonuses, Leaderboards, Boss levels
- ✓ Primary color theme
- ✓ "Play Classic Mode" button works

**Casual Mode Card:**
- ✓ SelfImprovement/Meditation icon displayed
- ✓ Shows 5 features: No timers, No disappearing words, Simple scoring, Save/resume, Stress-free
- ✓ Tertiary color theme
- ✓ "Play Casual Mode" button works

**Clicking Mode:**
- ✓ Navigates to GameScreen with correct gameMode parameter
- ✓ GameScreen receives both categoryId and gameMode

---

### 4. Game Screen - Classic Mode

**UI Elements:**
- ✓ Shows Score stat chip
- ✓ Shows Words stat chip (found/total)
- ✓ Shows Combo stat chip (Nx format)
- ✓ Primary/secondary color scheme
- ✓ "Words to Find" title

**Word List:**
- ✓ Uses search icon for unfound words
- ✓ Uses check icon for found words
- ✓ Green color for found words
- ✓ Line-through text decoration
- ✓ SurfaceVariant background color

**Gameplay:**
- ✓ Word selection works (drag gesture)
- ✓ Submit button enabled when 3+ cells selected
- ✓ Clear button clears selection
- ✓ Correct word submission increases score
- ✓ Incorrect word shows error message
- ✓ Found words appear in list with check mark

**End Game:**
- ✓ GameOverScreen displays with final stats
- ✓ Shows final score, words found, session duration
- ✓ "Back to Categories" button works

---

### 5. Game Screen - Casual Mode

**UI Elements:**
- ✓ Shows Score stat chip
- ✓ Shows Words stat chip (found/total)
- ✓ Does NOT show Combo stat chip
- ✓ Tertiary color scheme
- ✓ "Find These Words" title (friendly wording)

**Word List:**
- ✓ Uses checkboxes instead of icons
- ✓ Checked checkbox for found words
- ✓ Unchecked checkbox for unfound words
- ✓ Faded tertiary color for found words
- ✓ Line-through text decoration
- ✓ TertiaryContainer background color

**Save Button:**
- ✓ "Save & Exit" button visible
- ✓ Tertiary color theme
- ✓ Save icon displayed
- ✓ Button positioned below Clear/Submit buttons

**Gameplay:**
- ✓ Word selection works (drag gesture)
- ✓ Submit button enabled when 3+ cells selected
- ✓ Clear button clears selection
- ✓ Correct word gives simple score (100 × length)
- ✓ No combo multiplier applied
- ✓ Found words checked in list

**Save Progress:**
- ✓ Clicking "Save & Exit" triggers save API
- ✓ CasualSavedScreen displays
- ✓ Shows save icon (tertiary color)
- ✓ "Progress Saved!" message
- ✓ Shows current score, words found, time played
- ✓ Friendly message displayed
- ✓ "Back to Categories" button works

---

### 6. Data Layer Integration

**GameRepository:**
- ✓ startSession() accepts gameMode parameter
- ✓ Defaults to "CLASSIC" if not provided
- ✓ saveCasualProgress() calls correct API endpoint
- ✓ resumeCasualGame() prepared for future use

**GameViewModel:**
- ✓ startGame() accepts gameMode parameter
- ✓ Passes gameMode to repository
- ✓ saveCasualProgress() method exists
- ✓ Handles CasualSaved state transition
- ✓ Error handling for save failures

**Data Models:**
- ✓ GameMode enum (CLASSIC, CASUAL)
- ✓ GameSession includes gameMode field
- ✓ UserProgress includes casualPuzzlesCompleted field
- ✓ StartSessionRequest includes gameMode field
- ✓ SessionSummary model exists

---

## End-to-End Test Scenarios

### Scenario 1: Complete Classic Game
1. User logs in
2. Selects a category
3. Chooses Classic Mode
4. Plays game finding all words
5. Receives combo bonuses
6. Completes game
7. Views final score on GameOverScreen
8. Returns to categories

**Expected:**
- High score with combo multipliers
- Fast completion bonus
- User progress updated
- casualPuzzlesCompleted NOT incremented

---

### Scenario 2: Complete Casual Game
1. User logs in
2. Selects a category
3. Chooses Casual Mode
4. Plays game finding some words
5. Clicks "Save & Exit"
6. Views CasualSavedScreen
7. Returns to categories

**Expected:**
- Simple scoring (100 per word length)
- No combo shown in UI
- Progress saved to backend
- casualPuzzlesCompleted incremented by 1
- Session marked as paused

---

### Scenario 3: Resume Casual Game (Future)
1. User has saved casual game
2. Returns to categories
3. Sees "Resume" option
4. Resumes saved game
5. Continues from where they left off
6. Completes game

**Expected:**
- Grid and words restored
- Previous score maintained
- Can complete puzzle

---

### Scenario 4: Multiple Concurrent Casual Games
1. User starts casual game A
2. Exits without saving
3. Starts casual game B in different category
4. Both sessions remain available

**Expected:**
- Classic mode: Only one active session
- Casual mode: Multiple concurrent sessions allowed

---

## Visual Regression Testing

### Classic Mode Theme
- ✓ Primary color: Blue/purple
- ✓ Energetic, competitive appearance
- ✓ Icons: Timer, Star, Trophy themes
- ✓ Bold, high-contrast colors

### Casual Mode Theme
- ✓ Tertiary color: Warm/soft tones
- ✓ Relaxed, calming appearance
- ✓ Icons: Meditation, Checkboxes
- ✓ Softer, less intense colors

---

## Performance Testing

### Backend
- ✓ Mode-specific scoring logic performs in <50ms
- ✓ Save progress API responds in <200ms
- ✓ Database queries optimized with indexes
- ✓ No N+1 query issues

### Android
- ✓ Mode selection screen loads instantly
- ✓ Game screen renders at 60fps
- ✓ UI doesn't lag during word selection
- ✓ State updates smoothly

---

## Edge Cases & Error Handling

### Backend
- ✓ Invalid gameMode defaults to CLASSIC
- ✓ Save on non-casual game returns error
- ✓ Resume on non-paused game returns error
- ✓ Unauthorized access rejected
- ✓ Concurrent classic sessions handled (end previous)

### Android
- ✓ Navigation back stack managed correctly
- ✓ Save failure shows error message
- ✓ Network errors handled gracefully
- ✓ Loading states displayed during API calls

---

## Security Testing

### Authentication
- ✓ JWT token required for all game endpoints
- ✓ UserId extracted from JWT, not request body
- ✓ Session ownership validated
- ✓ Cannot save/resume another user's game

### Authorization
- ✓ Category unlock level enforced
- ✓ Premium features gated correctly
- ✓ Cannot manipulate scores client-side

---

## Compatibility Testing

### Backend
- ✓ Spring Boot 3.x compatible
- ✓ PostgreSQL 15+ compatible
- ✓ Flyway migration V4 executes cleanly
- ✓ Backward compatible with existing data

### Android
- ✓ Material 3 design system
- ✓ Jetpack Compose latest stable
- ✓ Minimum SDK 24 (Android 7.0)
- ✓ Target SDK 34 (Android 14)

---

## Deployment Checklist

### Backend
- ✓ Run database migration V4
- ✓ Verify game_mode and is_paused columns
- ✓ Verify casualPuzzlesCompleted column
- ✓ Test save/resume endpoints
- ✓ Monitor API performance

### Android
- ✓ Test on physical device
- ✓ Test on various screen sizes
- ✓ Test dark mode appearance
- ✓ Verify navigation flows
- ✓ Test save/resume functionality
- ✓ Smoke test both modes

---

## Known Issues & Future Improvements

### Current Limitations
1. Resume functionality UI not yet implemented (backend ready)
2. No visual indication of saved games on categories screen
3. Timer not displayed (neither mode uses it yet)
4. No animation on mode selection

### Future Enhancements
1. Add "Resume Game" button on categories screen
2. Show saved game preview (score, words found)
3. Add mode selection animation
4. Add casual mode progress persistence across app restarts
5. Implement Quote Boss Levels (from customer feedback)
6. Implement Hidden Words Mode (from customer feedback)

---

## Sign-Off

### Backend Team
- [ ] All API endpoints tested
- [ ] Database migration verified
- [ ] Performance benchmarks met
- [ ] Security audit passed

### Android Team
- [ ] Both modes UI tested
- [ ] Navigation flows verified
- [ ] Visual themes reviewed
- [ ] Save/resume functionality tested

### QA Team
- [ ] End-to-end scenarios passed
- [ ] Edge cases handled
- [ ] Error messages user-friendly
- [ ] Performance acceptable

### Product Team
- [ ] User experience meets requirements
- [ ] Customer feedback addressed
- [ ] Visual design approved
- [ ] Ready for launch

---

## Test Results Summary

**Total Tests:** TBD
**Passed:** TBD
**Failed:** TBD
**Blocked:** TBD

**Test Coverage:**
- Backend: TBD%
- Android: TBD%

**Critical Bugs:** TBD
**Major Bugs:** TBD
**Minor Bugs:** TBD

---

## Conclusion

This comprehensive testing plan ensures both Classic and Casual modes are fully functional, visually distinct, and provide the intended user experience. All critical paths have been identified and test cases documented for verification before launch.

**Status:** ✅ Ready for Testing
**Next Steps:** Execute test plan, fix any issues found, deploy to production
