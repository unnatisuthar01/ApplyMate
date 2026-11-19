package com.example.applymate;

import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.applymate.database.DatabaseHelper;
import com.example.applymate.models.Reminder;
import com.google.android.material.card.MaterialCardView;

import java.util.List;
import java.util.Map;

public class StatisticsActivity extends AppCompatActivity {

    private long userId;
    private ApplicationPieChartView pieChart;
    private MonthlyApplicationsChartView monthlyChart;
    private LinearLayout upcomingContainer;
    private TextView noUpcoming;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        ScreenInsets.apply(this);
        userId = ScreenNavigation.requireUserId(this);
        if (userId < 1) {
            return;
        }
        pieChart = findViewById(R.id.chart_application_status);
        monthlyChart = findViewById(R.id.chart_monthly_applications);
        upcomingContainer = findViewById(R.id.statistics_upcoming_reminders);
        noUpcoming = findViewById(R.id.text_statistics_no_reminders);
        findViewById(R.id.button_view_all_reminders)
                .setOnClickListener(view -> openReminders());
        ScreenNavigation.configureBottomNavigation(this, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userId > 0 && pieChart != null) {
            loadStatistics();
        }
    }

    private void loadStatistics() {
        try (DatabaseHelper database = new DatabaseHelper(this)) {
            Map<String, Integer> counts = database.getApplicationCounts(userId);
            setCount(R.id.card_stat_total, R.string.total_applications, counts.get("Total"));
            setCount(R.id.card_stat_applied, R.string.status_applied, counts.get("Applied"));
            setCount(R.id.card_stat_pending, R.string.status_pending, counts.get("Pending"));
            setCount(R.id.card_stat_interview, R.string.status_interview, counts.get("Interview"));
            setCount(R.id.card_stat_selected, R.string.status_selected, counts.get("Selected"));
            setCount(R.id.card_stat_rejected, R.string.status_rejected, counts.get("Rejected"));
            pieChart.setValues(new int[]{
                    counts.get("Saved"), counts.get("Applied"), counts.get("Pending"),
                    counts.get("Interview"), counts.get("Selected"), counts.get("Rejected")
            });
            monthlyChart.setValues(database.getMonthlyApplicationCounts(userId, 6));
            showUpcomingReminders(database.getUpcomingReminders(userId, 3));
        } catch (SQLiteException exception) {
            Toast.makeText(this, R.string.database_error, Toast.LENGTH_LONG).show();
        }
    }

    private void setCount(int cardId, int labelId, Integer count) {
        MaterialCardView card = findViewById(cardId);
        ((TextView) card.findViewById(R.id.text_status_name)).setText(labelId);
        ((TextView) card.findViewById(R.id.text_status_count))
                .setText(String.valueOf(count == null ? 0 : count));
    }

    private void showUpcomingReminders(List<Reminder> reminders) {
        upcomingContainer.removeAllViews();
        noUpcoming.setVisibility(reminders.isEmpty() ? View.VISIBLE : View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Reminder reminder : reminders) {
            View row = inflater.inflate(R.layout.item_reminder_summary, upcomingContainer, false);
            ((TextView) row.findViewById(R.id.text_reminder_summary_title))
                    .setText(reminder.getTitle());
            ((TextView) row.findViewById(R.id.text_reminder_summary_time))
                    .setText(getString(R.string.reminder_date_time,
                            JobForm.formatDate(reminder.getDate()), reminder.getTime()));
            upcomingContainer.addView(row);
        }
    }

    private void openReminders() {
        startActivity(new Intent(this, ReminderActivity.class));
    }
}
