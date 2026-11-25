package com.superahua.lifesystem;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 到点触发：发一条最高优先级通知，需要时同时拉起全屏闹钟页。
 */
public class AlarmReceiver extends BroadcastReceiver {

    static final String CHANNEL = "life_due";

    private static final String A_DONE = "com.superahua.lifesystem.ACT_DONE";
    private static final String A_MIN = "com.superahua.lifesystem.ACT_MIN";
    private static final String A_SNOOZE = "com.superahua.lifesystem.ACT_SNOOZE";

    static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (nm.getNotificationChannel(CHANNEL) != null) return;

        NotificationChannel ch = new NotificationChannel(CHANNEL, "到点提醒", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("清单到点时提醒你；可在系统设置里调节音量与震动");
        ch.enableVibration(true);
        ch.setVibrationPattern(new long[]{0, 400, 200, 400});
        ch.enableLights(true);
        ch.setLightColor(Color.RED);
        ch.setShowBadge(true);
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        ch.setSound(sound == null ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) : sound, attrs);
        nm.createNotificationChannel(ch);
    }

    private static PendingIntent act(Context c, String id, String action, int offset) {
        Intent i = new Intent(c, AlarmReceiver.class)
                .setAction(action)
                .setData(Uri.parse("lifesys://act/" + action + "/" + id))
                .putExtra("id", id);
        return PendingIntent.getBroadcast(c, id.hashCode() + offset, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public void onReceive(Context c, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        String id = intent.getStringExtra("id");
        if (id == null) return;

        if (A_DONE.equals(action) || A_MIN.equals(action)) {
            handleAction(c, id, A_DONE.equals(action) ? "done" : "min");
            return;
        }
        if (A_SNOOZE.equals(action)) {
            snooze(c, id);
            return;
        }
        if (!AlarmScheduler.ACTION_FIRE.equals(action)) return;

        JSONObject item = AlarmStore.findItem(c, id);
        if (item == null) return;
        fire(c, item);
    }

    private void handleAction(Context c, String id, String status) {
        JSONObject item = AlarmStore.findItem(c, id);
        String habitId = item == null ? "" : item.optString("habitId", "");
        String date = item == null ? "" : item.optString("date", "");
        if (habitId.length() > 0 && date.length() > 0) {
            AlarmStore.addPending(c, habitId, date, status);
        }
        NotificationManagerCompat.from(c).cancel(id.hashCode());
        // App 若在前台，让它立刻刷新一下
        try {
            c.sendBroadcast(new Intent(MainActivity.ACTION_WRITTEN).setPackage(c.getPackageName()));
        } catch (Exception ignored) {
        }
    }

    private void snooze(Context c, String id) {
        JSONObject item = AlarmStore.findItem(c, id);
        NotificationManagerCompat.from(c).cancel(id.hashCode());
        if (item == null) return;
        try {
            long at = System.currentTimeMillis() + 10 * 60 * 1000L;
            String nid = id + "-s" + at;
            JSONObject copy = new JSONObject(item.toString());
            copy.put("id", nid);
            copy.put("at", at);
            JSONArray arr = AlarmStore.items(c);
            arr.put(copy);
            AlarmStore.putItems(c, arr);
            AlarmScheduler.scheduleAt(c, copy);
        } catch (Exception ignored) {
        }
    }

    private void fire(Context c, JSONObject item) {
        ensureChannel(c);

        String id = item.optString("id", "");
        String habitId = item.optString("habitId", "");
        String name = item.optString("name", "该做点什么了");
        String min = item.optString("min", "");
        String hint = item.optString("hint", "");
        String date = item.optString("date", "");
        boolean full = item.optBoolean("full", true);

        Intent open = new Intent(c, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPi = PendingIntent.getActivity(c, id.hashCode() + 31, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String body = min.length() > 0 ? ("做不动就做这个：" + min) : "现在，就现在";

        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle(name)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setColor(0xFF2E7D5B)
                .setAutoCancel(false)
                .setOngoing(false)
                .setShowWhen(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(openPi);

        if (habitId.length() > 0) {
            b.addAction(0, "做了", act(c, id, A_DONE, 41));
            b.addAction(0, "最小版", act(c, id, A_MIN, 43));
        }
        b.addAction(0, "10 分钟后", act(c, id, A_SNOOZE, 47));

        if (full) {
            Intent ai = new Intent(c, AlarmActivity.class)
                    .setData(Uri.parse("lifesys://full/" + id))
                    .putExtra("id", id)
                    .putExtra("name", name)
                    .putExtra("min", min)
                    .putExtra("hint", hint)
                    .putExtra("habitId", habitId)
                    .putExtra("date", date)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent fullPi = PendingIntent.getActivity(c, id.hashCode() + 53, ai,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // 锁屏时系统直接铺满全屏；非锁屏时退化为顶部横幅
            b.setFullScreenIntent(fullPi, true);

            // 有「显示在其他应用上层」权限时，任何状态下都能直接把页面顶上来
            if (Perm.overlay(c)) {
                try {
                    c.startActivity(new Intent(c, AlarmActivity.class)
                            .putExtra("id", id)
                            .putExtra("name", name)
                            .putExtra("min", min)
                            .putExtra("hint", hint)
                    .putExtra("habitId", habitId)
                    .putExtra("date", date)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                } catch (Exception ignored) {
                }
            }
        }

        try {
            NotificationManagerCompat.from(c).notify(id.hashCode(), b.build());
        } catch (SecurityException ignored) {
            // 用户关了通知权限
        }
    }
}
