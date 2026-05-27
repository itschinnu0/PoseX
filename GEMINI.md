## graphify

This project has a graphify knowledge graph at graphify-out/.

Rules:
- Before answering architecture or codebase questions, read graphify-out/GRAPH_REPORT.md for god nodes and community structure
- If graphify-out/wiki/index.md exists, navigate it instead of reading raw files
- After modifying code files in this session, run `graphify update .` to keep the graph current (AST-only, no API cost)

## Project Structure & Key Components

### Core Application
- **[MainActivity.kt](file:///C:/Users/Chinnu0/Desktop/PoseX/app/src/main/java/com/example/posex/MainActivity.kt)**: Entry point of the app. Hosts `PoseXApp` which manages top-level navigation and theme.

### Exercise Analysis (`exercise` package)
- **[SquatsAnalyzer.kt](file:///C:/Users/Chinnu0/Desktop/PoseX/app/src/main/java/com/example/posex/exercise/SquatsAnalyzer.kt)**: Logic for squat detection, depth validation, and leg-raise filtering.
- **[PushupAnalyzer.kt](file:///C:/Users/Chinnu0/Desktop/PoseX/app/src/main/java/com/example/posex/exercise/PushupAnalyzer.kt)**: Logic for push-up rep counting and body alignment (plank line) feedback.
- **[PlankAnalyzer.kt](file:///C:/Users/Chinnu0/Desktop/PoseX/app/src/main/java/com/example/posex/exercise/PlankAnalyzer.kt)**: Logic for plank hold duration tracking and torso alignment validation.
- **[WorkoutSession.kt](file:///C:/Users/Chinnu0/Desktop/PoseX/app/src/main/java/com/example/posex/exercise/WorkoutSession.kt)**: Orchestrates workout lifecycle (Countdown -> Active -> Paused -> Finished).
- **[CalibratingRepCounter.kt](file:///C:/Users/Chinnu0/Desktop/PoseX/app/src/main/java/com/example/posex/exercise/CalibratingRepCounter.kt)**: Base class for rep counting with dynamic baseline calibration.
- **[CuePrioritizer.kt](file:///C:/Users/Chinnu0/Desktop/PoseX/app/src/main/java/com/example/posex/exercise/CuePrioritizer.kt)**: Filters and prioritizes form correction feedback to avoid overwhelming the user.

### Pose Detection (`pose` package)
- **[PoseAnalyzer.kt](file:///C:/Users/Chinnu0/Desktop/PoseX/app/src/main/java/com/example/posex/pose/PoseAnalyzer.kt)**: Wraps Google ML Kit Pose Detection. Processes `ImageProxy` frames and throttles detection based on `processIntervalMs`.

### User Interface (`ui` package)
- **[HomeScreen.kt](file:///C:/Users/Chinnu0/Desktop/PoseX/app/src/main/java/com/example/posex/ui/screens/HomeScreen.kt)**: Dashboard for selecting exercises and viewing Personal Bests.
- **[WorkoutScreen.kt](file:///C:/Users/Chinnu0/Desktop/PoseX/app/src/main/java/com/example/posex/ui/screens/WorkoutScreen.kt)**: Active workout UI featuring camera preview, real-time metrics, and TTS feedback integration.
- **[CameraPreview.kt](file:///C:/Users/Chinnu0/Desktop/PoseX/app/src/main/java/com/example/posex/ui/components/CameraPreview.kt)**: Configures CameraX `Preview` and binds the `PoseAnalyzer` to the `ImageAnalysis` use case.
- **[PoseOverlay.kt](file:///C:/Users/Chinnu0/Desktop/PoseX/app/src/main/java/com/example/posex/ui/components/PoseOverlay.kt)**: Custom Canvas drawing for the skeleton skeleton with mirroring for front camera.

### Feedback & Data (`feedback` & `data` packages)
- **[PoseXTtsManager.kt](file:///C:/Users/Chinnu0/Desktop/PoseX/app/src/main/java/com/example/posex/feedback/PoseXTtsManager.kt)**: Manages Text-to-Speech feedback with frequency limiting and workout state awareness.
- **[StorageService.kt](file:///C:/Users/Chinnu0/Desktop/PoseX/app/src/main/java/com/example/posex/data/StorageService.kt)**: Handles persistence of `SessionRecord` and `PersonalBest` using JSON serialization in `SharedPreferences`.
