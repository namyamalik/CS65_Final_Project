package edu.dartmouth.cs.finditems.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.soundcloud.android.crop.Crop;

import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import edu.dartmouth.cs.finditems.BuildConfig;
import edu.dartmouth.cs.finditems.R;
import edu.dartmouth.cs.finditems.database.ItemsDataSource;
import edu.dartmouth.cs.finditems.models.Preference;

public class RegisterActivity extends AppCompatActivity {

    // keys
    private static final String TAG ="Debug";
    public Boolean parentLogin = false;
    public Boolean parentMainPage = false;

    // constants for saved instance keys
    private static final String RUN_KEY = "run";
    private static final String URI_INSTANCE_STATE_KEY = "saved_uri";

    // extras

    // constants for request codes
    private static final int STORAGE_PERMISSION_RC = 0;
    private static final int CAMERA_PERMISSION_RC = 1;
    private static final int LAUNCH_GALLERY_RC = 10;
    private static final int LAUNCH_CAMERA_RC = 11;

    // references for widgets
    private Button mChangeProfPicButton;
    private TextInputEditText mNameField, mEmailField, mPasswordField, mPhoneNumberField;
    private RadioButton mMaleRadioButton, mFemaleRadioButton;
    private ImageView mProfPicImageView;

    // reference to Preference
    private Preference preference;

    // run once variables
    private Boolean runOnce = true, run = false;

    // camera stuff
    private Uri imageUri;
    File photoFile;
    private Bitmap bitmap;

    // data source
    private ItemsDataSource datasource;

    // authentication
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Log.d(TAG, "RegisterActivity: onCreate()");

        // Action Bar set up
        setupActionBar();

        // reference to instantiation of widgets
        mProfPicImageView = findViewById(R.id.prof_pic_image_view);
        mChangeProfPicButton = findViewById(R.id.change_prof_pic_button);
        mNameField = findViewById(R.id.register_screen_name);
        mMaleRadioButton = findViewById(R.id.male_radio_button);
        mFemaleRadioButton = findViewById(R.id.female_radio_button);
        mEmailField = findViewById(R.id.register_screen_email);
        mPasswordField = findViewById(R.id.register_screen_password);
        mPhoneNumberField = findViewById(R.id.register_screen_phone);

        // Preference object
        preference = new Preference(this); // create Preference object

        // authentication instance
        mAuth = FirebaseAuth.getInstance();

