package edu.dartmouth.cs.finditems.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

class MySQLiteHelper extends SQLiteOpenHelper {

    // Key
    private static final String DATABASE_NAME = "Items.db";
    private static final int DATABASE_VERSION = 1;
    public static final String TABLE_ITEMS = "items_table";

    public static final String ROW_ID                   = "_id";
    public static final String KEY_ITEM_NAME            = "item_name"; // must be 1 word
    public static final String KEY_ITEM_IMAGE           = "item_image";
    public static final String KEY_ITEM_LOCALISATION    = "ItemLocalisation";
    public static final String KEY_ITEM_DATE            = "ItemDate";
    public static final String KEY_ITEM_TIME            = "ItemTime";
    public static final String KEY_GPS_DATA             = "gps_data";



    private static final String DATABASE_CREATE = "CREATE TABLE "
            + TABLE_ITEMS + "( "
            + ROW_ID + " integer primary key, "
            + KEY_ITEM_NAME + " text, "
            + KEY_ITEM_IMAGE + " blob, "
            + KEY_ITEM_LOCALISATION + " text, "
            + KEY_ITEM_DATE + " text, "
            + KEY_ITEM_TIME + " text, "
            + KEY_GPS_DATA + " blob " + ");";

    public MySQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ITEMS);
        onCreate(db);
    }
}
