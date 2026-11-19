package com.example.applymate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.applymate.models.JobApplication;

final class JobAdapter extends ListAdapter<JobApplication, JobAdapter.JobViewHolder> {

    interface OnJobClickListener {
        void onJobClick(JobApplication application);
    }

    private static final DiffUtil.ItemCallback<JobApplication> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<JobApplication>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull JobApplication oldItem, @NonNull JobApplication newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull JobApplication oldItem, @NonNull JobApplication newItem) {
                    return oldItem.getUserId() == newItem.getUserId()
                            && java.util.Objects.equals(
                                    oldItem.getCompanyName(), newItem.getCompanyName())
                            && java.util.Objects.equals(
                                    oldItem.getJobRole(), newItem.getJobRole())
                            && java.util.Objects.equals(
                                    oldItem.getLocation(), newItem.getLocation())
                            && java.util.Objects.equals(
                                    oldItem.getJobType(), newItem.getJobType())
                            && java.util.Objects.equals(
                                    oldItem.getSalary(), newItem.getSalary())
                            && oldItem.getApplicationDate() == newItem.getApplicationDate()
                            && java.util.Objects.equals(
                                    oldItem.getStatus(), newItem.getStatus())
                            && java.util.Objects.equals(
                                    oldItem.getDescription(), newItem.getDescription())
                            && java.util.Objects.equals(
                                    oldItem.getJobLink(), newItem.getJobLink())
                            && java.util.Objects.equals(oldItem.getNotes(), newItem.getNotes());
                }
            };

    private final OnJobClickListener listener;

    JobAdapter(OnJobClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    static void bind(View view, JobApplication application) {
        ((TextView) view.findViewById(R.id.text_company_name))
                .setText(application.getCompanyName());
        ((TextView) view.findViewById(R.id.text_job_role)).setText(application.getJobRole());
        ((TextView) view.findViewById(R.id.text_job_status)).setText(application.getStatus());
        ((TextView) view.findViewById(R.id.text_application_date)).setText(
                view.getContext().getString(
                        R.string.application_date_label,
                        JobForm.formatDate(application.getApplicationDate())));
    }

    @NonNull
    @Override
    public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_job_application, parent, false);
        return new JobViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JobViewHolder holder, int position) {
        JobApplication application = getItem(position);
        bind(holder.itemView, application);
        holder.itemView.setOnClickListener(view -> listener.onJobClick(application));
    }

    static final class JobViewHolder extends RecyclerView.ViewHolder {
        JobViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
