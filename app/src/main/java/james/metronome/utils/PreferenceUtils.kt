package james.metronome.utils

import android.content.SharedPreferences

/**
 * Edits the calling SharedPreferences instance through the provided
 * lambda and applies changes after completion
 */
fun SharedPreferences.edit(block: SharedPreferences.Editor.() -> Unit) {
    val editor = edit()
    editor.block()
    editor.apply()
}