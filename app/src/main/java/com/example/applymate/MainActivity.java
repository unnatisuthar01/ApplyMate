package com.example.applymate;

import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.applymate.database.DatabaseHelper;
import com.example.applymate.models.JobApplication;
import com.example.applymate.models.User;
import com.example.applymate.utils.SessionManager;
import com.google.android.material.card.MaterialCardView;

import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private long userId;
    private TextView greeting;
    private TextView totalCount;
    private LinearLayout recentContainer;
    private TextView emptyRecent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userId = ScreenNavigation.requireUserId(this);
        if (userId < 1) {
            return;
        }

        greeting = findViewById(R.id.text_greeting);
        totalCount = findViewById(R.id.count_total);
        recentContainer = findViewById(R.id.recent_applications_container);
        emptyRecent = findViewById(R.id.text_recent_empty);

        Button addButton = findViewById(R.id.button_add_application);
        Button logoutButton = findViewById(R.id.button_logout);
        addButton.setOnClickListener(view ->
                startActivity(new Intent(this, AddJobActivity.class)));
        logoutButton.setOnClickListener(view -> {
            new SessionManager(this).clearSession();
            Intent welcomeIntent = new Intent(this, WelcomeActivity.class);
            welcomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(welcomeIntent);
            finish();
        });

        MaterialCardView totalCard = findViewById(R.id.card_total);
        totalCard.setOnClickListener(view -> openApplications(null));
        findViewById(R.id.link_all_applications).setOnClickListener(
                view -> openApplications(null));
        bindStatusCard(R.id.card_saved, "Saved", R.string.status_saved);
        bindStatusCard(R.id.card_applied, "Applied", R.string.status_applied);
        bindStatusCard(R.id.card_interview, "Interview", R.string.status_interview);
        bindStatusCard(R.id.card_selected, "Selected", R.string.status_selected);
        bindStatusCard(R.id.card_rejected, "Rejected", R.string.status_rejected);
        ScreenNavigation.configureBottomNavigation(this, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (greeting != null && userId > 0) {
            loadDashboard();
        }
    }

    private void loadDashboard() {
        try (DatabaseHelper database = new DatabaseHelper(this)) {
            User user = database.getUserById(userId);
            if (user == null) {
                new SessionManager(this).clearSession();
                Intent welcomeIntent = new Intent(this, WelcomeActivity.class);
                welcomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(welcomeIntent);
                finish();
                return;
            }

            String firstName = user.getName().trim().split("\\s+")[0];
            greeting.setText(getString(R.string.dashboard_greeting, firstName));
            Map<String, Integer> counts = database.getApplicationCounts(userId);
            totalCount.setText(String.valueOf(counts.get("Total")));
            setStatusCount(R.id.card_saved, counts.get("Saved"));
            setStatusCount(R.id.card_applied, counts.get("Applied"));
            setStatusCount(R.id.card_interview, counts.get("Interview"));
            setStatusCount(R.id.card_selected, counts.get("Selected"));
            setStatusCount(R.id.card_rejected, counts.get("Rejected"));
            showRecentApplications(database.getJobApplications(userId, null, 5));
        } catch (SQLiteException exception) {
            Toast.makeText(this, R.string.database_error, Toast.LENGTH_LONG).show();
        }
    }

    private void bindStatusCard(int cardId, String status, int labelResource) {
        MaterialCardView card = findViewById(cardId);
        ((TextView) card.findViewById(R.id.text_status_name)).setText(labelResource);
        card.setOnClickListener(view -> openApplications(status));
    }

    private void setStatusCount(int cardId, Integer count) {
        MaterialCardView card = findViewById(cardId);
        ((TextView) card.findViewById(R.id.text_status_count))
                .setText(String.valueOf(count == null ? 0 : count));
    }

    private void showRecentApplications(List<JobApplication> applications) {
        recentContainer.removeAllViews();
        emptyRecent.setVisibility(applications.isEmpty() ? View.VISIBLE : View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (JobApplication application : applications) {
            View row = inflater.inflate(R.layout.item_job_application, recentContainer, false);
            JobAdapter.bind(row, application);
            row.setOnClickListener(view -> openJobDetails(application.getId()));
            recentContainer.addView(row);
        }
    }

    private void openApplications(String status) {
        Intent intent = new Intent(this, ApplicationsActivity.class);
        if (status != null) {
            intent.putExtra(ApplicationsActivity.EXTRA_STATUS_FILTER, status);
        }
        startActivity(intent);
    }

    private void openJobDetails(long jobId) {
        Intent intent = new Intent(this, JobDetailsActivity.class);
        intent.putExtra(JobDetailsActivity.EXTRA_JOB_ID, jobId);
        startActivity(intent);
    }
}
