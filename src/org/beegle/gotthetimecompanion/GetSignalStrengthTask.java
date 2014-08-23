// Task to get signal strength information and send to the watch.
// This doesn't really need to be a Task since it doesn't do anything
// long running that might block, but it keeps it consistent with the
// other tasks that the PollingService can start.
package org.beegle.gotthetimecompanion;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellSignalStrength;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.util.Log;

import java.util.List;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import org.beegle.gotthetimecompanion.PebbleKitSender;

class GetSignalStrengthTask extends AsyncTask<Void, Void, Void> implements Runnable {

    private Context context; // For sending data to the Pebble.
    private PhoneStateListener listener;

    public GetSignalStrengthTask(Context appContext) {
	context = appContext;
	TelephonyManager telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
	telephonyManager.listen(listener,
				PhoneStateListener.LISTEN_SIGNAL_STRENGTHS |
				PhoneStateListener.LISTEN_SERVICE_STATE);
    }

    public class MyPhoneStateListener extends PhoneStateListener {
	@Override
	public void onSignalStrengthsChanged(SignalStrength signalStrength) {
	    Log.d("MyPhoneStateListener", "Signal strength changed!");
	    super.onSignalStrengthsChanged(signalStrength);
	    if (signalStrength.isGsm()) {
		int level = signalStrength.getGsmSignalStrength();
		sendSignalStrengthToWatch(WatchInfo.SIGNAL_LEVEL_CELL, level);
	    }
	}
	public void onServiceStateChanged(ServiceState serviceState) {
	    boolean have_service = !(serviceState.getState() == ServiceState.STATE_OUT_OF_SERVICE ||
				     serviceState.getState() == ServiceState.STATE_POWER_OFF);
	    sendCellServiceStateToWatch(have_service);
	}
    };

    @Override
    public void run() {
	doInBackground((Void[]) null);
    }

    @Override
    protected Void doInBackground(Void... v) {
	// TelephonyManager telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
	// List<CellInfo> cellInfos = telephonyManager.getAllCellInfo();

	// int phoneType = telephonyManager.getPhoneType();
	// int networkType = telephonyManager.getNetworkType();

	// for (CellInfo info : cellInfos) {
	//     switch (phoneType) {
	// 	case TelephonyManager.PHONE_TYPE_GSM:
	// 	    CellInfoGsm gsm = (CellInfoGsm) info;
	// 	    sendSignalStrengthToWatch(WatchInfo.SIGNAL_LEVEL_CELL,
	// 				      gsm.getCellSignalStrength().getLevel());
	// 	    break;
	// 	default:
	// 	    break;
	//     }
	// }


	// In a sane world, cellInfo.getCellSignalStrength() would return you
	// the subclass of CellSignalStrength for the appropriate cell radio type.
	// In reality, it doesn't, and the CellInfo doesn't have a getCellSignalStrength()
	// method anyway.  WTF.
	//	for (CellInfo info : cellInfo) {
	    //	    if (info.
	//	CellInfoGsm gsm = (CellInfoGsm)cellInfo.get(0); // XXX TODO do all or get just the right one
	//	sendSignalStrengthCell(gsm.getCellSignalStrength());

	return null;
    }

    protected void sendSignalStrengthCell(CellSignalStrength sstrength) {
	int level = sstrength.getLevel();

	sendSignalStrengthToWatch(WatchInfo.SIGNAL_LEVEL_CELL, level);
    }

    protected void sendSignalStrengthToWatch(int signalType, int level) {
	Log.d("GetSignalStrengthTask", String.format("%d %d %d %d", signalType, level));

	PebbleDictionary data = new PebbleDictionary();
	data.addUint8(signalType, (byte) level);

	// Send the assembled dictionary to the weather watch-app; this is a no-op if the app isn't running or is not
	// installed
	PebbleKitSender.getInstance().sendData(context, data);
	//PebbleKit.sendDataToPebble(context, WatchInfo.WATCH_APP_UUID, data);
    }

    protected void sendCellServiceStateToWatch(boolean have_service) {
	Log.d("GetSignalStrengthTask", String.format("have_service? %d", (byte) (have_service?1:0)));
	PebbleDictionary data = new PebbleDictionary();
	data.addUint8(WatchInfo.CELL_SERVICE_STATE, (byte) (have_service?1:0));
	PebbleKitSender.getInstance().sendData(context, data);
    }
}
