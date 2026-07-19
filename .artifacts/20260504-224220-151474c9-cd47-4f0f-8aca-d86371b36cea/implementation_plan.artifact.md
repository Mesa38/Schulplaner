# Gemini AI Integration Plan

Integrate Gemini AI into the Schulplaner app to allow users to add homework, exams, and grades via a chat interface, and to get assistance with learning and homework.

## User Review Required

> [!IMPORTANT]
> **Firebase Setup Required:** This plan uses the Firebase AI Logic SDK. The user will need to:
> 1. Create a Firebase project.
> 2. Enable the Gemini API in the Firebase console.
> 3. Add the `google-services.json` file to the app module.
> 4. Provide a Gemini API key (via Google AI Studio or Vertex AI).

## Proposed Changes

### Build Configuration

#### [libs.versions.toml](file:///C:/Users/luisj/AndroidStudioProjects/Schulplaner/gradle/libs.versions.toml)
- Add Firebase BoM and Firebase AI dependencies.
- Add `kotlinx-serialization-json` for function calling arguments.

#### [build.gradle.kts (App)](file:///C:/Users/luisj/AndroidStudioProjects/Schulplaner/app/build.gradle.kts)
- Apply Google Services plugin.
- Add Firebase and Serialization dependencies.

---

### Core Components

#### [NEW] [AiService.kt](file:///C:/Users/luisj/AndroidStudioProjects/Schulplaner/app/src/main/java/com/example/schulplaner/AiService.kt)
- Centralized service to handle Gemini API calls.
- Defines Function Declarations for database operations (`addHomework`, `addExam`, `addGrade`, `getSubjects`, etc.).
- Implements the multi-turn chat logic with function calling execution.

#### [NEW] [AiChatScreen.kt](file:///C:/Users/luisj/AndroidStudioProjects/Schulplaner/app/src/main/java/com/example/schulplaner/AiChatScreen.kt)
- Composable for the AI chat interface.
- Handles user input, displays chat history, and shows loading states.

---

### UI Integration

#### [MainActivity.kt](file:///C:/Users/luisj/AndroidStudioProjects/Schulplaner/app/src/main/java/com/example/schulplaner/MainActivity.kt)
- Add "AI Chat" tab to the navigation.
- Add `composable("ai_chat")` to the `NavHost`.

---

## Verification Plan

### Automated Tests
- N/A (UI and API dependent)

### Manual Verification
1. Open the "AI" tab.
2. Type "Füge eine Hausaufgabe für Mathe hinzu: S. 54 Nr. 1 bis morgen".
3. Verify that the AI confirms and the homework appears in the Homework list.
4. Type "Wann ist meine nächste Klausur?".
5. Verify that the AI retrieves and displays the correct exam information.
6. Ask for help: "Erklär mir das Thema meiner Bio-Klausur".
