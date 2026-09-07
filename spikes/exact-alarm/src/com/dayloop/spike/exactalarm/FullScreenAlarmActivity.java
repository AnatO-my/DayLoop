package com.dayloop.spike.exactalarm;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

public final class FullScreenAlarmActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String source = getIntent() == null ? "unknown" : getIntent().getStringExtra("source");
        MainActivity.appendLog(this, "FULL_SCREEN_ACTIVITY_CREATED source=" + source);
        TextView textView = new TextView(this);
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(24f);
        textView.setText("DayLoop alarm-style UI spike\n" + source);
        setContentView(textView);
    }
}
