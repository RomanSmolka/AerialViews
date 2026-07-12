package com.neilturner.aerialviews.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.UUID

@Serializable
data class Preset(
    val id: String,
    val name: String,
    val booleans: Map<String, Boolean> = emptyMap(),
    val strings: Map<String, String> = emptyMap(),
    val stringSets: Map<String, Set<String>> = emptyMap(),
)

/**
 * Stores named snapshots ("presets") of all media source settings and
 * can re-apply them to the live preferences at runtime.
 */
object PresetHelper {
    private const val PRESET_STORE_NAME = "presets"
    private const val PRESETS_KEY = "presets"

    private val json = Json { ignoreUnknownKeys = true }

    // Key prefixes of all media source (provider) preferences
    private val providerKeyPrefixes =
        listOf(
            "apple_videos_",
            "amazon_videos_",
            "comm1_videos_",
            "comm2_videos_",
            "local_videos_",
            "local_media_",
            "samba_videos", // covers samba_videos_ and samba_videos2_
            "samba_media_", // media selection + Wake-on-LAN
            "webdav_media", // covers webdav_media_ and webdav_media2_
            "immich_media_",
            "ncmemories_media_",
            "custom_media_",
        )

    private fun isProviderKey(key: String): Boolean = providerKeyPrefixes.any { key.startsWith(it) }

    private fun presetStore(context: Context): SharedPreferences =
        context.getSharedPreferences(PRESET_STORE_NAME, Context.MODE_PRIVATE)

    private fun livePrefs(context: Context): SharedPreferences =
        context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)

    fun getPresets(context: Context): List<Preset> {
        val data = presetStore(context).getString(PRESETS_KEY, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<Preset>>(data)
        } catch (ex: Exception) {
            Timber.e(ex, "Failed to parse presets")
            emptyList()
        }
    }

    fun getPreset(
        context: Context,
        id: String,
    ): Preset? = getPresets(context).firstOrNull { it.id == id }

    private fun storePresets(
        context: Context,
        presets: List<Preset>,
    ) {
        presetStore(context)
            .edit()
            .putString(PRESETS_KEY, json.encodeToString(presets))
            .apply()
    }

    /** Captures the current media source settings as a new named preset. */
    fun addPreset(
        context: Context,
        name: String,
    ): Preset {
        val preset = captureSnapshot(context, UUID.randomUUID().toString(), name)
        storePresets(context, getPresets(context) + preset)
        return preset
    }

    /**
     * Creates a new preset using the app's default media source settings.
     * An empty snapshot means applying it clears all provider keys, so they
     * fall back to their defaults.
     */
    fun addDefaultPreset(
        context: Context,
        name: String,
    ): Preset {
        val preset = Preset(id = UUID.randomUUID().toString(), name = name)
        storePresets(context, getPresets(context) + preset)
        return preset
    }

    /** Re-captures the current media source settings into an existing preset. */
    fun updatePreset(
        context: Context,
        id: String,
    ) {
        val presets = getPresets(context)
        val existing = presets.firstOrNull { it.id == id } ?: return
        val updated = captureSnapshot(context, id, existing.name)
        storePresets(context, presets.map { if (it.id == id) updated else it })
    }

    fun renamePreset(
        context: Context,
        id: String,
        newName: String,
    ) {
        val presets = getPresets(context)
        storePresets(context, presets.map { if (it.id == id) it.copy(name = newName) else it })
    }

    fun deletePreset(
        context: Context,
        id: String,
    ) {
        storePresets(context, getPresets(context).filterNot { it.id == id })
    }

    /**
     * Applies a preset's snapshot to the live preferences. All provider keys not
     * present in the snapshot are removed so they fall back to their defaults.
     */
    fun applyPreset(
        context: Context,
        id: String,
    ): Preset? {
        val preset = getPreset(context, id) ?: return null
        val prefs = livePrefs(context)
        val editor = prefs.edit()

        prefs.all.keys
            .filter { isProviderKey(it) }
            .forEach { editor.remove(it) }

        preset.booleans.forEach { (key, value) -> editor.putBoolean(key, value) }
        preset.strings.forEach { (key, value) -> editor.putString(key, value) }
        preset.stringSets.forEach { (key, value) -> editor.putStringSet(key, value) }

        editor.apply()
        Timber.i("Applied preset '${preset.name}'")
        return preset
    }

    private fun captureSnapshot(
        context: Context,
        id: String,
        name: String,
    ): Preset {
        val booleans = mutableMapOf<String, Boolean>()
        val strings = mutableMapOf<String, String>()
        val stringSets = mutableMapOf<String, Set<String>>()

        livePrefs(context)
            .all
            .filterKeys { isProviderKey(it) }
            .forEach { (key, value) ->
                when (value) {
                    is Boolean -> booleans[key] = value
                    is String -> strings[key] = value
                    is Set<*> -> stringSets[key] = value.filterIsInstance<String>().toSet()
                    else -> Timber.w("Skipping unsupported preference type for key $key")
                }
            }

        return Preset(
            id = id,
            name = name,
            booleans = booleans,
            strings = strings,
            stringSets = stringSets,
        )
    }
}
