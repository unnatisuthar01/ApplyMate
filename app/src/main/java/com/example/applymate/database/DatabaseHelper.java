package com.example.applymate.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.applymate.models.User;
import com.example.applymate.models.JobApplication;
import com.example.applymate.models.Reminder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "APPLYMATE.db";
    private static final int DATABASE_VERSION = 3;

    private static final String TABLE_USERS = "users";
    private static final String TABLE_JOB_APPLICATIONS = "job_applications";
    private static final String TABLE_REMINDERS = "reminders";
    private static final String CREATE_USERS = "CREATE TABLE users ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "name TEXT NOT NULL, "
            + "email TEXT COLLATE NOCASE UNIQUE NOT NULL, "
            + "mobile TEXT, "
            + "password_hash TEXT NOT NULL)";

    private static final String CREATE_JOB_APPLICATIONS = "CREATE TABLE job_applications ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "user_id INTEGER NOT NULL, "
            + "company_name TEXT NOT NULL, "
            + "job_role TEXT NOT NULL, "
            + "location TEXT, "
            + "job_type TEXT, "
            + "salary REAL, "
            + "application_date INTEGER, "
            + "status TEXT NOT NULL DEFAULT 'Applied', "
            + "description TEXT, "
            + "job_link TEXT, "
            + "notes TEXT, "
            + "FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE)";

    private static final String CREATE_REMINDERS = "CREATE TABLE reminders ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "user_id INTEGER, "
            + "job_id INTEGER, "
            + "reminder_title TEXT NOT NULL, "
            + "description TEXT, "
            + "reminder_date INTEGER NOT NULL, "
            + "reminder_time TEXT, "
            + "reminder_type TEXT, "
            + "status TEXT NOT NULL DEFAULT 'Upcoming', "
            + "FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE, "
            + "FOREIGN KEY(job_id) REFERENCES job_applications(id) ON DELETE CASCADE)";

    public DatabaseHelper(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase database) {
        super.onConfigure(database);
        database.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(CREATE_USERS);
        database.execSQL(CREATE_JOB_APPLICATIONS);
        database.execSQL(CREATE_REMINDERS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if (oldVersion < 2 && newVersion >= 2) {
            migrateVersion1To2(database);
            oldVersion = 2;
        }
        if (oldVersion == 2 && newVersion >= 3) {
            migrateVersion2To3(database);
            oldVersion = 3;
        }
        if (oldVersion != newVersion) {
            throw new SQLiteException("No migration is defined from database version "
                    + oldVersion + " to " + newVersion + ".");
        }
    }

    private void migrateVersion1To2(SQLiteDatabase database) {
        database.execSQL("ALTER TABLE reminders RENAME TO reminders_v1");
        database.execSQL("ALTER TABLE job_applications RENAME TO job_applications_v1");

        database.execSQL(CREATE_JOB_APPLICATIONS.replace(
                "CREATE TABLE job_applications",
                "CREATE TABLE job_applications_v2"));
        database.execSQL(CREATE_REMINDERS.replace(
                "CREATE TABLE reminders",
                "CREATE TABLE reminders_v2").replace(
                "REFERENCES job_applications(id)",
                "REFERENCES job_applications_v2(id)"));

        database.execSQL("INSERT INTO job_applications_v2 "
                        + "(id, user_id, company_name, job_role, location, job_type, "
                        + "application_date, status, job_link, notes) "
                        + "SELECT id, user_id, company_name, job_title, location, "
                        + "employment_type, applied_at, status, job_url, notes "
                        + "FROM job_applications_v1");
        database.execSQL("INSERT INTO reminders_v2 "
                        + "(id, job_id, reminder_title, reminder_date) "
                        + "SELECT id, application_id, title, remind_at "
                        + "FROM reminders_v1 "
                        + "WHERE application_id IS NOT NULL AND EXISTS ("
                        + "SELECT 1 FROM job_applications_v2 "
                        + "WHERE job_applications_v2.id = reminders_v1.application_id "
                        + "AND job_applications_v2.user_id = reminders_v1.user_id)");

        database.execSQL("DROP TABLE reminders_v1");
        database.execSQL("DROP TABLE job_applications_v1");
        database.execSQL("ALTER TABLE job_applications_v2 RENAME TO job_applications");
        database.execSQL("ALTER TABLE reminders_v2 RENAME TO reminders");
    }

    private void migrateVersion2To3(SQLiteDatabase database) {
        database.execSQL("ALTER TABLE reminders RENAME TO reminders_v2");
        database.execSQL(CREATE_REMINDERS.replace(
                "CREATE TABLE reminders", "CREATE TABLE reminders_v3"));
        database.execSQL("INSERT INTO reminders_v3 "
                        + "(id, user_id, job_id, reminder_title, description, reminder_date, "
                        + "reminder_time, reminder_type, status) "
                        + "SELECT reminders_v2.id, job_applications.user_id, "
                        + "reminders_v2.job_id, reminders_v2.reminder_title, NULL, "
                        + "reminders_v2.reminder_date, reminders_v2.reminder_time, "
                        + "reminders_v2.reminder_type, 'Upcoming' "
                        + "FROM reminders_v2 LEFT JOIN job_applications "
                        + "ON reminders_v2.job_id = job_applications.id");
        database.execSQL("DROP TABLE reminders_v2");
        database.execSQL("ALTER TABLE reminders_v3 RENAME TO reminders");
    }

    public Cursor getJobApplicationsForUser(long userId) {
        return getReadableDatabase().query(
                TABLE_JOB_APPLICATIONS,
                null,
                "user_id = ?",
                new String[]{Long.toString(userId)},
                null,
                null,
                "application_date DESC, id DESC");
    }

    public List<JobApplication> getJobApplications(long userId, String status, int limit) {
        return searchJobApplications(
                userId, null, status, ApplicationSortOrder.NEWEST_FIRST, limit);
    }

    public List<JobApplication> searchJobApplications(
            long userId,
            String searchQuery,
            String status,
            ApplicationSortOrder sortOrder,
            int limit) {
        List<JobApplication> applications = new ArrayList<>();
        String selection = "user_id = ?";
        List<String> selectionArgs = new ArrayList<>();
        selectionArgs.add(Long.toString(userId));
        if (status != null && !status.trim().isEmpty()) {
            selection += " AND status = ? COLLATE NOCASE";
            selectionArgs.add(status);
        }
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            String searchPattern = "%" + escapeLikePattern(searchQuery.trim()) + "%";
            selection += " AND (company_name LIKE ? ESCAPE '\\' COLLATE NOCASE"
                    + " OR job_role LIKE ? ESCAPE '\\' COLLATE NOCASE)";
            selectionArgs.add(searchPattern);
            selectionArgs.add(searchPattern);
        }

        String safeLimit = limit > 0 ? Integer.toString(limit) : null;
        try (Cursor cursor = getReadableDatabase().query(
                TABLE_JOB_APPLICATIONS,
                null,
                selection,
                selectionArgs.toArray(new String[0]),
                null,
                null,
                sortOrder.getOrderBy(),
                safeLimit)) {
            while (cursor.moveToNext()) {
                applications.add(readJobApplication(cursor));
            }
        }
        return applications;
    }

    private static String escapeLikePattern(String value) {
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    public enum ApplicationSortOrder {
        NEWEST_FIRST("application_date DESC, id DESC"),
        OLDEST_FIRST("application_date ASC, id ASC"),
        COMPANY_NAME_ASC("company_name COLLATE NOCASE ASC, job_role COLLATE NOCASE ASC, id ASC");

        private final String orderBy;

        ApplicationSortOrder(String orderBy) {
            this.orderBy = orderBy;
        }

        private String getOrderBy() {
            return orderBy;
        }
    }

    public JobApplication getJobApplication(long jobId, long userId) {
        try (Cursor cursor = getReadableDatabase().query(
                TABLE_JOB_APPLICATIONS,
                null,
                "id = ? AND user_id = ?",
                new String[]{Long.toString(jobId), Long.toString(userId)},
                null,
                null,
                null,
                "1")) {
            return cursor.moveToFirst() ? readJobApplication(cursor) : null;
        }
    }

    public Map<String, Integer> getApplicationCounts(long userId) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("Total", 0);
        counts.put("Saved", 0);
        counts.put("Applied", 0);
        counts.put("Pending", 0);
        counts.put("Interview", 0);
        counts.put("Selected", 0);
        counts.put("Rejected", 0);

        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT status, COUNT(*) AS count FROM " + TABLE_JOB_APPLICATIONS
                        + " WHERE user_id = ? GROUP BY status",
                new String[]{Long.toString(userId)})) {
            while (cursor.moveToNext()) {
                String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
                int count = cursor.getInt(cursor.getColumnIndexOrThrow("count"));
                counts.put("Total", counts.get("Total") + count);
                if (counts.containsKey(status)) {
                    counts.put(status, count);
                }
            }
        }
        return counts;
    }

    public int[] getMonthlyApplicationCounts(long userId, int monthCount) {
        if (monthCount < 1) {
            throw new IllegalArgumentException("monthCount must be greater than zero.");
        }
        int[] counts = new int[monthCount];
        Calendar firstMonth = Calendar.getInstance();
        firstMonth.set(Calendar.DAY_OF_MONTH, 1);
        firstMonth.set(Calendar.HOUR_OF_DAY, 0);
        firstMonth.set(Calendar.MINUTE, 0);
        firstMonth.set(Calendar.SECOND, 0);
        firstMonth.set(Calendar.MILLISECOND, 0);
        firstMonth.add(Calendar.MONTH, -(monthCount - 1));

        for (int index = 0; index < monthCount; index++) {
            long start = firstMonth.getTimeInMillis();
            Calendar nextMonth = (Calendar) firstMonth.clone();
            nextMonth.add(Calendar.MONTH, 1);
            try (Cursor cursor = getReadableDatabase().rawQuery(
                    "SELECT COUNT(*) FROM " + TABLE_JOB_APPLICATIONS
                            + " WHERE user_id = ? AND application_date >= ? "
                            + "AND application_date < ?",
                    new String[]{Long.toString(userId), Long.toString(start),
                            Long.toString(nextMonth.getTimeInMillis())})) {
                if (cursor.moveToFirst()) {
                    counts[index] = cursor.getInt(0);
                }
            }
            firstMonth = nextMonth;
        }
        return counts;
    }

    public long insertJobApplication(JobApplication application) {
        ContentValues values = jobValues(application);
        values.put("user_id", application.getUserId());
        return getWritableDatabase().insertOrThrow(TABLE_JOB_APPLICATIONS, null, values);
    }

    public boolean updateJobApplication(JobApplication application) {
        ContentValues values = jobValues(application);
        return getWritableDatabase().update(
                TABLE_JOB_APPLICATIONS,
                values,
                "id = ? AND user_id = ?",
                new String[]{Long.toString(application.getId()),
                        Long.toString(application.getUserId())}) == 1;
    }

    public boolean deleteJobApplication(long jobId, long userId) {
        return getWritableDatabase().delete(
                TABLE_JOB_APPLICATIONS,
                "id = ? AND user_id = ?",
                new String[]{Long.toString(jobId), Long.toString(userId)}) == 1;
    }

    public User getUserById(long userId) {
        try (Cursor cursor = getReadableDatabase().query(
                TABLE_USERS,
                new String[]{"id", "name", "email", "mobile", "password_hash"},
                "id = ?",
                new String[]{Long.toString(userId)},
                null,
                null,
                null,
                "1")) {
            if (!cursor.moveToFirst()) {
                return null;
            }
            return new User(
                    cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    cursor.getString(cursor.getColumnIndexOrThrow("email")),
                    cursor.getString(cursor.getColumnIndexOrThrow("mobile")),
                    cursor.getString(cursor.getColumnIndexOrThrow("password_hash")));
        }
    }

    private static ContentValues jobValues(JobApplication application) {
        ContentValues values = new ContentValues();
        values.put("company_name", application.getCompanyName());
        values.put("job_role", application.getJobRole());
        values.put("location", application.getLocation());
        values.put("job_type", application.getJobType());
        if (application.getSalary() == null) {
            values.putNull("salary");
        } else {
            values.put("salary", application.getSalary());
        }
        values.put("application_date", application.getApplicationDate());
        values.put("status", application.getStatus());
        values.put("description", application.getDescription());
        values.put("job_link", application.getJobLink());
        values.put("notes", application.getNotes());
        return values;
    }

    private static JobApplication readJobApplication(Cursor cursor) {
        int salaryIndex = cursor.getColumnIndexOrThrow("salary");
        Double salary = cursor.isNull(salaryIndex) ? null : cursor.getDouble(salaryIndex);
        return new JobApplication(
                cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                cursor.getLong(cursor.getColumnIndexOrThrow("user_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("company_name")),
                cursor.getString(cursor.getColumnIndexOrThrow("job_role")),
                cursor.getString(cursor.getColumnIndexOrThrow("location")),
                cursor.getString(cursor.getColumnIndexOrThrow("job_type")),
                salary,
                cursor.getLong(cursor.getColumnIndexOrThrow("application_date")),
                cursor.getString(cursor.getColumnIndexOrThrow("status")),
                cursor.getString(cursor.getColumnIndexOrThrow("description")),
                cursor.getString(cursor.getColumnIndexOrThrow("job_link")),
                cursor.getString(cursor.getColumnIndexOrThrow("notes")));
    }

    public Cursor getRemindersForUser(long userId) {
        return getReadableDatabase().rawQuery(
                "SELECT reminders.* FROM reminders "
                        + "LEFT JOIN job_applications "
                        + "ON reminders.job_id = job_applications.id "
                        + "WHERE reminders.user_id = ? OR "
                        + "(reminders.user_id IS NULL AND job_applications.user_id = ?) "
                        + "ORDER BY reminders.reminder_date, reminders.reminder_time",
                new String[]{Long.toString(userId), Long.toString(userId)});
    }

    public List<Reminder> getRemindersForUser(long userId, String filter) {
        List<Reminder> reminders = new ArrayList<>();
        String selection = "user_id = ?";
        List<String> selectionArgs = new ArrayList<>();
        selectionArgs.add(Long.toString(userId));
        long startOfToday = startOfToday();
        long startOfTomorrow = startOfTomorrow();
        String currentTime = currentTimeText();
        if ("Completed".equals(filter)) {
            selection += " AND status = ?";
            selectionArgs.add("Completed");
        } else if ("Past".equals(filter)) {
            selection += " AND status != ? AND (reminder_date < ? OR "
                    + "(reminder_date >= ? AND reminder_date < ? "
                    + "AND reminder_time < ?))";
            selectionArgs.add("Completed");
            selectionArgs.add(Long.toString(startOfToday));
            selectionArgs.add(Long.toString(startOfToday));
            selectionArgs.add(Long.toString(startOfTomorrow));
            selectionArgs.add(currentTime);
        } else if ("Upcoming".equals(filter)) {
            selection += " AND status != ? AND (reminder_date >= ? AND "
                    + "(reminder_date >= ? OR reminder_time >= ?))";
            selectionArgs.add("Completed");
            selectionArgs.add(Long.toString(startOfToday));
            selectionArgs.add(Long.toString(startOfTomorrow));
            selectionArgs.add(currentTime);
        }
        try (Cursor cursor = getReadableDatabase().query(
                TABLE_REMINDERS,
                null,
                selection,
                selectionArgs.toArray(new String[0]),
                null,
                null,
                "reminder_date ASC, reminder_time ASC, id ASC")) {
            while (cursor.moveToNext()) {
                reminders.add(readReminder(cursor));
            }
        }
        return reminders;
    }

    public List<Reminder> getUpcomingReminders(long userId, int limit) {
        List<Reminder> reminders = new ArrayList<>();
        String currentTime = currentTimeText();
        long startOfToday = startOfToday();
        long startOfTomorrow = startOfTomorrow();
        try (Cursor cursor = getReadableDatabase().query(
                TABLE_REMINDERS,
                null,
                "user_id = ? AND status != ? AND reminder_date >= ? AND "
                        + "(reminder_date >= ? OR reminder_time >= ?)",
                new String[]{Long.toString(userId), "Completed",
                        Long.toString(startOfToday), Long.toString(startOfTomorrow), currentTime},
                null,
                null,
                "reminder_date ASC, reminder_time ASC, id ASC",
                limit > 0 ? Integer.toString(limit) : null)) {
            while (cursor.moveToNext()) {
                reminders.add(readReminder(cursor));
            }
        }
        return reminders;
    }

    public List<Reminder> getActiveReminders() {
        List<Reminder> reminders = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                TABLE_REMINDERS,
                null,
                "status != ? AND user_id IS NOT NULL",
                new String[]{"Completed"},
                null,
                null,
                "reminder_date ASC, reminder_time ASC, id ASC")) {
            while (cursor.moveToNext()) {
                reminders.add(readReminder(cursor));
            }
        }
        return reminders;
    }

    public List<Reminder> getActiveReminders(long userId) {
        List<Reminder> reminders = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                TABLE_REMINDERS,
                null,
                "status != ? AND user_id = ?",
                new String[]{"Completed", Long.toString(userId)},
                null,
                null,
                "reminder_date ASC, reminder_time ASC, id ASC")) {
            while (cursor.moveToNext()) {
                reminders.add(readReminder(cursor));
            }
        }
        return reminders;
    }

    public Reminder getReminder(long reminderId, long userId) {
        try (Cursor cursor = getReadableDatabase().query(
                TABLE_REMINDERS,
                null,
                "id = ? AND user_id = ?",
                new String[]{Long.toString(reminderId), Long.toString(userId)},
                null,
                null,
                null,
                "1")) {
            return cursor.moveToFirst() ? readReminder(cursor) : null;
        }
    }

    public long insertReminder(Reminder reminder) {
        if (reminder.getUserId() < 1) {
            throw new IllegalArgumentException("A reminder must belong to a user.");
        }
        return getWritableDatabase().insertOrThrow(TABLE_REMINDERS, null, reminderValues(reminder));
    }

    public boolean updateReminder(Reminder reminder) {
        if (reminder.getUserId() < 1 || reminder.getId() < 1) {
            throw new IllegalArgumentException("A reminder must have a valid ID and user.");
        }
        return getWritableDatabase().update(
                TABLE_REMINDERS,
                reminderValues(reminder),
                "id = ? AND user_id = ?",
                new String[]{Long.toString(reminder.getId()),
                        Long.toString(reminder.getUserId())}) == 1;
    }

    public boolean deleteReminder(long reminderId, long userId) {
        return getWritableDatabase().delete(
                TABLE_REMINDERS,
                "id = ? AND user_id = ?",
                new String[]{Long.toString(reminderId), Long.toString(userId)}) == 1;
    }

    public boolean setReminderStatus(long reminderId, long userId, String status) {
        ContentValues values = new ContentValues();
        values.put("status", status);
        return getWritableDatabase().update(
                TABLE_REMINDERS,
                values,
                "id = ? AND user_id = ?",
                new String[]{Long.toString(reminderId), Long.toString(userId)}) == 1;
    }

    private static ContentValues reminderValues(Reminder reminder) {
        ContentValues values = new ContentValues();
        values.put("user_id", reminder.getUserId());
        if (reminder.getJobId() == null) {
            values.putNull("job_id");
        } else {
            values.put("job_id", reminder.getJobId());
        }
        values.put("reminder_title", reminder.getTitle());
        values.put("description", reminder.getDescription());
        values.put("reminder_date", reminder.getDate());
        values.put("reminder_time", reminder.getTime());
        values.put("reminder_type", reminder.getType());
        values.put("status", reminder.getStatus());
        return values;
    }

    private static Reminder readReminder(Cursor cursor) {
        int jobIdIndex = cursor.getColumnIndexOrThrow("job_id");
        return new Reminder(
                cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                cursor.getLong(cursor.getColumnIndexOrThrow("user_id")),
                cursor.isNull(jobIdIndex) ? null : cursor.getLong(jobIdIndex),
                cursor.getString(cursor.getColumnIndexOrThrow("reminder_title")),
                cursor.getString(cursor.getColumnIndexOrThrow("description")),
                cursor.getString(cursor.getColumnIndexOrThrow("reminder_type")),
                cursor.getLong(cursor.getColumnIndexOrThrow("reminder_date")),
                cursor.getString(cursor.getColumnIndexOrThrow("reminder_time")),
                cursor.getString(cursor.getColumnIndexOrThrow("status")));
    }

    private static long startOfToday() {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        return today.getTimeInMillis();
    }

    private static long startOfTomorrow() {
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.set(Calendar.HOUR_OF_DAY, 0);
        tomorrow.set(Calendar.MINUTE, 0);
        tomorrow.set(Calendar.SECOND, 0);
        tomorrow.set(Calendar.MILLISECOND, 0);
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        return tomorrow.getTimeInMillis();
    }

    private static String currentTimeText() {
        Calendar now = Calendar.getInstance();
        return String.format(Locale.ROOT, "%02d:%02d",
                now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
    }

    public long insertUser(String name, String email, String mobile, String passwordHash) {
        ContentValues values = new ContentValues();
        values.put("name", name.trim());
        values.put("email", normalizeEmail(email));
        values.put("mobile", mobile.trim());
        values.put("password_hash", passwordHash);

        try {
            long id = getWritableDatabase().insertOrThrow(TABLE_USERS, null, values);
            if (id < 1) {
                throw new SQLiteException("The user record could not be saved.");
            }
            return id;
        } catch (SQLiteConstraintException exception) {
            if (userExists(email)) {
                return -1L;
            }
            throw exception;
        }
    }

    public User findUserByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        try (Cursor cursor = getReadableDatabase().query(
                TABLE_USERS,
                new String[]{"id", "name", "email", "mobile", "password_hash"},
                "email = ?",
                new String[]{normalizedEmail},
                null,
                null,
                null,
                "1")) {
            if (!cursor.moveToFirst()) {
                return null;
            }
            return new User(
                    cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    cursor.getString(cursor.getColumnIndexOrThrow("email")),
                    cursor.getString(cursor.getColumnIndexOrThrow("mobile")),
                    cursor.getString(cursor.getColumnIndexOrThrow("password_hash")));
        }
    }

    public boolean userExists(String email) {
        String normalizedEmail = normalizeEmail(email);
        try (Cursor cursor = getReadableDatabase().query(
                TABLE_USERS,
                new String[]{"id"},
                "email = ?",
                new String[]{normalizedEmail},
                null,
                null,
                null,
                "1")) {
            return cursor.moveToFirst();
        }
    }

    public boolean updatePasswordHash(long userId, String passwordHash) {
        ContentValues values = new ContentValues();
        values.put("password_hash", passwordHash);
        return getWritableDatabase().update(
                TABLE_USERS, values, "id = ?", new String[]{Long.toString(userId)}) == 1;
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
