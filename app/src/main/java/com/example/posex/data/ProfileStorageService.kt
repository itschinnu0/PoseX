package com.example.posex.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class ProfileStorageService(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun saveProfile(profile: UserProfile) {
        val profiles = getAllProfiles().toMutableList()
        val existingIndex = profiles.indexOfFirst { it.id == profile.id }
        if (existingIndex >= 0) {
            profiles[existingIndex] = profile
        } else {
            profiles.add(profile)
        }
        persistProfiles(profiles)
    }

    fun getAllProfiles(): List<UserProfile> {
        val raw = prefs.getString(PROFILES_KEY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val profiles = mutableListOf<UserProfile>()
            for (i in 0 until array.length()) {
                decodeProfile(array.getJSONObject(i))?.let { profiles.add(it) }
            }
            profiles
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getActiveProfile(): UserProfile? {
        val activeId = prefs.getString(ACTIVE_PROFILE_KEY, null) ?: return null
        return getAllProfiles().firstOrNull { it.id == activeId }
    }

    fun setActiveProfile(profileId: String) {
        prefs.edit().putString(ACTIVE_PROFILE_KEY, profileId).apply()
    }

    fun deleteProfile(profileId: String) {
        val updated = getAllProfiles().filterNot { it.id == profileId }
        persistProfiles(updated)

        val activeId = prefs.getString(ACTIVE_PROFILE_KEY, null)
        if (activeId == profileId) {
            prefs.edit().remove(ACTIVE_PROFILE_KEY).apply()
        }
    }

    fun updateLastActive(profileId: String) {
        val profiles = getAllProfiles().toMutableList()
        val index = profiles.indexOfFirst { it.id == profileId }
        if (index < 0) return

        val profile = profiles[index]
        profiles[index] = profile.copy(lastActiveAt = System.currentTimeMillis())
        persistProfiles(profiles)
    }

    private fun persistProfiles(profiles: List<UserProfile>) {
        val array = JSONArray()
        profiles.forEach { array.put(encodeProfile(it)) }
        prefs.edit().putString(PROFILES_KEY, array.toString()).apply()
    }

    private fun encodeProfile(profile: UserProfile): JSONObject = JSONObject().apply {
        put("id", profile.id)
        put("name", profile.name)
        put("age", profile.age)
        put("gender", profile.gender)
        put("weightKg", profile.weightKg)
        put("heightCm", profile.heightCm)
        put("avatarUri", profile.avatarUri ?: "")
        put("createdAt", profile.createdAt)
        put("lastActiveAt", profile.lastActiveAt)
    }

    private fun decodeProfile(obj: JSONObject): UserProfile? {
        return try {
            val avatar = obj.optString("avatarUri", "")
            UserProfile(
                id = obj.optString("id", ""),
                name = obj.optString("name", ""),
                age = obj.optInt("age", 0),
                gender = obj.optString("gender", ""),
                weightKg = obj.optDouble("weightKg", 0.0).toFloat(),
                heightCm = obj.optDouble("heightCm", 0.0).toFloat(),
                avatarUri = avatar.ifBlank { null },
                createdAt = obj.optLong("createdAt", 0L),
                lastActiveAt = obj.optLong("lastActiveAt", 0L)
            )
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val PREFS_NAME = "posex_profiles"
        private const val PROFILES_KEY = "profiles"
        private const val ACTIVE_PROFILE_KEY = "active_profile_id"
    }
}

