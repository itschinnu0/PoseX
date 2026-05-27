# AGENTS.md - PoseX Development Guide

## Project Overview
**PoseX** is an Android fitness app that uses real-time pose detection (via ML Kit) to analyze exercise form during workouts. The app provides visual feedback overlays and text-to-speech guidance to users performing squats, push-ups, or planks.

## Architecture Overview

### Core Data Flow
1. **Camera Input** → `PoseAnalyzer` (ML Kit integration) → `Pose` object
2. **Pose Object** → Exercise-specific analyzer (e.g., `SquatsAnalyzer`) → `ExerciseAnalysisResult`
3. **Cues** → `CuePrioritizer` → UI banner + `PoseXTtsManager` (text-to-speech)

### Key Modules
- **UI & Navigation**: `MainActivity` hosts `PoseXApp()` which switches between `HomeScreen` and `WorkoutScreen` based on selected exercise.
- **Session State**: `WorkoutSession` + `WorkoutState` handle countdown, pause/resume, rep tracking, and cue counts.
- **Pose Readiness**: `PoseReadinessChecker` validates the user is in frame before countdown starts.
- **Camera Pipeline**: `CameraPreview` configures CameraX front camera + `ImageAnalysis` analyzer and streams frames to `PoseAnalyzer`.
- **Pose Rendering**: `PoseOverlay` draws skeleton + landmarks with front-camera mirroring and rotated frame scaling.
- **Exercise Logic**: `SquatsAnalyzer`, `PushupAnalyzer`, `PlankAnalyzer` compute metrics + feedback and update `CalibratingRepCounter` or `ActiveHoldTimer`.
- **Feedback Output**: `PoseXTtsManager` throttles speech and respects workout state.
- **Persistence**: `StorageService` stores `SessionRecord` + `PersonalBest` in SharedPreferences.

### Key Architectural Decisions

**Exercise Analyzers as Objects** (`package exercise/`):
- `SquatsAnalyzer`, `PushupAnalyzer`, `PlankAnalyzer` are **singleton objects**, not classes
- Each implements `analyze(pose: Pose): ExerciseAnalysisResult`
- **Why objects**: the analyzers keep per-exercise state (baseline, timers) without needing DI
- `WorkoutScreen` uses `CuePrioritizer.topCue(cues)` to surface the highest severity cue

**Pose Landmark Confidence Filtering**:
- All analyzers enforce `MIN_CONFIDENCE = 0.5f` to validate pose landmarks
- This prevents feedback on poor/ambiguous detections
- Missing or low-confidence landmarks trigger exercise-specific "Move into frame" messages
- Safe-screen aborts reset state: `repCounter.abortCurrentRep()` (squat/pushup) and `timer.forcePause()` (plank)

**Front Camera Coordinate Transformation** (`PoseOverlay.kt`):
- CameraX in portrait delivers rotated frames (width ↔ height swap)
- X-axis is mirrored for front-facing camera
- **Critical for overlay accuracy**: landmarks must be scaled and flipped correctly using the incoming image dimensions

## Adding New Exercises

**Required Steps**:
1. Add new enum variant to `ExerciseType.kt`
2. Create new analyzer object following the pattern:
   ```kotlin
   object NewExerciseAnalyzer {
       private const val MIN_CONFIDENCE = 0.5f
       fun analyze(pose: Pose): ExerciseAnalysisResult { /* return result */ }
   }
   ```
3. Add case in `WorkoutScreen.handlePoseFrame()`
4. Add exercise card to `HomeScreen.kt`
5. Ensure rep/hold reset is included in `WorkoutScreen.resetAnalyzers()`

**Common Pose Landmarks** (from `com.google.mlkit.vision.pose.PoseLandmark`):
- Shoulders/Hips/Elbows/Wrists/Knees/Ankles (left/right pairs)
- Access via `pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)` or similar
- Check `.inFrameLikelihood` before using; access position via `.position.x/.position.y`

## Angle Calculation Pattern

**Shared helper** in `PoseUtils`:
```kotlin
fun calculateAngle(first: PoseLandmark, mid: PoseLandmark, last: PoseLandmark): Double
```
Uses vector dot product to compute angle at `mid` point and is reused by all analyzers.

## Build & Test Workflow

**Gradle Configuration** (`app/build.gradle.kts`):
- Target SDK: 36, Min SDK: 24 (compileSdk uses release 36.1)
- Java 11 compatibility
- Compose enabled with Material3
- Key dependencies: CameraX, ML Kit Pose Detection (standard + accurate)

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
- Displayed in banner at screen bottom (WorkoutScreen)
- Color changes based on cue severity (success = green, warning = amber, critical = red)
- Cue selection uses `CuePrioritizer` (CRITICAL > WARNING > INFO > SUCCESS)
- Text-to-speech is handled by `PoseXTtsManager` and respects cooldowns and workout state

**Exercise Analysis Result**:
- `ExerciseAnalysisResult` bundles `cues`, `repCount`, `metricValue`, and `holdDurationSeconds`
- `exerciseCompleted` ends the workout immediately (e.g., plank knees down)
- Plank uses `holdDurationSeconds` while squat/pushup use `repCount`

## Performance Considerations

**Pose Detection Streaming**:
- `PoseAnalyzer` runs in **STREAM_MODE** (continuous, fast) not accuracy mode
- Throttled to one ML Kit inference every ~100ms to reduce thermal load
- Callbacks include proper cleanup: `imageProxy.close()` on completion

**TextToSpeech Throttling**:
- `PoseXTtsManager` enforces global and same-message cooldowns
- Also halts speech when workout is not in `WorkoutState.Active`

## Dependencies & External Integration

**ML Kit Pose Detection**:
- Version: 18.0.0-beta5 (note: beta version)
- Two variants available: standard & accurate (both included)
- Requires Google Play Services installed on device

**AndroidX Camera**:
- Used for real-time camera frame capture and analysis
- Lifecycle-aware; properly disposed in composables

**Text-to-Speech**:
- Android built-in; wrapped by `PoseXTtsManager` in `com.example.posex.feedback`
- Must be shut down explicitly (composable `DisposableEffect` in `WorkoutScreen`)

## Testing Notes

- Test instrumentation runner: `androidx.test.runner.AndroidJUnitRunner`
- UI tests use Compose testing framework (espresso + compose-ui-test)
- Unit tests include `RepCounterTest` and `PushupAnalyzerTest`
- Exercise analyzers would still benefit from broader unit test coverage

## Common Pitfalls & Gotchas

1. **Pose landmark may be null** even with high confidence; always check before dereferencing
2. **Screen coordinates ≠ image coordinates**: overlay applies rotation & scale transforms
3. **Cue priority**: `CuePrioritizer` always selects highest severity, not first emitted
4. **Front camera mirroring**: X-axis flip required for correct visual overlay
5. **ML Kit beta version**: breaking changes possible in future updates; monitor release notes

## Git History (Recent)

- e027194: Integrate graphify knowledge graph and configure Gemini workspace
- ff563dd: Refine plank analysis logic and improve type safety
- f58bba1: Refactor exercise analysis logic and implement plank hold tracking
- 97c7560: Unify rep counting and improve workout UI
- 05bd428: Implement squat rep counting and enhance feedback
