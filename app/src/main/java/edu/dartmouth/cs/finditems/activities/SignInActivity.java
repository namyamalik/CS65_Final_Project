package edu.dartmouth.cs.finditems.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import edu.dartmouth.cs.finditems.R;
import edu.dartmouth.cs.finditems.models.Preference;

public class SignInActivity extends AppCompatActivity {

    // keys
    private static final String TAG = "Debug";

    // extras
    public static final String PARENT_LOGIN = "login parent";

    // references for widgets
    private Button mSignInButton, mRegisterButton;
    private TextInputEditText mEmailField, mPasswordField;

    // reference to Preference
    private Preference preference;

    // authentication
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        Log.d(TAG, "SignInActivity: onCreate()");

        // Action Bar set up
        setupActionBar();

        // reference to instantiation of widgets
        mSignInButton = findViewById(R.id.sign_in_button);
        mRegisterButton = findViewById(R.id.register_button);
        mEmailField = findViewById(R.id.login_screen_email);
        mPasswordField = findViewById(R.id.login_screen_password);

        // Preference object
        preference = new Preference(this);

        // authentication instance
        mAuth = FirebaseAuth.getInstance();

        // auto-fill registered credentials if user has previously registered
        if (!preference.getProfileEmail().equals("nan") && !preference.getProfilePassword().equals("nan")) {
            mEmailField.setText(preference.getProfileEmail());
            mPasswordField.setText(preference.getProfilePassword());
        }

//        FirebaseUser currentUser = mAuth.getCurrentUser();

        // if login button is clicked
        mSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // set up error checking for name, email, password fields
                mEmailField.setError(null);
                mPasswordField.setError(null);

                String mEmailValue = mEmailField.getText().toString();
                String mPasswordValue = mPasswordField.getText().toString();

                // if user has inputted something for both fields
                if (!mEmailValue.isEmpty() && !mPasswordValue.isEmpty()) {
                    // check if user has registered
                    if (preference.getProfileEmail().equals("nan") && preference.getProfilePassword().equals("nan")) {
                        Toast.makeText(SignInActivity.this, "Unregistered user. Please click register button.", Toast.LENGTH_SHORT).show();
                        mEmailField.setError("Unregistered Email");
                        mPasswordField.setError("Unregistered Password");
                    }

                    // check if inputted email & password match registered email & password
                    else if (mEmailValue.equals(preference.getProfileEmail())
                            && mPasswordValue.equals(preference.getProfilePassword())) {

                        mAuth.signInWithEmailAndPassword(mEmailValue, mPasswordValue)
                                .addOnCompleteListener(SignInActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        // Sign in success, update UI with the signed-in user's information
                                        Log.d(TAG, "signInWithEmail:success");
                                        FirebaseUser user = mAuth.getCurrentUser();
                                        Toast.makeText(SignInActivity.this, "Login Successful!",
                                                Toast.LENGTH_LONG).show();

                                        // Toast.makeText(SignInActivity.this, "Login Successful!", Toast.LENGTH_LONG).show();
                                        Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                                        startActivity(intent);

                                    } else {
                                        // If sign in fails, display a message to the user.
                                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                                        Toast.makeText(SignInActivity.this, "Authentication failed: "
                                                        + task.getException().getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    }

                                    }
                                });
                }
                // notify user that inputted credentials are incorrect
                else {
                    Toast.makeText(SignInActivity.this, "Incorrect email or password", Toast.LENGTH_SHORT).show();
                    mEmailField.setError("Incorrect Credentials");
                    mPasswordField.setError("Incorrect Credentials");
                }
                }
                // if user has left one or both of the fields empty
                else {
                    Toast.makeText(SignInActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    if (mEmailValue.isEmpty()) {
                        mEmailField.setError("This field is required");
                    }
                    if (mPasswordValue.isEmpty()) {
                        mPasswordField.setError("This field is required");
                    }
                }

            }
        });

        // if register button is clicked
        mRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SignInActivity.this, RegisterActivity.class);
                intent.putExtra(PARENT_LOGIN, true);
                startActivity(intent);
            }
        });
    }

    // ****************** action bar methods *************************** //

    // helper method to set up action bar
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar(); // returns reference to ActionBar object
        if (actionBar != null) {
            actionBar.setTitle("Welcome to FindItems!"); // could also have set label in manifest
        }
    }

    // override to create items inside the menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // inflate the ABOUT THIS APP action button inside menu_sign_in and add to action bar
        getMenuInflater().inflate(R.menu.menu_sign_in, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.about_action_button) {
            Intent intent = new Intent(SignInActivity.this, AboutActivity.class);
            intent.putExtra(PARENT_LOGIN, true);
            startActivity(intent); // direct user to about page
            return true;
        }
        return super.onOptionsItemSelected(item);
        // no up button on this activity, so no code needed for that
    }
}
