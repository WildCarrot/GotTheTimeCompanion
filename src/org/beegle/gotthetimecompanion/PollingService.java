// Polling service for getting information and pushing it
// to the GotTheTime watchface.
// Currently gets weather and phone battery information.

package org.beegle.gotthetimecompanion;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.LocationManager;
import android.os.IBinder;
import android.util.Log;
import com.getpebble.android.kit.*;
import com.getpebble.android.kit.util.PebbleDictionary;

public class PollingService extends Service {
	public final static String POLL_TYPE = "POLL_TYPE";
	public final static String GET_WEATHER = "GET_WEATHER";
	public final static String GET_BATTERY = "GET_BATTERY";
	
	private ScheduledThreadPoolExecutor threadPoolExec;
	private LocationManager locationMgr;

	public String getLocalClassName() {
		return this.getClass().getName();
	}
	
	@Override
	public void onCreate() {
        threadPoolExec = new ScheduledThreadPoolExecutor(10);
        locationMgr = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Log.d(getLocalClassName(), "onCreate");
        // Setup listening for pebble messages.  When the companion
        // watchface is loaded, it will ping this app to get updated
        // information.
        // XXX THis might leak, but we're not an activity, so it might not apply.
        PebbleKit.registerReceivedDataHandler(this, new PebbleKit.PebbleDataReceiver(WatchInfo.WATCH_APP_UUID) {
            @Override
            public void receiveData(Context context, int transactionId, PebbleDictionary data) {
        		PebbleKit.sendAckToPebble(getApplicationContext(), transactionId);
        		Log.d(getLocalClassName(), "Got ping from watch to update info");
            	doWeatherUpdate();
            	doBatteryUpdate();
            }
        });
	}
	
	protected void doWeatherUpdate() {
		// Helper method to trigger a weather lookup from within this class.
       	Intent intent = new Intent(this, org.beegle.gotthetimecompanion.PollingService.class);
       	intent.setAction(PollingService.GET_WEATHER);
       	startService(intent);
	}
	
	protected void doBatteryUpdate() {
		// Helper method to trigger a battery check from within this class.
       	Intent intent = new Intent(this, org.beegle.gotthetimecompanion.PollingService.class);
       	intent.setAction(PollingService.GET_BATTERY);
       	startService(intent);
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