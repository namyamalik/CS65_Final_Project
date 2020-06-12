package edu.dartmouth.cs.finditems.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import edu.dartmouth.cs.finditems.R;
import edu.dartmouth.cs.finditems.adapters.MainListViewAdapter;
import edu.dartmouth.cs.finditems.database.ItemsDataSource;
import edu.dartmouth.cs.finditems.database.ItemsListLoader;
import edu.dartmouth.cs.finditems.database.Items;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<Items>>{

    // keys
    private static final String TAG ="Debug";
    private static final int ALL_ITEMS_LOAD_ID = 1;

    // extras
    public static final String EXTRA_PARENT_MAINPAGE = "parent main page";
    public static final String EXTRA_PARENT_NEW_ITEM_BUTTON = "Add item";

    // references to data structures
    private ListView mItemsListView;
    private MainListViewAdapter mMainListViewAdapter;
    private ArrayList<Items> mItemsArrayList;

    // reference to database
    private ItemsDataSource datasource;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "MainActivity: onCreate()");

        // Action Bar set up
        setupActionBar();

        // instantiation of data structures
        mItemsListView = findViewById(R.id.ItemsListView);
        mItemsArrayList = new ArrayList<>();
        mMainListViewAdapter = new MainListViewAdapter(this, mItemsArrayList);

        // bind adapter to list view on this main page
        mItemsListView.setAdapter(mMainListViewAdapter);

        // instantiation of database
        datasource = new ItemsDataSource(this);
        datasource.open();

        // create specific loader for loading all exercises (similar paradigm as fragment manager)
        LoaderManager mLoader = LoaderManager.getInstance(this);
        mLoader.initLoader(ALL_ITEMS_LOAD_ID, null, this).forceLoad(); // the class has to implement LoaderManager.LoaderCallbacks<List<Items>>

    }

    // ****************** overriding methods for AsyncTaskLoader *************************** //
    @NonNull
    @Override
    public Loader<List<Items>> onCreateLoader(int id, @Nullable Bundle args) {
        if (id == ALL_ITEMS_LOAD_ID) {
            return new ItemsListLoader(this); // ExercisesListLoader class is async task loader not on UI thread
        }
        return null;
    }

    @Override
    public void onLoadFinished(@NonNull Loader<List<Items>> loader, List<Items> data) {
        if (loader.getId() == ALL_ITEMS_LOAD_ID) {
            mMainListViewAdapter.notifyDataSetChanged();
            if (data != null && data.size() > 0) {
                Log.d(TAG, "data size is " + data.size());
                mMainListViewAdapter.addAll(data); // add all exercises saved in database to array adapter, which will display them on history fragment
            }
        }
    }
    @Override
    public void onLoaderReset(@NonNull Loader<List<Items>> loader) {

    }

    // ****************** overriding methods for fragment lifecycle *************************** //
    @Override
    public void onResume() {
        datasource.open();
        super.onResume();
    }

    @Override
    public void onPause() {
        datasource.close();
        super.onPause();
    }

    //---------------------------------ActionBar----------------------------------
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar(); // returns reference to ActionBar object
        if (actionBar != null) {
            actionBar.setTitle("Your Items"); // could also have set label in manifest
        }
    }

    //Action Bar Overwrite Methods
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.EditProfileItem) {
            Intent profileIntent = new Intent(MainActivity.this, RegisterActivity.class);
            profileIntent.putExtra(EXTRA_PARENT_MAINPAGE, true);
            startActivity(profileIntent); // direct user to register page
            return true;
        }
        else if (item.getItemId() == R.id.SettingsItem) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        else if (item.getItemId() == R.id.DeleteAllItems) {
            deleteAllItems();
        }

        else if (item.getItemId() == R.id.AddItemButton) {
            Intent intent = new Intent(MainActivity.this, SaveItemActivity.class);
            intent.putExtra(EXTRA_PARENT_NEW_ITEM_BUTTON, true);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
        // no up button on this activity, so no code needed for that
    }

    private void deleteAllItems() {
        Log.d(TAG, "MainActivity: deleteAllItems()");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Are you sure?")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        datasource.deleteAllItems();
                        mMainListViewAdapter.clear();
                    }
                });
        builder.show();
    }

}
