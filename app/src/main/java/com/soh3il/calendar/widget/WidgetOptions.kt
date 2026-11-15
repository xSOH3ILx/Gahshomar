package com.soh3il.calendar.widget

import android.content.Context
import com.soh3il.core.jalali.JalaliDate
import org.json.JSONObject

enum class WidgetTheme { SYSTEM, LIGHT, DARK }
enum class WidgetAccent { BLUE, GREEN, PURPLE }

data class WidgetOptions(
    val theme: WidgetTheme = WidgetTheme.SYSTEM,
    val dynamicColors: Boolean = false,
    val accent: WidgetAccent = WidgetAccent.BLUE,
    val opacity: Float = 1f,
    val textScale: Float = 1f,
    val showGregorian: Boolean = true,
    val showHijri: Boolean = true,
    val showOccasions: Boolean = true,
    val showReminders: Boolean = true,
    val hideTitles: Boolean = false,
    val monthJdn: Long? = null
) {
    init {
        require(opacity in 0.25f..1f && textScale in 0.85f..1.3f) { "Invalid widget appearance" }
        require(monthJdn == null || monthJdn in JalaliDate(1, 1, 1).toJdn()..JalaliDate(9999, 12, JalaliDate.daysInMonth(9999, 12)).toJdn()) { "Invalid widget month" }
    }
}

/** Each installed instance owns one entry; missing entries use the documented defaults. */
object WidgetOptionsStore {
    private fun prefs(context: Context) = context.getSharedPreferences("soh3il_widgets", Context.MODE_PRIVATE)
    private fun key(id: Int) = "widget_$id"

    fun read(context: Context, id: Int): WidgetOptions {
        val raw = prefs(context).getString(key(id), null) ?: return WidgetOptions()
        return decode(raw)
    }

    fun decode(raw: String): WidgetOptions {
        val json = JSONObject(raw)
        require(json.getInt("version") == 1) { "Unsupported widget settings version" }
        return WidgetOptions(
            theme = WidgetTheme.valueOf(json.getString("theme")),
            dynamicColors = json.getBoolean("dynamic"),
            accent = WidgetAccent.valueOf(json.getString("accent")),
            opacity = json.getDouble("opacity").toFloat(), textScale = json.getDouble("scale").toFloat(),
            showGregorian = json.getBoolean("gregorian"), showHijri = json.getBoolean("hijri"),
            showOccasions = json.getBoolean("occasions"), showReminders = json.getBoolean("reminders"),
            hideTitles = json.getBoolean("hideTitles"),
            monthJdn = if (json.isNull("month")) null else json.getLong("month")
        )
    }

    fun save(context: Context, id: Int, options: WidgetOptions) {
        require(id > 0) { "Invalid widget ID" }
        check(prefs(context).edit().putString(key(id), encode(options)).commit()) { "Widget settings could not be saved" }
    }

    fun encode(options: WidgetOptions): String = JSONObject().apply {
            put("version", 1); put("theme", options.theme.name); put("dynamic", options.dynamicColors)
            put("accent", options.accent.name); put("opacity", options.opacity); put("scale", options.textScale)
            put("gregorian", options.showGregorian); put("hijri", options.showHijri)
            put("occasions", options.showOccasions); put("reminders", options.showReminders)
            put("hideTitles", options.hideTitles); put("month", options.monthJdn ?: JSONObject.NULL)
        }.toString()

    fun delete(context: Context, ids: IntArray) {
        val edit = prefs(context).edit()
        ids.forEach { edit.remove(key(it)) }
        check(edit.commit()) { "Widget settings could not be deleted" }
    }

    fun restore(context: Context, oldIds: IntArray, newIds: IntArray) {
        require(oldIds.size == newIds.size && oldIds.all { it > 0 } && newIds.all { it > 0 } &&
            oldIds.distinct().size == oldIds.size && newIds.distinct().size == newIds.size) { "Invalid widget restore mapping" }
        val preferences = prefs(context)
        val entries = oldIds.map { preferences.getString(key(it), null) }
        val edit = preferences.edit()
        oldIds.forEach { edit.remove(key(it)) }
        newIds.forEachIndexed { index, id ->
            if (entries[index] == null) edit.remove(key(id)) else edit.putString(key(id), entries[index])
        }
        check(edit.commit()) { "Widget settings could not be restored" }
    }
}
