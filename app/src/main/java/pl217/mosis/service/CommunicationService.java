package pl217.mosis.service;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import pl217.mosis.GeofenceHelper;
import pl217.mosis.R;
import pl217.mosis.RESTful;
import pl217.mosis.activity.EventInfoActivity;
import pl217.mosis.activity.MainActivity;
import pl217.mosis.model.Event;

public class CommunicationService extends Service {

    private PowerManager.WakeLock wakeLock;
    private LocationManager locationManager;
    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {

            try {

                SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file_key),
                        Context.MODE_PRIVATE);

                JSONObject data = new JSONObject();
                data.put("username", sharedPref.getString(getString(R.string.stored_username), "Not provided"));
                data.put("latitude", location.getLatitude());
                data.put("longitude", location.getLongitude());

                new LocationWebService().execute(data.toString());

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    public CommunicationService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "KeepCPUp");
        wakeLock.acquire();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return START_STICKY;
        }
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        String provider = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ?
                LocationManager.NETWORK_PROVIDER : locationManager.getBestProvider(criteria, true);
        locationManager.requestLocationUpdates(provider, 10000, 5, locationListener);

        Toast.makeText(CommunicationService.this, "Service started", Toast.LENGTH_SHORT).show();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        wakeLock.release();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.removeUpdates(locationListener);

        Toast.makeText(CommunicationService.this, "Service destroyed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class LocationWebService extends AsyncTask<String, Void, Void> {

        private Event event;

        @Override
        protected Void doInBackground(String... params) {

            event = RESTful.updateUserLocation(params[0]);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Toast.makeText(CommunicationService.this, "User location successfully updated", Toast.LENGTH_SHORT).show();

            if (event != null) {
                SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean(getString(R.string.stored_download), false);
                editor.commit();

                GeofenceHelper globalObject = (GeofenceHelper) getApplicationContext();
                if (!globalObject.getNotifiedEvents().contains(event.getuID())) {
                    Intent mainActivity = new Intent(CommunicationService.this, MainActivity.class);
                    mainActivity.putExtra("username", sharedPref.getString(getString(R.string.stored_username), "Not provided"));

                    Intent eventIntent = new Intent(CommunicationService.this, EventInfoActivity.class);
                    eventIntent.putExtra("uid", event.getuID());
                    eventIntent.putExtra("latitude", event.getLatitude());
                    eventIntent.putExtra("longitude", event.getLongitude());

                    // Construct a task stack.
                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(CommunicationService.this);

                    // Add the main Activity to the task stack as the parent.
                    stackBuilder.addNextIntent(mainActivity);

                    // Push the content Intent onto the stack.
                    stackBuilder.addNextIntent(eventIntent);

                    // Get a PendingIntent containing the entire back stack.
                    PendingIntent eventPendingIntent =
                            stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

                    // Get a notification builder that's compatible with platform versions >= 4
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(CommunicationService.this);

                    //Get notification sound from ringtone preference
                    SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(CommunicationService.this);
                    String strRingtonePreference = preference.getString("notifications_ringtone", "DEFAULT_SOUND");

                    // Define the notification settings.
                    builder.setSmallIcon(R.drawable.ic_notifications_black_24dp)
                            // In a real app, you may want to use a library like Volley
                            // to decode the Bitmap.
                            .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                                    R.drawable.ic_notifications_black_24dp))
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setSound(Uri.parse(strRingtonePreference))
                            .setContentTitle("Event nearby")
                            .setContentText("You are in a proximity of " + event.getCategory() + " event")
                            .setContentIntent(eventPendingIntent);

                    // Dismiss notification once the user touches it.
                    builder.setAutoCancel(true);

                    // Get an instance of the Notification manager
                    NotificationManager mNotificationManager =
                            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                    // Issue the notification
                    mNotificationManager.notify(0, builder.build());

                    globalObject.addNotifiedEvents(event.getuID());
                }
            }


        }
    }
}
