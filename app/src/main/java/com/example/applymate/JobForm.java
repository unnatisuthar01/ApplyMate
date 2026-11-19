package com.example.applymate;

import android.app.DatePickerDialog;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.applymate.models.JobApplication;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

final class JobForm {

    private final Context context;
    private final EditText companyInput;
    private final EditText roleInput;
    private final EditText locationInput;
    private final Spinner jobTypeSpinner;
    private final EditText salaryInput;
    private final EditText dateInput;
    private final Spinner statusSpinner;
    private final EditText descriptionInput;
    private final EditText linkInput;
    private final EditText notesInput;
    private long applicationDate;

    JobForm(Context context) {
        this.context = context;
        companyInput = findEditText(R.id.input_company_name);
        roleInput = findEditText(R.id.input_job_role);
        locationInput = findEditText(R.id.input_location);
        jobTypeSpinner = findSpinner(R.id.spinner_job_type, R.array.job_types);
        salaryInput = findEditText(R.id.input_salary);
        dateInput = findEditText(R.id.input_application_date);
        statusSpinner = findSpinner(R.id.spinner_job_status, R.array.application_statuses);
        descriptionInput = findEditText(R.id.input_description);
        linkInput = findEditText(R.id.input_job_link);
        notesInput = findEditText(R.id.input_notes);

        Calendar today = Calendar.getInstance();
        setDate(today);
        dateInput.setOnClickListener(view -> showDatePicker());
    }

    private EditText findEditText(int id) {
        return ((android.app.Activity) context).findViewById(id);
    }

    private Spinner findSpinner(int id, int optionsId) {
        Spinner spinner = ((android.app.Activity) context).findViewById(id);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                context, optionsId, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return spinner;
    }

    private void showDatePicker() {
        Calendar selectedDate = Calendar.getInstance();
        selectedDate.setTimeInMillis(applicationDate);
        new DatePickerDialog(
                context,
                (picker, year, month, day) -> {
                    Calendar date = Calendar.getInstance();
                    date.clear();
                    date.set(year, month, day);
                    setDate(date);
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void setDate(Calendar date) {
        applicationDate = date.getTimeInMillis();
        dateInput.setText(formatDate(applicationDate));
        dateInput.setError(null);
    }

    boolean validate() {
        boolean valid = true;
        if (companyInput.getText().toString().trim().isEmpty()) {
            companyInput.setError(context.getString(R.string.required_field));
            valid = false;
        }
        if (roleInput.getText().toString().trim().isEmpty()) {
            roleInput.setError(context.getString(R.string.required_field));
            valid = false;
        }
        if (jobTypeSpinner.getSelectedItemPosition() == 0) {
            jobTypeSpinner.requestFocus();
            Toast.makeText(context, R.string.select_job_type, Toast.LENGTH_SHORT).show();
            valid = false;
        }
        String salary = salaryInput.getText().toString().trim();
        if (!salary.isEmpty()) {
            try {
                double value = Double.parseDouble(salary);
                if (Double.isNaN(value) || Double.isInfinite(value) || value < 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException exception) {
                salaryInput.setError(context.getString(R.string.invalid_salary));
                valid = false;
            }
        }
        String link = linkInput.getText().toString().trim();
        if (!link.isEmpty()
                && !android.util.Patterns.WEB_URL.matcher(link).matches()) {
            linkInput.setError(context.getString(R.string.invalid_job_link));
            valid = false;
        }
        return valid;
    }

    JobApplication toJobApplication(long id, long userId) {
        String salaryText = salaryInput.getText().toString().trim();
        Double salary = salaryText.isEmpty() ? null : Double.parseDouble(salaryText);
        return new JobApplication(
                id,
                userId,
                companyInput.getText().toString().trim(),
                roleInput.getText().toString().trim(),
                locationInput.getText().toString().trim(),
                jobTypeSpinner.getSelectedItemPosition() == 0
                        ? "" : jobTypeSpinner.getSelectedItem().toString(),
                salary,
                applicationDate,
                statusSpinner.getSelectedItem().toString(),
                descriptionInput.getText().toString().trim(),
                linkInput.getText().toString().trim(),
                notesInput.getText().toString().trim());
    }

    void populate(JobApplication application) {
        companyInput.setText(application.getCompanyName());
        roleInput.setText(application.getJobRole());
        locationInput.setText(application.getLocation());
        selectSpinnerItem(jobTypeSpinner, application.getJobType());
        salaryInput.setText(application.getSalary() == null
                ? "" : formatSalary(application.getSalary()));
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(application.getApplicationDate());
        setDate(date);
        selectSpinnerItem(statusSpinner, application.getStatus());
        descriptionInput.setText(application.getDescription());
        linkInput.setText(application.getJobLink());
        notesInput.setText(application.getNotes());
    }

    private static void selectSpinnerItem(Spinner spinner, String value) {
        for (int position = 0; position < spinner.getCount(); position++) {
            if (spinner.getItemAtPosition(position).toString().equals(value)) {
                spinner.setSelection(position);
                return;
            }
        }
    }

    static String formatDate(long timestamp) {
        return new SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                .format(new Date(timestamp));
    }

    static String formatSalary(double salary) {
        return String.format(Locale.getDefault(), "%,.2f", salary)
                .replaceAll("\\.?0+$", "");
    }
}
