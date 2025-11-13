package com.payten.whitelabel.utils;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;

import com.payten.whitelabel.R;
import com.payten.whitelabel.databinding.DialogPinRegistrationBinding;

import java.util.HashMap;
import java.util.Map;

public final class Utility {

    private static final String emailPattern = "^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$";

    public static boolean isEmailValid(String email) {
        if (email.matches(emailPattern)) {
            return true;
        }
        return false;
    }


    public static void checkLanguage(int i) {
        if (i == 1) {
            AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(
                            "en"
                    )
            );
        } else {
            AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(
                            "sl"
                    )
            );
        }
    }

    public static String getLanguage(int i) {
        if (i == 1) {
            return "en";
        } else {
            return "sl";
        }
    }

    public static String convertSerbianToEnglish(String text) {
        Map<Character, Character> mapping = new HashMap<>();
        mapping.put('Š', 'S');
        mapping.put('Č', 'C');
        mapping.put('Ć', 'C');
        mapping.put('Ž', 'Z');
        mapping.put('Đ', 'D');
        mapping.put('š', 's');
        mapping.put('č', 'c');
        mapping.put('ć', 'c');
        mapping.put('ž', 'z');
        mapping.put('đ', 'd');

        StringBuilder convertedText = new StringBuilder(text);
        for (Map.Entry<Character, Character> entry : mapping.entrySet()) {
            char serbianChar = entry.getKey();
            char englishChar = entry.getValue();
            convertedText = new StringBuilder(convertedText.toString().replace(serbianChar, englishChar));
        }

        return convertedText.toString();
    }

    public static void showDialogInfo(Context context, String message, boolean success, boolean isDark) {
        Dialog dialog = new Dialog(context);
        DialogPinRegistrationBinding dialogBinding = DialogPinRegistrationBinding.inflate(LayoutInflater.from(context));

        handleDialogDarkMode(dialogBinding, context, isDark);
        dialog.getWindow().setBackgroundDrawableResource(R.color.transparent);
        if (success) {
            dialogBinding.icon.setImageResource(R.drawable.icon_success);
            dialogBinding.warningLabel.setText(message);
        } else {
            dialogBinding.icon.setImageResource(R.drawable.icon_warning);
            dialogBinding.warningLabel.setText(message);
        }

        dialogBinding.btn.setText(context.getString(R.string.button_registration_back));

        dialogBinding.btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.setContentView(dialogBinding.getRoot());
        dialog.show();
    }

    public static void showDialogInfoEndOfDay(Context context, String message, boolean success, boolean isDark) {
        Dialog dialog = new Dialog(context);
        DialogPinRegistrationBinding dialogBinding = DialogPinRegistrationBinding.inflate(LayoutInflater.from(context));

        handleDialogDarkMode(dialogBinding, context, isDark);
        dialog.getWindow().setBackgroundDrawableResource(R.color.transparent);
        if (success) {
            dialogBinding.icon.setImageResource(R.drawable.icon_success);
            dialogBinding.warningLabel.setText(message);
        } else {
            dialogBinding.icon.setImageResource(R.drawable.icon_warning);
            dialogBinding.warningLabel.setText(message);
        }

        dialogBinding.btn.setText(context.getString(R.string.button_e2e_back));

        dialogBinding.btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.setContentView(dialogBinding.getRoot());
        dialog.show();
    }


    public static void handleDialogDarkMode(DialogPinRegistrationBinding dialogBinding, Context context, Boolean isDark) {
        if (isDark) {
            dialogBinding.getRoot().setBackgroundColor(
                    ContextCompat.getColor(
                            context,
                            R.color.globalBlackDialog
                    )
            );
            dialogBinding.warningLabel.setTextColor(
                    ContextCompat.getColor(context, R.color.white)
            );
            dialogBinding.btn.setBackgroundColor(
                    ContextCompat.getColor(
                            context,
                            R.color.globalBlackDialog
                    )
            );
        }
    }

}