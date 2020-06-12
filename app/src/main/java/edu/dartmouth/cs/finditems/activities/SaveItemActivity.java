package edu.dartmouth.cs.finditems.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceImageLabelerOptions;
import com.google.gson.Gson;
import com.soundcloud.android.crop.Crop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import edu.dartmouth.cs.finditems.BuildConfig;
import edu.dartmouth.cs.finditems.R;
import edu.dartmouth.cs.finditems.adapters.MainListViewAdapter;
import edu.dartmouth.cs.finditems.database.ItemsDataSource;

public class SaveItemActivity extends AppCompatActivity {

    // KEYS
    private static final String TAG ="Debug";

    // EXTRAS TO SEND TO OTHER ACTIVITIES
    public static final String EXTRA_ITEM_NAME = "extra item name";
    public static final String EXTRA_ITEM_DATE = "extra item date";
    public static final String EXTRA_ITEM_TIME = "extra item time";
    public static final String EXTRA_ITEM_LOCALIZATION = "extra item localization";
    public static final String EXTRA_ITEM_ROW_ID = "extra item row id";
    public static final String EXTRA_GPS_DATA = "extra gps data";

//    public static boolean dbInsertion;

    // BOOLEANS FOR GRABBING EXTRAS FROM OTHER ACTIVITIES
    public Boolean parentAddNewItem = false;
    public Boolean parentSavedItemTouchable = false;
    public Boolean parentMap = false;

    // references for widgets
    private EditText mItemNameEditText, mItemLocalisationEditText;
    private TextView mItemDateTextView, mItemTimeTextView;
    private ImageView mItemPictureImageView, mMicViewItemName, mMicViewItemLoc;
    private Button mChangeImageButton, mMapButton;

    // constants for request codes
    private static final int STORAGE_PERMISSION_RC = 0;
    private static final int CAMERA_PERMISSION_RC = 1;
    private static final int LOCATION_PERMISSION_RC = 2;
    private static final int LAUNCH_GALLERY_RC = 10;
    private static final int LAUNCH_CAMERA_RC = 11;
    private static final int SPEECH_INPUT_NAME = 20;
    private static final int SPEECH_INPUT_LOCALISATION = 30;

    // constants for saved instance keys (to save item picture upon rotation)
    private static final String RUN_KEY = "run";
    private static final String URI_INSTANCE_STATE_KEY = "saved_uri";
    private static final String MIC_ITEM_NAME_KEY = "mic_item_name";
    private static final String MIC_ITEM_LOCALISATION_KEY = "mic_item_localisation";

    // camera stuff
    public Uri imageUri;
    File photoFile;
    private Bitmap bitmap;
    private Bitmap defaultBitmap;

    // run once variables
    private Boolean runOnce = true, run = false;

    private ItemsDataSource datasource;
    private Iterable<? extends FirebaseVisionImageLabel> labels;

