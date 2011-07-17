package cn.yo2.aquarium.toggleapn;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.android.internal.telephony.ITelephony;

public class ToggleApnAppWidgetProvider extends AppWidgetProvider {
	
    static final String TAG = "ToggleApnAppWidgetProvider";
    
    private static final int BUTTON_DATA = 0;

    @Override
    public void onEnabled(Context context) {
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(
        		new ComponentName(context, ToggleApnAppWidgetProvider.class),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    @Override
    public void onDisabled(Context context) {
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(
        		new ComponentName(context, ToggleApnAppWidgetProvider.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    /**
     * Receives and processes a button pressed intent or state change.
     *
     * @param context
     * @param intent  Indicates the pressed button.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        
        Log.d(TAG, "onReceive -> intent = " + intent);
        
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            sDataState.onActualStateChange(context, intent);
        } else if (intent.hasCategory(Intent.CATEGORY_ALTERNATIVE)) {
            Uri data = intent.getData();
            int buttonId = Integer.parseInt(data.getSchemeSpecificPart());
            if (buttonId == BUTTON_DATA) {
                sDataState.toggleState(context);
            } 
        } else {
            // Don't fall-through to updating the widget.  The Intent
            // was something unrelated or that our super class took
            // care of.
            return;
        }

        // State changes fall through
        updateWidget(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        // Update each requested appWidgetId
        RemoteViews view = buildUpdate(context, -1);

        for (int i = 0; i < appWidgetIds.length; i++) {
            appWidgetManager.updateAppWidget(appWidgetIds[i], view);
        }
    }
	
	/**
     * Updates the widget when something changes, or when a button is pushed.
     *
     * @param context
     */
    public static void updateWidget(Context context) {
        RemoteViews views = buildUpdate(context, -1);
        // Update specific list of appWidgetIds if given, otherwise default to all
        final AppWidgetManager gm = AppWidgetManager.getInstance(context);
        gm.updateAppWidget(new ComponentName(context, ToggleApnAppWidgetProvider.class), views);
    }
    
    /**
     * Load image for given widget and build {@link RemoteViews} for it.
     */
    static RemoteViews buildUpdate(Context context, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(),
                R.layout.widget);
        views.setOnClickPendingIntent(R.id.btn_data, getLaunchPendingIntent(context, appWidgetId,
                BUTTON_DATA));

        updateButtons(views, context);
        return views;
    }
    
    /**
     * Creates PendingIntent to notify the widget of a button click.
     *
     * @param context
     * @param appWidgetId
     * @return
     */
    private static PendingIntent getLaunchPendingIntent(Context context, int appWidgetId,
            int buttonId) {
        Intent launchIntent = new Intent();
        launchIntent.setClass(context, ToggleApnAppWidgetProvider.class);
        launchIntent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        launchIntent.setData(Uri.parse("custom:" + buttonId));
        PendingIntent pi = PendingIntent.getBroadcast(context, 0 /* no requestCode */,
                launchIntent, 0 /* no flags */);
        return pi;
    }
    
    // This widget keeps track of two sets of states:
    // "3-state": STATE_DISABLED, STATE_ENABLED, STATE_INTERMEDIATE
    // "5-state": STATE_DISABLED, STATE_ENABLED, STATE_TURNING_ON, STATE_TURNING_OFF, STATE_UNKNOWN
    private static final int STATE_DISABLED = 0;
    private static final int STATE_ENABLED = 1;
    private static final int STATE_TURNING_ON = 2;
    private static final int STATE_TURNING_OFF = 3;
    private static final int STATE_UNKNOWN = 4;
    private static final int STATE_INTERMEDIATE = 5;
    
    private static final StateTracker sDataState = new DataStateTracker();
    
    /**
     * The state machine for Data, Wifi and Bluetooth toggling, tracking
     * reality versus the user's intent.
     *
     * This is necessary because reality moves relatively slowly
     * (turning on &amp; off radio drivers), compared to user's
     * expectations.
     */
    private abstract static class StateTracker {
        // Is the state in the process of changing?
        private boolean mInTransition = false;
        private Boolean mActualState = null;  // initially not set
        private Boolean mIntendedState = null;  // initially not set

        // Did a toggle request arrive while a state update was
        // already in-flight?  If so, the mIntendedState needs to be
        // requested when the other one is done, unless we happened to
        // arrive at that state already.
        private boolean mDeferredStateChangeRequestNeeded = false;

        /**
         * User pressed a button to change the state.  Something
         * should immediately appear to the user afterwards, even if
         * we effectively do nothing.  Their press must be heard.
         */
        public final void toggleState(Context context) {
            int currentState = getTriState(context);
            boolean newState = false;
            switch (currentState) {
                case STATE_ENABLED:
                    newState = false;
                    break;
                case STATE_DISABLED:
                    newState = true;
                    break;
                case STATE_INTERMEDIATE:
                    if (mIntendedState != null) {
                        newState = !mIntendedState;
                    }
                    break;
            }
            mIntendedState = newState;
            if (mInTransition) {
                // We don't send off a transition request if we're
                // already transitioning.  Makes our state tracking
                // easier, and is probably nicer on lower levels.
                // (even though they should be able to take it...)
                mDeferredStateChangeRequestNeeded = true;
            } else {
                mInTransition = true;
                requestStateChange(context, newState);
            }
        }

        /**
         * Update internal state from a broadcast state change.
         */
        public abstract void onActualStateChange(Context context, Intent intent);

        /**
         * Sets the value that we're now in.  To be called from onActualStateChange.
         *
         * @param newState one of STATE_DISABLED, STATE_ENABLED, STATE_TURNING_ON,
         *                 STATE_TURNING_OFF, STATE_UNKNOWN
         */
        protected final void setCurrentState(Context context, int newState) {
            final boolean wasInTransition = mInTransition;
            switch (newState) {
                case STATE_DISABLED:
                    mInTransition = false;
                    mActualState = false;
                    break;
                case STATE_ENABLED:
                    mInTransition = false;
                    mActualState = true;
                    break;
                case STATE_TURNING_ON:
                    mInTransition = true;
                    mActualState = false;
                    break;
                case STATE_TURNING_OFF:
                    mInTransition = true;
                    mActualState = true;
                    break;
            }

            if (wasInTransition && !mInTransition) {
                if (mDeferredStateChangeRequestNeeded) {
                    Log.v(TAG, "processing deferred state change");
                    if (mActualState != null && mIntendedState != null &&
                        mIntendedState.equals(mActualState)) {
                        Log.v(TAG, "... but intended state matches, so no changes.");
                    } else if (mIntendedState != null) {
                        mInTransition = true;
                        requestStateChange(context, mIntendedState);
                    }
                    mDeferredStateChangeRequestNeeded = false;
                }
            }
        }


        /**
         * If we're in a transition mode, this returns true if we're
         * transitioning towards being enabled.
         */
        public final boolean isTurningOn() {
            return mIntendedState != null && mIntendedState;
        }

        /**
         * Returns simplified 3-state value from underlying 5-state.
         *
         * @param context
         * @return STATE_ENABLED, STATE_DISABLED, or STATE_INTERMEDIATE
         */
        public final int getTriState(Context context) {
            if (mInTransition) {
                // If we know we just got a toggle request recently
                // (which set mInTransition), don't even ask the
                // underlying interface for its state.  We know we're
                // changing.  This avoids blocking the UI thread
                // during UI refresh post-toggle if the underlying
                // service state accessor has coarse locking on its
                // state (to be fixed separately).
                return STATE_INTERMEDIATE;
            }
            switch (getActualState(context)) {
                case STATE_DISABLED:
                    return STATE_DISABLED;
                case STATE_ENABLED:
                    return STATE_ENABLED;
                default:
                    return STATE_INTERMEDIATE;
            }
        }

        /**
         * Gets underlying actual state.
         *
         * @param context
         * @return STATE_ENABLED, STATE_DISABLED, STATE_ENABLING, STATE_DISABLING,
         *         or or STATE_UNKNOWN.
         */
        public abstract int getActualState(Context context);

        /**
         * Actually make the desired change to the underlying radio
         * API.
         */
        protected abstract void requestStateChange(Context context, boolean desiredState);
    }
    
    /**
     * Subclass of StateTracker to get/set Data state.
     */
    private static final class DataStateTracker extends StateTracker {
    	
        @Override
        public int getActualState(Context context) {
        	TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        	
        	if (telephony != null) {
        		return dataStateToFiveState(telephony.getDataState());
        	}
        	     	
            return STATE_UNKNOWN;
        }

        @Override
        protected void requestStateChange(Context context, boolean desiredState) {
            ITelephony telephony = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
            
            if (telephony != null) {
            	if (desiredState) {
            		try {
						telephony.enableDataConnectivity();
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
            	} else {
            		try {
						telephony.disableDataConnectivity();
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
            	}
            }
        }

        @Override
        public void onActualStateChange(Context context, Intent intent) {
            if (!ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                return;
            }
            
            setCurrentState(context, getActualState(context));
        }

        /**
         * Converts ITelephony's data state values into our
         * Data/Wifi/Bluetooth-common state values.
         */
        private static int dataStateToFiveState(int dataState) {
            switch (dataState) {
                case TelephonyManager.DATA_DISCONNECTED:
                    return STATE_DISABLED;
                case TelephonyManager.DATA_CONNECTED:
                    return STATE_ENABLED;
                case TelephonyManager.DATA_CONNECTING:
                    return STATE_TURNING_ON;
                
                default:
                    return STATE_UNKNOWN;
            }
        }
    }
    
    /**
     * Updates the buttons based on the underlying states of wifi, etc.
     *
     * @param views   The RemoteViews to update.
     * @param context
     */
    private static void updateButtons(RemoteViews views, Context context) {
        switch (sDataState.getTriState(context)) {
            case STATE_DISABLED:
                views.setImageViewResource(R.id.img_data,
                                           R.drawable.ic_appwidget_settings_data_off);
                views.setImageViewResource(R.id.ind_data,
                                           R.drawable.appwidget_settings_ind_off_l);
                break;
            case STATE_ENABLED:
                views.setImageViewResource(R.id.img_data,
                                           R.drawable.ic_appwidget_settings_data_on);
                views.setImageViewResource(R.id.ind_data,
                                           R.drawable.appwidget_settings_ind_on_l);
                break;
            case STATE_INTERMEDIATE:
                // In the transitional state, the bottom green bar
                // shows the tri-state (on, off, transitioning), but
                // the top dark-gray-or-bright-white logo shows the
                // user's intent.  This is much easier to see in
                // sunlight.
                if (sDataState.isTurningOn()) {
                    views.setImageViewResource(R.id.img_data,
                                               R.drawable.ic_appwidget_settings_data_on);
                    views.setImageViewResource(R.id.ind_data,
                                               R.drawable.appwidget_settings_ind_mid_l);
                } else {
                    views.setImageViewResource(R.id.img_data,
                                               R.drawable.ic_appwidget_settings_data_off);
                    views.setImageViewResource(R.id.ind_data,
                                               R.drawable.appwidget_settings_ind_off_l);
                }
                break;
        }
    }
}
