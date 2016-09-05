package pl217.mosis;

import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;

import pl217.mosis.service.GeofenceTransitionsIntentService;

public class GeofenceHelper extends Application implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

    public final int GEOFENCE_RADIUS_IN_METERS = 100;
    public final int GEOFENCE_EXPIRATION_IN_MILLISECONDS = 20 * 1000 * 60; //20 minutes

    //Provides the entry point to Google Play services.
    protected GoogleApiClient mGoogleApiClient;

    //The list of geofences used in this sample.
    protected ArrayList<Geofence> mGeofenceList;

    //Used when requesting to add or remove geofences.
    private PendingIntent mGeofencePendingIntent;

    private ArrayList<Long> notifiedEvents;

    @Override
    public void onCreate() {
        super.onCreate();

        mGeofenceList = new ArrayList<>();

        // Initially set the PendingIntent used in addGeofences() and removeGeofences() to null.
        mGeofencePendingIntent = null;

        notifiedEvents = new ArrayList<>();

        // Kick off the request to build GoogleApiClient.
        buildGoogleApiClient();
    }

    public void addGeofenceToList(double latitude, double longitude) {

        mGeofenceList.add(new Geofence.Builder()
                        // Set the request ID of the geofence. This is a string to identify this
                        // geofence.
                        .setRequestId(String.valueOf((int) (Math.random() * 1000 + 1)))

                                // Set the circular region of this geofence.
                        .setCircularRegion(
                                latitude,
                                longitude,
                                GEOFENCE_RADIUS_IN_METERS
                        )

                                // Set the expiration duration of the geofence. This geofence gets automatically
                                // removed after this period of time.
                        .setExpirationDuration(GEOFENCE_EXPIRATION_IN_MILLISECONDS)

                                // Set the transition types of interest. Alerts are only generated for these
                                // transition. We track entry and exit transitions in this sample.
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
                                // Create the geofence.
                        .build()
        );
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(mGeofenceList);

        // Return a GeofencingRequest.
        return builder.build();
    }

    /**
     * Adds geofences, which sets alerts to be notified when the device enters or exits one of the
     * specified geofences. Handles the success or failure results returned by addGeofences().
     */
    public void addGeofences() {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, "Google Api client not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    // The GeofenceRequest object.
                    getGeofencingRequest(),
                    // A pending intent that that is reused when calling addGeofences(). This
                    // pending intent is used to generate an intent when a matched geofence
                    // transition is observed.
                    getGeofencePendingIntent()
            ).setResultCallback(this); // Result processed in onResult().
        } catch (SecurityException securityException) {
            Log.w("GeofenceHelper", securityException);
        }
    }

    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    //Builds a GoogleApiClient. Uses the {@code #addApi} method to request the LocationServices API.
    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    /**
     * Removes geofences, which stops further notifications when the device enters or exits
     * previously registered geofences.
     */
    public void removeGeofences() {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, "Google API client not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            // Remove geofences.
            LocationServices.GeofencingApi.removeGeofences(
                    mGoogleApiClient,
                    // This is the same pending intent that was used in addGeofences().
                    getGeofencePendingIntent()
            );

            //mGeofenceList.clear();
        } catch (SecurityException securityException) {
            Log.w("Geofence API", securityException);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    /**
     * Runs when the result of calling addGeofences() becomes available.
     * Method can complete successfully or with an error.
     * <p/>
     * Since this class implements the {@link ResultCallback} interface, we are required to
     * define this method.
     *
     * @param status The Status returned through a PendingIntent when addGeofences() gets called.
     */
    @Override
    public void onResult(@NonNull Status status) {
        if (status.isSuccess()) {

            SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();

            editor.putBoolean(getString(R.string.stored_geofence), true);
            editor.commit();

            Toast.makeText(GeofenceHelper.this, "GeofenceHelper onResult hit", Toast.LENGTH_SHORT).show();

        } else {
            Log.e("GeofenceHelper", "Error in onResult");
        }
    }


    public ArrayList<Long> getNotifiedEvents() {
        return notifiedEvents;
    }

    public void addNotifiedEvents(Long notifiedEvent) {
        this.notifiedEvents.add(notifiedEvent);
    }
}
