package edu.dartmouth.cs.finditems.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.firebase.auth.FirebaseAuth;

import edu.dartmouth.cs.finditems.R;
// need to add implementation in gradle

public class SettingsActivity extends AppCompatActivity {

    //KEY
    private static final String TAG ="Debug";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Log.d(TAG, "SettingsActivity: onCreate()");

        //Action Bar set up
        getSupportActionBar().setTitle("Settings");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getSupportFragmentManager().beginTransaction().replace(R.id.settings, new SettingsFragment()).commit();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            // preference to logout
            final androidx.preference.Preference logoutPreference = findPreference("logout");
            logoutPreference.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    getActivity().finish();
                    Intent intent = new Intent(getActivity(), SignInActivity.class); // takes user back to login screen
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK); // clears back stack of existing activities so that user cannot press Android back button (on login screen) and go back into the app
                    FirebaseAuth.getInstance().signOut();
                    startActivity(intent);
                    return false;
                }
            });
        }
    }
}