        // as user for permissions when RegisterActivity is launched
        checkUserPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, STORAGE_PERMISSION_RC);

        // if change prof pic button is clicked
        mChangeProfPicButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAlertDialog();
            }
        });

        // detect what is parent activity to dictate the layout of the register page
        parentLogin = getIntent().getBooleanExtra(SignInActivity.PARENT_LOGIN, false);
        parentMainPage = getIntent().getBooleanExtra(MainActivity.EXTRA_PARENT_MAINPAGE, false);

        // save profile pic image state
        if (savedInstanceState != null) {
            run = savedInstanceState.getBoolean(RUN_KEY);
            if (run) {
                Log.d(TAG, "override");
                imageUri = savedInstanceState.getParcelable(URI_INSTANCE_STATE_KEY);
                Log.d(TAG, "saved uri " + imageUri);
                try {
                    Bitmap saved_bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri); // THIS ONLY WORKS FOR GALLERY PIC
                    mProfPicImageView.setImageBitmap(saved_bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // if user wants to edit profile then auto-fill registered credentials
        if (parentMainPage) {
            profileSetup();
        }
    }

    // ****************** action bar methods *************************** //

    // helper method to setup action bar
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar(); // returns reference to ActionBar object
        // detect what is parent activity to dictate the layout of the register page
        parentLogin = getIntent().getBooleanExtra(SignInActivity.PARENT_LOGIN, false);
        parentMainPage = getIntent().getBooleanExtra(MainActivity.EXTRA_PARENT_MAINPAGE, false);
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true); // show up button to navigate to one of two parent activities
            if (parentLogin) {
                actionBar.setTitle("Register");
            }
            else if (parentMainPage) {
                actionBar.setTitle("Profile");
            }
        }
    }

    // override to create items inside the menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // inflate the REGISTER or SAVE action button inside menu_register and add to action bar
        if (parentLogin) { // parent activity is login page
            getMenuInflater().inflate(R.menu.menu_register, menu);
        }
        else if (parentMainPage){ // parent activity is main page
            getMenuInflater().inflate(R.menu.menu_save, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: // if user clicks back button on menu
                if (parentLogin) { // go to login page
                    Intent intent = new Intent(RegisterActivity.this, SignInActivity.class);
                    startActivity(intent);
                }
                else if (parentMainPage) { // go to main activity page
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    startActivity(intent);
                }
                return true;
            case R.id.register_action_button: // if user clicks register action button on menu
                registerProfile();
                // delete all exercises from database for fresh user
                datasource = new ItemsDataSource(this);
                datasource.open();
                datasource.deleteAllItems();
                datasource.close();
                return true;
            case R.id.save_action_button: // if user clicks save action button on menu
                saveProfile();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ****************** profile related methods *************************** //

    // helper method to validate registration of profile
    private void registerProfile() {

        // set up error checking for name, email, password fields
        mNameField.setError(null);
        mEmailField.setError(null);
        mPasswordField.setError(null);

        // collect info to be saved by retrieving user input as strings
        String mNameValue = mNameField.getText().toString();
        String mGenderValue = null;
        if (mMaleRadioButton.isChecked()) {
            mGenderValue = "Male";
        } else if (mFemaleRadioButton.isChecked()) {
            mGenderValue = "Female";
        }
        String mEmailValue = RegisterActivity.this.mEmailField.getText().toString();
        String mPasswordValue = RegisterActivity.this.mPasswordField.getText().toString();
        String mPhoneNumberValue = RegisterActivity.this.mPhoneNumberField.getText().toString();

        // check if required fields are filled in
        if (TextUtils.isEmpty(mNameValue) || TextUtils.isEmpty(mEmailValue) || TextUtils.isEmpty(mPasswordValue)) {
            Toast.makeText(RegisterActivity.this, "Please fill in required fields", Toast.LENGTH_SHORT).show();
            if (TextUtils.isEmpty(mNameValue)) {
                mNameField.setError("This field is required");
            }
            if (TextUtils.isEmpty(mEmailValue)) {
                mEmailField.setError("This field is required");
            }
            if (TextUtils.isEmpty(mPasswordValue)) {
                mPasswordField.setError("This field is required");
            }
        }
        else if (!TextUtils.isEmpty(mEmailValue) && !mEmailValue.contains("@")
                || (!TextUtils.isEmpty(mPasswordValue) && mPasswordValue.length() < 6)) {
            // check if email address is valid and pop up error message otherwise
            if (!TextUtils.isEmpty(mEmailValue) && !mEmailValue.contains("@")) {
                mEmailField.setError("Email address must contain @ symbol");
            }
            // check if password is valid and pop up error message otherwise
            if (!TextUtils.isEmpty(mPasswordValue) && mPasswordValue.length() < 6) {
                mPasswordField.setError("Password must be at least 6 characters");
            }
        }
        else {
            // add field vales to shared preference object
            preference.setProfileName(mNameValue);
            Preference.setProfileGender(mGenderValue);
            preference.setProfileEmail(mEmailValue);
            preference.setProfilePassword(mPasswordValue);
            preference.setProfilePhone(mPhoneNumberValue);

            mAuth.createUserWithEmailAndPassword(mEmailValue, mPasswordValue)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();

                            Toast.makeText(RegisterActivity.this, "Authentication & Registration Successful!", Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(RegisterActivity.this, SignInActivity.class);
                            startActivity(intent); // direct user to login screen
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(RegisterActivity.this, "Authentication failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        }
    }

    // helper method to autofill the fields when profile page is displayed
    public void profileSetup() {
        // load prof pic
        loadSnap();

        // autofill name, email, pwd since these are required fields
        mNameField.setText(preference.getProfileName());
        mEmailField.setText(preference.getProfileEmail());
        mPasswordField.setText(preference.getProfilePassword());

        // check if user filled in the other non-required fields when registering and then autofill
        if (!preference.getProfilePhone().equals("nan")) {
            mPhoneNumberField.setText(preference.getProfilePhone());
        }
        if (preference.getProfileGender().equals("Male")){
            mMaleRadioButton.setChecked(true);
        }
        if (preference.getProfileGender().equals("Female")){
            mFemaleRadioButton.setChecked(true);
        }

        // make email field uneditable
        mEmailField.setEnabled(false);
    }

    // helper method to validate saving of profile (after user edits profile)
    private void saveProfile() {
        // set up error checking for required name and password fields
        mNameField.setError(null);
        mPasswordField.setError(null);

        // collect info to be saved by retrieving user input as strings
        String mNameValue = mNameField.getText().toString();
        String mGenderValue = null;
        if (mMaleRadioButton.isChecked()) {
            mGenderValue = "Male";
        } else if (mFemaleRadioButton.isChecked()) {
            mGenderValue = "Female";
        }
        final String mPasswordValue = RegisterActivity.this.mPasswordField.getText().toString();
        String mPhoneNumberValue = RegisterActivity.this.mPhoneNumberField.getText().toString();

        // make sure required fields are filled in (just name and pwd because email is uneditable)
        if (TextUtils.isEmpty(mNameValue) || TextUtils.isEmpty(mPasswordValue)) {
            Toast.makeText(RegisterActivity.this, "Please fill in required fields", Toast.LENGTH_SHORT).show();
            if (TextUtils.isEmpty(mNameValue)) {
                mNameField.setError("This field is required");
            }
            if (TextUtils.isEmpty(mPasswordValue)) {
                mPasswordField.setError("This field is required");
            }
        }
        // check if password is valid and pop up error message otherwise
        else if (!TextUtils.isEmpty(mPasswordValue) && mPasswordValue.length() < 6) {
            mPasswordField.setError("Password must be at least 6 characters");
        } else {
            // detect if password was changed
            boolean passwordChanged = false;
            if (!mPasswordValue.equals(preference.getProfilePassword())) {
                passwordChanged = true;
            }

            // take user to login screen if password was changed
            if (passwordChanged) {

                // update password credentials in Firebase
                final FirebaseUser currentUser = mAuth.getCurrentUser();
                assert currentUser != null;
                AuthCredential credentials = EmailAuthProvider.
                        getCredential(preference.getProfileEmail(), preference.getProfilePassword()); // old password (not yet added to preferences)
                currentUser.reauthenticate(credentials).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            currentUser.updatePassword(mPasswordValue).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Log.d(TAG, "Password updated");
                                    } else {
                                        Log.d(TAG, "Error password not updated");
                                    }
                                }
                            });
                        }
                        else {
                            Log.d(TAG, "Error auth failed");
                        }
                    }
                });

                // add field values to shared preference object
                preference.setProfileName(mNameValue);
                Preference.setProfileGender(mGenderValue);
                preference.setProfilePassword(mPasswordValue);
                preference.setProfilePhone(mPhoneNumberValue);

                Toast.makeText(RegisterActivity.this, "Password Updated. Please login again.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RegisterActivity.this, SignInActivity.class);
                startActivity(intent);
            }
            // otherwise take user to main page
            else {
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                startActivity(intent);
            }
        }
    }

    // ****************** permission methods *************************** //

    // check if camera and writing external permissions have previously been granted by user
    public void checkUserPermission (String permissionName,int requestCode){
        if (checkSelfPermission(permissionName) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{permissionName}, requestCode); // android automatically creates permission dialog
        }
    }

    // this method is invoked when requestPermissions is called to find out whether user granted permission or not
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_RC) {
            // user gave permission for external storage
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            }
            // want camera permission dialog box to pop up right after storage permission dialog box but only on OnCreate()
            if (runOnce) {
                checkUserPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_RC);
                runOnce = false;
            }
        }
        else if (requestCode == CAMERA_PERMISSION_RC) {
            // user gave permission for camera
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            }
        }
    }

    // helper method to show the alert dialog to change prof pic
    private void showAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Profile Picture");
        String[] items = new String[]{"Choose photo from gallery", "Take a new photo"};
        builder.setItems(items, new DialogInterface.OnClickListener() { // set items so that list view
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // user wants to choose photo from gallery
                if (which == 0) {
                    if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        startGallery();
                    } else {
                        checkUserPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, STORAGE_PERMISSION_RC);
                    }
                }
                // user wants to take a photo
                else if (which == 1) {
                    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        startCamera();
                    } else {
                        checkUserPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_RC);
                    }
                }
            }
        });
        // create and show the alert
        AlertDialog alert = builder.create();
        alert.show();
    }

    // third tier helper method to create image file
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        return image;
    }

    // second tier helper method to start gallery and choose a photo
    void startGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (galleryIntent.resolveActivity(getPackageManager()) != null) {
            photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // error while creating File
            }
            // if File was created successfully
            if (photoFile != null) {
                startActivityForResult(galleryIntent, LAUNCH_GALLERY_RC);
                Log.d(TAG, "starting gallery");
            }
        }
    }

    // second tier helper method to start camera and take a photo
    public void startCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            // create File where the photo should go
            photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // error while creating File
                Log.e("Error", ex.toString());
            }
            // if File created successfully
            if (photoFile != null) {
                imageUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID, photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(cameraIntent, LAUNCH_CAMERA_RC);
                Log.d(TAG, "starting cam");
            }
        }
    }

    // automatically invoked to process gallery and camera activities being launched
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LAUNCH_GALLERY_RC) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    imageUri = data.getData();
                    run = true;
                    beginCrop(imageUri);
                }
            }
        }
        else if (requestCode == LAUNCH_CAMERA_RC) {
            if (resultCode == RESULT_OK) {
                if (photoFile != null) {
                    beginCrop(FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID, photoFile));
                }
            }
        } else if (requestCode == Crop.REQUEST_CROP) {
            if (resultCode == RESULT_OK) {
                handleCrop(resultCode, data);
            }
        }
    }

    // ****************** cropping helper methods *************************** //

    // third tier method to start crop
    private void beginCrop(Uri source) {
        // Continue only if the File was successfully created
        if (photoFile != null) {
            Uri destination = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID, photoFile);
            Crop.of(source, destination).asSquare().start(this);
        }
    }

    // third tier method to handle crop
    private void handleCrop(int resultCode, Intent result) {
        if (resultCode == RESULT_OK) {
            Uri uri = Crop.getOutput(result);
            imageUri = uri; // cropped image becomes the new image uri (so that cropped image gets saved in onSave)
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                mProfPicImageView.setImageBitmap(bitmap);
                Log.d(TAG, "handle crop uri " + uri);
            } catch (Exception e) {
                Log.d("Error", "error");
            }

        } else if (resultCode == Crop.RESULT_ERROR) {
            Toast.makeText(this, Crop.getError(result).getMessage(), Toast.LENGTH_SHORT).show();
        }
        saveSnap();
    }

    // ****************** saving state *************************** //

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the cropped image capture uri before the activity goes into background
        outState.putBoolean(RUN_KEY, run);
        outState.putParcelable(URI_INSTANCE_STATE_KEY, imageUri);
        Log.d(TAG, "instance save uri " + imageUri);
    }

    // ****************** private helper functions ***************************//

    private void loadSnap() {
        // Load profile photo from internal storage
        try {
            FileInputStream fis = openFileInput(getString(R.string.profile_pic_filename));
            Bitmap bmap = BitmapFactory.decodeStream(fis);
            mProfPicImageView.setImageBitmap(bmap);
            fis.close();
        } catch (IOException e) {
            // Default profile photo if no photo saved before.
            mProfPicImageView.setImageResource(R.drawable.prof_pic_image_view);
        }
    }

    private void saveSnap() {
        // Commit all the changes into preference file
        // Save profile image into internal storage.
        mProfPicImageView.buildDrawingCache();
        Bitmap bmap = mProfPicImageView.getDrawingCache();
        try {
            FileOutputStream fos = openFileOutput(getString(R.string.profile_pic_filename), MODE_PRIVATE);
            bmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
