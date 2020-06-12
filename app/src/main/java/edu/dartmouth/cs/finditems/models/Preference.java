package edu.dartmouth.cs.finditems.models;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Preference {

    private static final String TAG = "debugging";

    private static SharedPreferences sharedPreferences;

    public Preference(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public String getProfileName() {
        return sharedPreferences.getString("name", "nan");
    }

    public void setProfileName(String name) {
        sharedPreferences.edit().putString("name", name).apply();
    }

    public String getProfileGender() {
        return sharedPreferences.getString("gender", "nan");
    }

    public static void setProfileGender(String gender) {
        sharedPreferences.edit().putString("gender", gender).apply();
    }

    public String getProfileEmail() {
        return sharedPreferences.getString("email", "nan");
    }

    public void setProfileEmail(String email) {
        sharedPreferences.edit().putString("email", email).apply();
    }

    public String getProfilePassword() {
        return sharedPreferences.getString("password", "nan");
    }

    public void setProfilePassword(String password) {
        sharedPreferences.edit().putString("password", password).apply();
    }

    public String getProfilePhone() {
        return sharedPreferences.getString("phone", "nan");
    }

    public void setProfilePhone(String phone) {
        sharedPreferences.edit().putString("phone", phone).apply();
    }
}
