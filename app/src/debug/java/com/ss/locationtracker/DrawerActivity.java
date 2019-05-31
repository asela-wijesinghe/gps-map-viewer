package com.ss.locationtracker;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.ss.locationtracker.helper.FetchURL;
import com.ss.locationtracker.helper.TaskLoadedCallback;
import com.ss.models.LocationModel;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DrawerActivity extends AppCompatActivity
        implements OnMapReadyCallback, LocationListener, TaskLoadedCallback, NavigationView.OnNavigationItemSelectedListener {

    final static int PERMISSION_ALL = 1;
    final static String[] PERMISSIONS = {android.Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION};
    private GoogleMap mMap;
    MarkerOptions myMarker;
    Marker marker;
    LocationManager locationManager;
    ArrayList<MarkerOptions> parks = new ArrayList <MarkerOptions>();
    ArrayList<MarkerOptions> arcs =  new ArrayList <MarkerOptions>();
    ArrayList<MarkerOptions> hotels =  new ArrayList <MarkerOptions>();
    TextView locationLabel;
    private Polyline currentPolyline;
    private ArrayList<LocationModel> locationList  = new ArrayList<LocationModel>();
    private String jsonResult;
//    private String url = "https://evening-ravine-75893.herokuapp.com/locations";
//    private String url = "http://localhost:8000/locations";

    private String url = "http://192.168.8.132:8000/locations";
    private String FLAG= "";


    @Override
    public void onTaskDone(Object... values) {
        if (currentPolyline != null)
            currentPolyline.remove();
        currentPolyline = mMap.addPolyline((PolylineOptions) values[0]);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);



        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        accessWebService();


        locationLabel = findViewById(R.id.locationLabel);


        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        myMarker = new MarkerOptions().position(new LatLng(6.854836, 79.903603)).title("My Current Location").icon(BitmapDescriptorFactory.fromResource(R.drawable.placeholder));
        if (Build.VERSION.SDK_INT >= 23 && !isPermissionGranted()) {
            requestPermissions(PERMISSIONS, PERMISSION_ALL);
        } else requestLocation();
        if (!isLocationEnabled())
            showAlert(1);
    }

    private void filterByFlag() {

    if(locationList!=null){
        for (LocationModel lm : locationList){
            String value = lm.getFlag();

            if (value.equals("H")) {
                hotels.add(new MarkerOptions().position(new LatLng(lm.getLat(), lm.getLon())).title(lm.getName()).icon(BitmapDescriptorFactory.fromResource(R.drawable.dish)));
            } else if (value.equals("N")) {
                arcs.add(new MarkerOptions().position(new LatLng(lm.getLat(), lm.getLon())).title(lm.getName()).icon(BitmapDescriptorFactory.fromResource(R.drawable.park)));
            } else if (value.equals("A")) {
                parks.add(new MarkerOptions().position(new LatLng(lm.getLat(), lm.getLon())).title(lm.getName()).icon(BitmapDescriptorFactory.fromResource(R.drawable.museum)));
            }
        }
    }
    }


    public void accessWebService() {
        JsonReadTask task = new JsonReadTask();
        // passes values for the urls string array
        task.execute(new String[] { url });
    }

    // Async Task to access the web
    private class JsonReadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet httpget = new HttpGet(url);
            try {
                HttpResponse response = httpclient.execute(httpget);
                jsonResult = inputStreamToString(
                        response.getEntity().getContent()).toString();
                System.out.println("jsonResult............................................." + jsonResult);
                //INITIALIZE LIST
                if(jsonResult!=null){

                    ListGenerator();
                }
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        private StringBuilder inputStreamToString(InputStream is) {
            String rLine = "";
            StringBuilder answer = new StringBuilder();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));

            try {
                while ((rLine = rd.readLine()) != null) {
                    answer.append(rLine);
                }
            } catch (IOException e) {
                // e.printStackTrace();
                Toast.makeText(getApplicationContext(),
                        "Error..." + e.toString(), Toast.LENGTH_LONG).show();
            }
            return answer;
        }
    }

    // build hash set for list view
    public void ListGenerator() {


        try {

            JSONArray jsonMainNode =  new JSONArray(jsonResult);

            System.out.println("jsonResponse............................................." + jsonMainNode);

            for (int i = 0; i < jsonMainNode.length(); i++) {
                JSONObject jsonChildNode = jsonMainNode.getJSONObject(i);

                System.out.println("Data from API : "+jsonChildNode);
                String flag = jsonChildNode.optString("flag");
                String id = jsonChildNode.optString("id");
                String desc = jsonChildNode.optString("desc");
                                String name = jsonChildNode.optString("name");
                                double lat = Double.parseDouble(jsonChildNode.optString("lat"));
                                double lon = Double.parseDouble(jsonChildNode.optString("lon"));

                locationList.add(new LocationModel( id,  flag,  name,  desc,  lat,  lon));
            }


            //set values from API
            System.out.println("locations.................................."+locationList);

            //prepare data to show on map
            filterByFlag();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;



        marker = mMap.addMarker(myMarker);


        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(myMarker.getPosition().latitude, myMarker.getPosition().longitude))
                .zoom(12)
                .build();
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));


        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker arg0) {
                if (marker.isInfoWindowShown()) {
                    marker.hideInfoWindow();
                } else {
                    marker.showInfoWindow();
                }
                if (myMarker != null) {
                    new FetchURL(DrawerActivity.this).execute(getUrl(myMarker.getPosition(), arg0.getPosition(), "driving"), "driving");
                }
                locationLabel.setText(arg0.getTitle());    //Change TextView text here like this
                return true;
            }
        });
    }

    @Override
    public void onLocationChanged(Location location) {
        LatLng myCoordinates = new LatLng(location.getLatitude(), location.getLongitude());
        marker.setPosition(myCoordinates);
        mMap.animateCamera(CameraUpdateFactory.newLatLng(myCoordinates),1000, null);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

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
        getMenuInflater().inflate(R.menu.drawer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long

        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
           FLAG = "*";
        } else if (id == R.id.nav_gallery) {
            FLAG = "H";

        } else if (id == R.id.nav_slideshow) {
            FLAG = "A";
        } else if (id == R.id.nav_manage) {
            FLAG = "N";
        }

        mMap.clear();


        if (FLAG.equals("*")) {


            for (MarkerOptions mark : parks){
                marker = mMap.addMarker(mark);
            }
            for (MarkerOptions mark : hotels){
                marker = mMap.addMarker(mark);
            }
            for (MarkerOptions mark : arcs){
                marker = mMap.addMarker(mark);
            }
        } else if (FLAG.equals("H")) {
            for (MarkerOptions mark : hotels){
                marker = mMap.addMarker(mark);
            }
        } else if (FLAG.equals("N")) {
            for (MarkerOptions mark : parks){
                marker = mMap.addMarker(mark);
            }
        } else if (FLAG.equals("A")) {

            for (MarkerOptions mark : arcs){
                marker = mMap.addMarker(mark);
            }
        }

        marker = mMap.addMarker(myMarker);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    //maps part

    private void requestLocation() {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        String provider = locationManager.getBestProvider(criteria, true);
        locationManager.requestLocationUpdates(provider, 10000, 10, this);
    }

    private boolean isLocationEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private boolean isPermissionGranted() {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED || checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Log.v("mylog", "Permission is granted");
            return true;
        } else {
            Log.v("mylog", "Permission not granted");
            return false;
        }
    }


    private String getUrl(LatLng origin, LatLng dest, String directionMode) {
        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        // Mode
        String mode = "mode=" + directionMode;
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + mode;
        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + getString(R.string.google_maps_key);
        return url;
    }


    private void showAlert(final int status) {
        String message, title, btnText;
        if (status == 1) {
            message = "Your Locations Settings is set to 'Off'.\nPlease Enable Location to " +
                    "use this app";
            title = "Enable Location";
            btnText = "Location Settings";
        } else {
            message = "Please allow this app to access location!";
            title = "Permission access";
            btnText = "Grant";
        }
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setCancelable(false);
        dialog.setTitle(title)
                .setMessage(message)
                .setPositiveButton(btnText, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        if (status == 1) {
                            Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(myIntent);
                        } else
                            requestPermissions(PERMISSIONS, PERMISSION_ALL);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        finish();
                    }
                });
        dialog.show();
    }
}

