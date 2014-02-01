// An application and service for pushing information from the phone
// to my Pebble watch.
// It will start the service on startup and leave it running, even
// if the app quits so the watch continues to get updates.
package org.beegle.gotthetimecompanion;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.content.Intent;

public class MainActivity extends Activity {
	
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // When debugging, uncomment this statement.
        // android.os.Debug.waitForDebugger();
        doWeatherUpdate();
        doBatteryUpdate();
    }
        
    @Override
    public void onDestroy() {
    	super.onDestroy();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void updateWeather(View view) {
		doWeatherUpdate();
	}
	
	public void updateBattery(View view) {
		doBatteryUpdate();
	}
	
	protected void doWeatherUpdate() {
		Intent intent = new Intent(MainActivity.this, org.beegle.gotthetimecompanion.PollingService.class);
		intent.setAction(PollingService.GET_WEATHER);
		startService(intent);
	}
	
	protected void doBatteryUpdate() {
		Intent intent = new Intent(MainActivity.this, org.beegle.gotthetimecompanion.PollingService.class);
		intent.setAction(PollingService.GET_BATTERY);
		startService(intent);
	}

}
