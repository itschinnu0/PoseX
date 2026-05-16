package com.example.posex.exercise

/**
 * A single piece of coaching feedback with an explicit priority level.
 *
 * Severity controls which cue surfaces to the user when multiple issues
 * are detected in the same frame. The prioritizer always picks the
 * highest-severity cue, regardless of the order analyzers emit them.
 *
 * CRITICAL — safety or rep-validity risk (e.g. knee caving, full collapse).
 *            Must be addressed before the movement can be considered valid.
 * WARNING  — form degradation that will cause injury or inefficiency over time.
 *            Common mid-set corrections.
 * INFO     — positional guidance or "move into frame" messages.
 *            Low urgency; contextual.
 * SUCCESS  — good form confirmation. Only shown when no CRITICAL/WARNING present.
 */
data class FormCue(
    val message: String,
    val severity: Severity
) {
    enum class Severity {
        CRITICAL,   // highest priority — shown first
        WARNING,
        INFO,
        SUCCESS     // lowest priority — only shown when nothing else is present
    }
}