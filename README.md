# PoseX - The AI-Based Fitness Companion

**PoseX** is a cutting-edge Android application designed to revolutionize home workouts through real-time Artificial Intelligence. By leveraging Google ML Kit and CameraX, PoseX acts as a digital personal trainer, analyzing your form, counting your reps, and providing instant feedback to ensure you get the most out of every movement.

## 🚀 Key Features

### 1. Real-Time Pose Analysis
*   **Dynamic Skeleton Overlay**: Visualize your form with a real-time skeleton projected over your camera feed.
*   **Multi-Exercise Support**: Specialized logic for **Squats**, **Push-ups**, **Planks**, and **Bicep Curls**.
*   **High Precision**: Monitors limb angles and joint alignment with a high-frequency analysis loop (~10fps).

### 2. Intelligent Form Feedback
*   **Visual Cues**: Real-time banners at the bottom of the screen guide your movements (e.g., "Go Lower", "Fix Your Back").
*   **Text-to-Speech (TTS) Guidance**: Stay focused on your workout with audio cues that keep you in the correct posture without looking at the screen.
*   **Cue Prioritization**: An intelligent ranking system ensures you hear the most critical form corrections first.

### 3. Automated Tracking
*   **Smart Rep Counting**: Automatically detects and counts repetitions using dynamic baseline calibration to adapt to different body types and camera angles.
*   **Hold Timer**: Precisely tracks duration for isometric exercises like Planks.
*   **Target Goal Setting**: Configure your workout goals (reps or seconds) before you start.

### 4. Progress & Personalization
*   **Multiple User Profiles**: Create and manage unique profiles with personalized height and weight tracking.
*   **Personal Bests**: Compete with your past self with tracked records for every exercise type.
*   **Session History & Stats**: Review detailed reports of your past workouts, including a "Form Score" that evaluates the quality of your repetitions.

---

## 🛠️ How It Works

PoseX combines high-performance libraries with custom-built biomechanical logic to provide a seamless training experience.

1.  **Vision Pipeline**: The app uses **CameraX** to stream live frames to the **ML Kit Pose Detection** engine.
2.  **Biometric Analysis**: Custom algorithms calculate joint angles using vector dot products (e.g., knee angle for squats, torso alignment for planks, elbow flexion for curls).
3.  **State Management**: A robust `WorkoutSession` state machine handles the transition from **Countdown** to **Active** and finally to **Summary**.
4.  **Baseline Calibration**: The system "learns" your standing or starting position at the beginning of each session to ensure accurate depth and movement detection.

---

## ✨ Why PoseX?

### ✅ Privacy First
All AI processing happens **locally on your device**. Your camera feed is never uploaded to the cloud, ensuring total privacy during your home workouts.

### ✅ Smart Prioritization
Unlike other apps that might overwhelm you with feedback, PoseX uses a **CuePrioritizer**. It filters and ranks information so you only hear the most important corrections at the right time.

### ✅ Accurate Calibration
The "Calibrating Rep Counter" dynamically adjusts to your range of motion. Whether you have deep squats or are performing a full bicep curl, the app adapts its "bottom" and "top" thresholds to match your current ability.

### ✅ Clean & Modern UI
Designed with a sleek **Dark Theme** and high-contrast cyan accents, the UI is optimized for visibility even when your phone is propped up across the room.

---

## 👨‍💻 Developers
**Vighnajit CHM & Sarabjot Singh**

---

> "Perfect form is the foundation of strength. PoseX helps you build it."
