package com.jawad.wifihotspotfinder;

import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.GridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.Locale;

import at.markushi.ui.CircleButton;

/*  This class the first class that is programmed in AndroidManifest.xml to be executed.
 *  This class does the most important class of the program. Tasks performed by this class are:
 *  1.  Sets up the layout of the first screen of the application suitable for round watches.
 *  2.  Sets up Ambient Mode system for this section of the application in order to
 *      save battery during app standby.
 *  3.  Sets up a Grid View allowing the code to keep several views together as a group.
 *  4.  Adds functionality to the Grid View using an Adapter in order to move from one layout
 *      to another through swiping.
 *  5.  Connects the watch to the phone using Google API Client and handles the connection created.
 *  6.  Connects the GPS Sensor in the paired phone to the watch to obtain location
 *      updates per set interval.
 *  7.  Access the User Location database to update the current location coordinates in the table.
 */
public class MainWearActivity extends WearableActivity
                              implements GoogleApiClient.OnConnectionFailedListener {

    // These are the global variables that are used throughout
    // the whole class and some in other classes.
    public static String Type;
    private static final int UPDATE_INTERVAL_MS = 5000;
    private static final int FASTEST_INTERVAL_MS = 4000;
    private static final String TAG = "MainWearActivity";
    private GoogleApiClient mGoogleApiClient;
    private int gridColumn;

    /*  This the first method that is executed in any class that extends 'Activity'
     *  ('WearableActivity' is extended by 'Activity' so this class is indirectly
     *  extended by 'Activity'). This method sets the layout of the app, sets the
     *  Grid View for the four layouts, sets a dot indicator for the Grid View and
     *  establishes a Google API connection with the phone.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_page_wear_round);
        setAmbientEnabled();  // Turns on Ambient Mode

        // Establishes a connection to the phone and its GPS Sensor.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addApi(Wearable.API)
                .addConnectionCallbacks(mConnectionCallbacks)
                .addOnConnectionFailedListener(this)
                .build();

        // Calls the Grid View and sets the class 'LayoutAdapter' in it.
        final GridViewPager pager = (GridViewPager) findViewById(R.id.pager);
        pager.setAdapter(new LayoutAdapter(this));

        // Calls the dot indicator in the layout and attaches it to the Grid View to work with it.
        DotsPageIndicator dotsIndicator = (DotsPageIndicator) findViewById(R.id.page_indicator);
        dotsIndicator.setDotRadius(5);
        dotsIndicator.setDotRadiusSelected(7);
        dotsIndicator.setDotSpacing(15);
        dotsIndicator.setPager(pager);

        // A Grid View Page Change Listener is added to get the current column number of Grid View.
        dotsIndicator.setOnPageChangeListener(new GridViewPager.OnPageChangeListener() {

            public void onPageSelected(int row, int column) {
                gridColumn = column;
            }

            public void onPageScrolled(int i, int i1, float v, float v1, int i2, int i3) {
            }

            public void onPageScrollStateChanged(int i) {
            }
        });
    }

    // Overridden Existing Method that sets up the Ambient Mode for the watch
    // when the app enters the mode on standby.
    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        Log.d(TAG, "Entering Grid View Ambient Mode");
        super.onEnterAmbient(ambientDetails);

        // Depending on the column number the colour of the layout is changed as they do
        // not exist currently. The layout are turned monochrome for ambient mode which makes
        // the watch lose less charge not all the pixels are turned on.
        if (gridColumn == 0) {
            LinearLayout welcomeBackground = (LinearLayout) findViewById(R.id.welcome_background);
            welcomeBackground.setBackgroundColor(Color.BLACK);

            TextView welcomeText1 = (TextView) findViewById(R.id.welcome_text_1);
            TextView welcomeText2 = (TextView) findViewById(R.id.welcome_text_2);
            TextView welcomeText3 = (TextView) findViewById(R.id.welcome_text_3);

            welcomeText1.setTextColor(Color.WHITE);
            welcomeText2.setTextColor(Color.WHITE);
            welcomeText3.setTextColor(Color.WHITE);

            welcomeText1.getPaint().setAntiAlias(false);
            welcomeText2.getPaint().setAntiAlias(false);
            welcomeText3.getPaint().setAntiAlias(false);
        } else if (gridColumn == 1) {
            ambientEnterChange(R.id.main_search_background,
                               R.id.main_search_button,
                               R.id.search_label            );
        } else if (gridColumn == 2) {
            ambientEnterChange(R.id.home_search_background,
                               R.id.home_search_button,
                               R.id.home_label              );
        } else if (gridColumn == 3) {
            ambientEnterChange(R.id.work_search_background,
                               R.id.work_search_button,
                               R.id.work_label              );
        }
    }

    // This method is created to allow similar changes to be carried out for
    // the last three layouts. They have similar layout so this method makes
    // the program more efficient to turn them monochrome.
    private void ambientEnterChange(int viewBackground, int viewButton, int viewLabel) {
        LinearLayout background = (LinearLayout) findViewById(viewBackground);
        background.setBackgroundColor(Color.BLACK);

        CircleButton button = (CircleButton) findViewById(viewButton);
        button.setColor(Color.BLACK);

        TextView label = (TextView) findViewById(viewLabel);
        label.setTextColor(Color.WHITE);
        label.getPaint().setAntiAlias(false);
    }

    // Overridden Existing Method that sets up the normal layout for the watch
    // when the app exits Ambient Mode from standby.
    @Override
    public void onExitAmbient() {
        Log.d(TAG, "Exiting Grid View Ambient Mode");
        super.onExitAmbient();

        // Depending on the column number, the layout is turned back
        // to normal to resume application usage.
        if (gridColumn == 0) {
            LinearLayout welcomeBackground = (LinearLayout) findViewById(R.id.welcome_background);
            welcomeBackground.setBackgroundResource(R.drawable.welcome_background);

            TextView welcomeText1 = (TextView) findViewById(R.id.welcome_text_1);
            TextView welcomeText2 = (TextView) findViewById(R.id.welcome_text_2);
            TextView welcomeText3 = (TextView) findViewById(R.id.welcome_text_3);

            welcomeText1.setTextColor(Color.BLACK);
            welcomeText2.setTextColor(Color.BLACK);
            welcomeText3.setTextColor(Color.BLACK);

            welcomeText1.getPaint().setAntiAlias(true);
            welcomeText2.getPaint().setAntiAlias(true);
            welcomeText3.getPaint().setAntiAlias(true);
        } else if (gridColumn == 1) {
            ambientExitChange( R.id.main_search_background,
                               R.drawable.maps_pic_1,
                               R.id.main_search_button,
                               R.id.search_label           );
        } else if (gridColumn == 2) {
            ambientExitChange( R.id.home_search_background,
                               R.drawable.maps_pic_2,
                               R.id.home_search_button,
                               R.id.home_label             );
        } else if (gridColumn == 3) {
            ambientExitChange( R.id.work_search_background,
                               R.drawable.maps_pic_3,
                               R.id.work_search_button,
                               R.id.work_label             );
        }
    }

    // This method is created to allow similar changes to be carried out for the
    // last three layouts. They have similar layout so this method makes the program
    // more efficient to turn them back to normal.
    private void ambientExitChange(int viewBackground, int image, int viewButton, int viewLabel) {
        LinearLayout background = (LinearLayout) findViewById(viewBackground);
        background.setBackgroundResource(image);

        CircleButton button = (CircleButton) findViewById(viewButton);
        button.setColor(Color.parseColor("#00CCFF"));

        TextView label = (TextView) findViewById(viewLabel);
        label.setTextColor(Color.BLACK);
        label.getPaint().setAntiAlias(true);
    }

    // This is acting as a global variable but sets up what happens
    // when Google API Client is connected and when it is suspended.
    private final GoogleApiClient.ConnectionCallbacks mConnectionCallbacks =
        new GoogleApiClient.ConnectionCallbacks() {

            // Overridden Existing Method that is executed when
            // there is a connection between phone and watch
            @Override
            public void onConnected(Bundle bundle) {
                requestLocationUpdates();
                Wearable.NodeApi.addListener(mGoogleApiClient, mNodeListener);
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(
                    new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                        @Override
                        public void onResult(NodeApi.GetConnectedNodesResult result) {
                            // If the phone is not paired to the watch...
                            if (result.getNodes().size() == 0) {
                                updateWarning(false);
                            }
                        }
                    });
                }

            // Overridden Existing Method that is executed when connection is suspended.
            @Override
            public void onConnectionSuspended(int i) {
            }
        };

    // Overridden Existing Method that is executed when connection is resumed.
    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    // Overridden Existing Method that is executed when connection is paused.
    @Override
    protected void onPause() {
        mGoogleApiClient.disconnect();
        if (!mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi
                            .removeLocationUpdates(mGoogleApiClient, mLocationListener);
        }
        super.onPause();
    }

    // Overridden Existing Method of the Interface that is executed when connection has failed.
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Connection to location client failed");
        }
    }

    // Declared like a global variable is a Node Listener that listens to when the Data API Layer
    // between phone and watch is connected and disconnected.
    private final NodeApi.NodeListener mNodeListener = new NodeApi.NodeListener() {
        @Override
        public void onPeerConnected(Node node) {
            updateWarning(true);
        }

        @Override
        public void onPeerDisconnected(Node node) {
            updateWarning(false);
        }
    };

    // Checks if watch has GPS.
    private boolean hasGps() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
    }

    // This method tells the Programmer(s) if the device has GPS.
    // If it does then the program continues but if it doesn't then
    // a notifier is sent to the user to pair with a phone.
    private void updateWarning(final boolean phoneAvailable) {
        // Ensures that the 'Toast' (notifier) function is called from the UI thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!phoneAvailable && !hasGps()) {
                    notifier("GPS unavailable. Pair watch to the phone to resume.");
                } else if (phoneAvailable && !hasGps()) {
                    notifier("GPS on watch now being used. Pair phone to save battery.");
                }
            }
        });
    }

    // This is the method that asks the phone GPS Sensor
    // or in-built Sensor to give location updates.
    private void requestLocationUpdates() {
        LocationRequest request = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL_MS)
                .setFastestInterval(FASTEST_INTERVAL_MS);

        LocationServices.FusedLocationApi
                        .requestLocationUpdates(mGoogleApiClient, request, mLocationListener)
                        .setResultCallback(new ResultCallback<Status>() {
                            @Override
                            public void onResult(Status status) {
                                if (status.getStatus().isSuccess()) {
                                    Log.e(TAG, "Successfully requested location updates");
                                } else {
                                    Log.d(TAG, "Failed in requesting location updates, " +
                                          "status code: " + status.getStatusCode() +
                                          ", message: " + status.getStatusMessage()         );
                                }
                            }
                        });
    }

    // This global variable contains an Overridden Existing
    // Method that listens to changes in location.
    private final LocationListener mLocationListener = new LocationListener() {
        // Overridden Existing Method that is executed for every new location update.
        @Override
        public void onLocationChanged(Location location) {
            // Gets the Latitude and Longitude values to 6 d.p. and converts it into string.
            String Latitude = String.format(Locale.UK, "%.6f", location.getLatitude());
            String Longitude = String.format(Locale.UK, "%.6f", location.getLongitude());

            try {
                DatabaseHelperWear myDBHelper;
                myDBHelper = new DatabaseHelperWear(MainWearActivity.this);

                // Creates and opens the database for the program to work with.
                // Catches any exceptions found that cannot be handled.
                myDBHelper.createDataBase();

                try {
                    myDBHelper.openDataBase();
                } catch(SQLException SQLExp){
                    throw new Error("Unable to open database due to " + SQLExp.getMessage());
                }

                // Gets a writable version of the database and enters/replaces the Postcode,
                // Latitude and Longitude inside the database where the 'Type' Column
                // contains the string 'Search'.
                SQLiteDatabase db = myDBHelper.getWritableDatabase();
                try {
                    // A container to contain the Latitude and Longitude strings.
                    ContentValues values = new ContentValues();
                    values.put("Latitude", Latitude);
                    values.put("Longitude", Longitude);

                    // The WHERE Clause is set up and then the database is updated
                    // using 'values' and the WHERE Clause.
                    String where = "Type = ?";
                    String[] whereArgs = new String[] {"Search"};
                    db.update(DatabaseHelperWear.DB_TABLE, values, where, whereArgs);

                    // Notifier to allow the user to carry on with the
                    // program after waiting for location update.
                    notifier("Location updated");
                } catch (SQLException exp) {
                    throw new Error("Unable to enter data into database");
                }
                // The database is closed to ensure lack of data leak from the database.
                db.close();
                myDBHelper.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    };

    // Access the vibrator motor and vibrates for given milliseconds.
    private void vibration(int duration) {
        Context context = getApplicationContext();
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator.hasVibrator()) { // If the device has a vibrator...
            Log.v(TAG, "Vibrator Found");
            vibrator.vibrate(duration);
        } else { // Otherwise...
            Log.v(TAG, "Vibrator Not Found");
        }
    }

    // Notifies the user about what is going on in the background.
    private void notifier(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    // This class is the class that creates an Adapter for Grid View for easy implementation.
    public class LayoutAdapter extends GridPagerAdapter {

        // These global variables for 'LayoutAdapter' class are kept a constant
        // as they don't require changes.
        final Context mContext;
        private final Integer[][] main_layout = {
                {R.layout.welcome_page,
                 R.layout.round_activity_main_wear,
                 R.layout.round_activity_home_page,
                 R.layout.round_activity_work_page}
        };

        // The class constructor assigns the Context obtained from the declaration to 'mContext'
        // to be used throughout the whole class.
        public LayoutAdapter(final Context context) {
            mContext = context;
        }

        // Overridden Existing Method that provides the number of rows for Grid View.
        @Override
        public int getRowCount() {
            return 1;
        }

        // Overridden Existing Method that provides the number of columns for Grid View.
        @Override
        public int getColumnCount(int row) {
            return 4;
        }

        // Overridden Existing Method that dynamically creates a view in Grid View
        // and calls a method to add functionality.
        @Override
        public Object instantiateItem(ViewGroup container, int row, int col) {
            //Dynamically adds the appropriate layout for the column position.
            final View view2 = LayoutInflater.from(mContext)
                                             .inflate(main_layout[row][col], container, false);
            container.addView(view2);

            // Depending on the column number, a Listener is set for the button in current layout.
            if (col == 1) {
                circleButtonListener(R.id.main_search_button, "Search");
            } else if (col == 2) {
                circleButtonListener(R.id.home_search_button, "Home");
            } else if (col == 3) {
                circleButtonListener(R.id.work_search_button, "Work");
            }
            return view2; // Returns the functional layout to be shown to the user.
        }

        // Method to add functionality to the circular button in the layout
        // and assign the Type of the search, based on the button.
        public void circleButtonListener(int buttonID, final String locationType) {
            CircleButton button = (CircleButton) findViewById(buttonID);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Type = locationType;
                    vibration(250);
                    Intent cardViewLayout = new Intent(MainWearActivity.this, CardViewLayout.class);
                    startActivity(cardViewLayout);
                }
            });
        }

        // Removes the layout when moved to another layout in the Grid View by swiping.
        @Override
        public void destroyItem(ViewGroup container, int row, int col, Object view) {
            container.removeView((View) view);
        }

        // Checks if the layout is the same as the one being considered.
        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }
}