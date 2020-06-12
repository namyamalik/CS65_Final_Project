package edu.dartmouth.cs.finditems.activities;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import edu.dartmouth.cs.finditems.R;

public class AboutActivity extends AppCompatActivity {

    // keys
    private static final String TAG = "Debug";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        Log.d(TAG, "AboutActivity: onCreate()");

        // Action Bar set up
        setupActionBar();
    }

    // ****************** action bar methods *************************** //

    // helper method to set up action bar
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar(); // returns reference to ActionBar object
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true); // show up button to navigate to parent activity (sign in page)
            actionBar.setTitle("About"); // could also have set label in manifest
        }
    }
}
