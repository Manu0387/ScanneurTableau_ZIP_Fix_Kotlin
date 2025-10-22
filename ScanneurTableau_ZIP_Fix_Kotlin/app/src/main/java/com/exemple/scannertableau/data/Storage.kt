package com.exemple.scannertableau.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ModuleRecord(val ssid: String, var customName: String? = null, var disjoncteur: String? = null)

object Storage {
  private const val PREF = "scan_prefs"
  private const val KEY_PREFIX = "prefix"
  private const val KEY_MODULES = "modules"

  fun getPrefix(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_PREFIX, "prise-") ?: "prise-"
  fun setPrefix(ctx: Context, v: String) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY_PREFIX, v).apply()

  fun loadModules(ctx: Context): MutableList<ModuleRecord> {
    val json = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_MODULES, "[]") ?: "[]"
    val arr = JSONArray(json); val list = mutableListOf<ModuleRecord>()
    for (i in 0 until arr.length()) {
      val o = arr.getJSONObject(i)
      list.add(ModuleRecord(o.getString("ssid"), o.optString("customName", null), o.optString("disjoncteur", null)))
    }
    return list
  }
  fun saveModules(ctx: Context, list: List<ModuleRecord>) {
    val arr = JSONArray()
    list.forEach {
      val o = JSONObject()
      o.put("ssid", it.ssid)
      it.customName?.let { o.put("customName", it) }
      it.disjoncteur?.let { o.put("disjoncteur", it) }
      arr.put(o)
    }
    ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY_MODULES, arr.toString()).apply()
  }
  fun upsertBySSID(ctx: Context, ssid: String, update: (ModuleRecord) -> Unit) {
    val list = loadModules(ctx)
    val idx = list.indexOfFirst { it.ssid == ssid }
    if (idx >= 0) { val r = list[idx]; update(r); list[idx] = r }
    else { val r = ModuleRecord(ssid); update(r); list.add(r) }
    saveModules(ctx, list)
  }
}
