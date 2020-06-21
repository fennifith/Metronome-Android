package me.jfenn.metronome.utils

import android.content.ContextWrapper
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KType

const val PREF_TICK = "tick"
const val PREF_INTERVAL = "interval"
const val PREF_EMPHASIS = "emphasis"
const val PREF_BOOKMARK = "bookmark"

/**
 * Edits the calling SharedPreferences instance through the provided
 * lambda and applies changes after completion
 */
inline fun SharedPreferences.edit(block: SharedPreferences.Editor.() -> Unit) {
    val editor = edit()
    editor.block()
    editor.apply()
}

open class PreferenceDelegate<T>(
        private val key: String,
        private val defaultValue: T? = null,
        private val onSet: (value: T) -> Unit = {}
) : ReadWriteProperty<ContextWrapper, T> {

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

    override operator fun getValue(thisRef: ContextWrapper, property: KProperty<*>) : T {
        return PreferenceManager.getDefaultSharedPreferences(thisRef).get(key, property.returnType, defaultValue)
    }

    override operator fun setValue(thisRef: ContextWrapper, property: KProperty<*>, value: T) {
        PreferenceManager.getDefaultSharedPreferences(thisRef).edit {
            put(key, property.returnType, value)
        }

        onSet(value)
    }

}
