package com.jawad.wifihotspotfinder;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

/*  This class converts the Database that handles the user's location to JSON.
 *  JSON stands for JavaScript Object Notation which is a lightweight data-interchange format.
 *  The whole idea for this class is to create a format through which data from the database can
 *  be easily sent to the watch and that afterwards data can be extracted easily on the other end.
 */
public class DatabaseToJSON {
    private final DatabaseHelper dbHandler;

    // This String variable is used to let Programmer(s) know where the
    // log entry in Android Monitor is coming from.
    private final String TAG = "DatabaseToJSON";

    /*  The constructor which shows the parameter required by this class.
     *  The parameter is the 'context' of the application and using the context obtained,
     *  the database class is called and assigned to a global variable.
     */
    public DatabaseToJSON(Context context) {
        dbHandler = new DatabaseHelper(context);
    }

    public JSONArray getJSON() {
        String myTable = DatabaseHelper.DB_TABLE; // Get the name of your table.

        // Creates and opens the database for the program to work with.
        dbHandler.createDataBase();

        try {
            dbHandler.openDataBase();
        } catch(SQLException SQLEx){
            throw new Error("Unable to open database due to " + SQLEx.getMessage());
        }

        // A readable database from the class is called here and queried to
        // obtain the whole database. The cursor is a navigator in android
        // that helps allows data to taken off of database individually.
        SQLiteDatabase db = dbHandler.getReadableDatabase();
        String searchQuery = "SELECT  * FROM " + myTable;
        Cursor cursor = db.rawQuery(searchQuery, null);

        // New JSON Array is created which will store the data.
        JSONArray resultSet = new JSONArray();

        // Cursor is moved to the first record and then looped through
        // the table until the end of table is reached.
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            //Gets the number of columns in the database to be used in the for loop.
            int totalColumn = cursor.getColumnCount();
            JSONObject rowObject = new JSONObject();

            // This for loop iterates through every column of the record
            // and enters them into the JSON Array.
            for(int i = 0; i < totalColumn; i++) {
                if(cursor.getColumnName(i) != null) {
                    try {
                        if( cursor.getString(i) != null ) {
                            Log.d(TAG, cursor.getString(i));
                            rowObject.put(cursor.getColumnName(i), cursor.getString(i));
                        } else {
                            // Empty string is kept if field is null.
                            rowObject.put(cursor.getColumnName(i), "");
                        }
                    } catch(Exception e) {
                        // Used to catch any errors that may not be easy to handle in normal code.
                        Log.d(TAG, e.getMessage());
                    }
                }
            }

            resultSet.put(rowObject);
            cursor.moveToNext();
        }
        cursor.close(); // Closes the cursor to prevent any further confusion for the program.
        db.close(); // Closes the database to prevent any further data leak.
        dbHandler.close();

        Log.d(TAG, resultSet.toString());
        return resultSet; // Sends back the JSON Array filled with data from the user's database.
    }
}
