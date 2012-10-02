package cn.yo2.aquarium.toggleapn;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.util.Log;

public class FlashLightHelper {
	static final String TAG = "FlashLightHelper";
	
	
	private static Camera sCamera;
	private static boolean sFlashLightOn;
	
	public static boolean isFlashLightOn() {
		return sFlashLightOn;
	}
	
	public static void toggleFlashLight(boolean enabled) {
		Log.d(TAG, "toggleFlashLight: " + enabled);
		if (enabled) {
			if (sCamera == null) {
				sCamera = Camera.open();
				
				Parameters parameters = sCamera.getParameters();  
                parameters.setFlashMode(Parameters.FLASH_MODE_TORCH); 
                sCamera.setParameters(parameters);
                
                sFlashLightOn = true;
			}
		} else {
			if (sCamera != null) {
				Parameters parameters = sCamera.getParameters();
				parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
				
				sCamera.release();
				sCamera = null;
				
				sFlashLightOn = false;
			}
		}
	}
}
