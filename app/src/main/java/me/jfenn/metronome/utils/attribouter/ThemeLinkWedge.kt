package me.jfenn.metronome.utils.attribouter

import android.content.Context
import android.content.ContextWrapper
import android.view.ContextThemeWrapper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import me.jfenn.attribouter.wedges.LinkWedge
import me.jfenn.metronome.Metronome
import me.jfenn.metronome.R
import me.jfenn.metronome.utils.PREF_THEME
import me.jfenn.metronome.utils.PreferenceDelegate
import java.util.*

val THEMES = mapOf(
        Pair(R.string.title_theme_auto, R.style.AppTheme),
        Pair(R.string.title_theme_light, R.style.AppTheme_Light),
        Pair(R.string.title_theme_wood, R.style.AppTheme_Wood),
        Pair(R.string.title_theme_dark, R.style.AppTheme_Dark)
)

class ThemeLinkWedge : LinkWedge(
        name = "@string/title_theme",
        icon = "@drawable/ic_theme"
) {

    override fun getListener(context: Context) = View.OnClickListener {
        val ctx = ContextThemeWrapper(context, R.style.AppTheme)
        var themeIndex by PreferenceDelegate(context, PREF_THEME, 0)

        MaterialDialog(ctx).show {
            title(R.string.title_theme)
            listItemsSingleChoice(
                    items = ArrayList(THEMES.keys.map { context.getString(it) }),
                    initialSelection = themeIndex
            ) { dialog, index, text ->
                themeIndex = index
            }
            positiveButton(android.R.string.ok) {
                it.dismiss()

                (context as ContextWrapper).let { it.baseContext as AppCompatActivity }.let {
                    (context.applicationContext as Metronome).onPremium(it)
                }
            }
            negativeButton(android.R.string.cancel) {
                it.dismiss()
            }
        }
    }

}