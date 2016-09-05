package pl217.mosis.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import pl217.mosis.GeofenceHelper;
import pl217.mosis.R;
import pl217.mosis.RESTful;
import pl217.mosis.model.Event;
import pl217.mosis.model.User;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    public static int SEARCH_CODE = 217;
    public static int ADD_CODE = 23;
    public static int CATEGORY_CODE = 2910;
    public static int CHECK_IN = 2204;
    private GoogleMap mMap;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private String provider;
    private View header;
    private Bitmap userImage = null;
    private Marker mMarker;
    private Handler guiThread;
    private ProgressDialog progressDialog;
    private HashMap<Marker, String> friendsMap;
    private HashMap<Marker, Long> eventsMap;
    private ScheduledFuture<?> futureHandle;
    private ScheduledExecutorService scheduler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        String username = getIntent().getStringExtra("username");

        if (savedInstanceState != null) userImage = savedInstanceState.getParcelable("image");

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        if (drawer != null)
            drawer.addDrawerListener(toggle);//setDrawerListener is deprecated
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView != null)
            navigationView.setNavigationItemSelectedListener(this);

        header = navigationView.getHeaderView(0);

        guiThread = new Handler();
        progressDialog = new ProgressDialog(this);
        friendsMap = new HashMap<>();
        eventsMap = new HashMap<>();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        setLocationManager();

        setNavigationHeaderInfo(username);

        scheduler = Executors.newScheduledThreadPool(1);
    }

    private void setNavigationHeaderInfo(String username) {

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        boolean done = sharedPref.getBoolean(getString(R.string.stored_download), false);

        if (!done) {
            getUserData(username);
        } else {
            TextView textView = (TextView) header.findViewById(R.id.nav_header_username);
            ImageView imageView = (ImageView) header.findViewById(R.id.imageView);

            if (textView != null) {
                String info = "Username: " + sharedPref.getString(getString(R.string.stored_username), "Not provided");
                info += "\n" + "Name: " + sharedPref.getString(getString(R.string.stored_name), "Not provided");
                info += "\n" + "Phone: " + sharedPref.getString(getString(R.string.stored_phone), "Not provided");
                textView.setText(info);
            }
            if (imageView != null && userImage != null) {
                imageView.setImageBitmap(userImage);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable("image", userImage);
    }

    private void UIsetNavigationHeaderInfo(final String info) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView textView = (TextView) header.findViewById(R.id.nav_header_username);
                ImageView imageView = (ImageView) header.findViewById(R.id.imageView);

                if (textView != null) {
                    textView.setText(info);
                }
                if (imageView != null && userImage != null) {
                    imageView.setImageBitmap(userImage);
                }
            }
        });
    }

    private void getUserData(final String username) {

        ExecutorService transThread = Executors.newSingleThreadExecutor();
        transThread.submit(new Runnable() {
            @Override
            public void run() {
                User user = RESTful.getUser(username);

                SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();

                editor.putString(getString(R.string.stored_username), username);
                editor.putBoolean(getString(R.string.stored_download), true);
                String info = "Username: " + username;

                if (user != null) {
                    editor.putString(getString(R.string.stored_name), user.getFullName());
                    info += "\nName: " + user.getFullName();
                    editor.putString(getString(R.string.stored_phone), user.getPhone());
                    info += "\nPhone: " + user.getPhone();
                    editor.putInt(getString(R.string.stored_points), user.getPoints());

                    //TODO: Uncomment following section
                    /*if (user.getPoints() < 50) {
                        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
                        if (navigationView != null) {
                            navigationView.getMenu().getItem(0).getSubMenu().getItem(0).setEnabled(false);
                        }
                    }*/

                    userImage = RESTful.myImage(username);
                }

                editor.commit();
                UIsetNavigationHeaderInfo(info);
            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settings = new Intent(this, SettingsActivity.class);
            startActivity(settings);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_add) {

            startActivityForResult(new Intent(MainActivity.this, NewEventActivity.class), ADD_CODE);

        } else if (id == R.id.nav_bluetooth) {
            bluetoothCheck();
        } else if (id == R.id.nav_friends) {
            if (futureHandle != null && !futureHandle.isDone()) {
                futureHandle.cancel(true);
            } else {
                Runnable getFriendsLocation = new Runnable() {
                    public void run() {
                        showFriendsOnMap();
                    }
                };

                futureHandle = scheduler.scheduleAtFixedRate(getFriendsLocation, 0, 20, TimeUnit.SECONDS);
                scheduler.schedule(new Runnable() {
                    public void run() {
                        futureHandle.cancel(true);
                    }
                }, 5, TimeUnit.MINUTES);
            }
        } else if (id == R.id.nav_search) {
            startActivityForResult(new Intent(MainActivity.this, NearbySearchActivity.class), SEARCH_CODE);
        } else if (id == R.id.nav_category) {
            startActivityForResult(new Intent(MainActivity.this, CategorySelectionActivity.class), CATEGORY_CODE);
        } else if (id == R.id.nav_ranking) {
            Intent rankings = new Intent(this, UserRankingsActivity.class);
            startActivity(rankings);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SEARCH_CODE && resultCode == RESULT_OK) {
            final Bundle bundle = data.getExtras();

            ExecutorService transThread = Executors.newSingleThreadExecutor();
            transThread.submit(new Runnable() {
                @Override
                public void run() {
                    guiStartProgressDialog("Connecting", "Finding nearby events");

                    if (bundle != null)
                        showEventsInRadius(bundle.getInt("radius"));

                    guiDissmissProgressDialog();
                }
            });

        } else if (requestCode == SEARCH_CODE && resultCode == RESULT_CANCELED)
            Toast.makeText(MainActivity.this, "Search not performed", Toast.LENGTH_SHORT).show();
        else if (requestCode == ADD_CODE && resultCode == RESULT_OK) {
            final Bundle bundle = data.getExtras();

            ExecutorService transThread = Executors.newSingleThreadExecutor();
            transThread.submit(new Runnable() {
                @Override
                public void run() {
                    guiStartProgressDialog("Connecting", "Inserting new event");

                    if (bundle != null) {
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        Location location = locationManager.getLastKnownLocation(provider);
                        if (location != null) {
                            final boolean result = RESTful.addNewEvent(bundle.getString("name"),
                                    bundle.getString("about"), bundle.getString("type"), location.getLatitude(),
                                    location.getLongitude(), bundle.getLong("deadline"));

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Snackbar snackbar = Snackbar.make(findViewById(R.id.map),
                                            "New event added", Snackbar.LENGTH_LONG);
                                    View sbView = snackbar.getView();
                                    sbView.setBackgroundColor(getResources()
                                            .getColor(android.R.color.holo_green_dark));

                                    if (!result) {
                                        snackbar.setText("Adding failed");
                                        sbView.setBackgroundColor(getResources()
                                                .getColor(android.R.color.holo_red_dark));
                                    }

                                    snackbar.show();
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, "Cannot add new event now", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }

                    guiDissmissProgressDialog();
                }
            });
        } else if (requestCode == CATEGORY_CODE && resultCode == RESULT_OK) {
            final Bundle bundle = data.getExtras();

            ExecutorService transThread = Executors.newSingleThreadExecutor();
            transThread.submit(new Runnable() {
                @Override
                public void run() {
                    guiStartProgressDialog("Connecting", "Finding events");

                    if (bundle != null)
                        showEventsByCategory(bundle.getString("category"));

                    guiDissmissProgressDialog();
                }
            });
        } else if (requestCode == CHECK_IN && resultCode == RESULT_OK) {
            // Calling Application class (see application tag in AndroidManifest.xml)
            final GeofenceHelper globalObject = (GeofenceHelper) getApplicationContext();

            Bundle bundle = data.getExtras();
            if (bundle != null) {
                globalObject.addGeofenceToList(bundle.getDouble("latitude"), bundle.getDouble("longitude"));
                globalObject.addGeofences();
            }
        }
    }

    private void bluetoothCheck() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Device does not support Bluetooth!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            AlertDialog.Builder aBuilder = new AlertDialog.Builder(this);
            aBuilder.setTitle("Bluetooth is disabled");
            aBuilder.setMessage("Do you want to enable bluetooth through app settings?");

            aBuilder.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent settings = new Intent(MainActivity.this, SettingsActivity.class);
                    startActivity(settings);
                }
            });

            aBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    //dialog.cancel();
                }
            });

            aBuilder.show();
        } else {
            Intent bluetooth = new Intent(this, BluetoothActivity.class);
            startActivity(bluetooth);

            SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            mBluetoothAdapter.setName(sharedPref.getString(getString(R.string.stored_username), "Attempt no landing there"));
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location location = locationManager.getLastKnownLocation(provider);
        if (location != null) {
            showCurrentLocation(location);
        } else {
            mMap.clear();

            LatLng defaultPosition = new LatLng(43.3315734, 21.892546713);

            mMarker = mMap.addMarker(new MarkerOptions()
                    .position(defaultPosition));

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultPosition, 18));
        }

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                String friendName = friendsMap.get(marker);
                Long uid = eventsMap.get(marker);

                if (friendName != null) {
                    Intent userInfo = new Intent(MainActivity.this, UserInfoActivity.class);
                    userInfo.putExtra("username", friendName);
                    startActivity(userInfo);
                } else if (uid != null) {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return true;
                    }
                    Location location = locationManager.getLastKnownLocation(provider);
                    if (location != null) {
                        Intent eventInfo = new Intent(MainActivity.this, EventInfoActivity.class);
                        eventInfo.putExtra("uid", uid);
                        eventInfo.putExtra("latitude", location.getLatitude());
                        eventInfo.putExtra("longitude", location.getLongitude());
                        startActivityForResult(eventInfo, CHECK_IN);
                    }
                }

                return true;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(provider, 5000, 2, locationListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.removeUpdates(locationListener);
    }

    private void setLocationManager() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);

        provider = locationManager.getBestProvider(criteria, true);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                showCurrentLocation(location);
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
    }

    private void showCurrentLocation(Location location) {

        if (mMarker != null)
            mMarker.remove();

        LatLng currentPosition = new LatLng(location.getLatitude(), location.getLongitude());

        mMarker = mMap.addMarker(new MarkerOptions()
                .position(currentPosition)
                .flat(true));

        // Zoom in, animating the camera.
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, 18));
    }

    private void showFriendsOnMap() {

        removeFriendsMarkers();

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        ArrayList<String> friends = RESTful.getFriends(sharedPref.getString(getString(R.string.stored_username), "Imaginary friends"));

        for (int i = 0; i < friends.size(); i++) {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(RESTful.getUserPosition(friends.get(i)));
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(RESTful.myImage(friends.get(i))));
            guiThreadAddFriendMarker(markerOptions, friends.get(i));
        }
    }

    private void removeEventMarkers() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (Marker marker : eventsMap.keySet()) {
                    marker.remove();
                }
                eventsMap.clear();
            }
        });
    }

    private void removeFriendsMarkers() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (Marker marker : friendsMap.keySet()) {
                    marker.remove();
                }
                friendsMap.clear();
            }
        });
    }

    private int getMarkerIcon(String category) {
        switch (category) {
            case "Sport":
                return R.drawable.sport_marker;
            case "Festival":
                return R.drawable.festival_marker;
            case "Music":
                return R.drawable.music_marker;
            case "Film":
                return R.drawable.film_marker;
            case "Shopping":
                return R.drawable.shopping_marker;
            case "Gallery":
                return R.drawable.gallery_marker;
            case "Theater":
                return R.drawable.theater_marker;
            case "Fair":
                return R.drawable.fair_marker;
            default:
                return 0;
        }
    }

    private void showEventsInRadius(int radius) {

        removeEventMarkers();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location location = locationManager.getLastKnownLocation(provider);
        if (location != null) {
            ArrayList<Event> events = RESTful.getEventsInRadius(location.getLatitude(), location.getLongitude(), radius);

            for (int j = 0; j < events.size(); j++) {
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(new LatLng(events.get(j).getLatitude(), events.get(j).getLongitude()));
                markerOptions.icon(BitmapDescriptorFactory.fromResource(getMarkerIcon(events.get(j).getCategory())));
                guiThreadAddEventMarker(markerOptions, events.get(j).getuID());
            }
        } else
            Toast.makeText(MainActivity.this, "Cannot find nearby places", Toast.LENGTH_SHORT).show();
    }

    private void showEventsByCategory(String category) {

        removeEventMarkers();

        ArrayList<Event> events = RESTful.getEventsByCategory(category);

        for (int j = 0; j < events.size(); j++) {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(new LatLng(events.get(j).getLatitude(), events.get(j).getLongitude()));
            markerOptions.icon(BitmapDescriptorFactory.fromResource(getMarkerIcon(category)));
            guiThreadAddEventMarker(markerOptions, events.get(j).getuID());
        }
    }

    private void guiThreadAddFriendMarker(final MarkerOptions markerOptions, final String friendName) {
        guiThread.post(new Runnable() {
            @Override
            public void run() {
                friendsMap.put(mMap.addMarker(markerOptions), friendName);
            }
        });
    }

    private void guiThreadAddEventMarker(final MarkerOptions markerOptions, final long uID) {
        guiThread.post(new Runnable() {
            @Override
            public void run() {
                eventsMap.put(mMap.addMarker(markerOptions), uID);
            }
        });
    }

    private void guiStartProgressDialog(final String title, final String message) {
        guiThread.post(new Runnable() {
            @Override
            public void run() {
                progressDialog.setTitle(title);
                progressDialog.setMessage(message);
                progressDialog.show();
            }
        });
    }

    private void guiDissmissProgressDialog() {
        guiThread.post(new Runnable() {
            @Override
            public void run() {
                progressDialog.dismiss();
            }
        });
    }

}
