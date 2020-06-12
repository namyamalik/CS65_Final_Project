package edu.dartmouth.cs.finditems.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import edu.dartmouth.cs.finditems.activities.MainActivity;
import edu.dartmouth.cs.finditems.R;
import edu.dartmouth.cs.finditems.activities.SaveItemActivity;
import edu.dartmouth.cs.finditems.database.Items;

// for adapting saved exercises into list view to display on main "your items" page
public class MainListViewAdapter extends ArrayAdapter {

    // EXTRAS FOR OTHER ACTIVITIES
    public static final String EXTRA_PARENT_SAVED_ITEM_TOUCHABLE = "saved item clicked";

    public static final String EXTRA_ROW_ID             = "row id";
    public static final String EXTRA_ITEM_NAME          = "item name";
    public static final String EXTRA_ITEM_IMAGE         = "item image";
    public static final String EXTRA_ITEM_LOCALISATION  = "item localisation";
    public static final String EXTRA_ITEM_DATE          = "item Date";
    public static final String EXTRA_ITEM_TIME          = "item Time";
    public static final String EXTRA_ITEM_GPS          = "item gps data";

    private ArrayList<Items> itemsEntityArrayList;
    private Context context;

    public MainListViewAdapter(@NonNull Context context, ArrayList<Items> items) {
        super(context, 0, items);
        this.context = context;
        this.itemsEntityArrayList = items;
    }

    // create and return custom views for each model
    @Override
    public View getView(final int position, View customView, ViewGroup parent) { // parent is the list view
        // get position of the saved exercise
        final Items item = itemsEntityArrayList.get(position);

        customView = LayoutInflater.from(context).inflate(R.layout.activity_main_listview, parent, false);

        // instantiate the text views (these text views show a summary of the saved item)
        final TextView item_name_text_view = customView.findViewById(R.id.ItemListViewTextView);
        final ImageView item_picture_image_view = customView.findViewById(R.id.ItemListViewImageView);
        final TextView item_date_time_text_view = customView.findViewById(R.id.ItemDateTimeListTextView);

        // set the values for the different fields for each saved item
        item_name_text_view.setText(item.getItemName());
        item_date_time_text_view.setText(item.getItemDate() + " " + item.getItemTime());

        if (item.getItemImage() != null) {
            Log.d("debug", "Adapter: pic is not null");
                item_picture_image_view.setImageBitmap(BitmapFactory.decodeByteArray(item.getItemImage(), 0, item.getItemImage().length));
        }


        // if any of the fields are clicked on
        customView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() != MotionEvent.ACTION_DOWN) { // if user is scrolling
                    return false;
                }
                else {
                    Intent intent = new Intent(getContext(), SaveItemActivity.class);
                    intent.putExtra(EXTRA_PARENT_SAVED_ITEM_TOUCHABLE, true);
                    intent.putExtra(EXTRA_ROW_ID, item.getId());
                    intent.putExtra(EXTRA_ITEM_NAME, item.getItemName());
//                    intent.putExtra(EXTRA_ITEM_IMAGE, ((BitmapDrawable)item_picture_image_view.getDrawable()).getBitmap());
                    // (bitmaps are too large to pass with extra so need to call picture from db manually whenever needed)
                    intent.putExtra(EXTRA_ITEM_LOCALISATION, item.getItemLocalisation());
                    intent.putExtra(EXTRA_ITEM_DATE, item.getItemDate());
                    intent.putExtra(EXTRA_ITEM_TIME, item.getItemTime());
                    intent.putExtra(EXTRA_ITEM_GPS, item.getItemGpsData());

                    context.startActivity(intent);
                    Log.d("debug", "info is: " + item.getId() + " " + item.getItemName()
                            + " " + item.getItemLocalisation() + " " + item.getItemDate() + " "
                            + item.getItemTime() + " " + item.getItemGpsData());
            }
                return true;
        }
        });
        return customView;
    }
}
