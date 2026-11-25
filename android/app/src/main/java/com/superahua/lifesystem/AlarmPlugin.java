package com.superahua.lifesystem;

import android.content.Context;
import android.os.Build;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 给网页层用的原生接口：排提醒、查权限、取回写记录、发测试。
 */
@CapacitorPlugin(
        name = "Alarm",
        permissions = {
                @Permission(alias = "notifications", strings = {"android.permission.POST_NOTIFICATIONS"})
        }
)
public class AlarmPlugin extends Plugin {

    /** 用一份"未来 7 天展开后的具体时刻"整体替换旧的排程 */
    @PluginMethod
    public void schedule(PluginCall call) {
        int n = 0;
        try {
            JSArray items = call.getArray("items");
            JSONArray arr = items == null ? new JSONArray() : new JSONArray(items.toString());
            n = AlarmScheduler.apply(getContext(), arr);
        } catch (Exception ignored) {
        }
        JSObject ret = new JSObject();
        ret.put("count", n);
        call.resolve(ret);
    }

    @PluginMethod
    public void cancelAll(PluginCall call) {
        AlarmScheduler.cancelAll(getContext());
        call.resolve();
    }

    /** 取走用户在通知/全屏页上点掉的记录 */
    @PluginMethod
    public void takePending(PluginCall call) {
        JSArray out = new JSArray();
        JSONArray arr = AlarmStore.takePending(getContext());
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            try {
                out.put(new JSObject(o.toString()));
            } catch (Exception ignored) {
            }
        }
        JSObject ret = new JSObject();
        ret.put("items", out);
        call.resolve(ret);
    }

    @PluginMethod
    public void status(PluginCall call) {
        Context c = getContext();
        JSObject r = new JSObject();
        r.put("native", true);
        r.put("notifications", Perm.notifications(c));
        r.put("exact", Perm.exact(c));
        r.put("fullScreen", Perm.fullScreen(c));
        r.put("overlay", Perm.overlay(c));
        r.put("battery", Perm.battery(c));
        r.put("sdk", Build.VERSION.SDK_INT);
        call.resolve(r);
    }

    @PluginMethod
    public void askNotifications(PluginCall call) {
        Context c = getContext();
        if (Perm.notifications(c)) {
            JSObject r = new JSObject();
            r.put("granted", true);
            call.resolve(r);
            return;
        }
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissionForAlias("notifications", call, "notifCallback");
        } else {
            Perm.open(c, "notif");
            JSObject r = new JSObject();
            r.put("granted", false);
            r.put("opened", true);
            call.resolve(r);
        }
    }

    @PermissionCallback
    private void notifCallback(PluginCall call) {
        JSObject r = new JSObject();
        r.put("granted", Perm.notifications(getContext()));
        call.resolve(r);
    }

    @PluginMethod
    public void openSettings(PluginCall call) {
        String which = call.getString("which", "app");
        Perm.open(getContext(), which);
        call.resolve();
    }

    /** 5 秒后响一次，用来确认整套链路通不通 */
    @PluginMethod
    public void test(PluginCall call) {
        Context c = getContext();
        AlarmReceiver.ensureChannel(c);
        try {
            long at = System.currentTimeMillis() + 5000L;
            JSONObject it = new JSONObject();
            it.put("id", "test-" + at);
            it.put("habitId", "");
            it.put("date", "");
            it.put("name", "这是一条测试提醒");
            it.put("min", "看到这一屏，说明提醒链路通了");
            it.put("hint", "五秒后见");
            it.put("at", at);
            it.put("full", true);
            JSONArray arr = AlarmStore.items(c);
            arr.put(it);
            AlarmStore.putItems(c, arr);
            AlarmScheduler.scheduleAt(c, it);
        } catch (Exception ignored) {
        }
        call.resolve();
    }
}
