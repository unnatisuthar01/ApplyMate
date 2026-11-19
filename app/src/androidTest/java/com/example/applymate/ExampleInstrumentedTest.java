package com.example.applymate;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.recyclerview.widget.RecyclerView;

import com.example.applymate.database.DatabaseHelper;
import com.example.applymate.models.JobApplication;
import com.example.applymate.models.Reminder;
import com.example.applymate.models.User;
import com.example.applymate.utils.PasswordHasher;
import com.example.applymate.utils.ReminderPreferences;
import com.example.applymate.utils.SessionManager;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.security.GeneralSecurityException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    private Context context;

    @Before
    public void resetLocalData() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME);
        new SessionManager(context).clearSession();
        ReminderPreferences.setReminderAlertsEnabled(context, true);
        assertEquals("com.example.applymate", context.getPackageName());
    }

    @Test
    public void registrationPersistsHashedPasswordAndRejectsDuplicateEmail()
            throws GeneralSecurityException {
        try (ActivityScenario<RegistrationActivity> ignored =
                     enterRegistration("Taylor@example.com", "Passw0rd99", "Passw0rd99")) {
            ignored.onActivity(activity -> {
                assertEquals("Taylor Applicant", ((TextInputEditText) activity.findViewById(
                        R.id.input_full_name)).getText().toString());
                assertEquals("Taylor@example.com", ((TextInputEditText) activity.findViewById(
                        R.id.input_email)).getText().toString());
                assertEquals("5551234567", ((TextInputEditText) activity.findViewById(
                        R.id.input_mobile)).getText().toString());
                assertEquals("Passw0rd99", ((TextInputEditText) activity.findViewById(
                        R.id.input_password)).getText().toString());
                assertEquals("Passw0rd99", ((TextInputEditText) activity.findViewById(
                        R.id.input_confirm_password)).getText().toString());
            });
            Espresso.onView(withId(R.id.button_register))
                    .perform(ViewActions.scrollTo(), ViewActions.click());

            try (DatabaseHelper database = new DatabaseHelper(context)) {
                User user = database.findUserByEmail("taylor@example.com");
                assertNotNull(user);
                assertEquals("Taylor Applicant", user.getName());
                assertEquals("5551234567", user.getMobile());
                assertNotEquals("Passw0rd99", user.getPasswordHash());
                assertTrue(PasswordHasher.verifyAndClear(
                        "Passw0rd99".toCharArray(), user.getPasswordHash()));
                SQLiteDatabase sqliteDatabase = database.getReadableDatabase();
                try (Cursor tables = sqliteDatabase.rawQuery(
                        "SELECT name FROM sqlite_master WHERE type = ? "
                                + "AND name IN (?, ?, ?)",
                        new String[]{"table", "users", "job_applications", "reminders"})) {
                    assertEquals(3, tables.getCount());
                }
            }
        }

        try (ActivityScenario<RegistrationActivity> ignored =
                     enterRegistration("TAYLOR@example.com", "Passw0rd99", "Passw0rd99")) {
            Espresso.onView(withId(R.id.button_register))
                    .perform(ViewActions.scrollTo(), ViewActions.click());
            Espresso.onView(withId(R.id.input_email))
                    .check(ViewAssertions.matches(
                            errorTextIs(context.getString(R.string.email_already_registered))));
        }
    }

    @Test
    public void registrationShowsInvalidEmailAndPasswordMismatch() {
        try (ActivityScenario<RegistrationActivity> ignored =
                     enterRegistration("not-an-email", "Passw0rd99", "Different99")) {
            Espresso.onView(withId(R.id.button_register))
                    .perform(ViewActions.scrollTo(), ViewActions.click());

            Espresso.onView(withId(R.id.input_email))
                    .check(ViewAssertions.matches(
                            errorTextIs(context.getString(R.string.invalid_email))));
            Espresso.onView(withId(R.id.input_confirm_password))
                    .check(ViewAssertions.matches(
                            errorTextIs(context.getString(R.string.passwords_mismatch))));
        }
    }

    @Test
    public void loginRejectsUnknownAndIncorrectCredentialsThenCreatesPersistentSession()
            throws GeneralSecurityException {
        long userId;
        try (DatabaseHelper database = new DatabaseHelper(context)) {
            String hash = PasswordHasher.hashAndClear("Passw0rd99".toCharArray());
            userId = database.insertUser("Taylor Applicant", "taylor@example.com",
                    "5551234567", hash);
        }

        try (ActivityScenario<LoginActivity> ignored =
                     ActivityScenario.launch(LoginActivity.class)) {
            Espresso.onView(withId(R.id.input_email))
                    .perform(ViewActions.click(), ViewActions.replaceText("missing@example.com"));
            Espresso.onView(withId(R.id.input_password))
                    .perform(ViewActions.click(), ViewActions.replaceText("Passw0rd99"),
                            ViewActions.closeSoftKeyboard());
            Espresso.onView(withId(R.id.button_login)).perform(ViewActions.click());
            assertFalse(new SessionManager(context).isLoggedIn());

            Espresso.onView(withId(R.id.input_email))
                    .perform(ViewActions.click(), ViewActions.replaceText("taylor@example.com"));
            Espresso.onView(withId(R.id.input_password))
                    .perform(ViewActions.click(), ViewActions.replaceText("WrongPass99"),
                            ViewActions.closeSoftKeyboard());
            Espresso.onView(withId(R.id.button_login)).perform(ViewActions.click());
            assertFalse(new SessionManager(context).isLoggedIn());

            Espresso.onView(withId(R.id.input_password))
                    .perform(ViewActions.click(), ViewActions.replaceText("Passw0rd99"),
                            ViewActions.closeSoftKeyboard());
            Espresso.onView(withId(R.id.button_login)).perform(ViewActions.click());
            Espresso.onView(withId(R.id.button_logout))
                    .perform(ViewActions.scrollTo())
                    .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        }

        SessionManager restoredSession = new SessionManager(context);
        assertTrue(restoredSession.isLoggedIn());
        assertEquals(userId, restoredSession.getCurrentUserId());
    }

    @Test
    public void passwordChangeAndLogoutUpdateLocalState() throws GeneralSecurityException {
        long userId;
        try (DatabaseHelper database = new DatabaseHelper(context)) {
            String oldHash = PasswordHasher.hashAndClear("Passw0rd99".toCharArray());
            userId = database.insertUser("Taylor Applicant", "taylor@example.com",
                    "5551234567", oldHash);
        }
        new SessionManager(context).createSession(userId);
        assertEquals(userId, new SessionManager(context).getCurrentUserId());

        try (ActivityScenario<ForgotPasswordActivity> ignored =
                     ActivityScenario.launch(ForgotPasswordActivity.class)) {
            ignored.onActivity(activity -> {
                ((TextInputEditText) activity.findViewById(R.id.input_email))
                        .setText("taylor@example.com");
                ((TextInputEditText) activity.findViewById(R.id.input_current_password))
                        .setText("Passw0rd99");
                ((TextInputEditText) activity.findViewById(R.id.input_new_password))
                        .setText("NewPassw0rd88");
                ((TextInputEditText) activity.findViewById(R.id.input_confirm_new_password))
                        .setText("NewPassw0rd88");
            });
            Espresso.onView(withId(R.id.button_reset_password))
                    .perform(ViewActions.scrollTo(), ViewActions.click());
        }

        assertFalse(new SessionManager(context).isLoggedIn());
        try (DatabaseHelper database = new DatabaseHelper(context)) {
            User user = database.findUserByEmail("taylor@example.com");
            assertNotNull(user);
            assertFalse(PasswordHasher.verifyAndClear(
                    "Passw0rd99".toCharArray(), user.getPasswordHash()));
            assertTrue(PasswordHasher.verifyAndClear(
                    "NewPassw0rd88".toCharArray(), user.getPasswordHash()));
        }

        new SessionManager(context).createSession(userId);
        try (ActivityScenario<MainActivity> ignored =
                     ActivityScenario.launch(MainActivity.class)) {
            Espresso.onView(withId(R.id.button_logout))
                    .perform(ViewActions.scrollTo(), ViewActions.click());
            assertFalse(new SessionManager(context).isLoggedIn());
            Espresso.onView(withId(R.id.button_login))
                    .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
            Espresso.pressBackUnconditionally();
        }
        try (ActivityScenario<MainActivity> ignored =
                     ActivityScenario.launch(MainActivity.class)) {
            Espresso.onView(withId(R.id.button_login))
                    .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        }

        try (DatabaseHelper reopenedDatabase = new DatabaseHelper(context)) {
            assertTrue(reopenedDatabase.userExists("taylor@example.com"));
        }
    }

    @Test
    public void profileEditAndChangePasswordPersistForTheCurrentUser()
            throws GeneralSecurityException {
        long userId;
        try (DatabaseHelper database = new DatabaseHelper(context)) {
            userId = database.insertUser(
                    "Taylor Applicant", "taylor@example.com", "5551234567",
                    PasswordHasher.hashAndClear("Passw0rd99".toCharArray()));
            database.insertUser(
                    "Other Applicant", "other@example.com", "", "hash");
            assertFalse(database.updateUserProfile(
                    userId, "Taylor Applicant", "other@example.com", "5551234567"));
        }
        new SessionManager(context).createSession(userId);

        try (ActivityScenario<MainActivity> ignored =
                     ActivityScenario.launch(MainActivity.class)) {
            Espresso.onView(withId(R.id.nav_profile)).perform(ViewActions.click());
            Espresso.onView(withId(R.id.text_profile_name))
                    .check(ViewAssertions.matches(
                            ViewMatchers.withText("Taylor Applicant")));
            Espresso.onView(withId(R.id.button_edit_profile))
                    .perform(ViewActions.scrollTo(), ViewActions.click());
            Espresso.onView(withId(R.id.input_profile_name))
                    .perform(ViewActions.replaceText("Taylor Updated"));
            Espresso.onView(withId(R.id.input_profile_email))
                    .perform(ViewActions.replaceText("updated@example.com"));
            Espresso.onView(withId(R.id.input_profile_mobile))
                    .perform(ViewActions.replaceText("5557654321"));
            Espresso.onView(withId(R.id.button_save_profile))
                    .perform(ViewActions.scrollTo(), ViewActions.click());

            Espresso.onView(withId(R.id.text_profile_name))
                    .check(ViewAssertions.matches(ViewMatchers.withText("Taylor Updated")));
            Espresso.onView(withId(R.id.button_change_password))
                    .perform(ViewActions.scrollTo(), ViewActions.click());
            Espresso.onView(withId(R.id.input_change_current_password))
                    .perform(ViewActions.typeText("Passw0rd99"));
            Espresso.onView(withId(R.id.input_change_new_password))
                    .perform(ViewActions.typeText("NewPassw0rd88"));
            Espresso.onView(withId(R.id.input_change_confirm_password))
                    .perform(ViewActions.scrollTo(), ViewActions.typeText("NewPassw0rd88"),
                            ViewActions.closeSoftKeyboard());
            Espresso.onView(withId(R.id.button_save_password))
                    .perform(ViewActions.scrollTo(), ViewActions.click());
        }

        assertTrue(new SessionManager(context).isLoggedIn());
        try (DatabaseHelper database = new DatabaseHelper(context)) {
            User user = database.findUserByEmail("updated@example.com");
            assertNotNull(user);
            assertEquals("Taylor Updated", user.getName());
            assertEquals("5557654321", user.getMobile());
            assertTrue(PasswordHasher.verifyAndClear(
                    "NewPassw0rd88".toCharArray(), user.getPasswordHash()));
        }
    }

    @Test
    public void deleteAccountCascadesOnlyItsOwnDataAndCancelsItsReminderAlarm() {
        long userId;
        long otherUserId;
        long userReminderId;
        long otherReminderId;
        java.util.Calendar reminderDate = java.util.Calendar.getInstance();
        reminderDate.add(java.util.Calendar.DAY_OF_MONTH, 2);
        reminderDate.set(java.util.Calendar.HOUR_OF_DAY, 0);
        reminderDate.set(java.util.Calendar.MINUTE, 0);
        reminderDate.set(java.util.Calendar.SECOND, 0);
        reminderDate.set(java.util.Calendar.MILLISECOND, 0);
        long triggerDate = reminderDate.getTimeInMillis();
        try (DatabaseHelper database = new DatabaseHelper(context)) {
            userId = database.insertUser(
                    "Taylor Applicant", "taylor@example.com", "5551234567", "hash");
            otherUserId = database.insertUser(
                    "Other Applicant", "other@example.com", "5557654321", "hash");
            database.insertJobApplication(jobAt(
                    userId, "Taylor Co", "Engineer", "Applied", System.currentTimeMillis()));
            database.insertJobApplication(jobAt(
                    otherUserId, "Other Co", "Designer", "Applied", System.currentTimeMillis()));
            userReminderId = database.insertReminder(new Reminder(
                    0, userId, null, "Taylor reminder", "", "Other",
                    triggerDate, "23:59", "Upcoming"));
            otherReminderId = database.insertReminder(new Reminder(
                    0, otherUserId, null, "Other reminder", "", "Other",
                    triggerDate, "23:59", "Upcoming"));
        }
        new SessionManager(context).createSession(userId);
        ReminderScheduler.schedule(context, new Reminder(
                userReminderId, userId, null, "Taylor reminder", "", "Other",
                triggerDate, "23:59", "Upcoming"));

        try (ActivityScenario<ProfileActivity> ignored =
                     ActivityScenario.launch(ProfileActivity.class)) {
            Espresso.onView(withId(R.id.button_delete_account))
                    .perform(ViewActions.scrollTo(), ViewActions.click());
            Espresso.onView(withId(android.R.id.button1))
                    .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
                    .perform(ViewActions.click());
        }

        assertFalse(new SessionManager(context).isLoggedIn());
        try (DatabaseHelper database = new DatabaseHelper(context)) {
            assertEquals(null, database.getUserById(userId));
            assertNotNull(database.getUserById(otherUserId));
            assertTrue(database.getJobApplications(userId, null, 0).isEmpty());
            assertEquals(1, database.getJobApplications(otherUserId, null, 0).size());
            assertTrue(database.getRemindersForUser(userId, null).isEmpty());
            assertEquals(otherReminderId,
                    database.getRemindersForUser(otherUserId, null).get(0).getId());
        }
        assertPendingReminderAlarmDoesNotExist(userReminderId);
    }

    @Test
    public void settingsReminderPreferencePausesAndRestoresScheduledAlarms() {
        long userId;
        long reminderId;
        java.util.Calendar reminderDate = java.util.Calendar.getInstance();
        reminderDate.add(java.util.Calendar.DAY_OF_MONTH, 2);
        reminderDate.set(java.util.Calendar.HOUR_OF_DAY, 0);
        reminderDate.set(java.util.Calendar.MINUTE, 0);
        reminderDate.set(java.util.Calendar.SECOND, 0);
        reminderDate.set(java.util.Calendar.MILLISECOND, 0);
        long triggerDate = reminderDate.getTimeInMillis();
        try (DatabaseHelper database = new DatabaseHelper(context)) {
            userId = database.insertUser(
                    "Taylor Applicant", "taylor@example.com", "5551234567", "hash");
            reminderId = database.insertReminder(new Reminder(
                    0, userId, null, "Future reminder", "", "Other",
                    triggerDate, "23:59", "Upcoming"));
        }
        new SessionManager(context).createSession(userId);
        Reminder reminder;
        try (DatabaseHelper database = new DatabaseHelper(context)) {
            reminder = database.getReminder(reminderId, userId);
        }
        ReminderScheduler.schedule(context, reminder);
        assertPendingReminderAlarmExists(reminderId);

        try (ActivityScenario<SettingsActivity> ignored =
                     ActivityScenario.launch(SettingsActivity.class)) {
            Espresso.onView(withId(R.id.switch_reminder_alerts)).perform(ViewActions.click());
            assertFalse(ReminderPreferences.areReminderAlertsEnabled(context));
            assertPendingReminderAlarmDoesNotExist(reminderId);
            Espresso.onView(withId(R.id.switch_reminder_alerts)).perform(ViewActions.click());
            assertTrue(ReminderPreferences.areReminderAlertsEnabled(context));
            assertPendingReminderAlarmExists(reminderId);
        }
        ReminderScheduler.cancel(context, reminderId);
    }

    @Test
    public void versionOneDatabaseMigratesAndPreservesApplicationAndReminderData()
            throws GeneralSecurityException {
        String oldPasswordHash = PasswordHasher.hashAndClear(
                "LegacyPass99".toCharArray());
        oldPasswordHash = oldPasswordHash.substring(oldPasswordHash.indexOf('$') + 1);
        SQLiteDatabase versionOne = SQLiteDatabase.openOrCreateDatabase(
                context.getDatabasePath(DatabaseHelper.DATABASE_NAME), null);
        versionOne.execSQL("CREATE TABLE users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, "
                + "email TEXT UNIQUE NOT NULL, mobile TEXT, password_hash TEXT NOT NULL)");
        versionOne.execSQL("CREATE TABLE job_applications ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL, "
                + "company_name TEXT NOT NULL, job_title TEXT NOT NULL, location TEXT, "
                + "employment_type TEXT, status TEXT NOT NULL DEFAULT 'Applied', "
                + "applied_at INTEGER, job_url TEXT, notes TEXT, created_at INTEGER NOT NULL, "
                + "updated_at INTEGER NOT NULL, "
                + "FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE)");
        versionOne.execSQL("CREATE TABLE reminders ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL, "
                + "application_id INTEGER, title TEXT NOT NULL, description TEXT, "
                + "remind_at INTEGER NOT NULL, is_completed INTEGER NOT NULL DEFAULT 0, "
                + "created_at INTEGER NOT NULL, "
                + "FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE, "
                + "FOREIGN KEY(application_id) REFERENCES job_applications(id) ON DELETE SET NULL)");
        versionOne.execSQL("INSERT INTO users (id, name, email, mobile, password_hash) "
                        + "VALUES (1, 'Taylor', 'taylor@example.com', '5551234567', ?)",
                new Object[]{oldPasswordHash});
        versionOne.execSQL("INSERT INTO job_applications "
                + "(id, user_id, company_name, job_title, location, employment_type, status, "
                + "applied_at, job_url, notes, created_at, updated_at) "
                + "VALUES (7, 1, 'Acme', 'Engineer', 'Remote', 'Full-time', 'Interview', "
                + "1700000000000, 'https://example.com/job', 'Follow up', 1, 2)");
        versionOne.execSQL("INSERT INTO reminders "
                + "(id, user_id, application_id, title, description, remind_at, created_at) "
                + "VALUES (9, 1, 7, 'Interview prep', 'Review notes', 1700000001000, 3)");
        versionOne.setVersion(1);
        versionOne.close();

        try (DatabaseHelper database = new DatabaseHelper(context)) {
            SQLiteDatabase upgraded = database.getReadableDatabase();
            assertEquals(3, upgraded.getVersion());
            User migratedUser = database.findUserByEmail("taylor@example.com");
            assertNotNull(migratedUser);
            assertTrue(PasswordHasher.verifyAndClear(
                    "LegacyPass99".toCharArray(), migratedUser.getPasswordHash()));
            assertTableColumns(upgraded, "users",
                    "id", "name", "email", "mobile", "password_hash");
            assertTableColumns(upgraded, "job_applications",
                    "id", "user_id", "company_name", "job_role", "location", "job_type",
                    "salary", "application_date", "status", "description", "job_link", "notes");
            assertTableColumns(upgraded, "reminders",
                    "id", "user_id", "job_id", "reminder_title", "description",
                    "reminder_date", "reminder_time", "reminder_type", "status");
            try (Cursor job = upgraded.query(
                    "job_applications",
                    new String[]{"job_role", "job_type", "application_date", "job_link"},
                    "id = ?",
                    new String[]{"7"},
                    null, null, null)) {
                assertTrue(job.moveToFirst());
                assertEquals("Engineer", job.getString(0));
                assertEquals("Full-time", job.getString(1));
                assertEquals(1700000000000L, job.getLong(2));
                assertEquals("https://example.com/job", job.getString(3));
            }
            try (Cursor reminder = upgraded.query(
                    "reminders",
                    new String[]{"user_id", "job_id", "reminder_title", "reminder_date",
                            "status"},
                    "id = ?",
                    new String[]{"9"},
                    null, null, null)) {
                assertTrue(reminder.moveToFirst());
                assertEquals(1, reminder.getLong(0));
                assertEquals(7, reminder.getLong(1));
                assertEquals("Interview prep", reminder.getString(2));
                assertEquals(1700000001000L, reminder.getLong(3));
                assertEquals("Upcoming", reminder.getString(4));
            }
        }
    }

    @Test
    public void reminderCrudAndStatisticsAreScopedToTheCurrentUser() {
        long firstUserId;
        long secondUserId;
        long futureReminderDate;
        long pastReminderDate;
        java.util.Calendar futureDate = java.util.Calendar.getInstance();
        futureDate.add(java.util.Calendar.DAY_OF_MONTH, 2);
        futureDate.set(java.util.Calendar.HOUR_OF_DAY, 0);
        futureDate.set(java.util.Calendar.MINUTE, 0);
        futureDate.set(java.util.Calendar.SECOND, 0);
        futureDate.set(java.util.Calendar.MILLISECOND, 0);
        futureReminderDate = futureDate.getTimeInMillis();
        java.util.Calendar pastDate = java.util.Calendar.getInstance();
        pastDate.add(java.util.Calendar.DAY_OF_MONTH, -2);
        pastDate.set(java.util.Calendar.HOUR_OF_DAY, 0);
        pastDate.set(java.util.Calendar.MINUTE, 0);
        pastDate.set(java.util.Calendar.SECOND, 0);
        pastDate.set(java.util.Calendar.MILLISECOND, 0);
        pastReminderDate = pastDate.getTimeInMillis();

        try (DatabaseHelper database = new DatabaseHelper(context)) {
            firstUserId = database.insertUser(
                    "First Applicant", "first@example.com", "", "hash");
            secondUserId = database.insertUser(
                    "Second Applicant", "second@example.com", "", "hash");
            database.insertJobApplication(jobAt(
                    firstUserId, "A", "Engineer", "Applied",
                    System.currentTimeMillis()));
            database.insertJobApplication(jobAt(
                    firstUserId, "B", "Designer", "Pending",
                    System.currentTimeMillis()));
            database.insertJobApplication(jobAt(
                    firstUserId, "C", "Researcher", "Interview",
                    System.currentTimeMillis()));
            database.insertJobApplication(jobAt(
                    secondUserId, "Private", "Engineer", "Selected",
                    System.currentTimeMillis()));

            assertEquals(3, database.getApplicationCounts(firstUserId).get("Total").intValue());
            assertEquals(1, database.getApplicationCounts(firstUserId).get("Pending").intValue());
            assertEquals(3, database.getMonthlyApplicationCounts(firstUserId, 6)[5]);
            assertEquals(1, database.getMonthlyApplicationCounts(secondUserId, 6)[5]);

            long reminderId = database.insertReminder(new com.example.applymate.models.Reminder(
                    0, firstUserId, null, "Interview preparation", "Review notes",
                    "Job interview", futureReminderDate, "10:30", "Upcoming"));
            database.insertReminder(new com.example.applymate.models.Reminder(
                    0, secondUserId, null, "Private follow-up", "",
                    "Application follow-up", futureReminderDate, "11:00", "Upcoming"));
            long pastId = database.insertReminder(new com.example.applymate.models.Reminder(
                    0, firstUserId, null, "Past HR contact", "",
                    "HR contact", pastReminderDate, "10:00", "Upcoming"));

            assertEquals(1, database.getRemindersForUser(firstUserId, "Upcoming").size());
            assertEquals(1, database.getRemindersForUser(firstUserId, "Past").size());
            assertEquals(2, database.getRemindersForUser(firstUserId, null).size());
            assertEquals(null, database.getReminder(reminderId, secondUserId));
            assertNotNull(database.getReminder(reminderId, firstUserId));
            assertTrue(database.setReminderStatus(reminderId, firstUserId, "Completed"));
            assertEquals(1, database.getRemindersForUser(firstUserId, "Completed").size());
            assertTrue(database.deleteReminder(pastId, firstUserId));
            assertFalse(database.deleteReminder(reminderId, secondUserId));
        }
    }

    @Test
    public void reminderFormSavesDateAndTimeAndSchedulesAlarm() {
        long userId;
        try (DatabaseHelper database = new DatabaseHelper(context)) {
            userId = database.insertUser(
                    "Taylor Applicant", "taylor@example.com", "5551234567", "test-hash");
        }
        new SessionManager(context).createSession(userId);

        try (ActivityScenario<AddEditReminderActivity> scenario =
                     ActivityScenario.launch(AddEditReminderActivity.class)) {
            scenario.onActivity(activity -> {
                ((TextView) activity.findViewById(R.id.input_reminder_title))
                        .setText("Interview preparation");
                ((TextView) activity.findViewById(R.id.input_reminder_description))
                        .setText("Review notes");
                ((Spinner) activity.findViewById(R.id.spinner_reminder_type)).setSelection(1);
                assertTrue(((TextView) activity.findViewById(R.id.input_reminder_date))
                        .getText().length() > 0);
                assertTrue(((TextView) activity.findViewById(R.id.input_reminder_time))
                        .getText().length() > 0);
                activity.findViewById(R.id.button_save_reminder).performClick();
            });
        }

        Reminder reminder;
        try (DatabaseHelper database = new DatabaseHelper(context)) {
            java.util.List<com.example.applymate.models.Reminder> reminders =
                    database.getRemindersForUser(userId, null);
            assertEquals(1, reminders.size());
            reminder = reminders.get(0);
            assertEquals("Interview preparation", reminder.getTitle());
            assertEquals("Job interview", reminder.getType());
            assertNotNull(reminder.getTime());
        }

        Intent alarmIntent = new Intent(context, ReminderReceiver.class)
                .setAction(ReminderScheduler.ACTION_REMINDER);
        int requestCode = (int) (reminder.getId() ^ (reminder.getId() >>> 32));
        PendingIntent alarm = PendingIntent.getBroadcast(
                context, requestCode, alarmIntent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        assertNotNull(alarm);
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(alarm);
        alarm.cancel();
    }

    @Test
    public void statisticsScreenUsesCurrentUsersApplicationData() {
        long userId;
        try (DatabaseHelper database = new DatabaseHelper(context)) {
            userId = database.insertUser(
                    "Taylor Applicant", "taylor@example.com", "5551234567", "test-hash");
            database.insertJobApplication(jobAt(
                    userId, "Acme", "Engineer", "Applied", System.currentTimeMillis()));
            database.insertJobApplication(jobAt(
                    userId, "Northwind", "Designer", "Pending", System.currentTimeMillis()));
            long otherUserId = database.insertUser(
                    "Other Applicant", "other@example.com", "", "test-hash");
            database.insertJobApplication(jobAt(
                    otherUserId, "Private", "Engineer", "Selected",
                    System.currentTimeMillis()));
        }
        new SessionManager(context).createSession(userId);

        try (ActivityScenario<StatisticsActivity> scenario =
                     ActivityScenario.launch(StatisticsActivity.class)) {
            scenario.onActivity(activity -> {
                MaterialCardView totalCard = activity.findViewById(R.id.card_stat_total);
                assertEquals("2", ((TextView) totalCard.findViewById(
                        R.id.text_status_count)).getText().toString());
                MaterialCardView pendingCard = activity.findViewById(R.id.card_stat_pending);
                assertEquals("1", ((TextView) pendingCard.findViewById(
                        R.id.text_status_count)).getText().toString());
                assertEquals(1, ((ApplicationPieChartView) activity.findViewById(
                        R.id.chart_application_status)).getValues()[1]);
                assertEquals(1, ((ApplicationPieChartView) activity.findViewById(
                        R.id.chart_application_status)).getValues()[2]);
                assertEquals(2, ((MonthlyApplicationsChartView) activity.findViewById(
                        R.id.chart_monthly_applications)).getValues()[5]);
            });
        }
    }

    @Test
    public void jobAndReminderQueriesAreScopedToTheOwningUser() {
        try (DatabaseHelper database = new DatabaseHelper(context)) {
            SQLiteDatabase writable = database.getWritableDatabase();
            writable.execSQL("INSERT INTO users (name, email, mobile, password_hash) "
                    + "VALUES ('First', 'first@example.com', '', 'hash')");
            writable.execSQL("INSERT INTO users (name, email, mobile, password_hash) "
                    + "VALUES ('Second', 'second@example.com', '', 'hash')");
            writable.execSQL("INSERT INTO job_applications "
                    + "(id, user_id, company_name, job_role) "
                    + "VALUES (1, 1, 'First Co', 'Engineer')");
            writable.execSQL("INSERT INTO job_applications "
                    + "(id, user_id, company_name, job_role) "
                    + "VALUES (2, 2, 'Second Co', 'Designer')");
            writable.execSQL("INSERT INTO reminders "
                    + "(job_id, reminder_title, reminder_date) VALUES (1, 'First reminder', 10)");
            writable.execSQL("INSERT INTO reminders "
                    + "(job_id, reminder_title, reminder_date) VALUES (2, 'Second reminder', 20)");

            try (Cursor jobs = database.getJobApplicationsForUser(1)) {
                assertEquals(1, jobs.getCount());
                assertTrue(jobs.moveToFirst());
                assertEquals("First Co", jobs.getString(
                        jobs.getColumnIndexOrThrow("company_name")));
            }
            try (Cursor reminders = database.getRemindersForUser(1)) {
                assertEquals(1, reminders.getCount());
                assertTrue(reminders.moveToFirst());
                assertEquals("First reminder", reminders.getString(
                        reminders.getColumnIndexOrThrow("reminder_title")));
            }
        }
    }

    @Test
    public void jobCrudAndDashboardAreScopedToTheCurrentUser() {
        long userId;
        try (DatabaseHelper database = new DatabaseHelper(context)) {
            userId = database.insertUser(
                    "Taylor Applicant", "taylor@example.com", "5551234567", "test-hash");
            long otherUserId = database.insertUser(
                    "Other Applicant", "other@example.com", "", "test-hash");
            database.insertJobApplication(job(
                    userId, "Acme", "Engineer", "Saved"));
            database.insertJobApplication(job(
                    userId, "Northwind", "Designer", "Interview"));
            long otherJobId = database.insertJobApplication(
                    job(otherUserId, "Other Co", "Analyst", "Applied"));

            assertEquals(2, database.getJobApplications(userId, null, 0).size());
            assertEquals(1, database.getJobApplications(userId, "Saved", 0).size());
            assertEquals(Integer.valueOf(2),
                    database.getApplicationCounts(userId).get("Total"));
            assertEquals(Integer.valueOf(1),
                    database.getApplicationCounts(userId).get("Interview"));
            assertEquals(null, database.getJobApplication(otherJobId, userId));

            JobApplication saved = database.getJobApplications(userId, "Saved", 1).get(0);
            JobApplication updated = job(userId, "Acme", "Senior Engineer", "Applied",
                    saved.getId());
            assertTrue(database.updateJobApplication(updated));
            assertEquals("Senior Engineer",
                    database.getJobApplication(saved.getId(), userId).getJobRole());
            assertTrue(database.deleteJobApplication(saved.getId(), userId));
            assertFalse(database.deleteJobApplication(otherJobId, userId));
        }

        try (DatabaseHelper reopenedDatabase = new DatabaseHelper(context)) {
            assertEquals(1, reopenedDatabase.getJobApplications(userId, null, 0).size());
            assertEquals("Interview", reopenedDatabase
                    .getJobApplications(userId, null, 0).get(0).getStatus());
        }
    }

    @Test
    public void dashboardAddEditAndDeleteApplicationFlow() {
        long userId;
        try (DatabaseHelper database = new DatabaseHelper(context)) {
            userId = database.insertUser(
                    "Taylor Applicant", "taylor@example.com", "5551234567", "test-hash");
        }
        new SessionManager(context).createSession(userId);

        try (ActivityScenario<MainActivity> ignored =
                     ActivityScenario.launch(MainActivity.class)) {
            Espresso.onView(withId(R.id.text_greeting))
                    .check(ViewAssertions.matches(
                            ViewMatchers.withText("Welcome back, Taylor")));

            Espresso.onView(withId(R.id.button_add_application))
                    .perform(ViewActions.scrollTo(), ViewActions.click());
            Espresso.onView(withId(R.id.button_save_job))
                    .perform(ViewActions.scrollTo(), ViewActions.click());
            Espresso.onView(withId(R.id.input_company_name))
                    .check(ViewAssertions.matches(
                            errorTextIs(context.getString(R.string.required_field))));

            Espresso.onView(withId(R.id.input_company_name))
                    .perform(ViewActions.scrollTo(), ViewActions.click(),
                            ViewActions.replaceText("Acme"));
            Espresso.onView(withId(R.id.input_job_role))
                    .perform(ViewActions.scrollTo(), ViewActions.click(),
                            ViewActions.replaceText("Engineer"),
                            ViewActions.closeSoftKeyboard());
            Espresso.onView(withId(R.id.spinner_job_type)).perform(ViewActions.click());
            Espresso.onData(org.hamcrest.Matchers.allOf(
                            org.hamcrest.Matchers.is("Full-time")))
                    .perform(ViewActions.click());
            Espresso.onView(withId(R.id.button_save_job))
                    .perform(ViewActions.scrollTo(), ViewActions.click());

            Espresso.onView(withId(R.id.count_total))
                    .check(ViewAssertions.matches(ViewMatchers.withText("1")));
            Espresso.onView(withId(R.id.text_company_name)).perform(ViewActions.click());
            Espresso.onView(withId(R.id.text_detail_company))
                    .check(ViewAssertions.matches(ViewMatchers.withText("Acme")));

            Espresso.onView(withId(R.id.button_edit_job)).perform(ViewActions.click());
            Espresso.onView(withId(R.id.input_job_role))
                    .perform(ViewActions.replaceText("Senior Engineer"),
                            ViewActions.closeSoftKeyboard());
            Espresso.onView(withId(R.id.button_save_job))
                    .perform(ViewActions.scrollTo(), ViewActions.click());
            Espresso.onView(withId(R.id.text_detail_role))
                    .check(ViewAssertions.matches(ViewMatchers.withText("Senior Engineer")));

            Espresso.onView(withId(R.id.button_delete_job)).perform(ViewActions.click());
            Espresso.onView(ViewMatchers.withText(R.string.confirm_delete))
                    .perform(ViewActions.click());
            Espresso.onView(withId(R.id.count_total))
                    .check(ViewAssertions.matches(ViewMatchers.withText("0")));
            Espresso.onView(withId(R.id.text_recent_empty))
                    .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        }
    }

    @Test
    public void dashboardStatusCardOpensUserFilteredApplications() {
        long userId;
        try (DatabaseHelper database = new DatabaseHelper(context)) {
            userId = database.insertUser(
                    "Taylor Applicant", "taylor@example.com", "5551234567", "test-hash");
            long otherUserId = database.insertUser(
                    "Other Applicant", "other@example.com", "", "test-hash");
            database.insertJobApplication(job(userId, "Acme", "Engineer", "Saved"));
            database.insertJobApplication(job(otherUserId, "Other Co", "Analyst", "Saved"));
        }
        new SessionManager(context).createSession(userId);

        try (ActivityScenario<MainActivity> ignored =
                     ActivityScenario.launch(MainActivity.class)) {
            Espresso.onView(withId(R.id.card_saved)).perform(ViewActions.click());
            Espresso.onView(withId(R.id.text_list_title))
                    .check(ViewAssertions.matches(ViewMatchers.withText("Saved")));
            Espresso.onView(withId(R.id.text_company_name))
                    .check(ViewAssertions.matches(ViewMatchers.withText("Acme")));
            Espresso.onView(withId(R.id.recycler_applications))
                    .check(ViewAssertions.matches(hasItemCount(1)));
        }
    }

    @Test
    public void applicationSearchFiltersStatusesAndSortsWithinCurrentUser() {
        long userId;
        try (DatabaseHelper database = new DatabaseHelper(context)) {
            userId = database.insertUser(
                    "Taylor Applicant", "taylor@example.com", "5551234567", "test-hash");
            long otherUserId = database.insertUser(
                    "Other Applicant", "other@example.com", "", "test-hash");
            database.insertJobApplication(jobAt(userId, "Zenith", "Software Engineer",
                    "Applied", 600L));
            database.insertJobApplication(jobAt(userId, "acme", "Product Designer",
                    "Pending", 500L));
            database.insertJobApplication(jobAt(userId, "Bright Labs", "Engineer",
                    "Interview", 400L));
            database.insertJobApplication(jobAt(userId, "Gamma", "Analyst",
                    "Selected", 300L));
            database.insertJobApplication(jobAt(userId, "Delta", "Researcher",
                    "Rejected", 200L));
            database.insertJobApplication(jobAt(otherUserId, "Acme", "Engineer",
                    "Selected", 700L));

            assertEquals(1, database.searchJobApplications(
                    userId, "ZENITH", null,
                    DatabaseHelper.ApplicationSortOrder.NEWEST_FIRST, 0).size());
            assertEquals(2, database.searchJobApplications(
                    userId, "engineer", null,
                    DatabaseHelper.ApplicationSortOrder.NEWEST_FIRST, 0).size());
            assertEquals("Pending", database.searchJobApplications(
                    userId, null, "pending",
                    DatabaseHelper.ApplicationSortOrder.NEWEST_FIRST, 0)
                    .get(0).getStatus());
            assertEquals(5, database.searchJobApplications(
                    userId, null, null,
                    DatabaseHelper.ApplicationSortOrder.NEWEST_FIRST, 0).size());
            assertEquals("Zenith", database.searchJobApplications(
                    userId, null, null,
                    DatabaseHelper.ApplicationSortOrder.NEWEST_FIRST, 0)
                    .get(0).getCompanyName());
            assertEquals("Delta", database.searchJobApplications(
                    userId, null, null,
                    DatabaseHelper.ApplicationSortOrder.OLDEST_FIRST, 0)
                    .get(0).getCompanyName());
            assertEquals("acme", database.searchJobApplications(
                    userId, null, null,
                    DatabaseHelper.ApplicationSortOrder.COMPANY_NAME_ASC, 0)
                    .get(0).getCompanyName());
            assertEquals(1, database.searchJobApplications(
                    userId, null, "Applied",
                    DatabaseHelper.ApplicationSortOrder.NEWEST_FIRST, 0).size());
            assertEquals(1, database.searchJobApplications(
                    userId, null, "Interview",
                    DatabaseHelper.ApplicationSortOrder.NEWEST_FIRST, 0).size());
            assertEquals(1, database.searchJobApplications(
                    userId, null, "Selected",
                    DatabaseHelper.ApplicationSortOrder.NEWEST_FIRST, 0).size());
            assertEquals(1, database.searchJobApplications(
                    userId, null, "Rejected",
                    DatabaseHelper.ApplicationSortOrder.NEWEST_FIRST, 0).size());
            assertEquals(0, database.searchJobApplications(
                    userId, "Other", null,
                    DatabaseHelper.ApplicationSortOrder.NEWEST_FIRST, 0).size());
        }
    }

    @Test
    public void applicationScreenClearsSearchStatusAndSortFilters() {
        long userId;
        try (DatabaseHelper database = new DatabaseHelper(context)) {
            userId = database.insertUser(
                    "Taylor Applicant", "taylor@example.com", "5551234567", "test-hash");
            database.insertJobApplication(jobAt(
                    userId, "Acme", "Engineer", "Applied", 300L));
            database.insertJobApplication(jobAt(
                    userId, "Northwind", "Designer", "Pending", 200L));
        }
        new SessionManager(context).createSession(userId);

        try (ActivityScenario<ApplicationsActivity> ignored =
                     ActivityScenario.launch(ApplicationsActivity.class)) {
            Espresso.onView(withId(R.id.input_search_applications))
                    .perform(ViewActions.typeText("nothing"), ViewActions.closeSoftKeyboard());
            Espresso.onView(withId(R.id.text_empty_applications))
                    .check(ViewAssertions.matches(
                            ViewMatchers.withText(R.string.no_results_found)));
            Espresso.onView(withId(R.id.button_empty_clear_filters))
                    .perform(ViewActions.click());
            awaitAdapterItemCount(ignored, 2);
            Espresso.onView(withId(R.id.recycler_applications))
                    .check(ViewAssertions.matches(hasItemCount(2)));

            Espresso.onView(withId(R.id.chip_filter_pending)).perform(ViewActions.click());
            awaitAdapterItemCount(ignored, 1);
            Espresso.onView(withId(R.id.recycler_applications))
                    .check(ViewAssertions.matches(hasItemCount(1)));
            Espresso.onView(withId(R.id.button_clear_filters)).perform(ViewActions.click());
            awaitAdapterItemCount(ignored, 2);
            Espresso.onView(withId(R.id.recycler_applications))
                    .check(ViewAssertions.matches(hasItemCount(2)));
            Espresso.onView(withId(R.id.chip_filter_all))
                    .check(ViewAssertions.matches(ViewMatchers.isChecked()));
        }
    }

    private JobApplication job(long userId, String company, String role, String status) {
        return job(userId, company, role, status, 0);
    }

    private JobApplication job(
            long userId, String company, String role, String status, long id) {
        return new JobApplication(
                id, userId, company, role, "Remote", "Full-time", 100000.0,
                1700000000000L, status, "Description", "https://example.com/job", "Notes");
    }

    private JobApplication jobAt(
            long userId, String company, String role, String status, long applicationDate) {
        return new JobApplication(
                0, userId, company, role, "Remote", "Full-time", 100000.0,
                applicationDate, status, "Description", "https://example.com/job", "Notes");
    }

    private ActivityScenario<RegistrationActivity> enterRegistration(
            String email, String password, String confirmation) {
        ActivityScenario<RegistrationActivity> scenario =
                ActivityScenario.launch(RegistrationActivity.class);
        scenario.onActivity(activity -> {
            ((TextInputEditText) activity.findViewById(R.id.input_full_name))
                    .setText("Taylor Applicant");
            ((TextInputEditText) activity.findViewById(R.id.input_email)).setText(email);
            ((TextInputEditText) activity.findViewById(R.id.input_mobile))
                    .setText("5551234567");
            ((TextInputEditText) activity.findViewById(R.id.input_password))
                    .setText(password);
            ((TextInputEditText) activity.findViewById(R.id.input_confirm_password))
                    .setText(confirmation);
        });
        return scenario;
    }

    private Matcher<View> errorTextIs(String expectedError) {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("has error text: ").appendText(expectedError);
            }

            @Override
            protected boolean matchesSafely(View view) {
                if (!(view instanceof TextView)) {
                    return false;
                }
                CharSequence error = ((TextView) view).getError();
                return error != null && expectedError.contentEquals(error);
            }
        };
    }

    private Matcher<View> hasItemCount(int expectedCount) {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("has RecyclerView item count: ").appendValue(expectedCount);
            }

            @Override
            protected boolean matchesSafely(View view) {
                if (!(view instanceof RecyclerView)) {
                    return false;
                }
                RecyclerView.Adapter<?> adapter = ((RecyclerView) view).getAdapter();
                return adapter != null && adapter.getItemCount() == expectedCount;
            }
        };
    }

    private void awaitAdapterItemCount(
            ActivityScenario<ApplicationsActivity> scenario, int expectedCount) {
        CountDownLatch itemCountReached = new CountDownLatch(1);
        RecyclerView.Adapter<?>[] adapterHolder = new RecyclerView.Adapter<?>[1];
        RecyclerView.AdapterDataObserver[] observerHolder =
                new RecyclerView.AdapterDataObserver[1];

        scenario.onActivity(activity -> {
            RecyclerView.Adapter<?> adapter =
                    ((RecyclerView) activity.findViewById(R.id.recycler_applications)).getAdapter();
            adapterHolder[0] = adapter;
            if (adapter.getItemCount() == expectedCount) {
                itemCountReached.countDown();
                return;
            }
            observerHolder[0] = new RecyclerView.AdapterDataObserver() {
                private void checkItemCount() {
                    if (adapter.getItemCount() == expectedCount) {
                        itemCountReached.countDown();
                    }
                }

                @Override
                public void onChanged() {
                    checkItemCount();
                }

                @Override
                public void onItemRangeInserted(int positionStart, int itemCount) {
                    checkItemCount();
                }

                @Override
                public void onItemRangeRemoved(int positionStart, int itemCount) {
                    checkItemCount();
                }
            };
            adapter.registerAdapterDataObserver(observerHolder[0]);
        });

        boolean reached;
        try {
            reached = itemCountReached.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for the application list.", exception);
        }
        if (observerHolder[0] != null) {
            scenario.onActivity(activity ->
                    adapterHolder[0].unregisterAdapterDataObserver(observerHolder[0]));
        }
        final String[] state = new String[1];
        if (!reached) {
            scenario.onActivity(activity -> {
                RecyclerView recyclerView =
                        activity.findViewById(R.id.recycler_applications);
                int databaseCount;
                try (DatabaseHelper database = new DatabaseHelper(context)) {
                    databaseCount = database.searchJobApplications(
                            new SessionManager(context).getCurrentUserId(),
                            "",
                            null,
                            DatabaseHelper.ApplicationSortOrder.NEWEST_FIRST,
                            0).size();
                }
                state[0] = "search="
                        + ((TextInputEditText) activity.findViewById(
                                R.id.input_search_applications)).getText()
                        + ", status="
                        + ((ChipGroup) activity.findViewById(R.id.chip_group_status))
                                .getCheckedChipId()
                        + ", sort="
                        + ((Spinner) activity.findViewById(
                                R.id.spinner_application_sort)).getSelectedItemPosition()
                        + ", adapter="
                        + recyclerView.getAdapter().getItemCount()
                        + ", database="
                        + databaseCount;
            });
        }
        assertTrue("RecyclerView did not reach the expected item count. " + state[0], reached);
    }

    private void assertTableColumns(SQLiteDatabase database, String table, String... expected) {
        java.util.List<String> actual = new java.util.ArrayList<>();
        try (Cursor columns = database.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            while (columns.moveToNext()) {
                actual.add(columns.getString(columns.getColumnIndexOrThrow("name")));
            }
        }
        assertEquals(java.util.Arrays.asList(expected), actual);
    }

    private void assertPendingReminderAlarmExists(long reminderId) {
        PendingIntent alarm = pendingReminderAlarm(reminderId);
        assertNotNull(alarm);
    }

    private void assertPendingReminderAlarmDoesNotExist(long reminderId) {
        PendingIntent alarm = pendingReminderAlarm(reminderId);
        assertEquals(null, alarm);
    }

    private PendingIntent pendingReminderAlarm(long reminderId) {
        Intent alarmIntent = new Intent(context, ReminderReceiver.class)
                .setAction(ReminderScheduler.ACTION_REMINDER);
        int requestCode = (int) (reminderId ^ (reminderId >>> 32));
        return PendingIntent.getBroadcast(
                context, requestCode, alarmIntent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
    }
}
