package com.jawad.wifihotspotfinder;

import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// This class here listens for a message from the phone and
// receives the new work and home locations that the user wants to search around for hotspots.
public class DataLayerListenerService extends WearableListenerService {

    // This String variable is used to let Programmer(s) know where
    // the log entry in Android Monitor is coming from.
    private final String TAG = "NodeListener";

    // This is a built-in method extended from WearableListenerService
    // class that deals with the messages received.
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        // Checks if the message is from the right place by checking address sent with the data.
        // If the address matches...
        if (messageEvent.getPath().equals(DatabaseHelperWear.DB_DIRECTORY)) {
            // Gets the JSON data from the message.
            final String message = new String(messageEvent.getData());
            Log.v(TAG, "Message path received on path is: " + messageEvent.getPath());
            Log.v(TAG, "Message received on watch is: " + message);

            // Creates and opens the database for the program to work with.
            // Catches any exceptions found that cannot be handled.
            try {
                DatabaseHelperWear databaseHelper;
                databaseHelper = new DatabaseHelperWear(this);

                databaseHelper.createDataBase();

                try {
                    databaseHelper.openDataBase();
                } catch (SQLException SQLEx) {
                    throw new Error("Unable to open database due to" + SQLEx.getMessage());
                }

                // Gets a writable version of the database and enters/replaces the Postcode,
                // Latitude and Longitude inside the database for home and work locations
                // from the JSON Array String.
                SQLiteDatabase db = databaseHelper.getWritableDatabase();
                try {
                    // Converts the JSON Array String to JSON Array.
                    JSONArray jArray = new JSONArray(message);
                    for (int i = 0; i < jArray.length(); i++) {
                        JSONObject json_data = jArray.getJSONObject(i);

                        // Container to contain the values with the location associated with it.
                        ContentValues initialValues = new ContentValues();
                        initialValues.put("Postcode", json_data.getString("Postcode"));
                        initialValues.put("Latitude", json_data.getString("Latitude"));
                        initialValues.put("Longitude", json_data.getString("Longitude"));

                        // The WHERE Clause is created and using that the database is updated.
                        String where = "_id = ?";
                        String[] whereArgs = new String[] {String.valueOf(i + 1)};
                        db.update(DatabaseHelperWear.DB_TABLE, initialValues, where, whereArgs);
                    }
                } catch (SQLException SQLEx) {
                    // Catches any SQL Exceptions that cannot be handled.
                    throw new Error("Unable to enter data due to " + SQLEx.getMessage());
                } catch (JSONException JSONEx) {
                    // Catches any JSON Exceptions that cannot be handled.
                    throw new Error("Unable to parse data due to " + JSONEx.getMessage());
                }

                //The database closed to prevent any memory leak from the database.
                db.close();
                databaseHelper.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else if (messageEvent.getPath().equals("MainPhoneActivity")) {
            // If the message is from MainWearActivity Class...

            // Converts the message from bytes to String.
            final String message = new String(messageEvent.getData());

            // If the command received is to open the watch's app...
            if (message.equals("Open Wear App")) {
                PackageManager manager = getPackageManager();
                Intent openWearApp = manager.getLaunchIntentForPackage(getPackageName());
                openWearApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                // Starts the application
                startActivity(openWearApp);
            } else {
                Log.d(TAG, "Didn't get the right message");
            }
        } else { // Otherwise, the event is logged for Programmer(s) to debug later.
            Log.d(TAG, "Didn't get the right message");
            super.onMessageReceived(messageEvent);
        }
    }
}