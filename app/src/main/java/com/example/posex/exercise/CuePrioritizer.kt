package com.example.posex.exercise

/**
 * Selects the single most important FormCue from a list.
 *
 * Rules:
 * - Severity enum ordinal order is CRITICAL(0) > WARNING(1) > INFO(2) > SUCCESS(3).
 *   minByOrNull on ordinal gives us the highest severity.
 * - If multiple cues share the same severity, the first one emitted by the
 *   analyzer wins (stable sort). Analyzer authors control tie-breaking by
 *   emission order within a severity level only — cross-severity priority
 *   is always enforced here, never by list position.
 * - Returns null only if the list is empty (should never happen in practice
 *   since all analyzers emit at least a SUCCESS or an INFO cue).
 */
object CuePrioritizer {

    fun topCue(cues: List<FormCue>): FormCue? {
        return cues.minByOrNull { it.severity.ordinal }
    }
}