package com.example.applymate;

import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.applymate.database.DatabaseHelper;
import com.example.applymate.models.JobApplication;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class ApplicationsActivity extends AppCompatActivity {

    public static final String EXTRA_STATUS_FILTER =
            "com.example.applymate.extra.STATUS_FILTER";

    private long userId;
    private String statusFilter;
    private JobAdapter adapter;
    private TextView emptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_applications);
        userId = ScreenNavigation.requireUserId(this);
        if (userId < 1) {
            return;
        }

        statusFilter = getIntent().getStringExtra(EXTRA_STATUS_FILTER);
        TextView title = findViewById(R.id.text_list_title);
        TextView subtitle = findViewById(R.id.text_list_subtitle);
        emptyState = findViewById(R.id.text_empty_applications);
        if (statusFilter == null || statusFilter.trim().isEmpty()) {
            title.setText(R.string.all_applications);
            subtitle.setText(R.string.dashboard_subtitle);
        } else {
            title.setText(statusFilter);
            subtitle.setText(getString(R.string.application_filter, statusFilter));
        }

        RecyclerView recyclerView = findViewById(R.id.recycler_applications);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new JobAdapter(this::openJobDetails);
        recyclerView.setAdapter(adapter);
        MaterialButton addButton = findViewById(R.id.button_add_application);
        addButton.setOnClickListener(view ->
                startActivity(new Intent(this, AddJobActivity.class)));
        ScreenNavigation.configureBottomNavigation(this, true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null && userId > 0) {
            loadApplications();
        }
    }

    private void loadApplications() {
        try (DatabaseHelper database = new DatabaseHelper(this)) {
            List<JobApplication> applications =
                    database.getJobApplications(userId, statusFilter, 0);
            adapter.submitList(applications);
            emptyState.setVisibility(applications.isEmpty() ? View.VISIBLE : View.GONE);
        } catch (SQLiteException exception) {
            Toast.makeText(this, R.string.database_error, Toast.LENGTH_LONG).show();
        }
    }

    private void openJobDetails(JobApplication application) {
        Intent intent = new Intent(this, JobDetailsActivity.class);
        intent.putExtra(JobDetailsActivity.EXTRA_JOB_ID, application.getId());
        startActivity(intent);
    }
}
