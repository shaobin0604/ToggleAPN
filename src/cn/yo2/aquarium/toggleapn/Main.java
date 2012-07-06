
package cn.yo2.aquarium.toggleapn;

import android.app.Activity;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;

public class Main extends Activity implements OnClickListener {

    private CheckBox mDataToggle;
    private ConnectivityManager mConnectivityManager;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mDataToggle = (CheckBox) findViewById(R.id.data_toggle);
        mDataToggle.setOnClickListener(this);

        mConnectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
    }

    public void onClick(View v) {
        if (v == mDataToggle) {
            ConnectivityHelper.setMobileDataEnabled(mConnectivityManager, mDataToggle.isChecked());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDataToggle.setChecked(ConnectivityHelper.getMobileDataEnabled(mConnectivityManager));
    }
}
