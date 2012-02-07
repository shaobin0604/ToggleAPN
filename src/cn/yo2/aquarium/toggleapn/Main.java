package cn.yo2.aquarium.toggleapn;

import android.app.Activity;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;

import com.android.internal.telephony.ITelephony;

public class Main extends Activity implements OnClickListener {
	
	private ITelephony mITelephony;
	private CheckBox mDataToggle;
	private ConnectivityManager mConnectivityManager;
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mDataToggle = (CheckBox) findViewById(R.id.data_toggle);
        mDataToggle.setOnClickListener(this);
        
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ECLAIR_MR1) {
        	mITelephony = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        } else {
        	mConnectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        }
    }
    
    
    
    
    public void onClick(View v) {
		if (v == mDataToggle) {
			if (mDataToggle.isChecked()) {
				try {
					if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ECLAIR_MR1) {
						mITelephony.enableDataConnectivity();
					} else {
						mConnectivityManager.setMobileDataEnabled(true);
					}
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				try {
					if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ECLAIR_MR1) {
						mITelephony.disableDataConnectivity();
					} else {
						mConnectivityManager.setMobileDataEnabled(false);
					}
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}



	@Override
    protected void onResume() {
    	super.onResume();
    	
    	try {
			boolean possible;
			if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ECLAIR_MR1) {
				possible = mITelephony.isDataConnectivityPossible();
			} else {
				possible = mConnectivityManager.getMobileDataEnabled();
			}
			
			mDataToggle.setChecked(possible);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}