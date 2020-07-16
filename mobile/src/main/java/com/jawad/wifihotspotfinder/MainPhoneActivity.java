package com.jawad.wifihotspotfinder;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONArray;

import java.util.List;

import xdroid.toaster.Toaster;

/*  This is the main class that is executed before any other classes.
 *  This class is responsible for six different tasks:
 *  1.  It sets the main layout for the application that the user will first see.
 *  2.  Handles the action of the user clicking the buttons available.
 *  3.  This class inserts a navigation button and drawer for the user to obtain more options.
 *  4.  This class obtains the postcode written in the Editable TextBox and finds the
 *      geo-coordinates of the postcode.
 *  5.  This class then enters them in the database for the user's locations in order to
 *      be sent to the watch.
 *  6.  Contains a sub-class that sends data from the database to the watch in a JSON Array
 *      format converted to string.
 */
public class MainPhoneActivity extends AppCompatActivity
                               implements  GoogleApiClient.ConnectionCallbacks,
                                           GoogleApiClient.OnConnectionFailedListener {
    private DrawerLayout drawer;
    private GoogleApiClient mGoogleApiClient;
    private final String TAG = "MainPhoneActivity";
    private Toolbar toolbar;

    /*  This the first method that is executed in any class that extends 'Activity'
     *  ('AppCompatActivity is extended by 'Activity' so this class is
     *  indirectly extended by 'Activity'). It sets the layout of the application
     *  and handles the buttons' action.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_phone);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        // The toolbar found at the top of the layout is created here by entering
        // the name and drawer call icon at the left.
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setTitle("Wi-Fi Hotspot Finder");

        // Navigation drawer is made functional here.
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarToggle toggle = new ActionBarToggle(
                this,                             /* host Activity */
                drawer,                           /* DrawerLayout object */
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        setUpNavigationView();

        // Connects to the Google API Client to establish connection between phone and watch.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        // A click listener is set up here for the button that updates Home Location of the user.
        final Button homeButton = (Button) findViewById(R.id.homeButton);
        homeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Perform action on click.
                String homeAddress = stringFromTextBox(R.id.homeText);
                updateDatabase(homeAddress, "Home");
            }
        });

        // A click listener is set up here for the button that updates Work Location of the user.
        final Button workButton = (Button) findViewById(R.id.workButton);
        workButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Perform action on click.
                String workAddress = stringFromTextBox(R.id.workText);
                updateDatabase(workAddress, "Work");
            }
        });

        // This is the click listener for the button that sends the updated database to the watch.
        final Button updateWearDatabase = (Button) findViewById(R.id.updateWearDatabaseButton);
        updateWearDatabase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Perform action on click.
                DatabaseToJSON dbJSON = new DatabaseToJSON(MainPhoneActivity.this);
                JSONArray json = dbJSON.getJSON();
                new SendToDataLayerThread(DatabaseHelper.DB_DIRECTORY, json.toString()).start();
                notifier("Wear Database Updated");
                vibration(new long[]{0, 200, 150, 200, 150, 200});
            }
        });

        //This is the click listener for the button that opens the watch app in the watch.
        final Button openWearApp = (Button) findViewById(R.id.openAppButton);
        openWearApp.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //Perform action on click.
                vibration(500);
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Informs the user the watch app is opening.
                            Toaster.toast("Opening App on Wear...");
                            Thread.sleep(2000);

                            // Sends command to the watch to open the application.
                            new SendToDataLayerThread(TAG, "Open Wear App").start();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();
            }
        });

        // Sets a click listener for the two Editable TextBoxes
        // to notify users that they can select and drag text to copy.
        editTextClickListener(R.id.homeText);
        editTextClickListener(R.id.workText);
    }

    // Class that sets up a dynamic toolbar for the layout
    private class ActionBarToggle extends ActionBarDrawerToggle {

        public ActionBarToggle(Activity activity, DrawerLayout drawer, int open, int close) {
            super(activity, drawer, open, close);
        }

        // Changes the title of the layout if the drawer is opened.
        @Override
        public void onDrawerOpened(View drawerView) {
            super.onDrawerOpened(drawerView);
            toolbar.setTitle("Options");
        }

        // Change the title of the layout back to what it was when it is closed.
        @Override
        public void onDrawerClosed(View drawerView) {
            super.onDrawerClosed(drawerView);
            toolbar.setTitle("Wi-Fi Hotspot Finder");
        }
    }

    // This method handles the click action for the Editable TextBoxes.
    private void editTextClickListener (int id) {
        final EditText editText = (EditText) findViewById(id);
        editText.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                notifier("Select Text and Drag for Copying");
            }
        });
    }

    // Notifies the user with a message to let them know about a situation.
    private void notifier (String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    // This method allows navigation drawer to be closed (if opened) when back button is clicked.
    @Override
    public void onBackPressed(){
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    // This method assigns what happens after the toolbar button is clicked
    // depending on the situation of the navigation drawer.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case android.R.id.home:
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                } else if (!drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.openDrawer(GravityCompat.START);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // This method is here to set up the functionality of the navigation view layout.
    private void setUpNavigationView(){
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
            new NavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(MenuItem item) {

                // The programs gets the ID of the item selected by the user
                // and then decides what to do for item clicked.
                int id = item.getItemId();
                if (id == R.id.settings_tab) {
                    Intent settings = new Intent(MainPhoneActivity.this, SettingsLayout.class);
                    startActivity(settings);
                } else if (id == R.id.update_tab) {
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(2000);
                                Toaster.toast("The Application is Up-To-Date");
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    thread.start();
                } else if (id == R.id.about_tab) {
                    Intent open_about = new Intent(MainPhoneActivity.this, AboutPage.class);
                    startActivity(open_about);
                } else if (id == R.id.user_guide_tab) {
                    Intent openUserGuide = new Intent(MainPhoneActivity.this, UserGuidePage.class);
                    startActivity(openUserGuide);
                }

                // After the item in the navigation drawer is clicked, the drawer is
                // closed for the new activity/action to take place.
                drawer.closeDrawer(GravityCompat.START);
                return true;
            }
        });
    }

    // This small method gets the string from the Editable TextBox and
    // formats it for the program to carry out further processing.
    private String stringFromTextBox(int textBoxID) {
        EditText editText = (EditText)findViewById(textBoxID);
        return editText.getText().toString();
    }

    // This is the method that gets the location of the postcode in terms of geo-coordinates.
    private LatLng getLocationFromAddress(String stringAddress) {
        Geocoder coder = new Geocoder(getApplicationContext());
        List<Address> address;
        LatLng coordinates = null;

        // Before getting the location the method calls another method that
        // checks whether or not the internet is turned on.
        if (isNetworkAvailable()) {
            try {
                // Sends the postcode to get a location with an Address type.
                address = coder.getFromLocationName(stringAddress, 1);

                // For cases where the GPS sensor cannot figure out the location of the address.
                if (address == null) {
                    return null;
                }

                // Gets the address from the list of addresses and enters the
                // Latitude and Longitude in a 'LatLng' Object.
                Address location = address.get(0);
                location.getLatitude();
                location.getLongitude();
                coordinates = new LatLng(location.getLatitude(), location.getLongitude() );
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            notifier("There's No Internet Connection");
        }
        return coordinates;
    }

    // This method checks that the internet is available for the user
    // as you need it to find coordinates for the postcode.
    private boolean isNetworkAvailable() {
        final Context context = getApplicationContext();
        String service = Context.CONNECTIVITY_SERVICE;
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(service);
        boolean isConnected = manager.getActiveNetworkInfo().isConnected();
        return manager.getActiveNetworkInfo() != null && isConnected;
    }

    // This is method that updates the database in the program about the user's
    // Work and Home Locations to be sent to the watch.
    private void updateDatabase(String strAddress, String locationType){
        try {
            // If user didn't enter any postcode in the corresponding Editable TextBox...
            if (strAddress.equals("")) {
                vibration(new long[] {0, 400, 200, 200});
                notifier("Enter Postcode First");
            } else { // Otherwise...
                vibration(200);

                // Gets the coordinates from the processed postcode.
                LatLng coordinates = getLocationFromAddress(strAddress);

                if (coordinates == null) { // If the location isn't found...
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Toaster.toast("Location Not Found");
                                Thread.sleep(1000);
                                Toaster.toast("Database Wasn't Updated");
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    thread.start();
                } else { // Otherwise...
                    DatabaseHelper myDBHelper;
                    myDBHelper = new DatabaseHelper(MainPhoneActivity.this);

                    // Creates and opens the database for the program to work with.
                    // Catches any exceptions found that cannot be handled.
                    myDBHelper.createDataBase();

                    try {
                        myDBHelper.openDataBase();
                    } catch (SQLException SQLExp) {
                        throw new Error("Unable to open database due to " + SQLExp.getMessage());
                    }

                    // Gets a writable version of the database and enters/replaces the Postcode,
                    // Latitude and Longitude inside the database for the type mentioned.
                    SQLiteDatabase db = myDBHelper.getWritableDatabase();
                    try {
                        // Container to contain the values with the location associated with it.
                        ContentValues values = new ContentValues();
                        values.put("Postcode", strAddress);
                        values.put("Latitude", String.valueOf(coordinates.latitude));
                        values.put("Longitude", String.valueOf(coordinates.longitude));

                        // The WHERE Clause is created and using that the database is updated.
                        String where = "Type = ?";
                        String[] whereArgs = {locationType};
                        db.update(DatabaseHelper.DB_TABLE, values, where, whereArgs);
                        notifier("Database Updated");
                    } catch (SQLException SQLEx) {
                        throw new Error("Unable to enter data due to" + SQLEx.getMessage());
                    }

                    // Closes the database after the update to prevent memory leak.
                    db.close();
                    myDBHelper.close();
                }
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    // This method is used to allow access to vibrator to vibrate for given milliseconds.
    private void vibration(final int duration) {
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

    // This method is used to allow access to vibrator to vibrate a series of given milliseconds.
    private void vibration(final long[] durationPattern) {
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
    private class SendToDataLayerThread extends Thread {
        public final String path;
        public final String message;

        // Constructor for the sub-class that tells what data is needed from
        // the program for the class to be executed without a hitch.
        SendToDataLayerThread(String p, String msg) {
            path = p;
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
                                                                            path,
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

