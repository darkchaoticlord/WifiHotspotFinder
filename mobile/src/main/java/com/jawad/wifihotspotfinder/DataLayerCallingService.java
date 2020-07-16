package com.jawad.wifihotspotfinder;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Vibrator;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

// This class here listens for a message from the watch and receives the telephone
// number sent from the watch and calls the number for the user.
public class DataLayerCallingService extends WearableListenerService {

    // This String variable is used to let Programmer(s) know where
    // the log entry in Android Monitor is coming from.
    private final String TAG = "CallingListener";

    // This is a built-in method extended from WearableListenerService
    // class that deals with the messages received.
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        // Checks if the message is from the right place by checking
        // the address(command in this case) sent with the data.
        if (messageEvent.getPath().equals("Call number from wear")) {  // If the command matches...
            // Gets the data from the message.
            final String message = new String(messageEvent.getData());
            Log.v(TAG, "Message command received on phone is: " + messageEvent.getPath());
            Log.v(TAG, "Message received on phone is: " + message);

            // Creates an action(intent) to call and sets the number as the data for the action.
            String number = "tel:" + message;
            Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse(number));
            callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Accesses the vibration sensor in device to vibrate for 500 milliseconds if found.
            Context context = getApplicationContext();
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator.hasVibrator()) {
                Log.v(TAG, "Vibrator Found");
                vibrator.vibrate(500);
            } else {
                Log.v(TAG, "Vibrator Not Found");
            }

            // Starts the call by carrying out the action(intent).
            startActivity(callIntent);
        } else {   // In case the message didn't have the right address/command.
            Log.d(TAG, "Didn't get the right message");
            super.onMessageReceived(messageEvent);
        }
    }
}
