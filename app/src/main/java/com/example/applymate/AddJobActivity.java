package com.example.applymate;

import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.applymate.database.DatabaseHelper;
import com.google.android.material.button.MaterialButton;

public class AddJobActivity extends AppCompatActivity {

    private long userId;
    private JobForm form;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_job);
        ScreenInsets.apply(this);
        userId = ScreenNavigation.requireUserId(this);
        if (userId < 1) {
            return;
        }

        form = new JobForm(this);
        ((TextView) findViewById(R.id.text_form_title)).setText(R.string.add_application_title);
        ((MaterialButton) findViewById(R.id.button_save_job)).setText(R.string.save_application);
        findViewById(R.id.button_save_job).setOnClickListener(view -> saveApplication());
    }

    private void saveApplication() {
        if (!form.validate()) {
            return;
        }
        try (DatabaseHelper database = new DatabaseHelper(this)) {
            database.insertJobApplication(form.toJobApplication(0, userId));
            Toast.makeText(this, R.string.application_saved, Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } catch (SQLiteException exception) {
            Toast.makeText(this, R.string.application_save_failed, Toast.LENGTH_LONG).show();
        }
    }
}
