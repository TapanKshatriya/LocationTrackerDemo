package com.location.tracker;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.esafirm.stubutton.StuButton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private StuButton stuButton;

    private GoogleMap googleMap;
    private LatLng latLng;
    private Marker currLocationMarker, newLocationMarker;
    private Polyline line;

    protected static final String TAG = "location-updates-sample";
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    //    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS;


    private final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    private final static String LOCATION_KEY = "location-key";
    private final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final int REQUEST_CHECK_SETTINGS = 10;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mCurrentLocation;
    private Boolean mRequestingLocationUpdates;
    private String mLastUpdateTime;

    private Utilities utilities;
    private CardView cardViewTime;
    private TextView textViewTotalTime;
    private Button buttonReset;
    //    private Animation animFadeIn, animFadeOut;
    private boolean isShiftStared = false;


    public static String latitude = "latitude", longitude = "longitude", startTime = "startTime", TimeLocation = "TimeLocation", isStarted = "isStarted";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        initControls(savedInstanceState);
        setListeners();
    }


    private void initControls(Bundle savedInstanceState) {
        utilities = new Utilities(MapsActivity.this);
        mRequestingLocationUpdates = false;
        mLastUpdateTime = "";

        updateValuesFromBundle(savedInstanceState);
        buildGoogleApiClient();

        stuButton = findViewById(R.id.stuButton);
        buttonReset = findViewById(R.id.buttonReset);
        cardViewTime = findViewById(R.id.cardViewTime);
        textViewTotalTime = findViewById(R.id.textViewTotalTime);

//        animFadeIn = AnimationUtils.loadAnimation(getApplicationContext(),
//                R.anim.fade_in);
//        animFadeOut = AnimationUtils.loadAnimation(getApplicationContext(),
//                R.anim.fade_out);
        float data = getSharedPreferencesLocation(longitude);
        if (data == 0f)
            buttonReset.setVisibility(View.GONE);
        else
            buttonReset.setVisibility(View.VISIBLE);

        if (getSharedPreferencesStatus(isStarted))
            stuButton.setRightPosition();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
//        checkLocationPermission();
    }

    private void setListeners() {

        buttonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearSharedPreferencesTimeLocation();
                buttonReset.setVisibility(View.GONE);
                stuButton.setLeftPosition();
                if (cardViewTime.getVisibility() == View.VISIBLE) {
                    cardViewTime.setVisibility(View.GONE);
                }
                clearMap();
            }
        });

        stuButton.setOnUnlockListener(new StuButton.OnUnlockListener() {
            @Override
            public void onUnlock() {
                buttonReset.setVisibility(View.VISIBLE);
                if (cardViewTime.getVisibility() == View.VISIBLE) {
                    cardViewTime.setVisibility(View.GONE);
                }
                clearMap();
                Toast.makeText(MapsActivity.this, "Start Shift", Toast.LENGTH_SHORT).show();
                setSharedPreferencesTimeLocation((float) mCurrentLocation.getLatitude(), (float) mCurrentLocation.getLongitude(), System.currentTimeMillis(), true);
            }

            @Override
            public void onLock() {
                if (getSharedPreferencesStatus(isStarted)) {
                    Toast.makeText(MapsActivity.this, "End Shift", Toast.LENGTH_SHORT).show();
                    showTime(getSharedPreferencesTime(startTime));
                    clearSharedPreferencesTimeLocation();
                    if (cardViewTime.getVisibility() == View.GONE) {
//                    cardViewTime.startAnimation(animFadeIn);
                        cardViewTime.setVisibility(View.VISIBLE);
                        buttonReset.setVisibility(View.GONE);
                    }
                }
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        this.googleMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        this.googleMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
//        this.googleMap.setMyLocationEnabled(true);
        this.googleMap.getUiSettings().setZoomControlsEnabled(false);
        this.googleMap.getUiSettings().setCompassEnabled(true);
        this.googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        this.googleMap.getUiSettings().setRotateGesturesEnabled(true);
        this.googleMap.getUiSettings().setAllGesturesEnabled(true);
        this.googleMap.getUiSettings().setZoomGesturesEnabled(true);
//        buildGoogleApiClient();
        if (mCurrentLocation != null)
            updateUI(mCurrentLocation);
    }


    private void updateValuesFromBundle(Bundle savedInstanceState) {
        Log.i(TAG, "Updating values from bundle");
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);
            }

            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);
            }
            if (mCurrentLocation != null)
                updateUI(mCurrentLocation);
        }
    }

    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        startUpdatesButtonHandler();
    }

    public void startUpdatesButtonHandler() {
        if (!isPlayServicesAvailable(this)) return;
        if (!mRequestingLocationUpdates) {
            mRequestingLocationUpdates = true;
        } else {
            return;
        }

        if (Build.VERSION.SDK_INT < 23) {
            startLocationUpdates();
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                showRationaleDialog();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        }
    }

    public void stopUpdatesButtonHandler(View view) {
        if (mRequestingLocationUpdates) {
            mRequestingLocationUpdates = false;
            stopLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        Log.i(TAG, "startLocationUpdates");

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        // 現在位置の取得の前に位置情報の設定が有効になっているか確認する
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();

                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // 設定が有効になっているので現在位置を取得する
                        if (ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, MapsActivity.this);
                        }
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // 設定が有効になっていないのでダイアログを表示する
                        try {
                            status.startResolutionForResult(MapsActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.
                        break;
                }
            }
        });
    }

    private void updateUI(Location location) {
        if (mCurrentLocation == null) return;
        if (currLocationMarker != null) {
            currLocationMarker.remove();
        }
        latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        if (googleMap != null) {
            currLocationMarker = googleMap.addMarker(markerOptions);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
        }
//        Toast.makeText(this, "Location Changed", Toast.LENGTH_SHORT).show();

        //zoom to current position:

//        mBinding.latitudeText.setText(String.format("%s: %f", mLatitudeLabel,
//                mCurrentLocation.getLatitude()));
//        mBinding.longitudeText.setText(String.format("%s: %f", mLongitudeLabel,
//                mCurrentLocation.getLongitude()));
//        mBinding.lastUpdateTimeText.setText(String.format("%s: %s", mLastUpdateTimeLabel,
//                mLastUpdateTime));
    }

    protected void stopLocationUpdates() {
        Log.i(TAG, "stopLocationUpdates");
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationUpdates();
                } else {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        mRequestingLocationUpdates = false;
                        Toast.makeText(MapsActivity.this, "To enable the function of this application please enable location permission of the application from the setting screen of the terminal.", Toast.LENGTH_SHORT).show();
                    } else {
                        showRationaleDialog();
                    }
                }
                break;
            }
        }
    }

    private void showRationaleDialog() {
        new AlertDialog.Builder(this)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(MapsActivity.this,
                                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(MapsActivity.this, "Position location permission was not allowed.", Toast.LENGTH_SHORT).show();
                        mRequestingLocationUpdates = false;
                    }
                })
                .setCancelable(false)
                .setMessage("This application needs to allow use of location permission.")
                .show();
    }

    public static boolean isPlayServicesAvailable(Context context) {
        // Google Play Service APKが有効かどうかチェックする
        int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance().getErrorDialog((Activity) context, resultCode, 2).show();
            return false;
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        startLocationUpdates();
                        break;
                    case Activity.RESULT_CANCELED:
                        break;
                }
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        isPlayServicesAvailable(this);

        // Within {@code onPause()}, we pause location updates, but leave the
        // connection to GoogleApiClient intact.  Here, we resume receiving
        // location updates if the user has requested them.

        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop location updates to save battery, but don't disconnect the GoogleApiClient object.
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    @Override
    protected void onStop() {
        stopLocationUpdates();
        mGoogleApiClient.disconnect();

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "onConnected");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (mCurrentLocation == null) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
            updateUI(mCurrentLocation);
        }

        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "onLocationChanged");
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        if (mCurrentLocation != null)
            updateUI(mCurrentLocation);
