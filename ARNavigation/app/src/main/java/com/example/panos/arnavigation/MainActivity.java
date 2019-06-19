package com.example.panos.arnavigation;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;


import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationEngineListener,
        PermissionsListener, MapboxMap.OnMapClickListener {

    private MapView mapView;
    private MapboxMap map;
    private Button startButton;
    private Spinner building;
    private Button chooseBuildings;
    private PermissionsManager permissionsManager;
    private LocationEngine locationEngine;
    private LocationLayerPlugin locationLayerPlugin;
    private Location originLocation;
    private Point originPosition;
    private Point destinationPosition;
    private Marker destinationMarker;
    private NavigationMapRoute navigationMapRoute;
    private static final String TAG = "MainActivity";
    private DirectionsRoute currentRoute;
    private String[] buildingItem = new String[]{"Gebäude 1", "Gebäude 1A", "Gebäude 2", "Gebäude 3", "Gebäude 4", "Gebäude 5",
            "Gebäude 6", "Gebäude 7", "Gebäude 8", "Gebäude 9", "Gebäude 10", "Gebäude 11", "Gebäude 12", "Gebäude 13",
            "Gebäude 14", "Gebäude 15", "Gebäude 16", "Gebäude 17", "Gebäude 18", "Gebäude 20"};

    private LatLng mypoint = new LatLng();

    public class mapCoordinates {
        public final double latitude;
        public final double longitude;

        public mapCoordinates(double lat, double lon) {
            latitude = lat;
            longitude = lon;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Checks for write and read permissions to storage
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);
        }


        //License from Mapbox to be able to load the map
        Mapbox.getInstance(this, "pk.eyJ1IjoieW9naWpzaCIsImEiOiJjandqN3Q5NDEwMnlnM3lvZTVwaG10bWh3In0.DH-WnRFP49wzW-KR5hxnaQ");

        //Creates the map, the buttons and the dropdownselection/spinner and adds them to the layout
        setContentView(R.layout.activity_main);
        mapView = (MapView) findViewById(R.id.mapView);
        startButton = findViewById(R.id.startButton);
        building = findViewById(R.id.buildings);
        chooseBuildings = findViewById(R.id.chooseBuilding);
        ArrayAdapter<String> buildItem = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, buildingItem);
        building.setAdapter(buildItem);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        //listener for the dropdownselection/spinner
        building.setOnItemSelectedListener(listener);

        //listener for the startbutton
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Sets options for the Turn-by-Turn navigation
                NavigationLauncherOptions options = NavigationLauncherOptions.builder()
                        .directionsProfile(DirectionsCriteria.PROFILE_WALKING) //change here for car navigation
                        .directionsRoute(currentRoute)
                        .shouldSimulateRoute(true) //For simulation set to true /////////////////////////////////////////
                        .build();


                //Launches the Turn-by-Turn navigation
                //NavigationLauncher.startNavigation(MainActivity.this, options);

                //Contains the data of the route with all the coordinates
                String fullRouteData = currentRoute.legs().toString();
                String tmp = currentRoute.legs().toString();


                int index = tmp.indexOf("rawLocation=");
                int count = 0;
                while (index != -1) {
                    count++;
                    tmp = tmp.substring(index + 1);
                    index = tmp.indexOf("rawLocation=");
                }

                String latitude = "";
                String longitude = "";

                //Filters the string and adds the coordinates in the coordinate List mapCoordinates
                List<mapCoordinates> mapCoordinates = new ArrayList<>();
                mapCoordinates.add(new mapCoordinates(originPosition.latitude(), originPosition.longitude()));
                index = fullRouteData.indexOf("rawLocation=");
                while (index != -1) {

                    fullRouteData = fullRouteData.substring(fullRouteData.indexOf("rawLocation=[") + 13, fullRouteData.length());
                    String temp = fullRouteData;
                    String temp2 = fullRouteData;
                    latitude = temp.substring(0, temp.indexOf(","));
                    longitude = temp2.substring(temp2.indexOf(",") + 1, temp2.indexOf("]"));
                    mapCoordinates.add(new mapCoordinates(Double.parseDouble(longitude), Double.parseDouble(latitude)));
                    index = fullRouteData.indexOf("rawLocation=");
                }
                mapCoordinates.add(new mapCoordinates(destinationPosition.latitude(), destinationPosition.longitude()));

                //Write Coordinates to text file
                // get the path to internal storage
                File sdcard = Environment.getExternalStorageDirectory();
                // to this path add a new directory path
                File dir = new File(sdcard.getAbsolutePath());
                // create this directory if not already created
                dir.mkdir();
                // create the file in which we will write the contents
                File file = new File(dir, "/arLocation/3/assets/coordinates.txt");
                FileOutputStream os = null;
                try {
                    os = new FileOutputStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                String data = "";
                for (int i = 0; i < mapCoordinates.size(); i++) {
                    data = data + mapCoordinates.get(i).latitude + ";" + mapCoordinates.get(i).longitude + "\n";
                }

                try {
                    os.write(data.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //Write HTML-File for Wikitude Implementation
                /////////////////////////////////////////////////////////////////
                File sdcard2 = Environment.getExternalStorageDirectory();
                // to this path add a new directory path
                File dir2 = new File(sdcard2.getAbsolutePath());
                // create this directory if not already created
                dir2.mkdir();
                // create the file in which we will write the contents
                File file2 = new File(dir2, "/arLocation/3/index.html");
                FileOutputStream os2 = null;
                try {
                    os2 = new FileOutputStream(file2);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                //Writes the JavaScript code in the HTML File which uses the Wikitude resources and creates the object in the GeoLocation
                String data2 = "<!DOCTYPE html>" + "\n";
                data2 += "<html lang=\"en\" dir=\"ltr\">" + "\n";
                data2 += "<head>" + "\n";
                data2 += "<meta charset=\"utf-8\">" + "\n";
                data2 += "<title></title>" + "\n";
                data2 += "<script src=\"https://www.wikitude.com/libs/architect.js\"></script>" + "\n";
                data2 += "<script src=\"../ade.js\"></script>" + "\n";
                data2 += "<link rel=\"stylesheet\" href=\"jquery/jquery.mobile-1.3.2.min.css\"/>" + "\n";
                data2 += "<link rel=\"stylesheet\" href=\"jquery/jquery-mobile-transparent-ui-overlay.css\"/>" + "\n";
                data2 += "<script type=\"text/javascript\" src=\"jquery/jquery-1.9.1.min.js\"></script>" + "\n";
                data2 += "<script type=\"text/javascript\" src=\"jquery/jquery.mobile-1.3.2.min.js\"></script>" + "\n";
                data2 += "<script src=\"js/marker.js\"></script>" + "\n";
                data2 += "<script type=\"text/javascript\" src=\"js/multiplepois.js\"></script>" + "\n";
                data2 += "</head>" + "\n";
                data2 += "<body>" + "\n";
                data2 += "</body>" + "\n";
                data2 += "<script>" + "\n";
                data2 += "var counter = 0;" + "\n";
                data2 += "function locationChanged(lat,lon,alt,acc)" + "\n";
                data2 += "{" + "\n";



                String tempLat ="";
                String tempLong ="";
                String distance = "";
                //Creates an AR Object in Wikitude for each coordinate
                for (int i = 0; i < mapCoordinates.size(); i++) {
                    if(!tempLat.equals(""+mapCoordinates.get(i).latitude) && !tempLong.equals(""+mapCoordinates.get(i).longitude)) {

                        tempLat = ""+mapCoordinates.get(i).latitude;
                        tempLong = ""+mapCoordinates.get(i).longitude;
                        distance = ""+ (4-(i/5));

                        data2 += "var tempLat" + i + " = " +mapCoordinates.get(i).latitude + ";" +"\n";
                        data2 += "var tempLon" + i + " = " +mapCoordinates.get(i).longitude + ";" +"\n";
                        data2 += "var R" + i + " = 6378.137" +";" +"\n";
                        data2 += "var dLat" + i + " = tempLat" + i + " * Math.PI / 180 - lat * Math.PI / 180;" +"\n";
                        data2 += "var dLon" + i + " = tempLon" + i + " * Math.PI / 180 - lon * Math.PI / 180;" +"\n";
                        data2 += "var a" + i + " = Math.sin(dLat" + i + "/ 2) * Math.sin(dLat" + i + "/ 2) + Math.cos(lat * Math.PI / 180) * Math.cos(tempLat" + i + " * Math.PI / 180) * Math.sin(dLon" + i + "/ 2) * Math.sin(dLon" + i + "/ 2);" +"\n";
                        data2 += "var c" + i + " = 2 * Math.atan2(Math.sqrt(a" + i + "), Math.sqrt(1 - a" + i + "));" +"\n";
                        data2 += "var distance" + i + " =+ R" + i + " * c" + i + ";" +"\n";
                        data2 += "distance" + i + " = distance" + i + " * 1000;" +"\n";

                        data2 += "var marker_image" + i + " = new AR.ImageResource(\"assets/marker.png\");" + "\n";
                        data2 += "if (counter < 1) {" +"\n";
                        data2 += "var marker_loc" + i + " = new AR.GeoLocation(" + mapCoordinates.get(i).latitude +i+"," + mapCoordinates.get(i).longitude +",0);" + "\n";
                        data2 += "}else{" +"\n";
                        data2 += "var marker_loc" + i + " = new AR.GeoLocation((" + mapCoordinates.get(i).latitude +i+"-dLat),(" + mapCoordinates.get(i).longitude +"-dLon),0);" + "\n";
                        data2 += "}" +"\n";

                        data2 += "var marker_drawable" + i + " = new AR.ImageDrawable(marker_image" + i + "," + distance + ");" + "\n";
                        data2 += "var marker_object = new AR.GeoObject(marker_loc" + i + ",{" + "\n";
                        data2 += "drawables:{" + "\n";
                        data2 += "cam: [marker_drawable" + i + "]" + "\n";
                        data2 += "}" + "\n";
                        data2 += "});" + "\n";
                    }
                    else{}
                }
                data2 += "counter=1;" +"\n";
                data2 += "}" +"\n";
                data2 += "AR.context.onLocationChanged = locationChanged;" +"\n";
                data2 += "</script>" +"\n";
                data2 += "</html>" +"\n";

                try {
                    os2.write(data2.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    os2.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                /////////////////////////////////////////////////////////////////

                //Opens wikitude
                //openApp(MainActivity.this, "com.wikitude.sdksamples");
                //Opens the ARCoreNavigation app
                openApp(MainActivity.this, "uk.co.appoly.sceneform_example");




            }
        });

        //Listener from the dropdownmenu for the buildings to choose the destination coordinates
        chooseBuildings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(building.getSelectedItemId() == 0){ //Gebäude 1
                    mypoint.setLatitude(48.481516);
                    mypoint.setLongitude(9.185066);
                }
                else if(building.getSelectedItemId() == 1){ //Gebäude 1A
                    mypoint.setLatitude(48.480967);
                    mypoint.setLongitude(9.184488);
                }
                else if(building.getSelectedItemId() == 2){ //Gebäude 2
                    mypoint.setLatitude(48.482342);
                    mypoint.setLongitude(9.185774);
                }
                else if(building.getSelectedItemId() == 3){ //Gebäude 3
                    mypoint.setLatitude(48.481952);
                    mypoint.setLongitude(9.186801);
                }
                else if(building.getSelectedItemId() == 4){ //Gebäude 4
                    mypoint.setLatitude(48.481721);
                    mypoint.setLongitude(9.187470);
                }
                else if(building.getSelectedItemId() == 5){ //Gebäude 5
                    mypoint.setLatitude(48.482673);
                    mypoint.setLongitude(9.186064);
                }
                else if(building.getSelectedItemId() == 6){ //Gebäude 6
                    mypoint.setLatitude(48.482441);
                    mypoint.setLongitude(9.187848);
                }
                else if(building.getSelectedItemId() == 7){ //Gebäude 7
                    mypoint.setLatitude(48.482329);
                    mypoint.setLongitude(9.188776);
                }
                else if(building.getSelectedItemId() == 8){ //Gebäude 8
                    mypoint.setLatitude(48.483363);
                    mypoint.setLongitude(9.186348);
                }
                else if(building.getSelectedItemId() == 9){ //Gebäude 9
                    mypoint.setLatitude(48.482979);
                    mypoint.setLongitude(9.187711);
                }
                else if(building.getSelectedItemId() == 10){ //Gebäude 10
                    mypoint.setLatitude(48.483100);
                    mypoint.setLongitude(9.188462);
                }
                else if(building.getSelectedItemId() == 11){ //Gebäude 11
                    mypoint.setLatitude(48.483733);
                    mypoint.setLongitude(9.188022);
                }
                else if(building.getSelectedItemId() == 12){ //Gebäude 12
                    mypoint.setLatitude(48.483463);
                    mypoint.setLongitude(9.188752);
                }
                else if(building.getSelectedItemId() == 13){ //Gebäude 13
                    mypoint.setLatitude(48.483975);
                    mypoint.setLongitude(9.188719);
                }
                else if(building.getSelectedItemId() == 14){ //Gebäude 14
                    mypoint.setLatitude(48.483768);
                    mypoint.setLongitude(9.189417);
                }
                else if(building.getSelectedItemId() == 15){ //Gebäude 15
                    mypoint.setLatitude(48.483171);
                    mypoint.setLongitude(9.189427);
                }
                else if(building.getSelectedItemId() == 16){ //Gebäude 16
                    mypoint.setLatitude(48.482069);
                    mypoint.setLongitude(9.189567);
                }
                else if(building.getSelectedItemId() == 17){ //Gebäude 17
                    mypoint.setLatitude(48.481500);
                    mypoint.setLongitude(9.189309);
                }
                else if(building.getSelectedItemId() == 18){ //Gebäude 18
                    mypoint.setLatitude(48.481656);
                    mypoint.setLongitude(9.187968);
                }
                else if(building.getSelectedItemId() == 19){ //Gebäude 20
                    mypoint.setLatitude(48.481059);
                    mypoint.setLongitude(9.186101);
                }

                //Creates the route with the selected building as the destination
                onMapClick(mypoint);
            }
        });
    }

    private AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            ((TextView) parent.getChildAt(0)).setTextColor(Color.WHITE);
        }

        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    //Opens a different application
    public static boolean openApp(Context context, String packageName) {
        PackageManager manager = context.getPackageManager();
        try {
            Intent i = manager.getLaunchIntentForPackage(packageName);
            if (i == null) {
                return false;
                //throw new ActivityNotFoundException();
            }
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            context.startActivity(i);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }

    //Once the Map was loaded it enables and searches for the Location of the device
    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        map = mapboxMap;
        map.addOnMapClickListener(this);
        enableLocation();
    }

    //Checks if location permissions were granted and finds the current location
    private void enableLocation(){
        if(PermissionsManager.areLocationPermissionsGranted(this))
        {
           initializeLocationEngine();
           initializeLocationLayer();
        }else{
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    //Finds the location and calls the camera setter to point the map to the current location
    @SuppressWarnings("MissingPermission")
    private void initializeLocationEngine(){
        locationEngine = new LocationEngineProvider(this).obtainBestLocationEngineAvailable();
        locationEngine.setPriority(LocationEnginePriority.BALANCED_POWER_ACCURACY);
        locationEngine.activate();

        Location lastLocation = locationEngine.getLastLocation();
        if(lastLocation != null){
            originLocation = lastLocation;
            setCameraPosition(lastLocation);
        } else{
            locationEngine.addLocationEngineListener(this);
        }
    }

    //Locationlayer to track the current location
    @SuppressWarnings("MissingPermission")
    private void initializeLocationLayer(){
        locationLayerPlugin = new LocationLayerPlugin(mapView,map,locationEngine);
        locationLayerPlugin.setLocationLayerEnabled(true);
        locationLayerPlugin.setCameraMode(CameraMode.TRACKING);
        locationLayerPlugin.setRenderMode(RenderMode.COMPASS);

    }

    //Sets the camera position to the current location on the map
    private void setCameraPosition(Location location){
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),
                location.getLongitude()),13.0));
    }

    //When the map is clicked it creates a route from the current location to the clicked destination
    @Override
    public void onMapClick(@NonNull LatLng point) {

        if (destinationMarker != null) {
            map.removeMarker(destinationMarker);
        }

        destinationMarker = map.addMarker(new MarkerOptions().position(point));

        destinationPosition = Point.fromLngLat(point.getLongitude(), point.getLatitude());
        originPosition = Point.fromLngLat(originLocation.getLongitude(), originLocation.getLatitude());
        getRoute(originPosition, destinationPosition);

        startButton.setEnabled(true);
        startButton.setVisibility(View.VISIBLE);
    }

    //Creates the current route from current location and destination
    private void getRoute(Point origin, Point destination){
        NavigationRoute.builder(this)
                .profile(DirectionsCriteria.PROFILE_WALKING) //Change Here for car navigation
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        if(response.body() == null)
                        {
                            Log.e(TAG, "No routes found, check right user and access token");
                            return;
                        }else if (response.body().routes().size() == 0){
                            Log.e(TAG,"No routes found");
                            return;
                        }

                        currentRoute = response.body().routes().get(0);

                        if(navigationMapRoute != null){
                            navigationMapRoute.removeRoute();
                        }
                        else{
                            navigationMapRoute = new NavigationMapRoute(null,mapView,map);
                        }
                        navigationMapRoute.addRoute(currentRoute);
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                        Log.e(TAG, "Error:" + t.getMessage());
                    }
                });
    }

    @Override
    @SuppressWarnings("MissingPermission")
    public void onConnected() {
        locationEngine.requestLocationUpdates();
    }

    //Moves the camera every time the location changes
    @Override
    public void onLocationChanged(Location location) {
        if(location != null){
            originLocation = location;
            setCameraPosition(location);
        }
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        //Present toast or dialog
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if(granted){
            enableLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode,permissions,grantResults);
    }

    //Starts the mapView the locationengine and the locationlayer
    @Override
    @SuppressWarnings("MissingPermission")
    public void onStart() {
        super.onStart();
        if(locationEngine != null){
            locationEngine.requestLocationUpdates();
        }
        if(locationLayerPlugin != null){
            locationLayerPlugin.onStart();
        }
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    //Stops the mapView the locationengine and the locationlayer
    @Override
    public void onStop() {
        super.onStop();
        if(locationEngine != null){
            locationEngine.removeLocationUpdates();
        }
        if(locationLayerPlugin != null){
            locationLayerPlugin.onStop();
        }

        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(locationEngine != null){
            locationEngine.deactivate();
        }
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }


    //Starts a new Actitvity
    public void launchNewActivity(Context context, String packageName) {
        Intent intent = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.CUPCAKE) {
            intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        }
        if (intent == null) {
            try {
                intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse("market://details?id=" + packageName));
                context.startActivity(intent);
            } catch (android.content.ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
            }
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
}
