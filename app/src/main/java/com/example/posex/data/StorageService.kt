package com.example.posex.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Single access point for all app persistence.
 * Nothing else in the app touches SharedPreferences directly.
 *
 * Storage layout (all under PREFS_NAME):
 *   "sessions_SQUAT"  → JSON array of SessionRecord objects
 *   "sessions_PUSHUP" → JSON array of SessionRecord objects
 *   "sessions_PLANK"  → JSON array of SessionRecord objects
 *   "pb_SQUAT"        → JSON object for PersonalBest
 *   "pb_PUSHUP"       → JSON object for PersonalBest
 *   "pb_PLANK"        → JSON object for PersonalBest
 *
 * All decode functions are defensive: missing or malformed keys return
 * default values rather than throwing. This means old installs without
 * a field will read a safe default after an app update — no migration needed
 * as long as you only ADD fields and never rename or remove them.
 */
class StorageService(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    // ── Session storage ───────────────────────────────────────────────────

    /**
     * Persists a completed session and updates the personal best for
     * this exercise type if the session beat it.
     */
    fun saveSession(session: SessionRecord) {
        appendSession(session)
        updatePersonalBestIfNeeded(session)
    }

    /**
     * Returns all sessions for [exerciseType] sorted newest-first.
     * Returns an empty list if none exist or if decoding fails.
     */
    fun getSessions(exerciseType: String): List<SessionRecord> {
        val raw = prefs.getString(sessionKey(exerciseType), null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val sessions = mutableListOf<SessionRecord>()
            for (i in 0 until array.length()) {
                decodeSession(array.getJSONObject(i))?.let { sessions.add(it) }
            }
            sessions.sortedByDescending { it.date }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Returns all sessions across all exercise types, sorted newest-first. */
    fun getAllSessions(): List<SessionRecord> {
        return listOf("SQUAT", "PUSHUP", "PLANK")
            .flatMap { getSessions(it) }
            .sortedByDescending { it.date }
    }

    // ── Personal best storage ─────────────────────────────────────────────

    /**
     * Returns the current personal best for [exerciseType].
     * Returns null if no session has ever been saved for this exercise.
     */
    fun getPersonalBest(exerciseType: String): PersonalBest? {
        val raw = prefs.getString(pbKey(exerciseType), null) ?: return null
        return try {
            decodePersonalBest(JSONObject(raw))
        } catch (e: Exception) {
            null
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private fun appendSession(session: SessionRecord) {
        val key = sessionKey(session.exerciseType)
        val existingRaw = prefs.getString(key, null)
        val array = if (existingRaw != null) {
            try { JSONArray(existingRaw) } catch (e: Exception) { JSONArray() }
        } else {
            JSONArray()
        }
        array.put(encodeSession(session))
        prefs.edit().putString(key, array.toString()).apply()
    }

    private fun updatePersonalBestIfNeeded(session: SessionRecord) {
        val key = pbKey(session.exerciseType)
        val current = getPersonalBest(session.exerciseType)

        val newBest = if (current == null) {
            // First session ever for this exercise
            PersonalBest(
                exerciseType = session.exerciseType,
                repCount = session.repCount,
                holdSeconds = session.holdSeconds
            )
        } else {
            // Only update the relevant field for this exercise type
            PersonalBest(
                exerciseType = session.exerciseType,
                repCount = maxOf(current.repCount, session.repCount),
                holdSeconds = maxOf(current.holdSeconds, session.holdSeconds)
            )
        }

        // Only write if something actually changed
        if (newBest != current) {
            prefs.edit().putString(key, encodePersonalBest(newBest).toString()).apply()
        }
    }

    // ── Encode ────────────────────────────────────────────────────────────

    private fun encodeSession(s: SessionRecord): JSONObject = JSONObject().apply {
        put("id", s.id)
        put("exerciseType", s.exerciseType)
        put("date", s.date)
        put("repCount", s.repCount)
        put("holdSeconds", s.holdSeconds)
        put("durationMs", s.durationMs)
        put("criticalCues", s.criticalCues)
        put("warningCues", s.warningCues)
    }

    private fun encodePersonalBest(pb: PersonalBest): JSONObject = JSONObject().apply {
        put("exerciseType", pb.exerciseType)
        put("repCount", pb.repCount)
        put("holdSeconds", pb.holdSeconds)
    }

    // ── Decode (defensive — missing keys fall back to defaults) ───────────

    private fun decodeSession(obj: JSONObject): SessionRecord? {
        return try {
            SessionRecord(
                id           = obj.optString("id", UUID.randomUUID().toString()),
                exerciseType = obj.optString("exerciseType", "UNKNOWN"),
                date         = obj.optLong("date", 0L),
                repCount     = obj.optInt("repCount", 0),
                holdSeconds  = obj.optInt("holdSeconds", 0),
                durationMs   = obj.optLong("durationMs", 0L),
                criticalCues = obj.optInt("criticalCues", 0),
                warningCues  = obj.optInt("warningCues", 0)
            )
        } catch (e: Exception) {
            // Corrupt record — skip it rather than crashing
            null
        }
    }

    private fun decodePersonalBest(obj: JSONObject): PersonalBest? {
        return try {
            PersonalBest(
                exerciseType = obj.optString("exerciseType", "UNKNOWN"),
                repCount     = obj.optInt("repCount", 0),
                holdSeconds  = obj.optInt("holdSeconds", 0)
            )
        } catch (e: Exception) {
            null
        }
    }

    // ── Key builders ──────────────────────────────────────────────────────

    private fun sessionKey(exerciseType: String) = "sessions_$exerciseType"
    private fun pbKey(exerciseType: String) = "pb_$exerciseType"

    companion object {
        private const val PREFS_NAME = "posex_storage"
    }
}