package edu.dartmouth.cs.finditems.activities;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;

import java.util.ArrayList;

import edu.dartmouth.cs.finditems.R;
import edu.dartmouth.cs.finditems.adapters.MainListViewAdapter;
import edu.dartmouth.cs.finditems.database.ItemsDataSource;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    // EXTRAS FOR OTHER ACTIVITIES
    public static final String EXTRA_PARENT_MAP = "parent map";
    public static final String EXTRA_ITEM_NAME = "extra item name";
    public static final String EXTRA_ITEM_DATE = "extra item date";
    public static final String EXTRA_ITEM_TIME = "extra item time";
    public static final String EXTRA_ITEM_LOCALIZATION = "extra item localization";
    public static final String EXTRA_ITEM_ROW_ID = "extra item row id";
    public static final String EXTRA_ITEM_GPS_DATA = "extra item gps data";

    private GoogleMap mMap;
    private int PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        setupActionBar();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (!checkPermission()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        }
        else {
        }
    }

    // ****************** action bar methods *************************** //

    // helper method to setup action bar
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar(); // returns reference to ActionBar object
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true); // show up button to navigate to parent activity (main page)
            actionBar.setTitle("Last Updated Location of Item");
        }
    }

    // override to create items inside the menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_plain, menu); // shows no buttons (except back arrow default)
        return super.onCreateOptionsMenu(menu);
    }

    // override to dictate the actions of the items inside the menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: // if user clicks back button on menu
                Intent intent = new Intent(MapsActivity.this, SaveItemActivity.class);

                // get extras of saved text widgets and send them back to save item activity
                intent.putExtra(EXTRA_ITEM_NAME, getIntent().getStringExtra(SaveItemActivity.EXTRA_ITEM_NAME));
                intent.putExtra(EXTRA_ITEM_DATE, getIntent().getStringExtra(SaveItemActivity.EXTRA_ITEM_DATE));
                intent.putExtra(EXTRA_ITEM_TIME, getIntent().getStringExtra(SaveItemActivity.EXTRA_ITEM_TIME));
                intent.putExtra(EXTRA_ITEM_LOCALIZATION, getIntent().getStringExtra(SaveItemActivity.EXTRA_ITEM_LOCALIZATION));

                // to remember what row id is to retrieve picture of item from db (bc bitmap too bag to pass in extra)
                intent.putExtra(EXTRA_ITEM_ROW_ID, getIntent().getLongExtra(SaveItemActivity.EXTRA_ITEM_ROW_ID, 0));
                Log.d("debug", "map act pic row id " + getIntent().getLongExtra(SaveItemActivity.EXTRA_ITEM_ROW_ID, 0));

                // to remember gps data in case map button is clicked again on save item activity
                intent.putExtra(EXTRA_ITEM_GPS_DATA, getIntent().getStringExtra(SaveItemActivity.EXTRA_GPS_DATA));

                // to remember the parent
                intent.putExtra(EXTRA_PARENT_MAP, true);

                startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }


    // ****************** permission methods *************************** //

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        } else {
            finish();
        }
    }

    //******** Check run time permission for locationManager. This is for v23+  ********
    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (result == PackageManager.PERMISSION_GRANTED)
            return true;
        else
            return false;
    }


    // ****************** map methods *************************** //

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Criteria criteria = new Criteria();
        // Note that fine as provider may return null object
        // write defensive code that determines what might be the best provider
        // to give the best last location see getLastKnownLocation() here
        // https://stackoverflow.com/questions/20438627/getlastknownlocation-returns-null
        //criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setSpeedRequired(false);
        criteria.setCostAllowed(true);
        String provider = locationManager.getBestProvider(criteria, true);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        // get extra gps data from save item activity
        String gps_data = getIntent().getStringExtra(SaveItemActivity.EXTRA_GPS_DATA);

        // convert JSON string back to array of strings
        Gson gson = new Gson();
        ArrayList locationLatLongArray = gson.fromJson(gps_data, ArrayList.class);

        // get item location latitude and longitude by accessing stored array
        if (locationLatLongArray != null) {
            if (!locationLatLongArray.isEmpty()) {
                Double savedLat = (Double) locationLatLongArray.get(0); // start latitude
                Double savedLong = (Double) locationLatLongArray.get(1); // start longitude

                // make LatLng out of the coordinates
                LatLng savedLocation = new LatLng(savedLat, savedLong);

                // add marker for item saved location and zoom in
                mMap.addMarker(new MarkerOptions().position(savedLocation).icon(BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_GREEN))); // set position and icon for the marker
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(savedLocation, 21)); // 21: max zoom level

            }
        }
    }
}
