package com.example.applymate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.applymate.models.Reminder;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder> {

    interface Listener {
        void onEdit(Reminder reminder);

        void onComplete(Reminder reminder);

        void onDelete(Reminder reminder);
    }

    private final Listener listener;
    private List<Reminder> reminders;

    ReminderAdapter(List<Reminder> reminders, Listener listener) {
        this.reminders = reminders;
        this.listener = listener;
    }

    void submitList(List<Reminder> reminders) {
        this.reminders = reminders;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reminder, parent, false);
        return new ReminderViewHolder(item);
    }

    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {
        Reminder reminder = reminders.get(position);
        holder.title.setText(reminder.getTitle());
        holder.dateTime.setText(holder.itemView.getContext().getString(
                R.string.reminder_date_time,
                new SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                        .format(new Date(reminder.getDate())),
                reminder.getTime() == null ? "" : reminder.getTime()));
        holder.type.setText(reminder.getType());
        boolean completed = "Completed".equals(reminder.getStatus());
        boolean past = !completed && ReminderScheduler.getTriggerAt(
                reminder.getDate(), reminder.getTime()) < System.currentTimeMillis();
        holder.status.setText(completed ? R.string.reminder_completed
                : past ? R.string.reminder_filter_past : R.string.reminder_filter_upcoming);
        holder.complete.setText(completed
                ? R.string.reminder_completed : R.string.reminder_mark_completed);
        holder.complete.setEnabled(!completed);
        holder.itemView.setOnClickListener(view -> listener.onEdit(reminder));
        holder.complete.setOnClickListener(view -> listener.onComplete(reminder));
        holder.delete.setOnClickListener(view -> listener.onDelete(reminder));
    }

    @Override
    public int getItemCount() {
        return reminders.size();
    }

    static final class ReminderViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView dateTime;
        private final TextView type;
        private final TextView status;
        private final MaterialButton complete;
        private final MaterialButton delete;

        ReminderViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_reminder_title);
            dateTime = itemView.findViewById(R.id.text_reminder_date_time);
            type = itemView.findViewById(R.id.text_reminder_type);
            status = itemView.findViewById(R.id.text_reminder_status);
            complete = itemView.findViewById(R.id.button_reminder_complete);
            delete = itemView.findViewById(R.id.button_reminder_delete);
        }
    }
}
