package com.neilturner.aerialviews.ui.sources

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.neilturner.aerialviews.R
import com.neilturner.aerialviews.data.preferences.Preset
import com.neilturner.aerialviews.data.preferences.PresetHelper
import com.neilturner.aerialviews.models.prefs.GeneralPrefs
import com.neilturner.aerialviews.ui.controls.MenuStateFragment

class PresetsFragment : MenuStateFragment() {
    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        preferenceScreen = preferenceManager.createPreferenceScreen(requireContext())
        rebuildPreferences()
    }

    override fun onResume() {
        super.onResume()
        // Persist any source edits made while a preset was active,
        // e.g. when returning from the sources editor
        val context = requireContext()
        val activeId = GeneralPrefs.activePreset
        if (activeId.isNotEmpty() && PresetHelper.getPreset(context, activeId) != null) {
            PresetHelper.updatePreset(context, activeId)
        }
        rebuildPreferences()
    }

    private fun rebuildPreferences() {
        val context = requireContext()
        val screen = preferenceScreen
        screen.removeAll()

        // Make sure there is always at least one preset to edit
        var presets = PresetHelper.getPresets(context)
        if (presets.isEmpty()) {
            val default = PresetHelper.addPreset(context, getString(R.string.presets_default_name))
            GeneralPrefs.activePreset = default.id
            presets = listOf(default)
        }

        val category =
            PreferenceCategory(context).apply {
                title = getString(R.string.category_presets)
            }
        screen.addPreference(category)

        presets.forEach { preset ->
            category.addPreference(
                Preference(context).apply {
                    title = preset.name
                    summary = getString(R.string.presets_tap_to_edit)
                    setOnPreferenceClickListener {
                        openPreset(preset)
                        true
                    }
                },
            )
        }

        val addCategory = PreferenceCategory(context)
        screen.addPreference(addCategory)

        addCategory.addPreference(
            Preference(context).apply {
                title = getString(R.string.presets_add)
                summary = getString(R.string.presets_add_summary)
                setOnPreferenceClickListener {
                    PresetDialogs.promptForName(context, getString(R.string.presets_add)) { name ->
                        val preset = PresetHelper.addDefaultPreset(context, name)
                        openPreset(preset)
                    }
                    true
                }
            },
        )
    }

    private fun openPreset(preset: Preset) {
        val context = requireContext()
        PresetHelper.applyPreset(context, preset.id)
        GeneralPrefs.activePreset = preset.id

        val preference =
            Preference(context).apply {
                title = preset.name
                fragment = SourcesFragment::class.java.name
            }
        (requireActivity() as? PreferenceFragmentCompat.OnPreferenceStartFragmentCallback)
            ?.onPreferenceStartFragment(this, preference)
    }
}