    // location stuff
    private FusedLocationProviderClient fusedLocationClient;
    ArrayList SavedLatLongs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "SaveItemActivity: onCreate()");

        // detect what is parent activity to dictate the layout of the save/edit item page
        parentAddNewItem = getIntent().getBooleanExtra(MainActivity.EXTRA_PARENT_NEW_ITEM_BUTTON, false);
        parentSavedItemTouchable = getIntent().getBooleanExtra(MainListViewAdapter.EXTRA_PARENT_SAVED_ITEM_TOUCHABLE, false);
        parentMap = getIntent().getBooleanExtra(MapsActivity.EXTRA_PARENT_MAP, false);

        // if parent is main page (user adding new item)
        if (parentAddNewItem) {
//            Log.d(TAG, "parent add new item");
            setContentView(R.layout.activity_save_item);
        }
        // if parent is main page (user clicking on saved item)
        else if (parentSavedItemTouchable) {
//            Log.d(TAG, "parent touchable");
            setContentView(R.layout.activity_save_item_edit);
        }
        // if parent is map
        else if (parentMap){
//            Log.d(TAG, "parent map");
            setContentView(R.layout.activity_save_item_edit);
        }

        // Action Bar set up
        setupActionBar();

        //open
        datasource = new ItemsDataSource(this);
        datasource.open();

        // instantiation of widgets
        mItemNameEditText = findViewById(R.id.ItemNameEditText);
        mItemLocalisationEditText = findViewById(R.id.ItemLocalisationEditText);
        mItemPictureImageView = findViewById(R.id.ItemPicture);
        mItemDateTextView = findViewById(R.id.ItemDateTextView);
        mItemTimeTextView = findViewById(R.id.ItemTimeTextView);
        mChangeImageButton = findViewById(R.id.ChangeImageButton);
        mMicViewItemLoc = findViewById(R.id.MicViewItemLoc);
        mMicViewItemName = findViewById(R.id.MicViewItemName);
        mMapButton = findViewById(R.id.MapButton);

        // if parent is main page (user clicking on saved item)
        if (parentSavedItemTouchable) {
            setFieldsParentSavedItem();
        }
        // if parent is map
        else if (parentMap){
            setFieldsParentMap();
        }

        // if change prof pic button is clicked
        if (mChangeImageButton != null) {
            mChangeImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showAlertDialog();
                }
            });
        }

        // if map button is clicked
        if (mMapButton != null){
            mMapButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                // retrieve gps data for map activity

                // extras for text views on save item page
                Intent intent = new Intent(SaveItemActivity.this, MapsActivity.class);
                intent.putExtra(EXTRA_ITEM_NAME, mItemNameEditText.getText().toString());
                intent.putExtra(EXTRA_ITEM_DATE, mItemDateTextView.getText().toString());
                intent.putExtra(EXTRA_ITEM_TIME, mItemTimeTextView.getText().toString());
                intent.putExtra(EXTRA_ITEM_LOCALIZATION, mItemLocalisationEditText.getText().toString());

                // if user clicks on touchable saved item from main page
                if (parentSavedItemTouchable) {
                    String saved_gps_data = getIntent().getStringExtra(MainListViewAdapter.EXTRA_ITEM_GPS);
                    Long saved_item_row_id = getIntent().getLongExtra(MainListViewAdapter.EXTRA_ROW_ID, 0); // retrieve row id of entry that is clicked
                    intent.putExtra(EXTRA_GPS_DATA, saved_gps_data); // extra for gps data (to show marker on map activity)
                    intent.putExtra(EXTRA_ITEM_ROW_ID, saved_item_row_id);
                }
                // if user clicks on map button, goes back to save item activity, then clicks on map button again
                else if (parentMap) {
                    String saved_gps_data = getIntent().getStringExtra(MapsActivity.EXTRA_ITEM_GPS_DATA);
                    Long saved_item_row_id = getIntent().getLongExtra(MapsActivity.EXTRA_ITEM_ROW_ID, 0); // retrieve row id of entry
                    intent.putExtra(EXTRA_ITEM_ROW_ID, saved_item_row_id);
                    intent.putExtra(EXTRA_GPS_DATA, saved_gps_data); // extra for gps data (to show marker on map activity)
//                    Log.d("debug", "sending row id " + saved_item_row_id);
                }

                startActivity(intent); // direct user to map activity;
                }
            });
        }

        //Voice Recognition
        if (mMicViewItemName != null) {
            mMicViewItemName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getSpeechInput(MIC_ITEM_NAME_KEY);
                }
            });
        }
        if (mMicViewItemLoc != null) {
            mMicViewItemLoc.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getSpeechInput(MIC_ITEM_LOCALISATION_KEY);
                }
            });
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(SaveItemActivity.this);

        checkUserPermission(Manifest.permission.ACCESS_FINE_LOCATION, LOCATION_PERMISSION_RC);

        // save profile pic image state
        if (savedInstanceState != null) {
            run = savedInstanceState.getBoolean(RUN_KEY);
            if (run) {
//                Log.d(TAG, "override");
                imageUri = savedInstanceState.getParcelable(URI_INSTANCE_STATE_KEY);
//                Log.d(TAG, "saved uri " + imageUri);

                try {
                    Bitmap saved_bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                    mItemPictureImageView.setImageBitmap(saved_bitmap);
//                    Log.d(TAG, "setImageBitmap : saved_bitmap = " + saved_bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    // ****************** API pictures methods *************************** //
    private void runDetector(Bitmap bitmap) {

        FirebaseVisionImage image;
        image = FirebaseVisionImage.fromBitmap(bitmap);

        FirebaseVisionOnDeviceImageLabelerOptions options =
                new FirebaseVisionOnDeviceImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.8f)
                .build();
        FirebaseVisionImageLabeler labeler = FirebaseVision.getInstance()
                .getOnDeviceImageLabeler(options);

        labeler.processImage(image)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionImageLabel> labels) {
                        // Task completed successfully
                        // ...
                        for (FirebaseVisionImageLabel label: labels) { //To see all label on Logcat but useless for the App
                            String text = label.getText();
                            String entityId = label.getEntityId();
                            float confidence = label.getConfidence();
                            Log.d(TAG, "Labelers" + " " + text + " " + entityId + " " + confidence );

                        }
                        if (labels != null && !labels.isEmpty()) {
                            String text_loc = labels.get(0).getText();
                            mItemNameEditText.setText(text_loc);
                        }
                        else {
                            mItemNameEditText.setText("Unknown Item");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Task failed with an exception
                        // ...
                    }
                });

    }

    // ****************** voice recognition methods *************************** //

    public void getSpeechInput(String MIC_KEY) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        if (intent.resolveActivity(getPackageManager()) !=null) {
            if (MIC_KEY.equals(MIC_ITEM_NAME_KEY)) {
                startActivityForResult(intent, SPEECH_INPUT_NAME);
            }
            else if (MIC_KEY.equals(MIC_ITEM_LOCALISATION_KEY)) {
                startActivityForResult(intent, SPEECH_INPUT_LOCALISATION);
            }
        }
        else {
            Toast.makeText(this, "Your device does not support speech input", Toast.LENGTH_SHORT).show();
        }
    }


    // ****************** action bar methods *************************** //

    // helper method to setup action bar
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar(); // returns reference to ActionBar object
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true); // show up button to navigate to parent activity (main page)
            if (parentAddNewItem) {
                actionBar.setTitle("Add New Item");
            }
            else if (parentMap || parentSavedItemTouchable){
                actionBar.setTitle("Saved Item Info");
            }
        }
    }

    // override to create items inside the menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (parentAddNewItem) {
            getMenuInflater().inflate(R.menu.menu_save, menu); // shows SAVE button
        }
        else {
            getMenuInflater().inflate(R.menu.menu_item_information, menu); // shows UPDATE and DELETE buttons
        }
            return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Log.d(TAG, "SaveItemActivity: onOptionsItemSelected()");
        switch (item.getItemId()) {
            case R.id.save_action_button:
                if (mItemNameEditText.getText().toString().isEmpty()) {
                    mItemNameEditText.setError("Field Required");
                    return true;
                }
                else {
                    addNewItem();
                    // add delay so that new activity (where getAllItems is called) is not started before insertion into db is complete
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Intent intent = new Intent(SaveItemActivity.this, MainActivity.class);
                    startActivity(intent);
                    return true;
                }
            case R.id.delete_action_button:
                deleteItem();
                // go to main page activity
                Intent deleteIntent = new Intent(SaveItemActivity.this, MainActivity.class);
                startActivity(deleteIntent);
            case R.id.update_action_button:
                if (mItemNameEditText.getText().toString().isEmpty()) {
                    mItemNameEditText.setError("Field Required");
                    return true;
                }
                else {
                    updateItem();
                    // add delay so that new activity (where getAllItems is called) is not started before update of db is complete
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Intent updateIntent = new Intent(SaveItemActivity.this, MainActivity.class);
                    startActivity(updateIntent);
                }
        }
        return super.onOptionsItemSelected(item);
    }

    // ****************** Set saved fields methods *************************** //

    // when saved item touchable ic clicked from main page
    void setFieldsParentSavedItem(){

        Log.d(TAG, "SaveItemActivity: setFieldsParentSavedItem()");

        // set item name
        String saved_item_name = getIntent().getStringExtra(MainListViewAdapter.EXTRA_ITEM_NAME);
        mItemNameEditText.setText(saved_item_name);

        // set item picture
        Long saved_item_row_id = getIntent().getLongExtra(MainListViewAdapter.EXTRA_ROW_ID, 0); // retrieve row id of entry that is clicked
        Bitmap b = datasource.getItemPicture(saved_item_row_id); // call method to retrieve saved picture from database
        if (b != null) { // if image has been saved to database
            mItemPictureImageView.setImageBitmap(b);
        }
        else { // default image
            mItemPictureImageView.setImageResource(R.drawable.default_item_location_pic);
        }

        // set item location
        String saved_item_localisation = getIntent().getStringExtra(MainListViewAdapter.EXTRA_ITEM_LOCALISATION);
        mItemLocalisationEditText.setText(saved_item_localisation);

        // set item date
        String saved_item_date = getIntent().getStringExtra(MainListViewAdapter.EXTRA_ITEM_DATE);
        mItemDateTextView.setText("Last Updated: " + saved_item_date);

        // set item time
        String saved_item_time = getIntent().getStringExtra(MainListViewAdapter.EXTRA_ITEM_TIME);
        mItemTimeTextView.setText("at " + saved_item_time);

        // don't need to set gps data on this screen (wait till map activity)
    }

    // when saved item touchable ic clicked from main page
    void setFieldsParentMap(){

        Log.d(TAG, "SaveItemActivity: setFieldsParentMap()");

        // set item name
        String saved_item_name = getIntent().getStringExtra(MapsActivity.EXTRA_ITEM_NAME);
        mItemNameEditText.setText(saved_item_name);

        // set item picture
        Long saved_item_row_id = getIntent().getLongExtra(MapsActivity.EXTRA_ITEM_ROW_ID, 0); // retrieve row id of entry that is clicked
        Bitmap b = datasource.getItemPicture(saved_item_row_id); // call method to retrieve saved picture from database
        Log.d(TAG, "SaveItem: set pic " + saved_item_row_id);
        if (b != null) { // if image has been saved to database
            Log.d(TAG, "SaveItem: set pic not null " + saved_item_row_id);
            mItemPictureImageView.setImageBitmap(b);
        }
        else { // default image
            mItemPictureImageView.setImageResource(R.drawable.default_item_location_pic);
        }

        // set item location
        String saved_item_localisation = getIntent().getStringExtra(MapsActivity.EXTRA_ITEM_LOCALIZATION);
        mItemLocalisationEditText.setText(saved_item_localisation);

        // set item date
        String saved_item_date = getIntent().getStringExtra(MapsActivity.EXTRA_ITEM_DATE);
        mItemDateTextView.setText(saved_item_date);

        // set item time
        String saved_item_time = getIntent().getStringExtra(MapsActivity.EXTRA_ITEM_TIME);
        mItemTimeTextView.setText(saved_item_time);

        // don't need to set gps data on this screen (only need for map activity)
    }


    // ****************** Item  methods *************************** //
    // save item in database when save button is clicked
    private void addNewItem() {
        // starts the task of saving/inserting the item in the database
        new saveInDatabaseTask().execute();
    }

    // delete item from database when delete button is clicked
    private void deleteItem() {
        // starts the task of deleting the item from the database
        new deleteFromDatabaseTask().execute();
    }

    // update item in database when update button is clicked
    private void updateItem() {
        // starts the task of updating the item in the database
        new updateInDatabaseTask().execute();
    }

    private String getDate() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy");
        String Date = simpleDateFormat.format(calendar.getTime());
        return Date;
    }

    private String getTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm:ss");
        String Time = simpleDateFormat.format(calendar.getTime());
        return Time;
    }

    // ****************** Async Task for insertion into database *************************** //
    class saveInDatabaseTask extends AsyncTask<Void, Integer, Void> {
        String itemName, itemLocalisation, itemDate, itemTime;
        byte[] itemImage;
        String jsonArray;

        // A callback method executed on UI thread on starting the task
        @SuppressLint("WrongThread")
        @Override
        protected void onPreExecute() {
            Log.d(TAG, "SaveItemActivity: saveInDatabaseTask onPreExecute()");

            // get data from the fields user has filled in (done on UI thread)
            itemName = mItemNameEditText.getText().toString();
            itemLocalisation = mItemLocalisationEditText.getText().toString();
            itemDate = getDate();
            itemTime = getTime();

            // convert from bitmap to byte array (saved in database as byte array)
            if (bitmap != null) { // if picture has been changed from default image
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 512,
                        512, true); // needed to scale down so it could be stored in db
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                scaled.compress(Bitmap.CompressFormat.PNG, 0, stream);
                itemImage = stream.toByteArray();
                Log.d("debug", "InsertionAsyncTask: bitmap not null");
            }
        }


        // A callback method executed on non UI thread, invoked after
        // onPreExecute method if exists
        // Takes a set of parameters of the type defined in your class implementation. This method will be
        // executed on the background thread, so it must not attempt to interact with UI objects.
        @Override
        protected Void doInBackground(Void... params) {
            Log.d(TAG, "SaveItemActivity: saveInDatabaseTask doInBackground()");

            // if location can be found then get lat/long and add everything to database
            fusedLocationClient.getLastLocation().addOnSuccessListener(SaveItemActivity.this,
                    new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // get last location
                            if (location != null) {
                                SavedLatLongs.add(location.getLatitude());
                                SavedLatLongs.add(location.getLongitude());
//                              //Log.d("debug location", "loc is " + SavedLatLongs.get(0) + SavedLatLongs.get(1));

                                Gson gson = new Gson();
                                jsonArray = gson.toJson(SavedLatLongs);

//                                Log.d("debug location", "json array is " + jsonArray);

                                // call the insertion into database method on background thread
                                datasource.addItems(itemName, itemImage, itemLocalisation, itemDate, itemTime, jsonArray);
                            }
                        }
                    });

            // if location cannot be found (user clicked deny location access) then just add to db
            fusedLocationClient.getLastLocation().addOnFailureListener(SaveItemActivity.this,
                    new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // call the insertion into database method on background thread
                            datasource.addItems(itemName, itemImage, itemLocalisation, itemDate, itemTime, jsonArray);
                        }
                    });

            // insert delay so tha insertion into db finishes before other methods of Aysnc Task happen
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            publishProgress(); // Invokes onProgressUpdate()
            return null;
        }

        // A callback method executed on UI thread, invoked by the publishProgress()
        // from doInBackground() method
        // Overrider this handler to post interim updates to the UI thread. This handler receives the set of parameters
        // passed in publishProgress from within doInbackground.
        @Override
        protected void onProgressUpdate(Integer... values) {
            Log.d(TAG, "SaveItemActivity: saveInDatabaseTask onProgressUpdate()");
        }

        // A callback method executed on UI thread, invoked after the completion of the task
        // When doInbackground has completed, the return value from that method is passed into this event
        // handler.
        @Override
        protected void onPostExecute(Void result) {
            Log.d(TAG, "SaveItemActivity: saveInDatabaseTask onPostExecute()");
            // the UI is actually updated in the Main Page since that is where the saved items are displayed
        }
    }

    // ****************** Async Task for deletion from database *************************** //
    class deleteFromDatabaseTask extends AsyncTask<Void, Integer, Void> {
        // A callback method executed on UI thread on starting the task
        @Override
        protected void onPreExecute() {
            // no task necessary to be done on UI before deletion begins
        }

        // A callback method executed on non UI thread, invoked after
        // onPreExecute method if exists
        // Takes a set of parameters of the type defined in your class implementation. This method will be
        // executed on the background thread, so it must not attempt to interact with UI objects.
        @Override
        protected Void doInBackground(Void... params) {
            // call the deletion from database method on background thread

            // if parent is touchable saved item, grab extra from main list view parent
            if (parentSavedItemTouchable) {
                datasource.deleteItem(getIntent().getLongExtra(MainListViewAdapter.EXTRA_ROW_ID, 0));
            }
            // if parent is touchable saved item, grab extra from map activity parent
            else if (parentMap) {
                datasource.deleteItem(getIntent().getLongExtra(MapsActivity.EXTRA_ITEM_ROW_ID, 0));
            }
            publishProgress(); // Invokes onProgressUpdate()
            return null;
        }

        // A callback method executed on UI thread, invoked by the publishProgress()
        // from doInBackground() method
        // Overrider this handler to post interim updates to the UI thread. This handler receives the set of parameters
        // passed in publishProgress from within doInbackground.
        @Override
        protected void onProgressUpdate(Integer... values) {
        }

        // A callback method executed on UI thread, invoked after the completion of the task
        // When doInbackground has completed, the return value from that method is passed into this event
        // handler.
        @Override
        protected void onPostExecute(Void result) {
            // the UI is actually updated in Main Page since that is where the saved items are displayed/removed from
        }
    }

    // ****************** Async Task for updating of database *************************** //
    class updateInDatabaseTask extends AsyncTask<Void, Integer, Void> {
        String itemName, itemLocalisation, itemDate, itemTime;
        byte[] itemImage;
        String jsonArray;
        Long saved_item_row_id;

        // A callback method executed on UI thread on starting the task
        @SuppressLint("WrongThread")
        @Override
        protected void onPreExecute() {
            Log.d(TAG, "SaveItemActivity: updateInDatabaseTask onPreExecute()");

            // get data from the fields user has filled in (done on UI thread)
            itemName = mItemNameEditText.getText().toString();
            itemLocalisation = mItemLocalisationEditText.getText().toString();
            itemDate = getDate();
            itemTime = getTime();

            // convert from bitmap to byte array (saved in database as byte array)
            Bitmap b = ((BitmapDrawable)mItemPictureImageView.getDrawable()).getBitmap();
            if (b != null) { // if picture has been changed from default image
                Bitmap scaled = Bitmap.createScaledBitmap(b, 512,
                        512, true); // needed to scale down so it could be stored in db
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                scaled.compress(Bitmap.CompressFormat.PNG, 0, stream);
                itemImage = stream.toByteArray();
                Log.d("debug", "UpdatingAsyncTask: b not null");
            }

            // get row id so that update function knows which row to update
            // if parent is touchable saved item, grab extra from main list view parent
            if (parentSavedItemTouchable) {
                saved_item_row_id = getIntent().getLongExtra(MainListViewAdapter.EXTRA_ROW_ID, 0);
            }
            else if (parentMap) {
                saved_item_row_id = getIntent().getLongExtra(MapsActivity.EXTRA_ITEM_ROW_ID, 0);
            }
        }

        // A callback method executed on non UI thread, invoked after
        // onPreExecute method if exists
        // Takes a set of parameters of the type defined in your class implementation. This method will be
        // executed on the background thread, so it must not attempt to interact with UI objects.
        @Override
        protected Void doInBackground(Void... params) {
            Log.d(TAG, "SaveItemActivity: updateInDatabaseTask doInBackground()");

            // if location can be found then get lat/long and add everything to database
            fusedLocationClient.getLastLocation().addOnSuccessListener(SaveItemActivity.this,
                    new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // get last location
                            if (location != null) {
                                SavedLatLongs.add(location.getLatitude());
                                SavedLatLongs.add(location.getLongitude());
//                              //Log.d("debug location", "loc is " + SavedLatLongs.get(0) + SavedLatLongs.get(1));

                                Gson gson = new Gson();
                                jsonArray = gson.toJson(SavedLatLongs);

//                                Log.d("debug location", "json array is " + jsonArray);

                                // call the insertion into database method on background thread
                                datasource.updateItem(itemName, itemImage, itemLocalisation, itemDate,
                                        itemTime, jsonArray, saved_item_row_id);
                            }
                        }
                    });

            // if location cannot be found (user clicked deny location access) then just add to db
            fusedLocationClient.getLastLocation().addOnFailureListener(SaveItemActivity.this,
                    new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // call the insertion into database method on background thread
                            datasource.updateItem(itemName, itemImage, itemLocalisation, itemDate,
                                    itemTime, jsonArray, saved_item_row_id);
                        }
                    });

            // insert delay so that update of db finishes before other methods of Aysnc Task happen
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            publishProgress(); // Invokes onProgressUpdate()
            return null;
        }

        // A callback method executed on UI thread, invoked by the publishProgress()
        // from doInBackground() method
        // Overrider this handler to post interim updates to the UI thread. This handler receives the set of parameters
        // passed in publishProgress from within doInbackground.
        @Override
        protected void onProgressUpdate(Integer... values) {
            Log.d(TAG, "SaveItemActivity: updateInDatabaseTask onProgressUpdate()");
        }

        // A callback method executed on UI thread, invoked after the completion of the task
        // When doInbackground has completed, the return value from that method is passed into this event
        // handler.
        @Override
        protected void onPostExecute(Void result) {
            Log.d(TAG, "SaveItemActivity: updateInDatabaseTask onPostExecute()");
            // the UI is actually updated in the Main Page since that is where the saved items are displayed
        }
    }

    // ****************** permission methods *************************** //

    // check if camera and writing external permissions have previously been granted by user
    public void checkUserPermission(String permissionName, int requestCode) {
//        Log.d("debug location", "1");
        if (checkSelfPermission(permissionName) != PackageManager.PERMISSION_GRANTED) {
//            Log.d("debug location", "2");
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
        else if (requestCode == LOCATION_PERMISSION_RC) {
//            Log.d("debug location", "3");
            // user gave permission for location
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    checkUserPermission(Manifest.permission.ACCESS_FINE_LOCATION, LOCATION_PERMISSION_RC);
            }
        }
    }

    private void showAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Use an image to remember item location");
        String[] items = new String[]{"Choose photo from gallery", "Take a new photo"};
        builder.setItems(items, new DialogInterface.OnClickListener() { // set items so that list view
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // user wants to choose photo from gallery
                if(which == 0) {
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
                    }
                    else {
                        checkUserPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_RC);
                    }

                }
            }
        });
        // create and show the alert
        AlertDialog alert = builder.create();
        alert.show();
    }

    // second tier helper method to start gallery and choose a photo
    private void startGallery() {
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
    private void startCamera() {
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

    // third tier helper method to create image file
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        return image;
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
        }
        else if (requestCode == Crop.REQUEST_CROP) {
            if (resultCode == RESULT_OK) {
                handleCrop(resultCode, data);
            }
        }
        else if (requestCode == SPEECH_INPUT_NAME) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                mItemNameEditText.setText(result.get(0));
                Log.d(TAG, "Speech Input : " + result);
            }
        }
        else if (requestCode == SPEECH_INPUT_LOCALISATION) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                mItemLocalisationEditText.setText(result.get(0));
                Log.d(TAG, "Speech Input : " + result);
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
            defaultBitmap = ((BitmapDrawable)mItemPictureImageView.getDrawable()).getBitmap();
        }
    }

    // third tier method to handle crop
    private void handleCrop(int resultCode, Intent result) {
        if (resultCode == RESULT_OK) {
            Uri uri = Crop.getOutput(result);
            imageUri = uri; // cropped image becomes the new image uri (so that cropped image gets saved in onSave)
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                mItemPictureImageView.setImageBitmap(bitmap);
//                Log.d(TAG, "handle crop uri " + uri);
                if (mItemNameEditText.getText().toString().isEmpty()) {
                    runDetector(bitmap);
                }
            } catch (Exception e) {
                Log.d("Error", "error");
            }

        } else if (resultCode == Crop.RESULT_ERROR) {
            Toast.makeText(this, Crop.getError(result).getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ****************** saving state *************************** //

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the cropped image capture uri before the activity goes into background
        outState.putBoolean(RUN_KEY, run);
        outState.putParcelable(URI_INSTANCE_STATE_KEY, imageUri);
//        Log.d(TAG, "instance save uri " + imageUri);
    }
}



