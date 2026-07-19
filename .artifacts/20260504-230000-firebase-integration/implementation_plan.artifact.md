# Achievement System Implementation

Add a comprehensive achievement system to the school planner app based on the user's provided list of 25 goals.

## Proposed Changes

### Core System
- **[Achievement.kt](file:///C:/Users/luisj/AndroidStudioProjects/Schulplaner/app/src/main/java/com/example/schulplaner/Achievement.kt)**: Define the data structure for achievements (ID, title, description, icon, condition).
- **[AchievementManager.kt](file:///C:/Users/luisj/AndroidStudioProjects/Schulplaner/app/src/main/java/com/example/schulplaner/AchievementManager.kt)**: Static registry of all 25 achievements with their logic conditions.

### Tracking Improvements
- **[UserStatsManager.kt](file:///C:/Users/luisj/AndroidStudioProjects/Schulplaner/app/src/main/java/com/example/schulplaner/UserStatsManager.kt)**: Add trigger methods for complex achievements (Schrödinger check, Sunday depression, procrastination count).
- **[Grade.kt](file:///C:/Users/luisj/AndroidStudioProjects/Schulplaner/app/src/main/java/com/example/schulplaner/Grade.kt)**: Add `date` field to track when grades were received.
- **[Homework.kt](file:///C:/Users/luisj/AndroidStudioProjects/Schulplaner/app/src/main/java/com/example/schulplaner/Homework.kt)**: Add `postponeCount` to track procrastination.
- **[Flashcard.kt](file:///C:/Users/luisj/AndroidStudioProjects/Schulplaner/app/src/main/java/com/example/schulplaner/Flashcard.kt)**: Add `failedStreak` to track "Amnesie".

### UI Integration
- **[AchievementsScreen.kt](file:///C:/Users/luisj/AndroidStudioProjects/Schulplaner/app/src/main/java/com/example/schulplaner/AchievementsScreen.kt)**: Display unlocked and locked achievements with flavor text and icons.
- **[HomeworkScreen.kt](file:///C:/Users/luisj/AndroidStudioProjects/Schulplaner/app/src/main/java/com/example/schulplaner/HomeworkScreen.kt)**: Add "Postpone" functionality to trigger related achievements.
- **[GradesScreen.kt](file:///C:/Users/luisj/AndroidStudioProjects/Schulplaner/app/src/main/java/com/example/schulplaner/GradesScreen.kt)**: Include date in grade insertion.

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to ensure compilation.

### Manual Verification
- Verify achievement list is visible in the UI.
- Test "Beta-Tester" achievement by clicking the logo 10 times.
- Test "Prokrastinations-Profi" by clicking the postpone button 3 times.
