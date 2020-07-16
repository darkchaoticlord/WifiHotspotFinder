package com.jawad.wifihotspotfinder;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.os.Vibrator;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import xdroid.toaster.Toaster;

/*  This is the class that handles some of the most important tasks of the application. They are:
 *  1.  Sets the layout of the list of nearby free Wi-Fi hotspots for the location provided.
 *  2.  Sets the Ambient Mode system for the layout, where the layout is made monochrome
 *      to save battery in standby.
 *  3.  Handles the Expandable List View of the layout as well as its functionality and
 *      features, like Headers.
 *  4.  Handles what will happen on short and long clicks of the different elements of the
 *      Expandable List View.
 *  5.  Handles the preparation of the list from the hotspot database that is nearby the
 *      location provided.
 *  6.  Handles the access of both the databases in the program: one for user locations
 *      and the other for hotspots.
 *  7.  Uses the geo-coordinates of the user's specified location and the hotspot's location
 *      to find out distance in miles to 2 d.p.
 *  8.  Sorts the list of the locations from the closest to the user to the farthest.
 *  9.  Handles the step-by-step navigation for the user if they need to use it.
 *  10. Handles sending the phone number to the phone to call over the Data Layer System
 *      built for phone-watch communication.
 */
public class CardViewLayout extends WearableActivity
                            implements GoogleApiClient.ConnectionCallbacks,
                                       GoogleApiClient.OnConnectionFailedListener {

    // Declaration of the global variables in the class.
    private List<String> listDataHeader;
    private LinkedHashMap<String, List<String>> listDataChild;
    private LinkedHashMap<String, List<Double>> listLatLng;
    private GoogleApiClient mGoogleApiClient;
    private final String TAG = "CardViewLayout";

    /*  This the first method that is executed in any class that extends 'Activity'
     *  ('WearableActivity' is extended by 'Activity' so this class is indirectly
     *  extended by 'Activity'). It sets up the layout and its functionality and
     *  handles the actions possible in the layout.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.round_activity_card_view_layout);
        setAmbientEnabled(); // Turns on Ambient Mode

        // The Google API Client is established for sending data to phone.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        // Gets the Expandable List View using its ID for entering the list.
        ExpandableListView expListView;
        expListView = (ExpandableListView) findViewById(R.id.round_expandableListView);

        // Calls the method that prepares the list to be shown to the user.
        prepareListData();

        // Send the prepared list to the Class 'ExpandableListAdapter' to be
        // created into a format suitable to enter into the layout.
        ExpandableListAdapter listAdapter;
        listAdapter = new ExpandableListAdapter(this, listDataHeader, listDataChild);

        // Chooses the appropriate header's ID based on whether
        // there are any entries in the prepared list or not.
        int cardViewHeader;
        if (listDataHeader.size() == 0) {
            cardViewHeader = R.layout.expandable_list_header_none;
        } else {
            cardViewHeader = R.layout.expandable_list_header_available;
        }

        // Inflates (opening another layout in the code) the header layout.
        LayoutInflater inflater = getLayoutInflater();
        LinearLayout mLayout = (LinearLayout) inflater.inflate(cardViewHeader, expListView, false);

        // Sets the Layout Parameters to fit into the current layout.
        AbsListView.LayoutParams params;
        params = new AbsListView.LayoutParams( LinearLayout.LayoutParams.WRAP_CONTENT,
                                               LinearLayout.LayoutParams.WRAP_CONTENT  );
        mLayout.setLayoutParams(params);

        //Adds the header on top of the list.
        expListView.addHeaderView(mLayout);

        // Enters the data into the layout and creates the Expandable List View.
        expListView.setAdapter(listAdapter);

	    // A listener that activates when the subsections of a location in the list is expanded.
        expListView.setOnGroupExpandListener(new OnGroupExpandListener() {

            @Override
            public void onGroupExpand(int groupPosition) {
                // Method notifies the user on what to do.
                vibration(200);
                notifier("Click On Subsections To See What's Available");
            }
        });

        // A listener implemented on the subsections when they are short clicked on.
        expListView.setOnChildClickListener(new OnChildClickListener() {

            // Method assigns what to notify for the subsection short press by the user.
            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id) {
                switch (childPosition) {
                    case 0:
                        // For the first subsection, the programs notifies user what
                        // long clicking the same subsection does.
                        vibration(100);
                        notifier("Long Click For Distance In km");
                        break;
                    case 2:
                        // For the third subsection, the programs notifies user what
                        // long clicking the same subsection does.
                        vibration(100);
                        notifier("Long Click For Automatic Navigation");
                        break;
                    case 3:
                        // For the fourth subsection, the program first checks for
                        // phone number and then notifies user appropriately.
                        vibration(100);
                        notifier("Long Click To Call Number");
                        break;
                }
                return false;
            }
        });

        // This is where the long click actions are assigned to the subsections.
        expListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                                           int position, long id) {

                if (ExpandableListView.getPackedPositionType(id) ==
                        ExpandableListView.PACKED_POSITION_TYPE_CHILD ) {

                    int groupPosition = ExpandableListView.getPackedPositionGroup(id);
                    int childPosition = ExpandableListView.getPackedPositionChild(id);

                    List<List<String>> headerChild = (new ArrayList<>(listDataChild.values()));
                    List<String> groupChild = headerChild.get(groupPosition);

                    List<List<Double>> headerLatLng = (new ArrayList<>(listLatLng.values()));
                    List<Double> groupLatLng = headerLatLng.get(groupPosition);

                    if (childPosition == 0) {
                        // Gets the string that contains the distance in miles from the list.
                        String distance = groupChild.get(childPosition);

                        // Gets the distance in miles from the string and converts
                        // it into km, rounding it to 2 d.p.
                        String miles = distance.substring(0, distance.indexOf(" "));
                        double distanceMiles = Double.parseDouble(miles);
                        double distanceKM = distanceMiles * 1.609344;
                        distanceKM = roundValues(distanceKM, 2);

                        // Notifies the user of the Distance in km.
                        vibration(300);
                        notifier("Distance: " + String.valueOf(distanceKM) + " km");
                    } else if (childPosition == 2) {
                        // Gets the Latitude and Longitude from the coordinates list.
                        String Latitude = String.valueOf(groupLatLng.get(1));
                        String Longitude = String.valueOf(groupLatLng.get(2));

                        // Creates an action to open Google Maps and
                        // send it the coordinates for step-by-step navigation.
                        String navCommand = "google.navigation:q=" + Latitude + "," + Longitude;
                        Uri gmmIntentUri = Uri.parse(navCommand);
                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                        mapIntent.setPackage("com.google.android.apps.maps");

                        // Starts the action.
                        vibration(new long[]{0, 300, 200, 500});
                        startActivity(mapIntent);
                    } else if (childPosition == 3) {
                        // Gets the string that contains the phone number from the list.
                        String phoneNumber = groupChild.get(childPosition);

                        // Checks if the number is available.
                        if (phoneNumber.equals("No Number")) { // If number is not available...
                            vibration(new long[]{0, 200, 200, 400});
                            notifier("No Number Available");
                        } else { // Otherwise...
                            // Removes any whitespaces in the string.
                            phoneNumber = phoneNumber.replace(" ", "");
                            final String finalPhoneNumber = phoneNumber;

                            // Sends the number to the phone to call it after waiting 2 seconds.
                            Thread thread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Toaster.toast("Calling On Phone...");
                                        vibration(500);
                                        Thread.sleep(2000);
                                        new SendToPhone( "Call number from wear",
                                                         finalPhoneNumber         ).start();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                            thread.start();
                        }
                    }
                    return true;
                }
                return false;
            }
        });
    }

    // This method is used to access the vibrator to vibrate for given milliseconds.
    private void vibration(final int duration) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // Calls system service to get access to the vibrator motor.
                Context context = getApplicationContext();
                Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator.hasVibrator()) { // If the device has a vibrator...
                    Log.v(TAG, "Vibrator Found");
                    vibrator.vibrate(duration);
                } else { // Otherwise...
                    Log.v(TAG, "Vibrator Not Found");
                }
            }
        });
        thread.start();
    }

    // This method is used to access the vibrator to vibrate a series of given milliseconds.
    private void vibration(final long[] durationPattern) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // Calls system service to get access to the vibrator motor.
                Context context = getApplicationContext();
                Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator.hasVibrator()) { // If the device has a vibrator...
                    Log.v(TAG, "Vibrator Found");
                    vibrator.vibrate(durationPattern, -1);
                } else { // Otherwise...
                    Log.v(TAG, "Vibrator Not Found");
                }
            }
        });
        thread.start();
    }

    private void notifier (String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    // When the application enters the ambient mode.
    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        Log.d(TAG, "Entering Grid View Ambient Mode");
        super.onEnterAmbient(ambientDetails);

        // The background of the layout is turned black for ambient mode.
        RelativeLayout background = (RelativeLayout) findViewById(R.id.expandable_list_background);
        background.setBackgroundColor(Color.BLACK);
    }

    // When the application exits the ambient mode.
    @Override
    public void onExitAmbient() {
        Log.d(TAG, "Exiting Grid View Ambient Mode");
        super.onExitAmbient();

        // The background of the layout is turned back to the same colour for ambient mode.
        RelativeLayout background = (RelativeLayout) findViewById(R.id.expandable_list_background);
        background.setBackgroundColor(Color.parseColor("#FF3C00"));
    }

    // Creates the Lists of data that is to be entered into the Expandable List View.
    private void prepareListData() {
        // The lists where data is to be entered are initialised.
        listDataHeader = new ArrayList<>();
        listDataChild = new LinkedHashMap<>();
        listLatLng = new LinkedHashMap<>();

        // The current location of the user is obtained from the database.
        LatLng searchCoordinates = provideSearchCoordinates(MainWearActivity.Type);
        if (searchCoordinates != null) { // If location's coordinates and available...
            DBLocationHelper myDBHelper;
            myDBHelper = new DBLocationHelper(CardViewLayout.this);

            // Creates and opens the database for the program to work with.
            // Catches any exceptions found that cannot be handled.
            myDBHelper.createDataBase();

            try {
                myDBHelper.openDataBase();
            } catch(SQLException SQLExp){
                throw new Error("Unable to open database due to " + SQLExp.getMessage());
            }

            // Gets a readable version of database and obtains the number of rows in database.
            SQLiteDatabase db = myDBHelper.getReadableDatabase();
            int numRows = (int) DatabaseUtils.queryNumEntries(db, DBLocationHelper.DB_TABLE);
            try {
                // Loops through every record in database and carries out instructions below.
                for (int i = 1; i <= numRows; i++) {
                    // Columns wanted from the Query.
                    String[] sqlColumns = new String[] {"Latitude", "Longitude"};
                    // The WHERE clause of SQL Query
                    String sqlWhere = "_id = ?";
                    // The WHERE value that will decide what record(s) is chosen.
                    String[] sqlArgs = new String[] {String.valueOf(i)};

                    // Queries database and gets a cursor that can navigate through queried table.
                    Cursor cursor = db.query( DBLocationHelper.DB_TABLE, sqlColumns, sqlWhere,
                                              sqlArgs, null, null, null                        );

                    // Moves to the first record.
                    cursor.moveToFirst();

                    // Obtains Latitude and Longitude of record and enters them in a LatLng Object.
                    LatLng recordLatLng;
                    do {
                        String strLat = cursor.getString(cursor.getColumnIndex(sqlColumns[0]));
                        double recordLatitude = Double.parseDouble(strLat);

                        String strLong = cursor.getString(cursor.getColumnIndex(sqlColumns[1]));
                        double recordLongitude = Double.parseDouble(strLong);

                        recordLatLng = new LatLng(recordLatitude, recordLongitude);
                    } while(cursor.moveToNext());

                    // The cursor is closed after being used.
                    cursor.close();

                    // Checks whether the location in the record is close to the user or not.
                    if ( ( (recordLatLng.latitude > searchCoordinates.latitude - 0.007)   &&
                           (recordLatLng.latitude < searchCoordinates.latitude + 0.007) )   &&
                         ( (recordLatLng.longitude > searchCoordinates.longitude - 0.007) &&
                           (recordLatLng.longitude < searchCoordinates.longitude + 0.007) )    ) {

                        // If the location is within user's range...
                        // The columns that the user will enter in the list for the layout.
                        String[] sqlColumns2 = new String[] {"Company", "Type", "Address", "Phone"};

                        // The database is queried again for the same WHERE Clause
                        // but different columns and cursor is obtained.
                        Cursor outputCursor = db.query( DBLocationHelper.DB_TABLE, sqlColumns2,
                                                        sqlWhere, sqlArgs, null, null, null     );

                        // The cursor is moved to the first.
                        outputCursor.moveToFirst();

                        do {
                            // Gets the company name and is used as the Key
                            // Value for the LinkedHashMap lists.
                            String searchHeader = String.valueOf(outputCursor.getString(0));

                            // Calls method that calculates distance in miles using coordinates.
                            double rawDistance = calculateDistance( searchCoordinates.latitude,
                                                                    searchCoordinates.longitude,
                                                                    recordLatLng.latitude,
                                                                    recordLatLng.longitude       );
                            double distance = roundValues(rawDistance, 2);

                            // Prepares the LinkedHashMap List for the location
                            // coordinates and the distance for sorting out later.
                            List<Double> values = new ArrayList<>();
                            values.add(distance);
                            values.add(recordLatLng.latitude);
                            values.add(recordLatLng.longitude);
                            listLatLng.put(searchHeader, values);

                            // Prepares LinkedHashMap List for the list to be entered into layout.
                            List<String> subsections = new ArrayList<>();
                            subsections.add(String.valueOf(distance) + " miles");
                            subsections.add(String.valueOf(outputCursor.getString(1)));
                            subsections.add(String.valueOf(outputCursor.getString(2)));
                            subsections.add(String.valueOf(outputCursor.getString(3)));
                            listDataChild.put(searchHeader, subsections);
                        } while(cursor.moveToNext());
                        // Closes the cursor after the query is done and cursor is not needed.
                        outputCursor.close();
                    }
                }
                // The lists are now sorted using their separate sorting methods.
                listDataChild = sortDataChildByValues(listDataChild);
                listLatLng = sortLatLngByValues(listLatLng);

                // The headers are then entered into a separate list.
                listDataHeader = new ArrayList<>(listDataChild.keySet());
            } catch (SQLException ex) {
                throw new Error("Unable to search through the database.");
            } finally {
                //After all that, the database is closed to prevent memory leak.
                db.close();
                myDBHelper.close();
            }
        } else { // Otherwise, notifies the user that there are no coordinates in the database.
            notifier("No Coordinates Found In The Database");
        }
    }

    // Obtains the coordinates from the user location database based on
    // what type the user wants: current, home or work location.
    private LatLng provideSearchCoordinates(String Type) {
        DatabaseHelperWear databaseHelperWear;
        databaseHelperWear = new DatabaseHelperWear(this);

        // Creates and opens the database for the program to work with.
        // Catches any exceptions found that cannot be handled.
        databaseHelperWear.createDataBase();

        try {
            databaseHelperWear.openDataBase();
        } catch (SQLException e) {
            Log.d("DatabaseHelperWear", "Cannot open database.");
            e.printStackTrace();
        }

        // Gets a readable version of database and sets variables for the columns and WHERE Clause.
        SQLiteDatabase db = databaseHelperWear.getReadableDatabase();
        String[] sqlColumns = new String[] {"Latitude", "Longitude"};
        String sqlWhere = "Type = ?";
        String[] sqlArgs = new String[] {Type};

        // Queries the database and gets a cursor to navigate. If there are no values in
        // either Longitude or Latitude, the method returns 'null' to the call.
        Cursor cursor = db.query( DatabaseHelperWear.DB_TABLE, sqlColumns, sqlWhere,
                                  sqlArgs, null, null, null                          );
        cursor.moveToFirst();
        if ( cursor.isNull(cursor.getColumnIndex("Latitude"))  ||
             cursor.isNull(cursor.getColumnIndex("Longitude"))    ) {
            Log.d(TAG, "No entry in either Latitude or Longitude or both.");
            return null;
        }

        // Gets Latitude and Longitude from database using cursor and is closed after finishing it.
        double searchLatitude;
        double searchLongitude;
        try{
            do {
                String strLat = cursor.getString(cursor.getColumnIndex("Latitude"));
                searchLatitude = Double.parseDouble(strLat);

                String strLong = cursor.getString(cursor.getColumnIndex("Longitude"));
                searchLongitude = Double.parseDouble(strLong);
            } while (cursor.moveToNext());
        } catch (SQLException e) {
            throw new Error("Unable to search through the database");
        } finally {
            cursor.close();
        }

        // Closes the database to prevent any memory leak in the program.
        db.close();
        databaseHelperWear.close();

        // Returns the coordinates obtain from the database.
        return new LatLng(searchLatitude, searchLongitude);
    }

    // This method calculates the distance between two geo-coordinates in miles.
    private static double calculateDistance(double lat1, double long1, double lat2, double long2) {
        double theta = long1 - long2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        return (dist);
    }

    // This method converts degrees to radians.
    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    // This method converts radians to degrees.
    private static double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }

    // This method rounds decimals sent to method to a decimal place mentioned by the program.
    private static double roundValues(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    // This method is used to sort out the coordinates list obtained
    // from the search according to ascending distance.
    private LinkedHashMap<String, List<Double>>
                sortLatLngByValues(LinkedHashMap<String, List<Double>> passedMap) {

        List<Map.Entry<String,List<Double>>> list = new LinkedList<>(passedMap.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<String,List<Double>>>() {
            @Override
            public int compare( Map.Entry<String,List<Double>> map1,
                                Map.Entry<String,List<Double>> map2 ) {

                return map1.getValue().get(0).compareTo(map2.getValue().get(0));
            }
        });

        LinkedHashMap<String, List<Double>> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<Double>> n : list)
            result.put(n.getKey(), n.getValue());
        return result;
    }

    // This method is used to sort out the list for the layout obtained
    // from the search according to ascending distance.
    private LinkedHashMap<String, List<String>>
                sortDataChildByValues(LinkedHashMap<String, List<String>> passedMap) {

        List<Map.Entry<String,List<String>>> list = new LinkedList<>(passedMap.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<String, List<String>>>() {
            @Override
            public int compare( Map.Entry<String, List<String>> map1,
                                Map.Entry<String, List<String>> map2 ) {

                return map1.getValue().get(0).compareTo(map2.getValue().get(0));
            }
        });

        LinkedHashMap<String, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> n : list)
            result.put(n.getKey(), n.getValue());
        return result;
    }

    // The five methods below deal with the events that happens with Google API Client for watch.

    // Methods below kept due to the super class 'WearableActivity'.
    @Override
    public void onStart(){ // When the Client is started.
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop(){ // When the Client is stopped.
        // Disconnected if only if was connected previously.
        if (null != mGoogleApiClient && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    // Method kept below due to the implementation of
    // the Interface 'GoogleApiClient.ConnectionCallbacks'.
    @Override
    public void onConnected(Bundle bundle) { // When the Client is connected.
        Log.v(TAG, "The Client is connected");
    }

    // Methods below kept due to the implementation of
    // the Interface 'GoogleApiClient.OnConnectionFailedListener'.
    @Override
    public void onConnectionSuspended(int i) {
        // When the connection to Client has been suspended.
        Log.v(TAG, "The connection to Client is suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // When the connection to Client has failed.
        Log.v(TAG, "The connection to Client has failed due to" + result.getErrorMessage());
    }

    // This sub-class is where the JSON Array converted to String is sent
    // to the watch using the the Node API for the watch.
    private class SendToPhone extends Thread {
        public final String command;
        public final String message;

        // Constructor for the sub-class that tells what data is needed from
        // the program for the class to be executed without a hitch.
        SendToPhone(String com, String msg) {
            command = com;
            message = msg;
        }

        // An existing method overridden to convert the String into nodes (little
        // pieces of bytes) to be sent to the phone.
        @Override
        public void run() {
            NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi
                                                            .getConnectedNodes(mGoogleApiClient)
                                                            .await();

            for (Node node : nodes.getNodes()) {
                MessageApi.SendMessageResult result = Wearable.MessageApi
                                                              .sendMessage( mGoogleApiClient,
                                                                            node.getId(),
                                                                            command,
                                                                            message.getBytes() )
                                                              .await();

                // Keeps a log of what happens for the Programmer(s) to see
                // what is being sent to the phone by the program.
                if (result.getStatus().isSuccess()) {
                    // Logged if sending was successful.
                    Log.v("myTag", "Message: {" + message + "} sent to: " + node.getDisplayName());
                }
                else {
                    // Logged if sending was unsuccessful.
                    Log.v("myTag", "ERROR: failed to send Message");
                }
            }
        }
    }
}