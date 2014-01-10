package org.beegle.gotthetimecompanion;

import android.os.BatteryManager;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.content.Intent;
import android.content.IntentFilter;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
//import android.location.LocationListener;
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

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
	// Tuple keys for the watch communication.
    private static final int WEATHER_ICON_KEY = 0;
    private static final int WEATHER_TEMP_KEY = 1;
	private static final int PHONE_BATTERY_PCT_KEY = 2;
	private static final int PHONE_BATTERY_CHARGE_KEY = 3;
	private static final int PHONE_BATTERY_PLUG_KEY = 4;
	
    // This UUID identifies the watch app
	private static final UUID WATCH_APP_UUID = UUID.fromString("c5cec51c-276b-44ba-ae22-580e74a5ad21");
	
	private LocationManager locationMgr;
	private ScheduledThreadPoolExecutor threadPoolExec;
	private UpdateWatchAction updateWatch;
	
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        locationMgr = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        updateWatch = new UpdateWatchAction();
        updateWatch.activity = this;
        threadPoolExec = new ScheduledThreadPoolExecutor(10);
        threadPoolExec.scheduleWithFixedDelay(updateWatch, 0/* initial delay*/, 60 /*delay*/, TimeUnit.MINUTES);
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public class UpdateWatchAction extends Object implements Runnable {
		protected MainActivity activity;
		public void run() {
			activity.doWeatherUpdate();
			activity.sendBatteryToWatch();
		}
	}
	
	public void updateWeather(View view) {
		doWeatherUpdate();
	}
	
	protected void doWeatherUpdate() {
		String locationProvider = locationMgr.getBestProvider(new Criteria(), true);
		if (locationProvider == null) {
			Toast.makeText(getApplicationContext(), "No location services enabled.",
					Toast.LENGTH_LONG).show();
		}
		else {
			new GetWeatherTask().execute(locationMgr.getLastKnownLocation(locationProvider));
		}
	}
	
class GetWeatherTask extends AsyncTask<Location, Void, Void> {
		
		private void sendWeatherDataToWatch(int weatherIconId, int temperatureCelsius) {
			Log.d("SendWeatherActivity", String.format("%d %d", weatherIconId, temperatureCelsius));

			// Build up a Pebble dictionary containing the weather icon and the current temperature in degrees celsius
			PebbleDictionary data = new PebbleDictionary();
			data.addUint8(WEATHER_ICON_KEY, (byte) weatherIconId);
			data.addString(WEATHER_TEMP_KEY, String.format("%d\u00B0C", temperatureCelsius));

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
	
	public void sendBattery(View view) {
		sendBatteryToWatch();
	}
	
	protected void sendBatteryToWatch() {
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent batteryStatus = getApplicationContext().registerReceiver(null, ifilter);
		
		int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		//int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
		//float batteryPct = level / (float)scale;
		
		int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
		boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
		                     status == BatteryManager.BATTERY_STATUS_FULL;
		
		int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
		boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
		boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
				
		Log.d("SendBatteryActivity", String.format("%d %d %d", level,
				(byte)(isCharging?1:0), (byte)((usbCharge || acCharge)?1:0)));
		
		PebbleDictionary data = new PebbleDictionary();
		data.addUint8(PHONE_BATTERY_PCT_KEY, (byte) level);
		data.addUint8(PHONE_BATTERY_CHARGE_KEY, (byte) (isCharging? 1: 0));
		data.addUint8(PHONE_BATTERY_PLUG_KEY, (byte) ((usbCharge || acCharge)? 1: 0));

		// Send the assembled dictionary to the weather watch-app; this is a no-op if the app isn't running or is not
		// installed
		PebbleKit.sendDataToPebble(getApplicationContext(), WATCH_APP_UUID, data);
	}

}
