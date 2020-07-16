package com.jawad.wifihotspotfinder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

// This class is the adapter that converts the lists of data prepared
// to be kept in a format that can be easily set to the layout.
public class ExpandableListAdapter extends BaseExpandableListAdapter {

    // These are the global variables that will hold the data obtained from the program.
    private final Context mContext;
    private final List<String> mListDataHeader;
    private final LinkedHashMap<String, List<String>> mListDataChild;

    // This is the class constructor which takes the application context (abstract data
    // about the class) from the CardViewLayout class, the list of the company name and
    // the HashMap List of data to be entered into the global variables of this class
    // to be used throughout the code.
    public ExpandableListAdapter(Context context, List<String> listDataHeader,
                                 LinkedHashMap<String, List<String>> listChildData) {
        mContext = context;
        mListDataHeader = listDataHeader;
        mListDataChild = listChildData;
    }

    // Overridden existing method that gets the data for the subsection based on the position.
    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return this.mListDataChild.get(this.mListDataHeader.get(groupPosition)).get(childPosition);
    }

    // Overridden existing method that gets the position of the subsection based on the position.
    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    // Overridden existing method that creates the subsection layout individually.
    @Override
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        // Gets the string to enter in the subsection.
        final String childText = (String) getChild(groupPosition, childPosition);

        // Dynamically inflates the pre-made subsection layout.
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this.mContext
                                                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.list_item, null);
        }

        // Depending on the position of the subsections, an image is entered
        // into the ImageView in the pre-made subsection layout.
        ImageView childIcons = (ImageView) convertView.findViewById(R.id.child_icon);
        switch (childPosition) {
            case 0:
                childIcons.setImageResource(R.drawable.distance_white);
                break;
            case 1:
                childIcons.setImageResource(R.drawable.type_white);
                break;
            case 2:
                childIcons.setImageResource(R.drawable.address_white);
                break;
            case 3:
                childIcons.setImageResource(R.drawable.phone_white);
                break;
        }

        // Enters the string text into the TextView of the pre-made subsection layout as well.
        TextView txtListChild = (TextView) convertView.findViewById(R.id.expListItem);
        txtListChild.setText(childText);

        // Sends the layout back to the program to add more of the pre-made subsections layout.
        return convertView;
    }

    // Overridden existing method that gets the size of the number of subsections for a location.
    @Override
    public int getChildrenCount(int groupPosition) {
        return this.mListDataChild.get(this.mListDataHeader.get(groupPosition)).size();
    }

    // Overridden existing method that gets the location.
    @Override
    public Object getGroup(int groupPosition) {
        return this.mListDataHeader.get(groupPosition);
    }

    // Overridden existing method that counts up the number of locations.
    @Override
    public int getGroupCount() {
        return this.mListDataHeader.size();
    }

    // Overridden existing method that gets the location position in the list
    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    // Overridden existing method that creates the location header layout individually.
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {

        // Gets the Company name for the location header.
        String headerTitle = (String) getGroup(groupPosition);

        // Dynamically inflates the pre-made location header layout.
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this.mContext
                                                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.list_group, null);
        }

        // Depending on the value in the second position of the subsections,
        // an image is entered into the ImageView in the pre-made location header layout.
        ImageView locationType = (ImageView) convertView.findViewById(R.id.group_icon);
        String checkType = (new ArrayList<>(mListDataChild.values())).get(groupPosition).get(1);
        switch (checkType){
            case "Library":
                locationType.setImageResource(R.drawable.library_white);
                break;
            case "Bank":
                locationType.setImageResource(R.drawable.bank_white);
                break;
            case "Restaurant":
                locationType.setImageResource(R.drawable.restaurant_white);
                break;
            case "Bar":
                locationType.setImageResource(R.drawable.bar_white);
                break;
            case "Station":
                locationType.setImageResource(R.drawable.station_white);
                break;
            case "Cafe":
                locationType.setImageResource(R.drawable.cafe_white);
                break;
            case "Hotel":
                locationType.setImageResource(R.drawable.hotel_white);
                break;
            case "Pub":
                locationType.setImageResource(R.drawable.pub_white);
                break;
            case "Venue":
                locationType.setImageResource(R.drawable.venue_white);
                break;
        }

        // Enters the string text into the TextView of the pre-made location header layout as well.
        TextView expListHeader = (TextView) convertView.findViewById(R.id.expListHeader);
        expListHeader.setText(headerTitle);

        // Sends the layout back to the program to add more of the pre-made location header layout.
        return convertView;
    }

    // Overridden existing method that lets the program know that the list doesn't have a set size.
    @Override
    public boolean hasStableIds() {
        return false;
    }

    // Overridden existing method that lets the program know that the subsections can be clicked.
    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
