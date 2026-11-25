package com.superahua.lifesystem;

import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 全屏提醒页：占满屏幕、点亮屏幕、显示在锁屏之上。
 * 三个按钮对应三态记录的「做了 / 最小版」，以及延后 10 分钟。
 */
public class AlarmActivity extends AppCompatActivity {

    private static final int COL_GREEN = 0xFF2E7D5B;
    private static final int COL_LINE = 0xFFE3DED3;

    private String habitId = "";
    private String date = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_alarm);

        Intent it = getIntent();
        habitId = it.getStringExtra("habitId") == null ? "" : it.getStringExtra("habitId");
        date = it.getStringExtra("date") == null ? "" : it.getStringExtra("date");
        String name = it.getStringExtra("name");
        String min = it.getStringExtra("min");
        String hint = it.getStringExtra("hint");
        final String itemId = it.getStringExtra("id");

        String clock = it.getStringExtra("clock");
        if (clock == null || clock.length() == 0) {
            clock = String.format("%02d:%02d",
                    java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY),
                    java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE));
        }

        ((TextView) findViewById(R.id.alarmClock)).setText(clock);
        ((TextView) findViewById(R.id.alarmName)).setText(name == null ? "该做点什么了" : name);

        TextView minTv = findViewById(R.id.alarmMin);
        if (min != null && min.length() > 0) {
            minTv.setText("做不动就做这个：" + min);
        } else if (hint != null && hint.length() > 0) {
            minTv.setText(hint);
        } else {
            minTv.setText("现在，就现在");
        }

        Button done = findViewById(R.id.btnDone);
        Button minBtn = findViewById(R.id.btnMin);
        Button snooze = findViewById(R.id.btnSnooze);
        paint(done, COL_GREEN, 0xFFFFFFFF, COL_GREEN);
        paint(minBtn, 0xFFFFFFFF, COL_GREEN, COL_GREEN);
        paint(snooze, 0x00FFFFFF, 0xFF8A857C, COL_LINE);

        done.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { record("done", itemId); }
        });
        minBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { record("min", itemId); }
        });
        snooze.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                snoozeTen(itemId);
                finish();
            }
        });
    }

    /** 把这个提醒往后挪 10 分钟：复制一份计划项，换个 id 再排一次 */
    private void snoozeTen(String itemId) {
        dismiss(itemId);
        if (itemId == null) return;
        try {
            org.json.JSONObject item = AlarmStore.findItem(this, itemId);
            if (item == null) return;
            long at = System.currentTimeMillis() + 10 * 60 * 1000L;
            String nid = itemId + "-s" + at;
            org.json.JSONObject copy = new org.json.JSONObject(item.toString());
            copy.put("id", nid);
            copy.put("at", at);
            org.json.JSONArray arr = AlarmStore.items(this);
            arr.put(copy);
            AlarmStore.putItems(this, arr);
            AlarmScheduler.scheduleAt(this, copy);
        } catch (Exception ignored) {
        }
    }

    private void record(String status, String itemId) {
        if (habitId.length() > 0 && date.length() > 0) {
            AlarmStore.addPending(this, habitId, date, status);
        }
        dismiss(itemId);
        // 让正在运行的 App 立刻刷新
        try {
            sendBroadcast(new Intent(MainActivity.ACTION_WRITTEN).setPackage(getPackageName()));
        } catch (Exception ignored) {
        }
        finish();
    }

    private void dismiss(String itemId) {
        if (itemId == null) return;
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(itemId.hashCode());
    }

    private void paint(Button b, int bg, int fg, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(14));
        g.setColor(bg);
        if (stroke != 0) {
            g.setStroke((int) dp(1.2f), stroke);
        }
        b.setBackground(g);
        b.setTextColor(fg);
        b.setAllCaps(false);
    }

    private float dp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
