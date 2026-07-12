package com.neilturner.aerialviews.ui.sources

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.neilturner.aerialviews.R
import com.neilturner.aerialviews.data.preferences.PresetHelper
import com.neilturner.aerialviews.models.prefs.GeneralPrefs
import com.neilturner.aerialviews.ui.controls.MenuStateFragment

class SourcesFragment : MenuStateFragment() {
    private var presetCategory: PreferenceCategory? = null

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        setPreferencesFromResource(R.xml.sources, rootKey)
        addPresetActions()
    }

    private fun addPresetActions() {
        val context = requireContext()
        val preset = PresetHelper.getPreset(context, GeneralPrefs.activePreset) ?: return

        val category =
            PreferenceCategory(context).apply {
                title = getString(R.string.presets_current)
            }
        preferenceScreen.addPreference(category)
        presetCategory = category

        category.addPreference(
            Preference(context).apply {
                title = getString(R.string.presets_option_rename)
                setOnPreferenceClickListener {
                    renamePreset()
                    true
                }
            },
        )

        category.addPreference(
            Preference(context).apply {
                title = getString(R.string.presets_option_delete)
                setOnPreferenceClickListener {
                    confirmDeletePreset()
                    true
                }
            },
        )
    }

    private fun renamePreset() {
        val context = requireContext()
        val preset = PresetHelper.getPreset(context, GeneralPrefs.activePreset) ?: return
        PresetDialogs.promptForName(context, getString(R.string.presets_option_rename), preset.name) { name ->
            PresetHelper.renamePreset(context, preset.id, name)
            presetCategory?.title = getString(R.string.presets_current, name)
            requireActivity().title = name
        }
    }

    private fun confirmDeletePreset() {
        val context = requireContext()
        val preset = PresetHelper.getPreset(context, GeneralPrefs.activePreset) ?: return
        AlertDialog
            .Builder(context)
            .setTitle(preset.name)
            .setMessage(getString(R.string.presets_delete_confirm, preset.name))
            .setPositiveButton(R.string.button_ok) { _, _ ->
                PresetHelper.deletePreset(context, preset.id)
                val remaining = PresetHelper.getPresets(context)
                if (remaining.isNotEmpty()) {
                    PresetHelper.applyPreset(context, remaining.first().id)
                    GeneralPrefs.activePreset = remaining.first().id
                } else {
                    GeneralPrefs.activePreset = ""
                }
                parentFragmentManager.popBackStack()
            }.setNegativeButton(R.string.button_cancel, null)
            .show()
    }
}
