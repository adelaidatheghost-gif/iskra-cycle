package com.adelaida.ritm;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(16, 16, 20));
        window.setNavigationBarColor(Color.rgb(16, 16, 20));
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                );
            }
        }

        setContentView(new CycleView(this));
    }

    static final class CycleView extends View {
        private final float d;
        private final SharedPreferences prefs;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Locale ru = new Locale("ru", "RU");

        private final int bg = Color.rgb(16, 16, 20);
        private final int card = Color.rgb(26, 26, 32);
        private final int card2 = Color.rgb(33, 33, 41);
        private final int text = Color.rgb(244, 244, 248);
        private final int muted = Color.rgb(154, 154, 169);
        private final int purple = Color.rgb(182, 124, 255);
        private final int period = Color.rgb(255, 103, 116);
        private final int fertile = Color.rgb(255, 158, 88);

        private YearMonth shownMonth = YearMonth.now();
        private LocalDate selected = LocalDate.now();
        private Set<String> periodDays;

        private final RectF prevBtn = new RectF();
        private final RectF nextBtn = new RectF();
        private final RectF periodBtn = new RectF();
        private final RectF flowBtn = new RectF();
        private final RectF painBtn = new RectF();
        private final RectF moodBtn = new RectF();
        private final RectF cycleBtn = new RectF();
        private final RectF durationBtn = new RectF();

        private float gridLeft;
        private float gridTop;
        private float cellW;
        private float rowH;
        private float gridBottom;

        CycleView(Context context) {
            super(context);
            d = getResources().getDisplayMetrics().density;
            prefs = context.getSharedPreferences("ritm_cycle", Context.MODE_PRIVATE);
            periodDays = new HashSet<>(prefs.getStringSet("period_days", Collections.emptySet()));

            p.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL));
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(dp(1.5f));
            setBackgroundColor(bg);
            setFocusable(true);
        }

        private float dp(float v) {
            return v * d;
        }

        private void fill(int color, float size) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(color);
            p.setTextSize(dp(size));
            p.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL));
        }

        private void medium(int color, float size) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(color);
            p.setTextSize(dp(size));
            p.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        }

        private void rounded(Canvas c, RectF r, int color, float radiusDp) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(color);
            c.drawRoundRect(r, dp(radiusDp), dp(radiusDp), p);
        }

        private void centered(Canvas c, String s, float x, float y, Paint paint) {
            c.drawText(s, x - paint.measureText(s) / 2f, y, paint);
        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);
            c.drawColor(bg);

            final float w = getWidth();
            final float h = getHeight();
            final float pad = dp(16);

            medium(text, 25);
            c.drawText("Ритм", pad, dp(40), p);
            fill(muted, 12);
            c.drawText("календарь цикла", pad, dp(59), p);

            RectF summary = new RectF(pad, dp(76), w - pad, dp(154));
            rounded(c, summary, card, 20);

            LocalDate today = LocalDate.now();
            medium(text, 16);
            String status = statusFor(today);
            c.drawText(status, summary.left + dp(16), summary.top + dp(28), p);

            fill(muted, 12);
            c.drawText(secondaryStatus(today), summary.left + dp(16), summary.top + dp(52), p);

            int avg = averageCycle();
            int len = prefs.getInt("period_length", 5);
            medium(purple, 12);
            String mini = avg + " д. цикл  •  " + len + " д. месячные";
            c.drawText(mini, summary.left + dp(16), summary.bottom - dp(10), p);

            float navY = dp(186);
            prevBtn.set(pad, navY - dp(24), pad + dp(48), navY + dp(18));
            nextBtn.set(w - pad - dp(48), navY - dp(24), w - pad, navY + dp(18));

            rounded(c, prevBtn, card, 14);
            rounded(c, nextBtn, card, 14);
            medium(text, 22);
            centered(c, "‹", prevBtn.centerX(), navY + dp(2), p);
            centered(c, "›", nextBtn.centerX(), navY + dp(2), p);

            medium(text, 16);
            String monthName = shownMonth.atDay(1)
                    .format(DateTimeFormatter.ofPattern("LLLL yyyy", ru));
            if (!monthName.isEmpty()) {
                monthName = Character.toUpperCase(monthName.charAt(0)) + monthName.substring(1);
            }
            centered(c, monthName, w / 2f, navY, p);

            gridLeft = pad;
            gridTop = dp(232);
            cellW = (w - pad * 2f) / 7f;
            float reserved = dp(300);
            rowH = Math.max(dp(34), Math.min(dp(49), (h - gridTop - reserved) / 6f));
            gridBottom = gridTop + rowH * 6f;

            String[] weekdays = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
            fill(muted, 11);
            for (int i = 0; i < 7; i++) {
                centered(c, weekdays[i], gridLeft + cellW * (i + .5f), gridTop - dp(15), p);
            }

            Set<String> predicted = predictedPeriodDays();
            Set<String> fertileDays = fertileDays();
            Set<String> ovulationDays = ovulationDays();

            LocalDate first = shownMonth.atDay(1);
            int firstColumn = first.getDayOfWeek().getValue() - 1;
            int days = shownMonth.lengthOfMonth();

            for (int day = 1; day <= days; day++) {
                int index = firstColumn + day - 1;
                int row = index / 7;
                int col = index % 7;
                float cx = gridLeft + cellW * (col + .5f);
                float cy = gridTop + rowH * row + rowH / 2f;
                LocalDate date = shownMonth.atDay(day);
                String key = date.toString();

                if (periodDays.contains(key)) {
                    p.setColor(period);
                    p.setStyle(Paint.Style.FILL);
                    c.drawCircle(cx, cy - dp(2), Math.min(dp(17), rowH * .38f), p);
                } else if (predicted.contains(key)) {
                    stroke.setColor(Color.argb(190, 255, 103, 116));
                    stroke.setStrokeWidth(dp(1.4f));
                    c.drawCircle(cx, cy - dp(2), Math.min(dp(16), rowH * .36f), stroke);
                }

                if (fertileDays.contains(key) && !periodDays.contains(key)) {
                    p.setColor(fertile);
                    p.setStyle(Paint.Style.FILL);
                    c.drawCircle(cx, cy + dp(14), dp(2.2f), p);
                }

                if (ovulationDays.contains(key)) {
                    stroke.setColor(purple);
                    stroke.setStrokeWidth(dp(2));
                    c.drawCircle(cx, cy - dp(2), Math.min(dp(19), rowH * .42f), stroke);
                }

                if (date.equals(selected)) {
                    stroke.setColor(text);
                    stroke.setStrokeWidth(dp(2));
                    c.drawCircle(cx, cy - dp(2), Math.min(dp(21), rowH * .46f), stroke);
                }

                medium(periodDays.contains(key) ? Color.WHITE : text, 13);
                centered(c, String.valueOf(day), cx, cy + dp(3), p);

                if (date.equals(LocalDate.now())) {
                    p.setColor(purple);
                    p.setStyle(Paint.Style.FILL);
                    c.drawCircle(cx, cy - dp(17), dp(2.1f), p);
                }
            }

            drawLegend(c, w);
            drawSelectedCard(c, w, h);
        }

        private void drawLegend(Canvas c, float w) {
            float y = gridBottom + dp(18);
            float x = dp(18);

            p.setStyle(Paint.Style.FILL);
            p.setColor(period);
            c.drawCircle(x + dp(5), y, dp(4), p);
            fill(muted, 10);
            c.drawText("месячные", x + dp(14), y + dp(4), p);

            float x2 = w * .39f;
            stroke.setColor(period);
            stroke.setStrokeWidth(dp(1.4f));
            c.drawCircle(x2 + dp(5), y, dp(4), stroke);
            fill(muted, 10);
            c.drawText("прогноз", x2 + dp(14), y + dp(4), p);

            float x3 = w * .70f;
            p.setColor(fertile);
            p.setStyle(Paint.Style.FILL);
            c.drawCircle(x3 + dp(5), y, dp(4), p);
            fill(muted, 10);
            c.drawText("ферт.", x3 + dp(14), y + dp(4), p);
        }

        private void drawSelectedCard(Canvas c, float w, float h) {
            float top = gridBottom + dp(37);
            float pad = dp(16);
            RectF box = new RectF(pad, top, w - pad, h - dp(16));
            if (box.height() < dp(224)) {
                top = h - dp(240);
                box.top = top;
            }
            rounded(c, box, card, 22);

            DateTimeFormatter f = DateTimeFormatter.ofPattern("d MMMM", ru);
            medium(text, 18);
            c.drawText(selected.format(f), box.left + dp(16), box.top + dp(30), p);

            fill(muted, 11);
            c.drawText(dayDescription(selected), box.left + dp(16), box.top + dp(50), p);

            float gap = dp(8);
            float innerW = box.width() - dp(32);
            float bw = (innerW - gap) / 2f;
            float bh = dp(42);
            float x1 = box.left + dp(16);
            float x2 = x1 + bw + gap;
            float y1 = box.top + dp(65);
            float y2 = y1 + bh + gap;
            float y3 = y2 + bh + gap;

            periodBtn.set(x1, y1, x1 + bw, y1 + bh);
            flowBtn.set(x2, y1, x2 + bw, y1 + bh);
            painBtn.set(x1, y2, x1 + bw, y2 + bh);
            moodBtn.set(x2, y2, x2 + bw, y2 + bh);
            cycleBtn.set(x1, y3, x1 + bw, y3 + bh);
            durationBtn.set(x2, y3, x2 + bw, y3 + bh);

            drawButton(c, periodBtn,
                    periodDays.contains(selected.toString()) ? "Месячные ✓" : "Отметить месячные",
                    periodDays.contains(selected.toString()) ? period : card2);

            int flow = prefs.getInt("flow_" + selected, 0);
            String[] flows = {"Интенсивность —", "Лёгкие", "Средние", "Обильные"};
            drawButton(c, flowBtn, flows[Math.max(0, Math.min(3, flow))], card2);

            boolean pain = prefs.getBoolean("pain_" + selected, false);
            drawButton(c, painBtn, pain ? "Боль ✓" : "Боль —", pain ? Color.rgb(82, 57, 94) : card2);

            int mood = prefs.getInt("mood_" + selected, 0);
            String[] moods = {"Настроение 🙂", "Настроение 😐", "Настроение 😔"};
            drawButton(c, moodBtn, moods[Math.max(0, Math.min(2, mood))], card2);

            drawButton(c, cycleBtn, "Цикл: " + prefs.getInt("cycle_length", 28) + " д.", card2);
            drawButton(c, durationBtn, "Месячные: " + prefs.getInt("period_length", 5) + " д.", card2);

            fill(muted, 9.5f);
            String hint = "Нажми на день в календаре, чтобы внести запись";
            c.drawText(hint, box.left + dp(16), Math.min(box.bottom - dp(10), y3 + bh + dp(22)), p);
        }

        private void drawButton(Canvas c, RectF r, String label, int color) {
            rounded(c, r, color, 13);
            medium(text, label.length() > 17 ? 10.5f : 11.5f);
            centered(c, label, r.centerX(), r.centerY() + dp(4), p);
        }

        private String statusFor(LocalDate date) {
            if (periodDays.contains(date.toString())) {
                return "Сегодня месячные";
            }
            LocalDate next = nextPredictedStartAfter(date.minusDays(1));
            if (next != null) {
                long diff = ChronoUnit.DAYS.between(date, next);
                if (diff == 0) return "Месячные ожидаются сегодня";
                if (diff > 0) return "До месячных " + diff + " д.";
            }
            return "Добавь последние месячные";
        }

        private String secondaryStatus(LocalDate date) {
            LocalDate last = lastActualStartOnOrBefore(date);
            if (last != null) {
                long day = ChronoUnit.DAYS.between(last, date) + 1;
                if (day > 0 && day < 100) {
                    return day + "-й день цикла  •  " + phaseFor(date);
                }
            }
            return "Прогноз появится после первой отметки";
        }

        private String dayDescription(LocalDate date) {
            String key = date.toString();
            if (periodDays.contains(key)) return "Отмечено: месячные";
            if (predictedPeriodDays().contains(key)) return "Прогноз месячных";
            if (ovulationDays().contains(key)) return "Предполагаемая овуляция";
            if (fertileDays().contains(key)) return "Предполагаемое фертильное окно";
            return phaseFor(date);
        }

        private String phaseFor(LocalDate date) {
            LocalDate last = lastActualStartOnOrBefore(date);
            if (last == null) return "Нет данных о цикле";
            int cycle = averageCycle();
            long cd = ChronoUnit.DAYS.between(last, date) + 1;
            if (cd <= 0 || cd > cycle + 10L) return "Вне текущего прогноза";
            if (cd <= prefs.getInt("period_length", 5)) return "Менструальная фаза";
            int ovDay = Math.max(8, cycle - 14);
            if (cd < ovDay - 5L) return "Фолликулярная фаза";
            if (cd <= ovDay + 1L) return "Фертильное окно";
            return "Лютеиновая фаза";
        }

        private List<LocalDate> actualStarts() {
            List<LocalDate> dates = new ArrayList<>();
            for (String s : periodDays) {
                try {
                    dates.add(LocalDate.parse(s));
                } catch (Exception ignored) {
                }
            }
            Collections.sort(dates);
            List<LocalDate> starts = new ArrayList<>();
            LocalDate prev = null;
            for (LocalDate date : dates) {
                if (prev == null || !date.equals(prev.plusDays(1))) {
                    starts.add(date);
                }
                prev = date;
            }
            return starts;
        }

        private int averageCycle() {
            List<LocalDate> starts = actualStarts();
            List<Long> intervals = new ArrayList<>();
            for (int i = 1; i < starts.size(); i++) {
                long d = ChronoUnit.DAYS.between(starts.get(i - 1), starts.get(i));
                if (d >= 14 && d <= 60) intervals.add(d);
            }
            if (intervals.isEmpty()) return prefs.getInt("cycle_length", 28);

            int from = Math.max(0, intervals.size() - 6);
            long sum = 0;
            for (int i = from; i < intervals.size(); i++) sum += intervals.get(i);
            return (int) Math.round(sum / (double) (intervals.size() - from));
        }

        private LocalDate lastActualStartOnOrBefore(LocalDate date) {
            LocalDate result = null;
            for (LocalDate s : actualStarts()) {
                if (!s.isAfter(date)) result = s;
            }
            return result;
        }

        private List<LocalDate> predictedStarts() {
            List<LocalDate> starts = actualStarts();
            if (starts.isEmpty()) return Collections.emptyList();
            LocalDate base = starts.get(starts.size() - 1);
            int cycle = averageCycle();
            List<LocalDate> result = new ArrayList<>();
            LocalDate next = base.plusDays(cycle);
            for (int i = 0; i < 6; i++) {
                result.add(next);
                next = next.plusDays(cycle);
            }
            return result;
        }

        private LocalDate nextPredictedStartAfter(LocalDate date) {
            for (LocalDate p : predictedStarts()) {
                if (p.isAfter(date)) return p;
            }
            return null;
        }

        private Set<String> predictedPeriodDays() {
            Set<String> set = new HashSet<>();
            int len = prefs.getInt("period_length", 5);
            for (LocalDate start : predictedStarts()) {
                for (int i = 0; i < len; i++) set.add(start.plusDays(i).toString());
            }
            return set;
        }

        private Set<String> ovulationDays() {
            Set<String> set = new HashSet<>();
            int cycle = averageCycle();
            List<LocalDate> actual = actualStarts();
            if (!actual.isEmpty()) {
                LocalDate last = actual.get(actual.size() - 1);
                set.add(last.plusDays(Math.max(7, cycle - 14)).toString());
            }
            for (LocalDate nextPeriod : predictedStarts()) {
                set.add(nextPeriod.minusDays(14).toString());
            }
            return set;
        }

        private Set<String> fertileDays() {
            Set<String> set = new HashSet<>();
            for (String ov : ovulationDays()) {
                LocalDate d = LocalDate.parse(ov);
                for (int i = 5; i >= 0; i--) set.add(d.minusDays(i).toString());
                set.add(d.plusDays(1).toString());
            }
            return set;
        }

        private void savePeriodDays() {
            prefs.edit().putStringSet("period_days", new HashSet<>(periodDays)).apply();
        }

        private LocalDate dateAt(float x, float y) {
            if (x < gridLeft || x > gridLeft + cellW * 7 || y < gridTop || y > gridBottom) {
                return null;
            }
            int col = (int) ((x - gridLeft) / cellW);
            int row = (int) ((y - gridTop) / rowH);
            LocalDate first = shownMonth.atDay(1);
            int firstColumn = first.getDayOfWeek().getValue() - 1;
            int day = row * 7 + col - firstColumn + 1;
            if (day < 1 || day > shownMonth.lengthOfMonth()) return null;
            return shownMonth.atDay(day);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() != MotionEvent.ACTION_UP) return true;
            float x = event.getX();
            float y = event.getY();

            if (prevBtn.contains(x, y)) {
                shownMonth = shownMonth.minusMonths(1);
                invalidate();
                return true;
            }
            if (nextBtn.contains(x, y)) {
                shownMonth = shownMonth.plusMonths(1);
                invalidate();
                return true;
            }

            LocalDate hit = dateAt(x, y);
            if (hit != null) {
                selected = hit;
                invalidate();
                return true;
            }

            String key = selected.toString();
            if (periodBtn.contains(x, y)) {
                if (periodDays.contains(key)) {
                    periodDays.remove(key);
                } else {
                    periodDays.add(key);
                }
                savePeriodDays();
                invalidate();
                return true;
            }
            if (flowBtn.contains(x, y)) {
                int v = (prefs.getInt("flow_" + key, 0) + 1) % 4;
                prefs.edit().putInt("flow_" + key, v).apply();
                invalidate();
                return true;
            }
            if (painBtn.contains(x, y)) {
                boolean v = !prefs.getBoolean("pain_" + key, false);
                prefs.edit().putBoolean("pain_" + key, v).apply();
                invalidate();
                return true;
            }
            if (moodBtn.contains(x, y)) {
                int v = (prefs.getInt("mood_" + key, 0) + 1) % 3;
                prefs.edit().putInt("mood_" + key, v).apply();
                invalidate();
                return true;
            }
            if (cycleBtn.contains(x, y)) {
                int v = prefs.getInt("cycle_length", 28) + 1;
                if (v > 45) v = 20;
                prefs.edit().putInt("cycle_length", v).apply();
                invalidate();
                return true;
            }
            if (durationBtn.contains(x, y)) {
                int v = prefs.getInt("period_length", 5) + 1;
                if (v > 10) v = 2;
                prefs.edit().putInt("period_length", v).apply();
                invalidate();
                return true;
            }

            return true;
        }
    }
}
