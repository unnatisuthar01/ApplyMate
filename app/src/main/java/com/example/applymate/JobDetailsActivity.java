package com.example.applymate;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.applymate.database.DatabaseHelper;
import com.example.applymate.models.JobApplication;

public class JobDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_JOB_ID = "com.example.applymate.extra.JOB_ID";

    private long userId;
    private long jobId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_details);
        ScreenInsets.apply(this);
        userId = ScreenNavigation.requireUserId(this);
        if (userId < 1) {
            return;
        }
        jobId = getIntent().getLongExtra(EXTRA_JOB_ID, -1L);
        if (jobId < 1) {
            finish();
            return;
        }

        findViewById(R.id.button_edit_job).setOnClickListener(view -> editApplication());
        findViewById(R.id.button_delete_job).setOnClickListener(view -> confirmDelete());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userId > 0 && jobId > 0) {
            loadApplication();
        }
    }

    private void loadApplication() {
        try (DatabaseHelper database = new DatabaseHelper(this)) {
            JobApplication application = database.getJobApplication(jobId, userId);
            if (application == null) {
                Toast.makeText(this, R.string.application_not_found, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            setText(R.id.text_detail_company, application.getCompanyName());
            setText(R.id.text_detail_role, application.getJobRole());
            setText(R.id.text_detail_status, application.getStatus());
            setText(R.id.text_detail_date, JobForm.formatDate(application.getApplicationDate()));
            setText(R.id.text_detail_type, optionalValue(application.getJobType()));
            setText(R.id.text_detail_location, optionalValue(application.getLocation()));
            setText(R.id.text_detail_salary, application.getSalary() == null
                    ? getString(R.string.unknown_value)
                    : getString(R.string.salary_value,
                            JobForm.formatSalary(application.getSalary())));
            setText(R.id.text_detail_description, optionalValue(application.getDescription()));
            setText(R.id.text_detail_link, optionalValue(application.getJobLink()));
            setText(R.id.text_detail_notes, optionalValue(application.getNotes()));
        } catch (SQLiteException exception) {
            Toast.makeText(this, R.string.database_error, Toast.LENGTH_LONG).show();
        }
    }

    private void editApplication() {
        Intent intent = new Intent(this, EditJobActivity.class);
        intent.putExtra(EditJobActivity.EXTRA_JOB_ID, jobId);
        startActivity(intent);
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.delete_application_confirmation)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.confirm_delete, (dialog, which) -> deleteApplication())
                .show();
    }

    private void deleteApplication() {
        try (DatabaseHelper database = new DatabaseHelper(this)) {
            if (database.deleteJobApplication(jobId, userId)) {
                Toast.makeText(this, R.string.application_deleted, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, R.string.application_not_found, Toast.LENGTH_LONG).show();
                finish();
            }
        } catch (SQLiteException exception) {
            Toast.makeText(this, R.string.application_save_failed, Toast.LENGTH_LONG).show();
        }
    }

    private void setText(int viewId, String value) {
        ((TextView) findViewById(viewId)).setText(value);
    }

    private String optionalValue(String value) {
        return value == null || value.trim().isEmpty()
                ? getString(R.string.unknown_value) : value;
    }
}