//        Toast.makeText(this, getResources().getString(R.string.location_updated_message), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }

    private void setSharedPreferencesTimeLocation(float startLatitude, float startLongitude, long time, boolean started) {
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(TimeLocation, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(latitude, startLatitude);
        editor.putFloat(longitude, startLongitude);
        editor.putLong(startTime, time);
        editor.putBoolean(isStarted, started);
        editor.commit();
    }

    private float getSharedPreferencesLocation(String key) {
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(TimeLocation, MODE_PRIVATE);
        return sharedPreferences.getFloat(key, 0f);
    }

    private long getSharedPreferencesTime(String key) {
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(TimeLocation, MODE_PRIVATE);
        return sharedPreferences.getLong(key, 0);
    }

    private boolean getSharedPreferencesStatus(String key) {
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(TimeLocation, MODE_PRIVATE);
        return sharedPreferences.getBoolean(key, false);
    }

    private void clearSharedPreferencesTimeLocation() {
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(TimeLocation, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(latitude, 0);
        editor.putFloat(longitude, 0);
        editor.putLong(startTime, 0);
        editor.putBoolean(isStarted, false);
        editor.commit();
    }


    private void showTime(long savedTime) {
        Calendar currentCalendar = Calendar.getInstance();
        Calendar oldCalendar = Calendar.getInstance();
        oldCalendar.setTimeInMillis(savedTime);
//        Log.e("TimeDifference", "TimeDifference : " + utilities.getTimeDifference(oldCalendar.getTime(), currentCalendar.getTime()));
        textViewTotalTime.setText(utilities.getTimeDifference(oldCalendar.getTime(), currentCalendar.getTime()));

        drawPath(getSharedPreferencesLocation(latitude), getSharedPreferencesLocation(longitude), 23.044563f, 72.524929f);

    }

    private void drawPath(float oldLatitude, float oldLongitude, float newLatitude, float newLongitude) {

        if (newLocationMarker != null)
            newLocationMarker.remove();

        latLng = new LatLng(newLatitude, newLongitude);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("New Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        if (googleMap != null) {
            newLocationMarker = googleMap.addMarker(markerOptions);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));

        }

        line = googleMap.addPolyline(new PolylineOptions()
                .add(new LatLng(oldLatitude, oldLongitude), new LatLng(newLatitude, newLongitude))
                .width(12)
                .color(Color.RED));


    }

    private void clearMap() {
        if (newLocationMarker != null)
            newLocationMarker.remove();

        if (currLocationMarker != null)
            currLocationMarker.remove();

        if (line != null)
            line.remove();

        if (mCurrentLocation != null)
            updateUI(mCurrentLocation);

    }
}
