package me.jfenn.metronome.utils

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KType

const val PREF_TICK = "tick"
const val PREF_INTERVAL = "interval"
const val PREF_EMPHASES = "emphases"
const val PREF_BOOKMARKS = "bookmarks"
const val PREF_THEME = "theme"

/**
 * Edits the calling SharedPreferences instance through the provided
 * lambda and applies changes after completion
 */
inline fun SharedPreferences.edit(block: SharedPreferences.Editor.() -> Unit) {
    val editor = edit()
    editor.block()
    editor.apply()
}

fun <T, V> Context.preference(key: String, defaultValue: V? = null, onSet: (value: V) -> Unit = {}) : PreferenceDelegate<T, V> {
    return PreferenceDelegate(this, key, defaultValue, onSet)
}

open class PreferenceDelegate<T, V>(
        private val context: Context,
        private val key: String,
        private val defaultValue: V? = null,
        private val onSet: (value: V) -> Unit = {}
) : ReadWriteProperty<T, V> {

    private fun <V> SharedPreferences.get(key: String, type: KType, defaultValue: V? = null) : V {
        return when (type.classifier) {
            Boolean::class -> getBoolean(key, defaultValue as? Boolean ?: false) as V
            String::class -> getString(key, defaultValue as? String ?: "") as V
            Int::class -> getInt(key, (defaultValue as? Int) ?: 0) as V
            Long::class -> getLong(key, defaultValue as? Long ?: 0L) as V
            Float::class -> getFloat(key, defaultValue as? Float ?: 0f) as V
            List::class, MutableList::class -> {
                val itemType = type.arguments[0].type!!
                val list = ArrayList<Any>()
                val size = getInt("${key}-length", -1)
                for (i in 0 until size) {
                    try {
                        get<Any>("${key}#${i}", itemType).also { list.add(it) }
                    } catch (e: Exception) {
                        Log.d(this@PreferenceDelegate.javaClass.name, "${key}#${i} not provided: ${e.message}")
                    }
                }

                return if (size == -1 && defaultValue != null) defaultValue else list as V;
            }
            else -> defaultValue!!
        }
    }

    private fun <V> SharedPreferences.Editor.put(key: String, type: KType, value: V) {
        when (type.classifier) {
            Boolean::class -> putBoolean(key, value as Boolean)
            String::class -> putString(key, value as String)
            Int::class -> putInt(key, value as Int)
            Long::class -> putLong(key, value as Long)
            Float::class -> putFloat(key, value as Float)
            List::class, MutableList::class -> {
                val itemType = type.arguments[0].type!!
                val list = value as List<*>

                putInt("${key}-length", list.size)
                list.forEachIndexed { index, item ->
                    put("${key}#${index}", itemType, item)
                }
            }
        }
    }

    override operator fun getValue(thisRef: T, property: KProperty<*>) : V {
        return PreferenceManager.getDefaultSharedPreferences(context).get(key, property.returnType, defaultValue)
    }

    override operator fun setValue(thisRef: T, property: KProperty<*>, value: V) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            put(key, property.returnType, value)
        }

        onSet(value)
    }

}
