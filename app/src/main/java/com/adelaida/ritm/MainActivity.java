package com.adelaida.ritm;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {
    private final int BG = Color.rgb(16,16,20);
    private final int CARD = Color.rgb(28,28,34);
    private final int CARD2 = Color.rgb(42,42,50);
    private final int TEXT = Color.rgb(245,245,248);
    private final int MUTED = Color.rgb(165,165,178);
    private final int PURPLE = Color.rgb(182,124,255);
    private final int RED = Color.rgb(255,103,116);
    private final int ORANGE = Color.rgb(255,158,88);

    private android.content.SharedPreferences prefs;
    private Set<String> periodDays;
    private LocalDate selected = LocalDate.now();
    private YearMonth shownMonth = YearMonth.now();

    private TextView status;
    private TextView monthTitle;
    private GridLayout calendarGrid;
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
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView subtitle = text("календарь цикла", 13, MUTED);
        LinearLayout.LayoutParams subLp = lp();
        subLp.bottomMargin = dp(14);
        root.addView(subtitle, subLp);

        status = text("", 17, TEXT);
        status.setTypeface(Typeface.DEFAULT_BOLD);
        status.setPadding(dp(16), dp(14), dp(16), dp(14));
        status.setBackground(roundRect(CARD, 18, 0, 0));
        LinearLayout.LayoutParams statusLp = lp();
        statusLp.bottomMargin = dp(14);
        root.addView(status, statusLp);

        LinearLayout calendarCard = new LinearLayout(this);
        calendarCard.setOrientation(LinearLayout.VERTICAL);
        calendarCard.setPadding(dp(12), dp(12), dp(12), dp(14));
        calendarCard.setBackground(roundRect(CARD, 18, 0, 0));
        LinearLayout.LayoutParams calCardLp = lp();
        calCardLp.bottomMargin = dp(14);
        root.addView(calendarCard, calCardLp);

        LinearLayout monthRow = new LinearLayout(this);
        monthRow.setOrientation(LinearLayout.HORIZONTAL);
        monthRow.setGravity(Gravity.CENTER_VERTICAL);

        Button prev = smallButton("‹");
        Button next = smallButton("›");
        monthTitle = text("", 16, TEXT);
        monthTitle.setGravity(Gravity.CENTER);
        monthTitle.setTypeface(Typeface.DEFAULT_BOLD);

        monthRow.addView(prev, new LinearLayout.LayoutParams(dp(46), dp(42)));
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        monthRow.addView(monthTitle, titleLp);
        monthRow.addView(next, new LinearLayout.LayoutParams(dp(46), dp(42)));
        calendarCard.addView(monthRow);

        prev.setOnClickListener(v -> {
            shownMonth = shownMonth.minusMonths(1);
            renderCalendar();
        });
        next.setOnClickListener(v -> {
            shownMonth = shownMonth.plusMonths(1);
            renderCalendar();
        });

        GridLayout weekdays = new GridLayout(this);
        weekdays.setColumnCount(7);
        String[] wd = {"Пн","Вт","Ср","Чт","Пт","Сб","Вс"};
        for (String s : wd) {
            TextView t = text(s, 11, MUTED);
            t.setGravity(Gravity.CENTER);
            GridLayout.LayoutParams p = cellLp();
            p.height = dp(28);
            weekdays.addView(t, p);
        }
        calendarCard.addView(weekdays, lp());

        calendarGrid = new GridLayout(this);
        calendarGrid.setColumnCount(7);
        calendarCard.addView(calendarGrid, lp());

        LinearLayout legend = new LinearLayout(this);
        legend.setOrientation(LinearLayout.HORIZONTAL);
        legend.setGravity(Gravity.CENTER_VERTICAL);
        legend.setPadding(0, dp(8), 0, 0);
        legend.addView(legendItem("● месячные", RED), new LinearLayout.LayoutParams(0, dp(28), 1f));
        legend.addView(legendItem("○ прогноз", RED), new LinearLayout.LayoutParams(0, dp(28), 1f));
        legend.addView(legendItem("• фертильные", ORANGE), new LinearLayout.LayoutParams(0, dp(28), 1f));
        calendarCard.addView(legend);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(16));
        card.setBackground(roundRect(CARD, 18, 0, 0));
        root.addView(card, lp());

        selectedLabel = text("", 19, TEXT);
        selectedLabel.setTypeface(Typeface.DEFAULT_BOLD);
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
            int nextValue = (prefs.getInt(key, 0) + 1) % 4;
            prefs.edit().putInt(key, nextValue).apply();
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
            int nextValue = (prefs.getInt(key, 0) + 1) % 3;
            prefs.edit().putInt(key, nextValue).apply();
            refresh();
        });
        card.addView(moodButton, buttonLp());

        TextView settingsTitle = text("Параметры цикла", 14, TEXT);
        settingsTitle.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams setTitleLp = lp();
        setTitleLp.topMargin = dp(16);
        setTitleLp.bottomMargin = dp(8);
        card.addView(settingsTitle, setTitleLp);

        cycleButton = button("");
        cycleButton.setOnClickListener(v -> {
            int nextValue = prefs.getInt("cycle_length", 28) + 1;
            if (nextValue > 45) nextValue = 20;
            prefs.edit().putInt("cycle_length", nextValue).apply();
            refresh();
        });
        card.addView(cycleButton, buttonLp());

        durationButton = button("");
        durationButton.setOnClickListener(v -> {
            int nextValue = prefs.getInt("period_length", 5) + 1;
            if (nextValue > 10) nextValue = 2;
            prefs.edit().putInt("period_length", nextValue).apply();
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

    private void renderCalendar() {
        monthTitle.setText(capitalize(shownMonth.atDay(1)
                .format(DateTimeFormatter.ofPattern("LLLL yyyy", new Locale("ru", "RU")))));

        calendarGrid.removeAllViews();
        int firstCol = shownMonth.atDay(1).getDayOfWeek().getValue() - 1;
        int days = shownMonth.lengthOfMonth();
        Set<String> predicted = predictedPeriodDays();
        Set<String> fertile = fertileDays();

        for (int i = 0; i < 42; i++) {
            TextView cell = new TextView(this);
            cell.setGravity(Gravity.CENTER);
            cell.setTextSize(13);
            cell.setTextColor(TEXT);

            int day = i - firstCol + 1;
            if (day >= 1 && day <= days) {
                LocalDate date = shownMonth.atDay(day);
                String key = date.toString();
                boolean isPeriod = periodDays.contains(key);
                boolean isPredicted = predicted.contains(key);
                boolean isFertile = fertile.contains(key);
                boolean isSelected = date.equals(selected);
                boolean isToday = date.equals(LocalDate.now());

                String label = String.valueOf(day);
                if (isFertile && !isPeriod) label += "\n•";
                cell.setText(label);

                int fill = Color.TRANSPARENT;
                int strokeColor = Color.TRANSPARENT;
                int strokeWidth = 0;

                if (isPeriod) {
                    fill = RED;
                    cell.setTextColor(Color.WHITE);
                    cell.setTypeface(Typeface.DEFAULT_BOLD);
                } else if (isPredicted) {
                    strokeColor = RED;
                    strokeWidth = 2;
                }

                if (isSelected) {
                    strokeColor = Color.WHITE;
                    strokeWidth = 2;
                } else if (isToday && !isPeriod && !isPredicted) {
                    strokeColor = PURPLE;
                    strokeWidth = 2;
                }

                cell.setBackground(roundRect(fill, 14, strokeColor, strokeWidth));
                cell.setOnClickListener(v -> {
                    selected = date;
                    shownMonth = YearMonth.from(date);
                    refresh();
                });
            } else {
                cell.setText("");
            }

            GridLayout.LayoutParams p = cellLp();
            p.height = dp(48);
            p.setMargins(dp(2), dp(2), dp(2), dp(2));
            calendarGrid.addView(cell, p);
        }
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
        periodButton.setBackground(roundRect(isPeriod ? RED : CARD2, 14, 0, 0));

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

        renderCalendar();
    }

    private String dayInfo(LocalDate date) {
        if (periodDays.contains(date.toString())) return "Отмечено: месячные";
        if (predictedPeriodDays().contains(date.toString())) return "Прогноз месячных";

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

    private List<LocalDate> predictedStarts() {
        List<LocalDate> s = starts();
        if (s.isEmpty()) return Collections.emptyList();
        LocalDate last = s.get(s.size() - 1);
        int cycle = averageCycle();
        List<LocalDate> result = new ArrayList<>();
        LocalDate next = last.plusDays(cycle);
        for (int i = 0; i < 6; i++) {
            result.add(next);
            next = next.plusDays(cycle);
        }
        return result;
    }

    private Set<String> predictedPeriodDays() {
        Set<String> result = new HashSet<>();
        int len = prefs.getInt("period_length", 5);
        for (LocalDate start : predictedStarts()) {
            for (int i = 0; i < len; i++) result.add(start.plusDays(i).toString());
        }
        return result;
    }

    private Set<String> fertileDays() {
        Set<String> result = new HashSet<>();
        for (LocalDate nextPeriod : predictedStarts()) {
            LocalDate ov = nextPeriod.minusDays(14);
            for (int i = 5; i >= 0; i--) result.add(ov.minusDays(i).toString());
            result.add(ov.plusDays(1).toString());
        }
        return result;
    }

    private TextView legendItem(String value, int color) {
        TextView t = text(value, 10, color);
        t.setGravity(Gravity.CENTER);
        return t;
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
        b.setBackground(roundRect(CARD2, 14, 0, 0));
        return b;
    }

    private Button smallButton(String value) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(value);
        b.setTextSize(22);
        b.setTextColor(TEXT);
        b.setPadding(0, 0, 0, 0);
        b.setBackground(roundRect(CARD2, 12, 0, 0));
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

    private GridLayout.LayoutParams cellLp() {
        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width = 0;
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        return p;
    }

    private android.graphics.drawable.Drawable roundRect(int fillColor, int radiusDp, int strokeColor, int strokeWidthDp) {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        if (fillColor == Color.TRANSPARENT) g.setColor(Color.TRANSPARENT); else g.setColor(fillColor);
        g.setCornerRadius(dp(radiusDp));
        if (strokeWidthDp > 0 && strokeColor != 0) g.setStroke(dp(strokeWidthDp), strokeColor);
        return g;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
