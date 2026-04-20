# Graph Report - C:\Users\Chinnu0\Desktop\PoseX  (2026-04-20)

## Corpus Check
- 23 files · ~23,850 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 68 nodes · 45 edges · 23 communities detected
- Extraction: 100% EXTRACTED · 0% INFERRED · 0% AMBIGUOUS
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Community 0|Community 0]]
- [[_COMMUNITY_Community 1|Community 1]]
- [[_COMMUNITY_Community 2|Community 2]]
- [[_COMMUNITY_Community 3|Community 3]]
- [[_COMMUNITY_Community 4|Community 4]]
- [[_COMMUNITY_Community 5|Community 5]]
- [[_COMMUNITY_Community 6|Community 6]]
- [[_COMMUNITY_Community 7|Community 7]]
- [[_COMMUNITY_Community 8|Community 8]]
- [[_COMMUNITY_Community 9|Community 9]]
- [[_COMMUNITY_Community 10|Community 10]]
- [[_COMMUNITY_Community 11|Community 11]]
- [[_COMMUNITY_Community 12|Community 12]]
- [[_COMMUNITY_Community 13|Community 13]]
- [[_COMMUNITY_Community 14|Community 14]]
- [[_COMMUNITY_Community 15|Community 15]]
- [[_COMMUNITY_Community 16|Community 16]]
- [[_COMMUNITY_Community 17|Community 17]]
- [[_COMMUNITY_Community 18|Community 18]]
- [[_COMMUNITY_Community 19|Community 19]]
- [[_COMMUNITY_Community 20|Community 20]]
- [[_COMMUNITY_Community 21|Community 21]]
- [[_COMMUNITY_Community 22|Community 22]]

## God Nodes (most connected - your core abstractions)
1. `RepCounterTest` - 8 edges
2. `RepCounter` - 4 edges
3. `PlankAnalyzer` - 3 edges
4. `PushupAnalyzer` - 3 edges
5. `SquatsAnalyzer` - 3 edges
6. `FeedbackEngine` - 3 edges
7. `PoseAnalyzer` - 3 edges
8. `ExampleInstrumentedTest` - 2 edges
9. `MainActivity` - 2 edges
10. `PoseUtils` - 2 edges

## Surprising Connections (you probably didn't know these)
- None detected - all connections are within the same source files.

## Communities

### Community 0 - "Community 0"
Cohesion: 0.22
Nodes (1): RepCounterTest

### Community 1 - "Community 1"
Cohesion: 0.4
Nodes (1): RepCounter

### Community 2 - "Community 2"
Cohesion: 0.5
Nodes (1): MainActivity

### Community 3 - "Community 3"
Cohesion: 0.5
Nodes (1): PlankAnalyzer

### Community 4 - "Community 4"
Cohesion: 0.5
Nodes (1): PushupAnalyzer

### Community 5 - "Community 5"
Cohesion: 0.5
Nodes (1): SquatsAnalyzer

### Community 6 - "Community 6"
Cohesion: 0.5
Nodes (1): FeedbackEngine

### Community 7 - "Community 7"
Cohesion: 0.5
Nodes (1): PoseAnalyzer

### Community 8 - "Community 8"
Cohesion: 0.67
Nodes (1): ExampleInstrumentedTest

### Community 9 - "Community 9"
Cohesion: 0.67
Nodes (1): PoseUtils

### Community 10 - "Community 10"
Cohesion: 0.67
Nodes (0): 

### Community 11 - "Community 11"
Cohesion: 0.67
Nodes (0): 

### Community 12 - "Community 12"
Cohesion: 0.67
Nodes (1): ExampleUnitTest

### Community 13 - "Community 13"
Cohesion: 1.0
Nodes (1): ExerciseAnalysisResult

### Community 14 - "Community 14"
Cohesion: 1.0
Nodes (1): ExerciseType

### Community 15 - "Community 15"
Cohesion: 1.0
Nodes (0): 

### Community 16 - "Community 16"
Cohesion: 1.0
Nodes (0): 

### Community 17 - "Community 17"
Cohesion: 1.0
Nodes (0): 

### Community 18 - "Community 18"
Cohesion: 1.0
Nodes (0): 

### Community 19 - "Community 19"
Cohesion: 1.0
Nodes (0): 

### Community 20 - "Community 20"
Cohesion: 1.0
Nodes (0): 

### Community 21 - "Community 21"
Cohesion: 1.0
Nodes (0): 

### Community 22 - "Community 22"
Cohesion: 1.0
Nodes (0): 

## Knowledge Gaps
- **2 isolated node(s):** `ExerciseAnalysisResult`, `ExerciseType`
  These have ≤1 connection - possible missing edges or undocumented components.
- **Thin community `Community 13`** (2 nodes): `ExerciseAnalysisResult.kt`, `ExerciseAnalysisResult`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 14`** (2 nodes): `ExerciseType.kt`, `ExerciseType`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 15`** (2 nodes): `PoseOverlay.kt`, `PoseOverlay()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 16`** (2 nodes): `WorkoutScreen.kt`, `WorkoutScreen()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 17`** (2 nodes): `Theme.kt`, `PoseXTheme()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 18`** (1 nodes): `build.gradle.kts`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 19`** (1 nodes): `settings.gradle.kts`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 20`** (1 nodes): `build.gradle.kts`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 21`** (1 nodes): `Color.kt`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 22`** (1 nodes): `Type.kt`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What connects `ExerciseAnalysisResult`, `ExerciseType` to the rest of the system?**
  _2 weakly-connected nodes found - possible documentation gaps or missing edges._