package cn.yo2.aquarium.toggleapn;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class WhiteScreenActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Window window = getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
	    lp.screenBrightness = 1;     // set to full bright
	    lp.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
	    window.setAttributes(lp);
		
		setContentView(R.layout.white_screen);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		
		if (!isFinishing()) {
			Slog.d("onStop but not finishing, try finish");
			finish();
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		Slog.d("onDestroy called");
	}
}
