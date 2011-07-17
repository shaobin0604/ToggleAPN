package cn.yo2.aquarium.toggleapn;

import android.app.Activity;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;

import com.android.internal.telephony.ITelephony;

public class Main extends Activity implements OnClickListener {
	
	private ITelephony mITelephony;
	private CheckBox m_DataToggle;
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        m_DataToggle = (CheckBox) findViewById(R.id.data_toggle);
        m_DataToggle.setOnClickListener(this);
        
        mITelephony = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
    }
    
    
    
    public void onClick(View v) {
		if (v == m_DataToggle) {
			if (m_DataToggle.isChecked()) {
				try {
					mITelephony.enableDataConnectivity();
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				try {
					mITelephony.disableDataConnectivity();
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
			boolean possible = mITelephony.isDataConnectivityPossible();
			
			m_DataToggle.setChecked(possible);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}