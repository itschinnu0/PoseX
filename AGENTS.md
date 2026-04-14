# AGENTS.md - PoseX Development Guide

## Project Overview
**PoseX** is an Android fitness app that uses real-time pose detection (via ML Kit) to analyze exercise form during workouts. The app provides visual feedback overlays and text-to-speech guidance to users performing squats, push-ups, or planks.

## Architecture Overview

### Core Data Flow
1. **Camera Input** → `PoseAnalyzer` (ML Kit integration) → `Pose` object
2. **Pose Object** → Exercise-specific analyzer (e.g., `SquatsAnalyzer`) → Feedback list
3. **Feedback** → `FeedbackEngine` (text-to-speech) + UI (visual overlay)

### Key Architectural Decisions

**Exercise Analyzers as Objects** (`package exercise/`):
- `SquatsAnalyzer`, `PushupAnalyzer`, `PlankAnalyzer` are **singleton objects**, not classes
- Each implements identical `analyze(pose: Pose): List<String>` contract
- **Why objects**: Single responsibility, stateless, reusable without instantiation
- Returns multiple feedback strings; **only the first one is shown** to the user (see `WorkoutScreen` line 84)

**Pose Landmark Confidence Filtering**:
- All analyzers enforce `MIN_CONFIDENCE = 0.5f` to validate pose landmarks
- This prevents feedback on poor/ambiguous detections
- Missing or low-confidence landmarks trigger generic "Move into frame" message

**Front Camera Coordinate Transformation** (`PoseOverlay.kt` lines 24-34):
- CameraX in portrait delivers rotated frames (width ↔ height swap)
- X-axis is mirrored for front-facing camera
- **Critical for overlay accuracy**: landmarks must be scaled and flipped correctly

## Adding New Exercises

**Required Steps**:
1. Add new enum variant to `ExerciseType.kt`
2. Create new analyzer object following the pattern:
   ```kotlin
   object NewExerciseAnalyzer {
       private const val MIN_CONFIDENCE = 0.5f
       fun analyze(pose: Pose): List<String> { /* return feedback list */ }
   }
   ```
3. Add case in `WorkoutScreen.processPose()` (line 77-81)
4. Add exercise card to `HomeScreen.kt` (line 24-28)

**Common Pose Landmarks** (from `com.google.mlkit.vision.pose.PoseLandmark`):
- Shoulders/Hips/Elbows/Wrists/Knees/Ankles (left/right pairs)
- Access via `pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)` or similar
- Check `.inFrameLikelihood` before using; access position via `.position.x/.position.y`

## Angle Calculation Pattern

**Repeated in all exercise analyzers** (duplicated helper methods):
```kotlin
private fun calculateAngle(first: PoseLandmark, mid: PoseLandmark, last: PoseLandmark): Double
```
Uses vector dot product to compute angle at `mid` point. **Future refactoring**: move to shared utility class.

## Build & Test Workflow

**Gradle Configuration** (`app/build.gradle.kts`):
- Target SDK: 36, Min SDK: 24
- Java 11 compatibility
- Compose enabled with Material3
- Key dependencies: CameraX 1.4.2, ML Kit Pose Detection 18.0.0-beta5

**Build Command**:
```bash
./gradlew build      # Full build
./gradlew assembleDebug  # Debug APK
./gradlew testDebug  # Run tests
```

**Camera Permission**:
- Required at runtime; app requests in `WorkoutScreen` via ActivityResultContracts
- Manifest declares `CAMERA` permission + front/back camera features

## UI Patterns & Conventions

**Compose Navigation** (state-based, no NavHost):
- `MainActivity` → `PoseXApp()` composable
- Navigation via `selectedExercise` state in `PoseXApp`
- `null` state shows `HomeScreen`, non-null shows `WorkoutScreen`

**Color Scheme** (dark theme with cyan accents):
- Primary accent: `0xFF00E5FF` (cyan)
- Background: `0xFF0A0F1E` (dark blue-black)
- Success feedback: `0xFF00E676` (green)
- Error feedback: `0xFFFF5252` (red)
- Used consistently in HomeScreen, WorkoutScreen, PoseOverlay

**Feedback System**:
- Displayed in banner at screen bottom (WorkoutScreen line 143-162)
- Color changes based on feedback type (good form = green, error = red)
- Text-to-speech triggered on non-success feedback with 4-second cooldown (FeedbackEngine line 12)

## Performance Considerations

**Pose Detection Streaming**:
- `PoseAnalyzer` runs in **STREAM_MODE** (continuous, fast) not accuracy mode
- Processes every frame; resource-intensive operation
- Callbacks include proper cleanup: `imageProxy.close()` on completion

**TextToSpeech Cooldown**:
- `FeedbackEngine` enforces 4-second minimum between spoken messages (line 12)
- Prevents audio spam during form corrections

## Dependencies & External Integration

**ML Kit Pose Detection**:
- Version: 18.0.0-beta5 (note: beta version)
- Two variants available: standard & accurate (both included)
- Requires Google Play Services installed on device

**AndroidX Camera**:
- Used for real-time camera frame capture and analysis
- Lifecycle-aware; properly disposed in composables

**Text-to-Speech**:
- Android built-in; initialized in FeedbackEngine with Locale.US
- Must be shut down explicitly (composable DisposableEffect in WorkoutScreen line 71-75)

## Testing Notes

- Test instrumentation runner: `androidx.test.runner.AndroidJUnitRunner`
- UI tests use Compose testing framework (espresso + compose-ui-test)
- Currently minimal test coverage; exercise analyzers would benefit from unit tests

## Common Pitfalls & Gotchas

1. **Pose landmark may be null** even with high confidence; always check before dereferencing
2. **Screen coordinates ≠ image coordinates**: overlay applies rotation & scale transforms
3. **First feedback only shown**: if multiple issues exist, only primary message reaches user
4. **Front camera mirroring**: X-axis flip required for correct visual overlay
5. **ML Kit beta version**: breaking changes possible in future updates; monitor release notes

