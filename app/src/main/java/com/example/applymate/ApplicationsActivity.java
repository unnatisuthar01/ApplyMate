package com.example.applymate;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.applymate.database.DatabaseHelper;
import com.example.applymate.database.DatabaseHelper.ApplicationSortOrder;
import com.example.applymate.models.JobApplication;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class ApplicationsActivity extends AppCompatActivity {

    public static final String EXTRA_STATUS_FILTER =
            "com.example.applymate.extra.STATUS_FILTER";

    private long userId;
    private String statusFilter;
    private String searchQuery = "";
    private JobAdapter adapter;
    private TextView title;
    private TextView subtitle;
    private TextView emptyState;
    private View emptyContainer;
    private View clearFiltersButton;
    private ChipGroup statusChipGroup;
    private Spinner sortSpinner;
    private boolean filtersReady;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_applications);
        ScreenInsets.apply(this);
        userId = ScreenNavigation.requireUserId(this);
        if (userId < 1) {
            return;
        }

        title = findViewById(R.id.text_list_title);
        subtitle = findViewById(R.id.text_list_subtitle);
        emptyState = findViewById(R.id.text_empty_applications);
        emptyContainer = findViewById(R.id.layout_empty_applications);
        statusChipGroup = findViewById(R.id.chip_group_status);
        sortSpinner = findViewById(R.id.spinner_application_sort);
        clearFiltersButton = findViewById(R.id.button_clear_filters);

        RecyclerView recyclerView = findViewById(R.id.recycler_applications);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new JobAdapter(this::openJobDetails);
        recyclerView.setAdapter(adapter);

        MaterialButton addButton = findViewById(R.id.button_add_application);
        addButton.setOnClickListener(view ->
                startActivity(new Intent(this, AddJobActivity.class)));

        MaterialButton emptyClearButton = findViewById(R.id.button_empty_clear_filters);
        emptyClearButton.setOnClickListener(view -> clearFilters());
        clearFiltersButton.setOnClickListener(view -> clearFilters());

        configureSortSpinner();
        configureStatusFilters();
        configureSearch();
        filtersReady = true;
        ScreenNavigation.configureBottomNavigation(this, true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null && userId > 0) {
            loadApplications();
        }
    }

    private void configureSearch() {
        TextInputEditText searchInput = findViewById(R.id.input_search_applications);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence value, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence value, int start, int before, int count) {
                if (!filtersReady) {
                    return;
                }
                searchQuery = value.toString();
                loadApplications();
            }

            @Override
            public void afterTextChanged(Editable value) {
            }
        });
    }

    private void configureStatusFilters() {
        statusFilter = getIntent().getStringExtra(EXTRA_STATUS_FILTER);
        int initialChipId = chipIdForStatus(statusFilter);
        if (initialChipId == R.id.chip_filter_all) {
            statusFilter = null;
        }
        statusChipGroup.check(initialChipId);
        updateHeader();

        statusChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!filtersReady || checkedIds.isEmpty()) {
                return;
            }
            statusFilter = statusForChip(checkedIds.get(0));
            updateHeader();
            loadApplications();
        });
    }

    private void configureSortSpinner() {
        ArrayAdapter<CharSequence> sortAdapter = ArrayAdapter.createFromResource(
                this, R.array.application_sort_options, android.R.layout.simple_spinner_item);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(sortAdapter);
        sortSpinner.setSelection(0, false);
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(
                    AdapterView<?> parent, View view, int position, long id) {
                if (filtersReady) {
                    loadApplications();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // The spinner always has a selected sort order.
            }
        });
    }

    private void clearFilters() {
        filtersReady = false;
        searchQuery = "";
        TextInputEditText searchInput = findViewById(R.id.input_search_applications);
        InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
        searchInput.clearFocus();
        searchInput.setText("");
        statusFilter = null;
        statusChipGroup.check(R.id.chip_filter_all);
        sortSpinner.setSelection(0, false);
        filtersReady = true;
        updateHeader();
        loadApplications();
    }

    private void loadApplications() {
        if (!filtersReady) {
            return;
        }
        try (DatabaseHelper database = new DatabaseHelper(this)) {
            List<JobApplication> applications = database.searchJobApplications(
                    userId,
                    searchQuery,
                    statusFilter,
                    selectedSortOrder(),
                    0);
            adapter.submitList(applications);
            boolean hasResults = !applications.isEmpty();
            emptyContainer.setVisibility(hasResults ? View.GONE : View.VISIBLE);
            emptyState.setText(hasActiveFilters()
                    ? R.string.no_results_found : R.string.no_applications_yet);
            clearFiltersButton.setVisibility(hasActiveFilters() ? View.VISIBLE : View.GONE);
        } catch (SQLiteException exception) {
            Toast.makeText(this, R.string.database_error, Toast.LENGTH_LONG).show();
        }
    }

    private boolean hasActiveFilters() {
        return !searchQuery.trim().isEmpty()
                || statusFilter != null
                || sortSpinner.getSelectedItemPosition() != 0;
    }

    private ApplicationSortOrder selectedSortOrder() {
        switch (sortSpinner.getSelectedItemPosition()) {
            case 1:
                return ApplicationSortOrder.OLDEST_FIRST;
            case 2:
                return ApplicationSortOrder.COMPANY_NAME_ASC;
            case 0:
            default:
                return ApplicationSortOrder.NEWEST_FIRST;
        }
    }

    private void updateHeader() {
        if (statusFilter == null) {
            title.setText(R.string.all_applications);
            subtitle.setText(R.string.dashboard_subtitle);
        } else {
            title.setText(statusFilter);
            subtitle.setText(getString(R.string.application_filter, statusFilter));
        }
    }

    private int chipIdForStatus(String status) {
        if (getString(R.string.status_saved).equals(status)) {
            return R.id.chip_filter_saved;
        }
        if (getString(R.string.status_applied).equals(status)) {
            return R.id.chip_filter_applied;
        }
        if (getString(R.string.status_pending).equals(status)) {
            return R.id.chip_filter_pending;
        }
        if (getString(R.string.status_interview).equals(status)) {
            return R.id.chip_filter_interview;
        }
        if (getString(R.string.status_selected).equals(status)) {
            return R.id.chip_filter_selected;
        }
        if (getString(R.string.status_rejected).equals(status)) {
            return R.id.chip_filter_rejected;
        }
        return R.id.chip_filter_all;
    }

    private String statusForChip(int chipId) {
        if (chipId == R.id.chip_filter_saved) {
            return getString(R.string.status_saved);
        }
        if (chipId == R.id.chip_filter_applied) {
            return getString(R.string.status_applied);
        }
        if (chipId == R.id.chip_filter_pending) {
            return getString(R.string.status_pending);
        }
        if (chipId == R.id.chip_filter_interview) {
            return getString(R.string.status_interview);
        }
        if (chipId == R.id.chip_filter_selected) {
            return getString(R.string.status_selected);
        }
        if (chipId == R.id.chip_filter_rejected) {
            return getString(R.string.status_rejected);
        }
        return null;
    }

    private void openJobDetails(JobApplication application) {
        Intent intent = new Intent(this, JobDetailsActivity.class);
        intent.putExtra(JobDetailsActivity.EXTRA_JOB_ID, application.getId());
        startActivity(intent);
    }
}
