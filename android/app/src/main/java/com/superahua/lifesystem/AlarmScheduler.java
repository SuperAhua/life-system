package com.superahua.lifesystem;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 提醒排程。
 *
 * 用 AlarmManager.setAlarmClock()：这是系统里优先级最高的闹钟，
 * 能穿透 Doze、不受省电策略限制，并且会在状态栏显示「下一个闹钟」。
 * 没有精确闹钟权限时逐级降级为 setExactAndAllowWhileIdle → set。
 */
public final class AlarmScheduler {

    static final String ACTION_FIRE = "com.superahua.lifesystem.FIRE";

    private AlarmScheduler() {}

    private static PendingIntent fireIntent(Context c, String id) {
        Intent i = new Intent(c, AlarmReceiver.class)
                .setAction(ACTION_FIRE)
                .setData(Uri.parse("lifesys://alarm/" + id))
                .putExtra("id", id);
        return PendingIntent.getBroadcast(c, id.hashCode(), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent showIntent(Context c, String id) {
        Intent i = new Intent(c, MainActivity.class)
                .setData(Uri.parse("lifesys://open/" + id))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(c, id.hashCode() + 17, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void scheduleAt(Context c, JSONObject it) {
        long at = it.optLong("at", 0L);
        String id = it.optString("id", "");
        if (at <= System.currentTimeMillis() || id.length() == 0) return;

        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        PendingIntent pi = fireIntent(c, id);
        try {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(at, showIntent(c, id)), pi);
        } catch (SecurityException e) {
            try {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            } catch (SecurityException e2) {
                am.set(AlarmManager.RTC_WAKEUP, at, pi);
            }
        }
    }

    private static void cancelOne(Context c, String id) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        am.cancel(fireIntent(c, id));
    }

    /** 用新的计划整体替换旧计划：先撤掉旧的，再排新的，最后落盘 */
    static int apply(Context c, JSONArray items) {
        JSONArray old = AlarmStore.items(c);
        for (int i = 0; i < old.length(); i++) {
            JSONObject o = old.optJSONObject(i);
            if (o != null) cancelOne(c, o.optString("id", ""));
        }
        JSONArray fresh = items == null ? new JSONArray() : items;
        int n = 0;
        for (int i = 0; i < fresh.length(); i++) {
            JSONObject o = fresh.optJSONObject(i);
            if (o == null) continue;
            scheduleAt(c, o);
            n++;
        }
        AlarmStore.putItems(c, fresh);
        return n;
    }

    static void cancelAll(Context c) {
        apply(c, new JSONArray());
    }

    /** 重启后重排：只保留还没到点的那些 */
    static void rescheduleAfterBoot(Context c) {
        JSONArray old = AlarmStore.items(c);
        long now = System.currentTimeMillis();
        for (int i = 0; i < old.length(); i++) {
            JSONObject o = old.optJSONObject(i);
            if (o != null && o.optLong("at", 0L) > now) scheduleAt(c, o);
        }
    }
}
