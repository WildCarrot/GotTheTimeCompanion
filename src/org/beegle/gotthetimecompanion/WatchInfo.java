// "Namespace" class for containing the watchface UUID and
// other static info that many tasks from the PollingService
// will need to push their information to the watch.
package org.beegle.gotthetimecompanion;

import java.util.UUID;

class WatchInfo extends Object {
    // Tuple keys for the watch communication.
    static final int PHONE_BATTERY_PCT_KEY = 0;
    static final int PHONE_BATTERY_CHARGE_KEY = 1;
    static final int PHONE_BATTERY_PLUG_KEY = 2;
    static final int PHONE_BATTERY_LOW = 3;
    static final int WEATHER_ICON_KEY = 4;
    static final int WEATHER_TEMP_KEY = 5;
    static final int TIMEZONE = 6;
    static final int SIGNAL_LEVEL_CELL = 7;
    static final int SIGNAL_LEVEL_WIFI = 8;
    static final int CELL_SERVICE_STATE = 9;

    // This UUID identifies the watch app
    static final UUID WATCH_APP_UUID = UUID.fromString("c5cec51c-276b-44ba-ae22-580e74a5ad21");
}
