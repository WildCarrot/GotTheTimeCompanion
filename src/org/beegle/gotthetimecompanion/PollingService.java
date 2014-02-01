package org.beegle.gotthetimecompanion;

// Polling service for getting information and pushing it
// to the GotTheTime watchface.
// Currently gets weather and phone battery information.

import java.util.concurrent.ScheduledThreadPoolExecutor;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.LocationManager;
import android.os.IBinder;
import org.beegle.gotthetimecompanion.GetWeatherTask;
import android.util.Log;

public class PollingService extends Service {
	public final static String POLL_TYPE = "POLL_TYPE";
	public final static String GET_WEATHER = "GET_WEATHER";
	public final static String GET_BATTERY = "GET_BATTERY";
	
	private ScheduledThreadPoolExecutor threadPoolExec;
	private LocationManager locationMgr;

	
	@Override
	public void onCreate() {
        threadPoolExec = new ScheduledThreadPoolExecutor(10);
        locationMgr = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Log.d("PollingService", "onCreate");
	}
	
	@Override
	public void onDestroy() {
		// Make sure no tasks are running and destroy the threadpool.
		threadPoolExec.shutdown();
		// XXX More to do?
		
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String action = intent.getAction();
		if (action == GET_WEATHER) {
			Log.d("PollingService", "GetWeather");
			String locationProvider = locationMgr.getBestProvider(new Criteria(), true);
			if (locationProvider != null) {
				GetWeatherTask weatherTask = new GetWeatherTask(getApplicationContext(),
															locationMgr.getLastKnownLocation(locationProvider));
				threadPoolExec.scheduleWithFixedDelay(weatherTask,
							0 /* initial delay */,
							1 /* delay */,
							java.util.concurrent.TimeUnit.HOURS);
			}
		}
		else if (action == GET_BATTERY) {
			Log.d("PollingService", "GetBattery");
			GetBatteryTask batteryTask = new GetBatteryTask(getApplicationContext());
			threadPoolExec.scheduleWithFixedDelay(batteryTask,
					0 /* initial delay */,
					30 /* delay */,
					java.util.concurrent.TimeUnit.MINUTES);
		}
		
		super.onStartCommand(intent, flags, startId);
		return START_STICKY;
	}
}