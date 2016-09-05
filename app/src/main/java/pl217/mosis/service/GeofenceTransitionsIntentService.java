package pl217.mosis.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import pl217.mosis.GeofenceHelper;
import pl217.mosis.R;

public class GeofenceTransitionsIntentService extends IntentService {

    protected static final String TAG = "GeofenceTransitionsIntentService";

    public GeofenceTransitionsIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        if (geofencingEvent.hasError()) {
            Toast.makeText(GeofenceTransitionsIntentService.this, "Geofencing error", Toast.LENGTH_SHORT).show();
            return;
        }

        int geofenceTransitionType = geofencingEvent.getGeofenceTransition();
        if (geofenceTransitionType == Geofence.GEOFENCE_TRANSITION_EXIT) {

            SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();

            editor.putBoolean(getString(R.string.stored_geofence), false);
            editor.commit();

            sendNotification("You checked out from the event");

            final GeofenceHelper globalObject = (GeofenceHelper) getApplicationContext();
            globalObject.removeGeofences();

        } else {
            // Log the error.
            Log.w(TAG.substring(0, 22), "Geofencing error");
        }
    }

    /**
     * Posts a notification in the notification bar when a transition is detected.
     * If the user clicks the notification, control goes to the MainActivity.
     */
    private void sendNotification(String notificationDetails) {
        // Create an explicit content Intent that starts the main Activity.
        /*Intent notificationIntent = new Intent(this, MainActivity.class);

        // Construct a task stack.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        // Add the main Activity to the task stack as the parent.
        stackBuilder.addParentStack(MainActivity.class);

        // Push the content Intent onto the stack.
        stackBuilder.addNextIntent(notificationIntent);

        // Get a PendingIntent containing the entire back stack.
        PendingIntent notificationPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);*/

        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        //Get notification sound from ringtone preference
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
        String strRingtonePreference = preference.getString("notifications_ringtone", "DEFAULT_SOUND");

        // Define the notification settings.
        builder.setSmallIcon(R.drawable.ic_notifications_black_24dp)
                // In a real app, you may want to use a library like Volley
                // to decode the Bitmap.
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.drawable.ic_notifications_black_24dp))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(Uri.parse(strRingtonePreference))
                .setContentTitle("You left geofence")
                .setContentText(notificationDetails);
        //.setContentIntent(notificationPendingIntent);

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(true);

        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Issue the notification
        mNotificationManager.notify(0, builder.build());
    }
}
