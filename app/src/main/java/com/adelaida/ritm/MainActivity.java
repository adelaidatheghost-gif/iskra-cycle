package com.adelaida.ritm;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {
    private final int BG = Color.rgb(16,16,20);
    private final int CARD = Color.rgb(28,28,34);
    private final int TEXT = Color.rgb(245,245,248);
    private final int MUTED = Color.rgb(165,165,178);
    private final int PURPLE = Color.rgb(182,124,255);
    private final int RED = Color.rgb(255,103,116);

    private android.content.SharedPreferences prefs;
    private Set<String> periodDays;
    private LocalDate selected = LocalDate.now();

    private TextView status;
    private TextView selectedLabel;
    private TextView selectedInfo;
    private Button periodButton;
    private Button flowButton;
    private Button painButton;
    private Button moodButton;
    private Button cycleButton;
    private Button durationButton;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        prefs = getSharedPreferences("ritm_cycle", MODE_PRIVATE);
        periodDays = new HashSet<>(prefs.getStringSet("period_days", Collections.emptySet()));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(18), dp(16), dp(24));
        root.setBackgroundColor(BG);
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        TextView title = text("Ритм", 28, TEXT);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView subtitle = text("календарь цикла", 13, MUTED);
        LinearLayout.LayoutParams subLp = lp();
        subLp.bottomMargin = dp(14);
        root.addView(subtitle, subLp);

        status = text("", 17, TEXT);
        status.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        status.setPadding(dp(16), dp(14), dp(16), dp(14));
        status.setBackground(roundRect(CARD, 18));
        LinearLayout.LayoutParams statusLp = lp();
        statusLp.bottomMargin = dp(14);
        root.addView(status, statusLp);

        CalendarView calendar = new CalendarView(this);
        calendar.setFirstDayOfWeek(java.util.Calendar.MONDAY);
        calendar.setDate(System.currentTimeMillis(), false, true);
        calendar.setShowWeekNumber(false);
        calendar.setSelectedWeekBackgroundColor(Color.TRANSPARENT);
        calendar.setFocusedMonthDateColor(TEXT);
        calendar.setUnfocusedMonthDateColor(MUTED);
        calendar.setWeekSeparatorLineColor(Color.TRANSPARENT);
        calendar.setSelectedDateVerticalBar(Color.TRANSPARENT);
        calendar.setBackground(roundRect(CARD, 18));
        calendar.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selected = LocalDate.of(year, month + 1, dayOfMonth);
            refresh();
        });
        LinearLayout.LayoutParams calLp = lp();
        calLp.height = dp(330);
        calLp.bottomMargin = dp(14);
        root.addView(calendar, calLp);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(16));
        card.setBackground(roundRect(CARD, 18));
        root.addView(card, lp());

        selectedLabel = text("", 19, TEXT);
        selectedLabel.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        card.addView(selectedLabel);

        selectedInfo = text("", 12, MUTED);
        LinearLayout.LayoutParams infoLp = lp();
        infoLp.topMargin = dp(4);
        infoLp.bottomMargin = dp(12);
        card.addView(selectedInfo, infoLp);

        periodButton = button("Отметить месячные");
        periodButton.setOnClickListener(v -> {
            String key = selected.toString();
            if (periodDays.contains(key)) periodDays.remove(key); else periodDays.add(key);
            prefs.edit().putStringSet("period_days", new HashSet<>(periodDays)).apply();
            refresh();
        });
        card.addView(periodButton, buttonLp());

        flowButton = button("");
        flowButton.setOnClickListener(v -> {
            String key = "flow_" + selected;
            int next = (prefs.getInt(key, 0) + 1) % 4;
            prefs.edit().putInt(key, next).apply();
            refresh();
        });
        card.addView(flowButton, buttonLp());

        painButton = button("");
        painButton.setOnClickListener(v -> {
            String key = "pain_" + selected;
            prefs.edit().putBoolean(key, !prefs.getBoolean(key, false)).apply();
            refresh();
        });
        card.addView(painButton, buttonLp());

        moodButton = button("");
        moodButton.setOnClickListener(v -> {
            String key = "mood_" + selected;
            int next = (prefs.getInt(key, 0) + 1) % 3;
            prefs.edit().putInt(key, next).apply();
            refresh();
        });
        card.addView(moodButton, buttonLp());

        TextView settingsTitle = text("Параметры цикла", 14, TEXT);
        settingsTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams setTitleLp = lp();
        setTitleLp.topMargin = dp(16);
        setTitleLp.bottomMargin = dp(8);
        card.addView(settingsTitle, setTitleLp);

        cycleButton = button("");
        cycleButton.setOnClickListener(v -> {
            int next = prefs.getInt("cycle_length", 28) + 1;
            if (next > 45) next = 20;
            prefs.edit().putInt("cycle_length", next).apply();
            refresh();
        });
        card.addView(cycleButton, buttonLp());

        durationButton = button("");
        durationButton.setOnClickListener(v -> {
            int next = prefs.getInt("period_length", 5) + 1;
            if (next > 10) next = 2;
            prefs.edit().putInt("period_length", next).apply();
            refresh();
        });
        card.addView(durationButton, buttonLp());

        TextView disclaimer = text("Прогноз приблизительный и не является методом контрацепции.", 11, MUTED);
        LinearLayout.LayoutParams discLp = lp();
        discLp.topMargin = dp(12);
        card.addView(disclaimer, discLp);

        setContentView(scroll);
        refresh();
    }

    private void refresh() {
        LocalDate today = LocalDate.now();
        LocalDate lastStart = lastActualStartOnOrBefore(today);
        int cycle = averageCycle();
        int periodLength = prefs.getInt("period_length", 5);

        if (periodDays.contains(today.toString())) {
            status.setText("Сегодня месячные");
        } else if (lastStart != null) {
            LocalDate next = lastStart.plusDays(cycle);
            long left = ChronoUnit.DAYS.between(today, next);
            long cycleDay = ChronoUnit.DAYS.between(lastStart, today) + 1;
            String top = cycleDay > 0 ? cycleDay + "-й день цикла" : "Цикл";
            if (left >= 0) top += "\nДо предполагаемых месячных: " + left + " д.";
            status.setText(top);
        } else {
            status.setText("Отметь хотя бы один день месячных — появится прогноз.");
        }

        selectedLabel.setText(selected.getDayOfMonth() + "." + String.format("%02d", selected.getMonthValue()) + "." + selected.getYear());
        selectedInfo.setText(dayInfo(selected));

        boolean isPeriod = periodDays.contains(selected.toString());
        periodButton.setText(isPeriod ? "Месячные ✓" : "Отметить месячные");
        periodButton.setBackground(roundRect(isPeriod ? RED : Color.rgb(42,42,50), 14));

        int flow = prefs.getInt("flow_" + selected, 0);
        String[] flows = {"Интенсивность: —", "Интенсивность: лёгкие", "Интенсивность: средние", "Интенсивность: обильные"};
        flowButton.setText(flows[Math.max(0, Math.min(3, flow))]);

        boolean pain = prefs.getBoolean("pain_" + selected, false);
        painButton.setText(pain ? "Боль ✓" : "Боль —");

        int mood = prefs.getInt("mood_" + selected, 0);
        String[] moods = {"Настроение: хорошее", "Настроение: обычное", "Настроение: плохое"};
        moodButton.setText(moods[Math.max(0, Math.min(2, mood))]);

        cycleButton.setText("Средняя длина цикла: " + cycle + " д.");
        durationButton.setText("Длительность месячных: " + periodLength + " д.");
    }

    private String dayInfo(LocalDate date) {
        if (periodDays.contains(date.toString())) return "Отмечено: месячные";

        LocalDate last = lastActualStartOnOrBefore(date);
        if (last == null) return "Нет данных о цикле";

        int cycle = averageCycle();
        long day = ChronoUnit.DAYS.between(last, date) + 1;
        if (day <= 0) return "До начала отмеченного цикла";
        if (day <= prefs.getInt("period_length", 5)) return day + "-й день цикла • менструальная фаза";

        int ov = Math.max(8, cycle - 14);
        if (day >= ov - 5L && day <= ov + 1L) {
            return day == ov ? day + "-й день цикла • предполагаемая овуляция" : day + "-й день цикла • предполагаемое фертильное окно";
        }
        if (day < ov - 5L) return day + "-й день цикла • фолликулярная фаза";
        return day + "-й день цикла • лютеиновая фаза";
    }

    private List<LocalDate> starts() {
        List<LocalDate> all = new ArrayList<>();
        for (String s : periodDays) {
            try { all.add(LocalDate.parse(s)); } catch (Exception ignored) {}
        }
        Collections.sort(all);
        List<LocalDate> result = new ArrayList<>();
        LocalDate prev = null;
        for (LocalDate d : all) {
            if (prev == null || !d.equals(prev.plusDays(1))) result.add(d);
            prev = d;
        }
        return result;
    }

    private LocalDate lastActualStartOnOrBefore(LocalDate date) {
        LocalDate result = null;
        for (LocalDate d : starts()) if (!d.isAfter(date)) result = d;
        return result;
    }

    private int averageCycle() {
        List<LocalDate> s = starts();
        if (s.size() < 2) return prefs.getInt("cycle_length", 28);
        long sum = 0;
        int count = 0;
        for (int i = Math.max(1, s.size() - 6); i < s.size(); i++) {
            long diff = ChronoUnit.DAYS.between(s.get(i - 1), s.get(i));
            if (diff >= 14 && diff <= 60) { sum += diff; count++; }
        }
        return count == 0 ? prefs.getInt("cycle_length", 28) : (int)Math.round(sum / (double)count);
    }

    private TextView text(String value, int sp, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(color);
        return v;
    }

    private Button button(String value) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(value);
        b.setTextSize(13);
        b.setTextColor(TEXT);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(12), 0, dp(12), 0);
        b.setBackground(roundRect(Color.rgb(42,42,50), 14));
        return b;
    }

    private LinearLayout.LayoutParams buttonLp() {
        LinearLayout.LayoutParams lp = lp();
        lp.height = dp(48);
        lp.bottomMargin = dp(8);
        return lp;
    }

    private LinearLayout.LayoutParams lp() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private android.graphics.drawable.Drawable roundRect(int color, int radiusDp) {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
