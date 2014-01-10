package org.beegle.gotthetimecompanion;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.support.v4.app.NavUtils;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.widget.Toast;
import android.util.Log;

import android.os.AsyncTask;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class SendWeatherActivity extends Activity {
    // the tuple key corresponding to the weather icon displayed on the watch
    private static final int ICON_KEY = 0;
    // the tuple key corresponding to the temperature displayed on the watch
    private static final int TEMP_KEY = 1;
    // This UUID identifies the weather app
	private static final UUID WATCH_APP_UUID = UUID.fromString("c5cec51c-276b-44ba-ae22-580e74a5ad21");
	
	private LocationManager locationMgr;
	private String mockLocationProvider;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_send_weather);
		// Show the Up button in the action bar.
		setupActionBar();
		Log.d("SendWeatherActivity", "created");
        locationMgr = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
	}
        
	public void updateWeather(View view) {
		Log.d("SendWeatherActivity", "updateWeather");
		// when this button is clicked, get the handset's approximate location and request weather data from a
		// third-party web service
		LocationListener locationListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				Log.d("SendWeatherActivity", "Location changed");
				locationMgr.removeUpdates(this);
				doWeatherUpdate(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };	
            
        String locationProvider = locationMgr.getBestProvider(new Criteria(), true);
            
        mockLocationProvider = LocationManager.GPS_PROVIDER;
        locationMgr.addTestProvider(mockLocationProvider, 
        		/* requiresNetwork*/ false, 
        		/* requiresSatellite*/ false, 
        		/* requiresCell*/ false, 
        		/* hasMonetaryCost*/ false, 
        		/* supportsAltitude*/ true, 
        		/* supportsSpeed */ true, 
        		/* supportsBearing */ true, 
        		/* powerRequirement*/ 0, 
        		/* accuracy */ 5);
        locationMgr.setTestProviderEnabled(mockLocationProvider, true);
        
        if (locationProvider == null) {
    		Log.d("SendWeatherActivity", "no location services enabled");

        	Toast.makeText(getApplicationContext(), "No location services enabled.",
        					Toast.LENGTH_LONG).show();
        } else {
        	Log.d("SendWeatherActivity", "requesting location updates and updating with last known loc");
        	doWeatherUpdate(locationMgr.getLastKnownLocation(locationProvider));
        	locationMgr.requestLocationUpdates(locationProvider, 0, 0, locationListener);
        	locationMgr.requestLocationUpdates(mockLocationProvider, 0, 0, locationListener);
        }
	}
	class GetWeatherTask extends AsyncTask<Location, Void, Void> {
		
		private void sendWeatherDataToWatch(int weatherIconId, int temperatureCelsius) {
			Log.d("SendWeatherActivity", String.format("%d %d", weatherIconId, temperatureCelsius));

			// Build up a Pebble dictionary containing the weather icon and the current temperature in degrees celsius
			PebbleDictionary data = new PebbleDictionary();
			data.addUint8(ICON_KEY, (byte) weatherIconId);
			data.addString(TEMP_KEY, String.format("%d\u00B0C", temperatureCelsius));

			// Send the assembled dictionary to the weather watch-app; this is a no-op if the app isn't running or is not
			// installed
			PebbleKit.sendDataToPebble(getApplicationContext(), WATCH_APP_UUID, data);
		}
		
		private int getIconFromWeatherId(int weatherId) {
	        if (weatherId < 600) {
	            return 2;
	        } else if (weatherId < 700) {
	            return 3;
	        } else if (weatherId > 800) {
	            return 1;
	        } else {
	            return 0;
	        }
		}
		
		// @Override
		protected Void doInBackground(Location... locations) {
	        double latitude = locations[0].getLatitude();
	        double longitude = locations[0].getLongitude();
	        Log.d("GetWeatherTask", String.format("%f %f", latitude, longitude));
	        
	        try {
	            URL u = new URL(String.format("http://api.openweathermap.org/data/2.1/find/city?lat=%f&lon=%f&cnt=1",
	                    latitude,
	                    longitude));

	            HttpURLConnection urlConnection = (HttpURLConnection) u.openConnection();
	            try {
	                BufferedReader reader =
	                        new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));
	                String json = reader.readLine();
	                Log.d("GetWeatherTask", json);

	                JSONObject jsonObject = new JSONObject(json);
	                JSONObject l = jsonObject.getJSONArray("list").getJSONObject(0);
	                JSONObject m = l.getJSONObject("main");
	                double temperature = m.getDouble("temp");
	                int wtype = l.getJSONArray("weather").getJSONObject(0).getInt("id");

	                int weatherIcon = getIconFromWeatherId(wtype);
	                int temp = (int) (temperature - 273.15);

	                sendWeatherDataToWatch(weatherIcon, temp);
	            } finally {
	                urlConnection.disconnect();
	            }

	            Log.d("GetWeatherTask", String.format("%f, %f", latitude, longitude));
	        } catch (Exception e) {
	            throw new RuntimeException(e);
	        }
	        
	        return null;
		}

	}
	
	public void doWeatherUpdate(Location location) {
		Log.d("SendWeatherActivity", "doWeatherUpdate");
        // A very sketchy, rough way of getting the local weather forecast from the phone's approximate location
        // using the OpenWeatherMap webservice: http://openweathermap.org/wiki/API/JSON_API
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        Log.d("SendWeatherActivity", String.format("%f %f", latitude, longitude));
        
        new GetWeatherTask().execute(location);

//        try {
//            URL u = new URL(String.format("http://api.openweathermap.org/data/2.1/find/city?lat=%f&lon=%f&cnt=1",
//                    latitude,
//                    longitude));
//
//            HttpURLConnection urlConnection = (HttpURLConnection) u.openConnection();
//            try {
//                BufferedReader reader =
//                        new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));
//                String json = reader.readLine();
//                Log.d("SendWeatherActivity", json);
//
//                JSONObject jsonObject = new JSONObject(json);
//                JSONObject l = jsonObject.getJSONArray("list").getJSONObject(0);
//                JSONObject m = l.getJSONObject("main");
//                double temperature = m.getDouble("temp");
//                int wtype = l.getJSONArray("weather").getJSONObject(0).getInt("id");
//
//                int weatherIcon = getIconFromWeatherId(wtype);
//                int temp = (int) (temperature - 273.15);
//
//                sendWeatherDataToWatch(weatherIcon, temp);
//            } finally {
//                urlConnection.disconnect();
//            }
//
//            Log.d("WeatherActivity", String.format("%f, %f", latitude, longitude));
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
    }

//	private int getIconFromWeatherId(int weatherId) {
//	        if (weatherId < 600) {
//	            return 2;
//	        } else if (weatherId < 700) {
//	            return 3;
//	        } else if (weatherId > 800) {
//	            return 1;
//	        } else {
//	            return 0;
//	        }
//	}
	
	
	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {

		getActionBar().setDisplayHomeAsUpEnabled(true);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.send_weather, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
