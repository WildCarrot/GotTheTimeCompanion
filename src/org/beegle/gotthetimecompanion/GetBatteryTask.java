// Task to get the phone battery information and send to the watch.
// This doesn't really need to be a Task since it doesn't do anything
// long running that might block, but it keeps it consistent with the
// other tasks that the PollingService can start.
package org.beegle.gotthetimecompanion;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.util.Log;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import org.beegle.gotthetimecompanion.PebbleKitSender;

class GetBatteryTask extends AsyncTask<Void, Void, Void> implements Runnable {

    private Context context; // For sending data to the Pebble.

    public GetBatteryTask(Context appContext) {
	context = appContext;
    }

    public class BatteryLevelReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
	    sendBatteryInfoFromIntent(intent);
	}
    };

    @Override
    public void run() {
	Log.d("GetBatteryTask", "Starting run()");
	doInBackground((Void[]) null);
	Log.d("GetBatteryTask", "Done with run()");
    }

    @Override
    protected Void doInBackground(Void... v) {
	// This could be made async, if we wanted to really register for a receiver.
	// That would let us update the battery when it changes, instead of on a schedule,
	// but since we're going to update the other polling services on a schedule,
	// this is okay for now.
	IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
	Intent batteryStatus = context.registerReceiver(null, ifilter);

	sendBatteryInfoFromIntent(batteryStatus);

	return null;
    }

    protected void sendBatteryInfoFromIntent(Intent batteryStatus) {
	int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
	// Here's how to get the percent if I ever want to send that instead.
	//int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
	//float batteryPct = level / (float)scale;

	int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
	boolean isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING ||
			      status == BatteryManager.BATTERY_STATUS_FULL);

	int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
	boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
	boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

	// XXX: Argh!  Intents!  I don't know how to tell if this is a battery low
	// or just and update.  This apparently isn't it.
	//	boolean battLow = (batteryStatus.action == ACTION_BATTERY_LOW);
	boolean battLow = false;

	sendBatteryToWatch(level, isCharging, (usbCharge || acCharge), battLow);
    }

    protected void sendBatteryToWatch(int level, boolean isCharging, boolean isPlugged, boolean isLow) {
	Log.d("GetBatteryTask", String.format("%d %d %d %d", level,
					      (byte)(isCharging?1:0),
					      (byte)(isPlugged?1:0),
					      (byte)(isLow?1:0)));

	PebbleDictionary data = new PebbleDictionary();
	data.addUint8(WatchInfo.PHONE_BATTERY_PCT_KEY, (byte) level);
	data.addUint8(WatchInfo.PHONE_BATTERY_CHARGE_KEY, (byte) (isCharging? 1: 0));
	data.addUint8(WatchInfo.PHONE_BATTERY_PLUG_KEY, (byte) (isPlugged? 1: 0));
	data.addUint8(WatchInfo.PHONE_BATTERY_LOW, (byte) (isLow? 1: 0));

	// Send the assembled dictionary to the weather watch-app; this is a no-op if the app isn't running or is not
	// installed
	PebbleKitSender.getInstance().sendData(context, data);
	//PebbleKit.sendDataToPebble(context, WatchInfo.WATCH_APP_UUID, data);
    }
}
