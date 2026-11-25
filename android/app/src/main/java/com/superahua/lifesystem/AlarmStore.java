package com.superahua.lifesystem;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 提醒数据的本地存储。
 *
 * items   —— JS 传来的待触发提醒计划（未来 7 天展开后的具体时刻）
 * pending —— 用户在通知/全屏页上点了「做了/最小版」但 App 没在运行时暂存的记录，
 *            下次打开 App 由 JS 取走并写进本地数据。
 */
public final class AlarmStore {

    private static final String PREF = "life_alarms";
    private static final String K_ITEMS = "items";
    private static final String K_PENDING = "pending";

    private AlarmStore() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    static JSONArray items(Context c) {
        try {
            return new JSONArray(prefs(c).getString(K_ITEMS, "[]"));
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    static void putItems(Context c, JSONArray arr) {
        prefs(c).edit().putString(K_ITEMS, arr == null ? "[]" : arr.toString()).apply();
    }

    static JSONObject findItem(Context c, String id) {
        JSONArray arr = items(c);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null && id.equals(o.optString("id"))) return o;
        }
        return null;
    }

    /** 追加一条待回写记录；date 形如 2026-11-25，status 为 done / min */
    static void addPending(Context c, String habitId, String date, String status) {
        try {
            JSONArray arr = new JSONArray(prefs(c).getString(K_PENDING, "[]"));
            JSONObject o = new JSONObject();
            o.put("habitId", habitId);
            o.put("date", date);
            o.put("status", status);
            o.put("ts", System.currentTimeMillis());
            arr.put(o);
            prefs(c).edit().putString(K_PENDING, arr.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    /** 取走全部待回写记录并清空 */
    static JSONArray takePending(Context c) {
        String s = prefs(c).getString(K_PENDING, "[]");
        prefs(c).edit().putString(K_PENDING, "[]").apply();
        try {
            return new JSONArray(s);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }
}
