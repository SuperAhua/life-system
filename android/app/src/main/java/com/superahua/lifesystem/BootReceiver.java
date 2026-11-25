package com.superahua.lifesystem;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 重启、应用更新后重排闹钟 —— 系统重启会清掉所有 AlarmManager 闹钟。
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context c, Intent intent) {
        String a = intent.getAction();
        if (a == null) return;
        if (Intent.ACTION_BOOT_COMPLETED.equals(a)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)
                || "android.intent.action.LOCKED_BOOT_COMPLETED".equals(a)
                || "android.intent.action.QUICKBOOT_POWERON".equals(a)) {
            AlarmReceiver.ensureChannel(c);
            AlarmScheduler.rescheduleAfterBoot(c);
        }
    }
}
