package edu.dartmouth.cs.finditems.database;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;

import java.util.List;

import edu.dartmouth.cs.finditems.activities.SaveItemActivity;

public class ItemsListLoader extends AsyncTaskLoader<List<Items>> {

    private ItemsDataSource dataSource;

    public ItemsListLoader(Context context) {
            super(context);
            dataSource = new ItemsDataSource(context);
            dataSource.open();
        }

        @Nullable
        @Override
        public List<Items> loadInBackground() {
//            if (SaveItemActivity.dbInsertion) {
                return dataSource.getAllItems(); // get all saved item in list
//            }
//            return null;
        }
}
