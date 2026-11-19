package com.example.applymate.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.applymate.models.User;

import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "APPLYMATE.db";
    private static final int DATABASE_VERSION = 2;

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
            + "job_id INTEGER NOT NULL, "
            + "reminder_title TEXT NOT NULL, "
            + "reminder_date INTEGER NOT NULL, "
            + "reminder_time TEXT, "
            + "reminder_type TEXT, "
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
        if (oldVersion == 1 && newVersion == 2) {
            migrateVersion1To2(database);
            return;
        }
        throw new SQLiteException("No migration is defined from database version "
                + oldVersion + " to " + newVersion + ".");
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

    public Cursor getRemindersForUser(long userId) {
        return getReadableDatabase().rawQuery(
                "SELECT reminders.* FROM reminders "
                        + "INNER JOIN job_applications "
                        + "ON reminders.job_id = job_applications.id "
                        + "WHERE job_applications.user_id = ? "
                        + "ORDER BY reminders.reminder_date, reminders.reminder_time",
                new String[]{Long.toString(userId)});
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
