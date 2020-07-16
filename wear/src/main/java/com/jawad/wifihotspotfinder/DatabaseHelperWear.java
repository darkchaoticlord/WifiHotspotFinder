package com.jawad.wifihotspotfinder;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// This class handles the database that contains the data for the locations
// required by the user, such as the user's home, work and current locations.
public class DatabaseHelperWear extends SQLiteOpenHelper {

    // The path, database name and table name of the Database is declared here as a global variable
    // that can be called from anywhere in the code when used to query or update the table.
    private final static String DB_PATH = "/data/data/com.jawad.wifihotspotfinder/databases/";
    private final static String DB_NAME = "user_locations.db";
    public final static String DB_DIRECTORY = DB_PATH + DB_NAME;
    public final static String DB_TABLE = "home_work_locations";

    // Version number is added to keep the updates in check internally.
    private static final int DATABASE_VERSION = 1;

    private SQLiteDatabase myDataBase;
    private final Context myContext;
    private final String TAG = "DatabaseHelperWear";

    // The class constructor that highlights the data required from the user
    // when the class is called from other classes.
    public DatabaseHelperWear(Context context) {
        super(context, DB_NAME, null, DATABASE_VERSION);
        myContext = context;
    }

    // Creates a empty database on the system and rewrites it with your own database.
    public void createDataBase() {
        boolean dbExist = checkDataBase();
        if(!dbExist){
            // By calling this method and empty database will be created into the
            // default system path of your application so we are gonna be able to
            // overwrite that database with our database.
            try {
                this.getReadableDatabase();
                copyDataBase();
                this.close();
            }
            catch (IOException e) {
                throw new Error("Error copying database");
            }
        }
    }

    /*  Check if the database already exist to avoid re-copying the file each time you
     *  open the application and returns true if it exists and false if it doesn't.
     *  It does this by trying to open the database and then assigning it to a variable
     *  to see if variable is null or not.
     */
    private boolean checkDataBase(){
        SQLiteDatabase checkDB = null;
        boolean exist = false;
        try{
            checkDB = SQLiteDatabase.openDatabase(DB_DIRECTORY, null, SQLiteDatabase.OPEN_READONLY);
        } catch(SQLiteException e) {
            // Logs for the user to see that database doesn't exist yet.
            Log.v("DB Log", "Database doesn't exist");
        }
        if(checkDB != null){
            exist = true;
            checkDB.close();
        }
        return exist;
    }

    /*  Copies your database from your local assets-folder to the just created empty
     *  database in the system folder, from where it can be accessed and handled.
     *  This is done by transferring using the 'bytestream' technique.
     */
    private void copyDataBase() throws IOException {
        // Open your local db as the input stream
        InputStream myInput = myContext.getAssets().open(DB_NAME);
        // Open the empty db as the output stream
        OutputStream myOutput = new FileOutputStream(DB_DIRECTORY);

        // Transfer bytes from the Input File to the Output File.
        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer))>0) {
            myOutput.write(buffer, 0, length);
        }

        // Code below closes the streams.
        myOutput.flush();
        myOutput.close();
        myInput.close();
    }

    public void openDataBase() throws SQLException{
        // Code below opens the database.
        myDataBase = SQLiteDatabase.openDatabase(DB_DIRECTORY, null, SQLiteDatabase.OPEN_READWRITE);
    }

    // Helps to keep the databases synchronised from the multiple accesses throughout the code.
    @Override
    public synchronized void close() {
        if(myDataBase != null)
            myDataBase.close();
        super.close();

    }

    // These two methods below must be declared as they are the extended part
    // of the SQLiteOpenHelper without which the DatabaseHelper class cannot
    // perform the same functions as SQLiteOpenHelper.

    // As we are not creating new databases but rather copying them from the database folder,
    // the onCreate method is left empty.
    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    // As we the code doesn't require any anything to be done after the database is updated,
    // the onUpgrade method is left empty.
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        String oldVers = String.valueOf(oldV);
        String newVers = String.valueOf(newV);
        Log.i(TAG, "The Database updated from Version " + oldVers + "to Version " + newVers);
    }
}