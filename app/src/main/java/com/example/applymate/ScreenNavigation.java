package com.example.applymate;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.applymate.utils.SessionManager;

final class ScreenNavigation {

    private ScreenNavigation() {
    }

    static long requireUserId(Activity activity) {
        SessionManager sessionManager = new SessionManager(activity);
        if (sessionManager.isLoggedIn()) {
            return sessionManager.getCurrentUserId();
        }
        Intent welcomeIntent = new Intent(activity, WelcomeActivity.class);
        welcomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(welcomeIntent);
        activity.finish();
        return -1L;
    }

    static void configureBottomNavigation(Activity activity, boolean applicationsSelected) {
        TextView home = activity.findViewById(R.id.nav_home);
        TextView applications = activity.findViewById(R.id.nav_applications);
        TextView statistics = activity.findViewById(R.id.nav_statistics);
        TextView profile = activity.findViewById(R.id.nav_profile);

        home.setTextColor(activity.getColor(
                activity instanceof MainActivity
                        ? R.color.primary_blue : R.color.secondary_text));
        applications.setTextColor(activity.getColor(
                applicationsSelected ? R.color.primary_blue : R.color.secondary_text));
        statistics.setTextColor(activity.getColor(
                activity instanceof StatisticsActivity
                        ? R.color.primary_blue : R.color.secondary_text));
        home.setOnClickListener(view -> {
            if (!(activity instanceof MainActivity)) {
                Intent intent = new Intent(activity, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                activity.startActivity(intent);
                activity.finish();
            }
        });
        applications.setOnClickListener(view -> {
            if (!(activity instanceof ApplicationsActivity)) {
                Intent intent = new Intent(activity, ApplicationsActivity.class);
                intent.addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                activity.startActivity(intent);
                activity.finish();
            }
        });
        statistics.setOnClickListener(view -> {
            if (!(activity instanceof StatisticsActivity)) {
                Intent intent = new Intent(activity, StatisticsActivity.class);
                intent.addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                activity.startActivity(intent);
                activity.finish();
            }
        });
        View.OnClickListener placeholderClick = view -> Toast.makeText(
                activity,
                activity.getString(R.string.placeholder_coming_soon,
                        ((TextView) view).getText()),
                Toast.LENGTH_SHORT).show();
        profile.setOnClickListener(placeholderClick);
    }
}
