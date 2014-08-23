// Synchronize sending info to the Pebble so that more than
// one task doesn't try to do it at the same time.
package org.beegle.gotthetimecompanion;

import android.content.Context;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import org.beegle.gotthetimecompanion.WatchInfo;

class PebbleKitSender extends Object {

    private static PebbleKitSender instance = null; // The one and only.

    protected PebbleKitSender() {
    }

    public static PebbleKitSender getInstance() {
	if (instance == null) {
	    instance = new PebbleKitSender();
	}
	return instance;
    }

    public synchronized void sendData(Context ctx, PebbleDictionary dict) {
	PebbleKit.sendDataToPebble(ctx, WatchInfo.WATCH_APP_UUID, dict);
    }

};
