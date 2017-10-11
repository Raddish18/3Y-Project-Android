package com.SDH3.VCA;

import android.bluetooth.BluetoothAdapter;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.text.Html;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.Manifest;
import android.widget.TextView;
import android.widget.Toast;

import com.integreight.onesheeld.sdk.OneSheeldConnectionCallback;
import com.integreight.onesheeld.sdk.OneSheeldDevice;
import com.integreight.onesheeld.sdk.OneSheeldManager;
import com.integreight.onesheeld.sdk.OneSheeldScanningCallback;
import com.integreight.onesheeld.sdk.OneSheeldSdk;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    //Layout references
    LinearLayout home_view;
    LinearLayout gps_view;
    LinearLayout weather_view;


    //Location
    LocationServicesManager locationServicesManager;

    //Weather var
    TextView cityField, detailsField, currentTemperatureField, humidity_field, weatherIcon, updatedField;

    Typeface weatherFont;


    // connected to a OneSheeld?
    private boolean connected = false;

    //GUI
    private Button scanButton;
    private Switch toggleLights;
    private Switch toggleHeating;
    private Button disconnectButton;
    private Button getGPS_button;


    //Sheeld
    private OneSheeldManager manager;
    private OneSheeldDevice sheeldDevice;

    private final int MY_PERMISSIONS_REQUEST_LOCATION = 123456789;

    private OneSheeldScanningCallback scanningCallback = new OneSheeldScanningCallback() {
        @Override
        public void onDeviceFind(OneSheeldDevice device){
            //cancel further scanning
            manager.cancelScanning();
            //connect to first-found oneSheeld
            device.connect();
        }
    };

    private OneSheeldConnectionCallback connectionCallback = new OneSheeldConnectionCallback() {
        @Override
        public void onConnect(OneSheeldDevice device) {
            sheeldDevice = device;

            // when a connection is established, enable device-specific buttons
            runOnUiThread(new Runnable() {@Override public void run()
            {
                toggleHeating.setEnabled(true);
                toggleLights.setEnabled(true);
                disconnectButton.setEnabled(true);
            }
            });
        }

        public void onDisconnect(OneSheeldDevice device) {
            sheeldDevice = null;

            //when a disconnect occurs, make sure all device-specific buttons are disabled
            runOnUiThread(new Runnable() {@Override public void run()
            {
                toggleHeating.setEnabled(false);
                toggleLights.setEnabled(false);
            }
            });
        }

    };

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        OneSheeldSdk.init(this);
        OneSheeldSdk.setDebugging(true);
        manager = OneSheeldSdk.getManager();
        manager.setConnectionRetryCount(1);
        manager.setScanningTimeOut(5);
        manager.setAutomaticConnectingRetriesForClassicConnections(true);
        // add callback functions for handling connections / scanning
        manager.addConnectionCallback(connectionCallback);
        manager.addScanningCallback(scanningCallback);

        // GUI SETUP
        setupGUI();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        //location services init
        boolean success = locationServicesInit();

        //Weather
        weatherServicesInit();
        if (success) weatherReport();
    }

    private void weatherServicesInit() {
        weatherFont = Typeface.createFromAsset(getApplicationContext().getAssets(), "fonts/weathericons-regular-webfont.ttf");
        Button refresh = (Button) findViewById(R.id.refresh_weather);
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                weatherReport();
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        // when a new layout needs to be shown, make all other included layout 'GONE',
        // and make the requested layout 'VISIBLE'
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            home_view.setVisibility(View.VISIBLE);
            weather_view.setVisibility(View.GONE);
            gps_view.setVisibility(View.GONE);

        } else if (id == R.id.nav_weather) {
            home_view.setVisibility(View.GONE);
            weather_view.setVisibility(View.VISIBLE);
            gps_view.setVisibility(View.GONE);

        } else if (id == R.id.nav_gps) {
            home_view.setVisibility(View.GONE);
            weather_view.setVisibility(View.GONE);
            gps_view.setVisibility(View.VISIBLE);

        } else if (id == R.id.nav_game) {

        } else if (id == R.id.nav_to) {

        } else if (id == R.id.nav_shop) {

        }
        else if (id == R.id.nav_taxi) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    //cleanly disconnect all devices if app is nearly destruction
    @Override
    protected void onDestroy(){
        manager.disconnectAll();
        manager.cancelConnecting();
        manager.cancelScanning();

        super.onDestroy();
    }

    public boolean checkLocationPermission(){
        boolean granted = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (shouldShowRequestPermissionRationale( Manifest.permission.ACCESS_FINE_LOCATION)) {

                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                    Toast.makeText(this,
                            "Location required to connect with bluetooth.", Toast.LENGTH_LONG).show();

                } else {

                    // No explanation needed, we can request the permission.
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            MY_PERMISSIONS_REQUEST_LOCATION);

                    // MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            }
            else
                granted = true;
        }

        return granted;
    }

    public boolean checkBlueTooth(){
        boolean active = false;

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isEnabled()) {
                active = true;
            }
            else
                Toast.makeText(this,"Turn on bluetooth to connect.", Toast.LENGTH_LONG).show();
        }
        else
            Toast.makeText(this,"Bluetooth is not supported on this device.", Toast.LENGTH_LONG).show();

        return active;
    }
    // GUI SETUP
    public void setupGUI() {
        // initialise included-layout references
        gps_view = (LinearLayout) findViewById(R.id.gps_include_tag);
        home_view = (LinearLayout) findViewById(R.id.home_layout);
        weather_view = (LinearLayout) findViewById(R.id.weather_id);

        getGPS_button = (Button) findViewById(R.id.getCoords_button);
        getGPS_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Location l = null;
                l = locationServicesManager.getLastLocation();

                String message;
                if (l != null ) {
                    message = "Your location is: Lat: " + l.getLatitude()
                            + ", Lon: " + l.getLongitude();

                    Toast.makeText(getApplicationContext(),
                            message,
                            Toast.LENGTH_LONG).show();
                }
            }}
        );


        scanButton = (Button) findViewById(R.id.scanButton);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            //cancel all existing scans / connections in progress befroe rescanning
            public void onClick(View v) {
                if(checkLocationPermission() && checkBlueTooth()) {
                    manager.cancelScanning();
                    manager.cancelConnecting();
                    manager.scan();
                }
            }
        });

        //disconnects all devices
        disconnectButton = (Button) findViewById(R.id.disconnectButton);
        disconnectButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                manager.disconnectAll();
                disconnectButton.setEnabled(false);
                toggleHeating.setEnabled(false);
                toggleLights.setEnabled(false);
            }
        });

        // add heating toggling functionality
        toggleHeating = (Switch) findViewById(R.id.toggle_heating);
        toggleHeating.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                // pin-3 is treated as the "heating" pin
                if (isChecked) sheeldDevice.digitalWrite(3, true);
                else sheeldDevice.digitalWrite(3, false);

            }
        });

        // add lighting toggling functionality
        toggleLights = (Switch) findViewById(R.id.toggle_lights);
        toggleLights.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // pin 4 is treated as the "lighting" pin
                if (isChecked) sheeldDevice.digitalWrite(4, true);
                else sheeldDevice.digitalWrite(4, false);
            }
        });

        // all device-specific buttons are disabled by default
        toggleHeating.setEnabled(false);
        toggleLights.setEnabled(false);
        disconnectButton.setEnabled(false);
    }


    //start location services
    public boolean locationServicesInit(){
        locationServicesManager = new LocationServicesManager(this);
        return locationServicesManager.locationManagerInit();
    }

    //get weather report for current lcoation
    public void weatherReport(){
        cityField = (TextView)findViewById(R.id.city_field);
        updatedField = (TextView)findViewById(R.id.updated_field);
        detailsField = (TextView)findViewById(R.id.details_field);
        currentTemperatureField = (TextView)findViewById(R.id.current_temperature_field);
        humidity_field = (TextView)findViewById(R.id.humidity_field);
        weatherIcon = (TextView)findViewById(R.id.weather_icon);
        weatherIcon.setTypeface(weatherFont);


        Location l = locationServicesManager.getLastLocation();
        weatherFunction.placeIdTask asyncTask = new weatherFunction.placeIdTask(new weatherFunction.AsyncResponse() {
            public void processFinish(String weather_city, String weather_description, String weather_temperature, String weather_humidity, String weather_updatedOn,String icon, String sun_rise) {
                cityField.setText(weather_city);
                updatedField.setText(weather_updatedOn);
                detailsField.setText(weather_description);
                currentTemperatureField.setText(weather_temperature);
                humidity_field.setText(getString(R.string.humid) + weather_humidity);
                weatherIcon.setText(Html.fromHtml(icon));
            }
        });

        //For parsing longitude and latitude (doubles) into Strings
        if (l != null) {
            double lat = l.getLatitude();
            double lon = l.getLongitude();
            String latS = String.valueOf(lat);
            String lonS = String.valueOf(lon);
            asyncTask.execute(latS, lonS);
        }
    }
}