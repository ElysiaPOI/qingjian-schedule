package com.elysiapoi.tianyangschedule;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public final class TodayWidgetProvider extends AppWidgetProvider {
    private static final String PREFS = "qingjian_widget";
    private static final String KEY_SCHEDULE = "schedule";
    private static final String[] START_TIMES = {
            "08:00", "08:50", "10:05", "10:55",
            "13:30", "14:20", "15:35", "16:25",
            "18:00", "18:50", "20:05", "20:55"
    };
    private static final String[] END_TIMES = {
            "08:45", "09:35", "10:50", "11:40",
            "14:15", "15:05", "16:20", "17:10",
            "18:45", "19:35", "20:50", "21:40"
    };
    private static final int[] ROW_IDS = {
            R.id.widget_row_1, R.id.widget_row_2, R.id.widget_row_3
    };
    private static final int[] TIME_IDS = {
            R.id.widget_time_1, R.id.widget_time_2, R.id.widget_time_3
    };
    private static final int[] NAME_IDS = {
            R.id.widget_name_1, R.id.widget_name_2, R.id.widget_name_3
    };
    private static final int[] ROOM_IDS = {
            R.id.widget_room_1, R.id.widget_room_2, R.id.widget_room_3
    };

    static void saveSchedule(Context context, String json) {
        if (!json.isEmpty()) {
            try {
                JSONObject value = new JSONObject(json);
                if (!value.has("startsOn") || !value.has("courses")) return;
            } catch (JSONException ignored) {
                return;
            }
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_SCHEDULE, json).apply();
    }

    static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, TodayWidgetProvider.class));
        if (ids.length > 0) update(context, manager, ids);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        update(context, manager, appWidgetIds);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (Intent.ACTION_DATE_CHANGED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            updateAll(context);
        }
    }

    private static void update(Context context, AppWidgetManager manager, int[] ids) {
        String json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_SCHEDULE, "");
        Calendar today = Calendar.getInstance();
        int weekday = (today.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1;
        String dayText = String.format(Locale.CHINA, "%d月%d日  周%s",
                today.get(Calendar.MONTH) + 1, today.get(Calendar.DAY_OF_MONTH),
                "一二三四五六日".substring(weekday - 1, weekday));
        List<Lesson> lessons = todayLessons(json, today);
        String state = !hasRealSchedule(json) ? "打开应用导入课表"
                : lessons.isEmpty() ? "今天没有课程，轻松一下" : "";
        String summary = lessons.isEmpty() ? "" : "今天 " + lessons.size() + " 门课";

        Intent launch = new Intent(context, MainActivity.class);
        launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent open = PendingIntent.getActivity(context, 0, launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        for (int id : ids) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_today);
            views.setTextViewText(R.id.widget_date, dayText);
            views.setTextViewText(R.id.widget_count, summary);
            views.setTextViewText(R.id.widget_empty, state);
            views.setViewVisibility(R.id.widget_empty, lessons.isEmpty() ? View.VISIBLE : View.GONE);
            for (int i = 0; i < ROW_IDS.length; i++) {
                if (i < lessons.size()) {
                    Lesson lesson = lessons.get(i);
                    views.setViewVisibility(ROW_IDS[i], View.VISIBLE);
                    views.setTextViewText(TIME_IDS[i], lesson.time);
                    views.setTextViewText(NAME_IDS[i], lesson.name);
                    views.setTextViewText(ROOM_IDS[i], lesson.room);
                    views.setViewVisibility(ROOM_IDS[i],
                            lesson.room.isEmpty() ? View.GONE : View.VISIBLE);
                } else {
                    views.setViewVisibility(ROW_IDS[i], View.GONE);
                }
            }
            views.setTextViewText(R.id.widget_more,
                    lessons.size() > ROW_IDS.length ? "还有 " + (lessons.size() - ROW_IDS.length) + " 门，打开查看" : "");
            views.setViewVisibility(R.id.widget_more,
                    lessons.size() > ROW_IDS.length ? View.VISIBLE : View.GONE);
            views.setOnClickPendingIntent(R.id.widget_root, open);
            manager.updateAppWidget(id, views);
        }
    }

    private static boolean hasRealSchedule(String json) {
        if (json == null || json.isEmpty()) return false;
        try {
            JSONObject schedule = new JSONObject(json);
            JSONArray courses = schedule.optJSONArray("courses");
            if (courses == null) return false;
            if (!"sample".equals(schedule.optString("source"))) return true;
            for (int i = 0; i < courses.length(); i++) {
                JSONObject course = courses.optJSONObject(i);
                if (course != null && course.optBoolean("custom")) return true;
            }
        } catch (JSONException ignored) {
            return false;
        }
        return false;
    }

    static List<Lesson> todayLessons(String json, Calendar today) {
        List<Lesson> result = new ArrayList<>();
        if (json == null || json.isEmpty()) return result;
        try {
            JSONObject schedule = new JSONObject(json);
            boolean sample = "sample".equals(schedule.optString("source"));
            String startsOn = schedule.optString("startsOn");
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
            format.setLenient(false);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date start = format.parse(startsOn);
            if (start == null || !format.format(start).equals(startsOn)) return result;
            Calendar dayUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            dayUtc.clear();
            dayUtc.set(today.get(Calendar.YEAR), today.get(Calendar.MONTH),
                    today.get(Calendar.DAY_OF_MONTH));
            long days = TimeUnit.MILLISECONDS.toDays(dayUtc.getTimeInMillis() - start.getTime());
            if (days < 0) return result;
            int week = (int) (days / 7) + 1;
            int weekday = (today.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1;
            JSONArray courses = schedule.optJSONArray("courses");
            if (courses == null) return result;
            for (int i = 0; i < courses.length(); i++) {
                JSONObject course = courses.optJSONObject(i);
                if (course == null || !contains(course.optJSONArray("weeks"), week)) continue;
                if (sample && !course.optBoolean("custom")) continue;
                JSONObject adjustment = null;
                JSONArray overrides = course.optJSONArray("overrides");
                if (overrides != null) {
                    for (int j = 0; j < overrides.length(); j++) {
                        JSONObject candidate = overrides.optJSONObject(j);
                        if (candidate != null && candidate.optInt("week") == week) {
                            adjustment = candidate;
                            break;
                        }
                    }
                }
                if (adjustment != null && adjustment.optBoolean("cancelled")) continue;
                int day = adjustment == null ? course.optInt("day") :
                        adjustment.optInt("day", course.optInt("day"));
                if (day != weekday) continue;
                int section = adjustment == null ? course.optInt("startSection") :
                        adjustment.optInt("startSection", course.optInt("startSection"));
                int end = adjustment == null ? course.optInt("endSection") :
                        adjustment.optInt("endSection", course.optInt("endSection"));
                String name = course.optString("name").trim();
                if (section < 1 || section > 12 || end < section || name.isEmpty()) continue;
                String room = adjustment == null ? course.optString("room") :
                        adjustment.optString("room", course.optString("room"));
                result.add(new Lesson(section, name,
                        timeForSections(section, end), room.trim()));
            }
            result.sort(Comparator.comparingInt(lesson -> lesson.section));
        } catch (Exception ignored) {
            result.clear();
        }
        return result;
    }

    private static boolean contains(JSONArray values, int target) {
        if (values == null) return false;
        for (int i = 0; i < values.length(); i++) {
            if (values.optInt(i) == target) return true;
        }
        return false;
    }

    private static String timeForSections(int start, int end) {
        return START_TIMES[start - 1] + "–" + END_TIMES[end - 1];
    }

    static final class Lesson {
        final int section;
        final String name;
        final String time;
        final String room;

        Lesson(int section, String name, String time, String room) {
            this.section = section;
            this.name = name;
            this.time = time;
            this.room = room;
        }
    }
}
