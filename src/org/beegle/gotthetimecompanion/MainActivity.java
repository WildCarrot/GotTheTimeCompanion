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
        doSignalStrengthUpdate();
	doTimezoneUpdate();
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

    // These are for button presses to force an update of the values from the
    // phone to the watch.

    public void updateWeather(View view) {
	doWeatherUpdate();
    }

    public void updateBattery(View view) {
	doBatteryUpdate();
    }

    public void updateSignalStrength(View view) {
	doSignalStrengthUpdate();
    }

    // These actually do the work of getting values from the phone and sending
    // to the watch.

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

    protected void doSignalStrengthUpdate() {
	Intent intent = new Intent(MainActivity.this, org.beegle.gotthetimecompanion.PollingService.class);
	intent.setAction(PollingService.GET_SIGNAL_STRENGTH_CELL);
	startService(intent);
    }

    protected void doTimezoneUpdate() {
	Intent intent = new Intent(MainActivity.this, org.beegle.gotthetimecompanion.PollingService.class);
	intent.setAction(PollingService.GET_TIMEZONE);
	startService(intent);
    }
}
