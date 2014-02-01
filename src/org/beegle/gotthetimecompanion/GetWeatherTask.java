// Get weather information and push it to the given watchface.

package org.beegle.gotthetimecompanion;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import org.beegle.gotthetimecompanion.WatchInfo;

class GetWeatherTask extends AsyncTask<Void, Void, Void> implements Runnable {
	
	private Context context; // For sending data to the Pebble.
	private Location loc; // Where to get weather for.
	
	public GetWeatherTask(Context appContext, Location location) {
		context = appContext;
		loc = location;
	}
	
	private void sendWeatherDataToWatch(int weatherIconId, int temperatureCelsius) {
		Log.d("SendWeatherActivity", String.format("%d %d", weatherIconId, temperatureCelsius));

		// Build up a Pebble dictionary containing the weather icon and the current temperature in degrees celsius
		PebbleDictionary data = new PebbleDictionary();
		data.addUint8(WatchInfo.WEATHER_ICON_KEY, (byte) weatherIconId);
		data.addString(WatchInfo.WEATHER_TEMP_KEY, String.format("%d\u00B0C", temperatureCelsius));

		// Send the assembled dictionary to the weather watch-app; this is a no-op if the app isn't running or is not
		// installed
		PebbleKit.sendDataToPebble(context, WatchInfo.WATCH_APP_UUID, data);
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
	public void run() {
		doInBackground((Void[]) null);
	}
	
	// @Override
	protected Void doInBackground(Void... args) {
        double latitude = loc.getLatitude();
        double longitude = loc.getLongitude();
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
        	Log.d("GetWeatherTask", "Exception getting weather, ignoring it and not crashing...");
//            throw new RuntimeException(e);
        }
        
        return null;
	}

}
