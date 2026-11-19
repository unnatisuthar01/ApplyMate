package com.example.applymate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteException;

import com.example.applymate.database.DatabaseHelper;
import com.example.applymate.models.Reminder;

public class ReminderBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null
                || (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction()))) {
            return;
        }
        final PendingResult pendingResult = goAsync();
        Thread worker = new Thread(() -> {
            try (DatabaseHelper database = new DatabaseHelper(context)) {
                for (Reminder reminder : database.getActiveReminders()) {
                    ReminderScheduler.schedule(context, reminder);
                }
            } catch (SQLiteException | IllegalStateException | SecurityException exception) {
                android.util.Log.e("ApplyMate", "Could not restore reminder alarms.", exception);
            } finally {
                pendingResult.finish();
            }
        }, "ApplyMateReminderRestore");
        worker.start();
    }
}
