package com.example.applymate;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.View;
import android.widget.TextView;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;

import com.example.applymate.database.DatabaseHelper;
import com.example.applymate.models.User;
import com.example.applymate.utils.PasswordHasher;
import com.example.applymate.utils.SessionManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.security.GeneralSecurityException;

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
        assertEquals("com.example.applymate", context.getPackageName());
    }

    @Test
    public void registrationPersistsHashedPasswordAndRejectsDuplicateEmail()
            throws GeneralSecurityException {
        try (ActivityScenario<RegistrationActivity> ignored =
                     enterRegistration("Taylor@example.com", "Passw0rd99", "Passw0rd99")) {
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
                    .perform(ViewActions.typeText("missing@example.com"));
            Espresso.onView(withId(R.id.input_password))
                    .perform(ViewActions.typeText("Passw0rd99"), ViewActions.closeSoftKeyboard());
            Espresso.onView(withId(R.id.button_login)).perform(ViewActions.click());
            assertFalse(new SessionManager(context).isLoggedIn());

            Espresso.onView(withId(R.id.input_email))
                    .perform(ViewActions.replaceText("taylor@example.com"));
            Espresso.onView(withId(R.id.input_password))
                    .perform(ViewActions.replaceText("WrongPass99"), ViewActions.closeSoftKeyboard());
            Espresso.onView(withId(R.id.button_login)).perform(ViewActions.click());
            assertFalse(new SessionManager(context).isLoggedIn());

            Espresso.onView(withId(R.id.input_password))
                    .perform(ViewActions.replaceText("Passw0rd99"), ViewActions.closeSoftKeyboard());
            Espresso.onView(withId(R.id.button_login)).perform(ViewActions.click());
            Espresso.onView(withId(R.id.button_logout))
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
            Espresso.onView(withId(R.id.input_email))
                    .perform(ViewActions.typeText("taylor@example.com"));
            Espresso.onView(withId(R.id.input_current_password))
                    .perform(ViewActions.typeText("Passw0rd99"));
            Espresso.onView(withId(R.id.input_new_password))
                    .perform(ViewActions.typeText("NewPassw0rd88"));
            Espresso.onView(withId(R.id.input_confirm_new_password))
                    .perform(ViewActions.typeText("NewPassw0rd88"), ViewActions.closeSoftKeyboard());
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
            Espresso.onView(withId(R.id.button_logout)).perform(ViewActions.click());
            assertFalse(new SessionManager(context).isLoggedIn());
            Espresso.onView(withId(R.id.button_login))
                    .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
            Espresso.pressBack();
            Espresso.onView(withId(R.id.button_login))
                    .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        }

        try (DatabaseHelper reopenedDatabase = new DatabaseHelper(context)) {
            assertTrue(reopenedDatabase.userExists("taylor@example.com"));
        }
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
            assertEquals(2, upgraded.getVersion());
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
                    "id", "job_id", "reminder_title", "reminder_date",
                    "reminder_time", "reminder_type");
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
                    new String[]{"job_id", "reminder_title", "reminder_date"},
                    "id = ?",
                    new String[]{"9"},
                    null, null, null)) {
                assertTrue(reminder.moveToFirst());
                assertEquals(7, reminder.getLong(0));
                assertEquals("Interview prep", reminder.getString(1));
                assertEquals(1700000001000L, reminder.getLong(2));
            }
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

    private ActivityScenario<RegistrationActivity> enterRegistration(
            String email, String password, String confirmation) {
        ActivityScenario<RegistrationActivity> scenario =
                ActivityScenario.launch(RegistrationActivity.class);
        Espresso.onView(withId(R.id.input_full_name))
                .perform(ViewActions.typeText("Taylor Applicant"));
        Espresso.onView(withId(R.id.input_email)).perform(ViewActions.typeText(email));
        Espresso.onView(withId(R.id.input_mobile)).perform(ViewActions.typeText("5551234567"));
        Espresso.onView(withId(R.id.input_password)).perform(ViewActions.typeText(password));
        Espresso.onView(withId(R.id.input_confirm_password))
                .perform(ViewActions.typeText(confirmation), ViewActions.closeSoftKeyboard());
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

    private void assertTableColumns(SQLiteDatabase database, String table, String... expected) {
        java.util.List<String> actual = new java.util.ArrayList<>();
        try (Cursor columns = database.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            while (columns.moveToNext()) {
                actual.add(columns.getString(columns.getColumnIndexOrThrow("name")));
            }
        }
        assertEquals(java.util.Arrays.asList(expected), actual);
    }
}
