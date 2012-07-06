package cn.yo2.aquarium.toggleapn;

import android.net.ConnectivityManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ConnectivityHelper {
    
    private static Method sMethodGetMobileDataEnabled;
    private static Method sMethodSetMobileDataEnabled;
    
    static {
        initReflectionMethod();
    }
    
    private static void initReflectionMethod() {
        Class<ConnectivityManager> clazz = ConnectivityManager.class;
        try {
            sMethodGetMobileDataEnabled = clazz.getMethod("getMobileDataEnabled", new Class[0]);
            sMethodGetMobileDataEnabled.setAccessible(true);
            sMethodSetMobileDataEnabled = clazz.getMethod("setMobileDataEnabled", new Class[] { Boolean.TYPE });
            sMethodSetMobileDataEnabled.setAccessible(true);
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    /**
     * Gets the value of the setting for enabling Mobile data.
     *
     * @param manager the {@link ConnectivityManager}
     * 
     * @return Whether mobile data is enabled.
     */
    public static boolean getMobileDataEnabled(ConnectivityManager manager) {
        try {
            return (Boolean) sMethodGetMobileDataEnabled.invoke(manager, new Object[0]);
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Sets the persisted value for enabling/disabling Mobile data.
     *
     * @param manager the {@link ConnectivityManager}
     * 
     * @param enabled Whether the mobile data connection should be
     *            used or not.
     */
    public static void setMobileDataEnabled(ConnectivityManager manager, boolean enabled) {
        try {
            sMethodSetMobileDataEnabled.invoke(manager, new Object[] {Boolean.valueOf(enabled)});
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
