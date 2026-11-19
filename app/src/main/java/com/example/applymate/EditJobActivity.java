package com.example.applymate;

import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.applymate.database.DatabaseHelper;
import com.example.applymate.models.JobApplication;
import com.google.android.material.button.MaterialButton;

public class EditJobActivity extends AppCompatActivity {

    public static final String EXTRA_JOB_ID = "com.example.applymate.extra.JOB_ID";

    private long userId;
    private long jobId;
    private JobForm form;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_job);
        userId = ScreenNavigation.requireUserId(this);
        if (userId < 1) {
            return;
        }
        jobId = getIntent().getLongExtra(EXTRA_JOB_ID, -1L);
        if (jobId < 1) {
            finish();
            return;
        }

        form = new JobForm(this);
        ((TextView) findViewById(R.id.text_form_title)).setText(R.string.edit_application_title);
        ((MaterialButton) findViewById(R.id.button_save_job)).setText(R.string.update_application);
        findViewById(R.id.button_save_job).setOnClickListener(view -> updateApplication());
        loadApplication();
    }

    private void loadApplication() {
        try (DatabaseHelper database = new DatabaseHelper(this)) {
            JobApplication application = database.getJobApplication(jobId, userId);
            if (application == null) {
                Toast.makeText(this, R.string.application_not_found, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            form.populate(application);
        } catch (SQLiteException exception) {
            Toast.makeText(this, R.string.database_error, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void updateApplication() {
        if (!form.validate()) {
            return;
        }
        try (DatabaseHelper database = new DatabaseHelper(this)) {
            boolean updated = database.updateJobApplication(form.toJobApplication(jobId, userId));
            if (!updated) {
                Toast.makeText(this, R.string.application_not_found, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            Toast.makeText(this, R.string.application_updated, Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } catch (SQLiteException exception) {
            Toast.makeText(this, R.string.application_save_failed, Toast.LENGTH_LONG).show();
        }
    }
}
