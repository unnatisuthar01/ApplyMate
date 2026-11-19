package com.example.applymate;

import android.util.Patterns;

final class InputValidator {

    private InputValidator() {
    }

    static boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches();
    }

    static boolean isValidPassword(String password) {
        return password.length() >= 8;
    }

    static boolean isValidMobileNumber(String mobileNumber) {
        String digits = mobileNumber.replaceAll("[\\s()+-]", "");
        return digits.matches("\\d{7,15}");
    }
}
