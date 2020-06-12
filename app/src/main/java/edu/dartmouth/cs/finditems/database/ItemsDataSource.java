package edu.dartmouth.cs.finditems.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemsDataSource {
    // keys
    private static final String TAG ="Debug";

    private MySQLiteHelper dbHelper;
    private SQLiteDatabase database;
    private Context appContext;
    private String[] allColumns = {MySQLiteHelper.ROW_ID,
            MySQLiteHelper.KEY_ITEM_NAME,
            MySQLiteHelper.KEY_ITEM_IMAGE,
            MySQLiteHelper.KEY_ITEM_LOCALISATION,
            MySQLiteHelper.KEY_ITEM_DATE,
            MySQLiteHelper.KEY_ITEM_TIME,
            MySQLiteHelper.KEY_GPS_DATA};

    public ItemsDataSource(Context context){
        dbHelper = new MySQLiteHelper(context);
        this.appContext = context;
    }

    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public Items addItems(String itemName, byte[] itemPicture, String itemLocalisation,
                          String itemDate, String itemTime, String gps_data) {
        Log.d(TAG, "Database: addItems()");

        ContentValues value = new ContentValues();

        value.put(MySQLiteHelper.KEY_ITEM_NAME, itemName);
        value.put(MySQLiteHelper.KEY_ITEM_IMAGE, itemPicture);
        value.put(MySQLiteHelper.KEY_ITEM_LOCALISATION, itemLocalisation);
        value.put(MySQLiteHelper.KEY_ITEM_DATE, itemDate);
        value.put(MySQLiteHelper.KEY_ITEM_TIME, itemTime);
        value.put(MySQLiteHelper.KEY_GPS_DATA, gps_data);
//        Log.d("debug", "item name is: " + itemName);

        long insertId = database.insert(MySQLiteHelper.TABLE_ITEMS, null, value);
//        Log.d("debug", "id is: " + insertId);

        Cursor cursor = database.query(MySQLiteHelper.TABLE_ITEMS,
                allColumns, String.valueOf(insertId),
                null,
                null,
                null,
                null);

        if(cursor != null && cursor.getCount() > 0) {
            cursor.moveToPosition(cursor.getCount() - 1); // cursor moves to latest entry row
            Items newItems = cursorToItem(cursor); // new item is added at the next row
            cursor.close();
            return newItems;
        }
        return null;
    }

    public Items updateItem(String itemName, byte[] itemPicture, String itemLocalisation,
                            String itemDate, String itemTime, String gps_data, Long row_id) {
        Log.d(TAG, "Database: updateItems()");

        // set new values for ContentValues
        ContentValues value = new ContentValues();
        value.put(MySQLiteHelper.KEY_ITEM_NAME, itemName);
        value.put(MySQLiteHelper.KEY_ITEM_IMAGE, itemPicture);
        value.put(MySQLiteHelper.KEY_ITEM_LOCALISATION, itemLocalisation);
        value.put(MySQLiteHelper.KEY_ITEM_DATE, itemDate);
        value.put(MySQLiteHelper.KEY_ITEM_TIME, itemTime);
        value.put(MySQLiteHelper.KEY_GPS_DATA, gps_data);

        // set new values for Items model
        Items item = new Items();
        item.setId(row_id); // row id should technically not change
        item.setItemName(itemName);
        item.setItemImage(itemPicture);
        item.setItemLocalisation(itemLocalisation);
        item.setItemDate(itemDate);
        item.setItemTime(itemTime);
        item.setItemGpsData(gps_data);

        Log.d("debug", "DB: item get pic " + item.getItemImage());

        database.update(MySQLiteHelper.TABLE_ITEMS, value,
                MySQLiteHelper.ROW_ID + "= + ?",
                new String[] { String.valueOf(row_id) });

        return null;
    }

    // get image of a specific entry from database
    public Bitmap getItemPicture(Long row_id) {
        String query = "SELECT * FROM items_table WHERE _id =" + row_id;
//        Log.d("debug", "db pic row id is " + row_id);
        Cursor cursor = database.rawQuery(query,null);

        if (cursor != null) {
//            if (cursor.moveToPosition(cursor.getCount() - 1)) {
            if (cursor.moveToFirst()) {
                byte[] image = cursor.getBlob(2);
                cursor.close();
                if (image != null) {
                    return BitmapFactory.decodeByteArray(image, 0, image.length);
                }
            }
        }
        return null;
    }

    private Items cursorToItem(Cursor cursor) {
        Items item = new Items();

        item.setId(cursor.getLong(0));
        item.setItemName(cursor.getString(1));
        item.setItemImage(cursor.getBlob(2));
        item.setItemLocalisation(cursor.getString(3));
        item.setItemDate(cursor.getString(4));
        item.setItemTime(cursor.getString(5));
        item.setItemGpsData(cursor.getString(6));

        return item;
    }

    public List<Items> getAllItems() {
        Log.d(TAG, "Database: getAllItems()");
        List<Items> items_list = new ArrayList<>();

        Cursor cursor = database.query(MySQLiteHelper.TABLE_ITEMS,
                allColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Items items = cursorToItem(cursor);
            items_list.add(items);
            cursor.moveToNext();
        }

        // Make sure to close the cursor
        cursor.close();
        return items_list;
    }

    // delete exercise from the database by using row_id to query
    public void deleteItem(Long row_id) {
//        Log.d(TAG, "delete row id = " + row_id);
        database.delete(MySQLiteHelper.TABLE_ITEMS,
                MySQLiteHelper.ROW_ID + "= + ?",
                new String[] { String.valueOf(row_id) });
    }

    public void deleteAllItems() {
        Log.d(TAG, "Database: deleteAllItems()");
        database.delete(MySQLiteHelper.TABLE_ITEMS,
                null, null);
    }
}
