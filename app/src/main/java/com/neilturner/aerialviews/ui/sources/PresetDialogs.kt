package com.neilturner.aerialviews.ui.sources

import android.content.Context
import android.text.InputType
import android.util.TypedValue
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import com.neilturner.aerialviews.R

internal object PresetDialogs {
    fun promptForName(
        context: Context,
        title: String,
        initialName: String = "",
        onConfirm: (name: String) -> Unit,
    ) {
        val editText =
            EditText(context).apply {
                inputType = InputType.TYPE_CLASS_TEXT
                hint = context.getString(R.string.presets_name_hint)
                setText(initialName)
                setSelection(initialName.length)
            }

        val padding =
            TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, context.resources.displayMetrics)
                .toInt()
        val container =
            FrameLayout(context).apply {
                setPadding(padding, padding, padding, padding)
                addView(editText)
            }

        val dialog =
            AlertDialog
                .Builder(context)
                .setTitle(title)
                .setView(container)
                .setPositiveButton(R.string.button_ok) { _, _ ->
                    val name = editText.text.toString().trim()
                    if (name.isNotEmpty()) onConfirm(name)
                }.setNegativeButton(R.string.button_cancel, null)
                .create()

        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = initialName.isNotBlank()
        editText.doAfterTextChanged {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = !it.isNullOrBlank()
        }
        editText.requestFocus()
    }
}
