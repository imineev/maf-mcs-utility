package com.oracle.maf.sample.mobile.utils;

import oracle.adf.model.datacontrols.device.DeviceManager;
import oracle.adf.model.datacontrols.device.DeviceManagerFactory;

/**
 * DeviceUtil provides convenience methods for accessing device information
 *
  * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class DeviceUtil {
    public DeviceUtil() {
        super();
    }
    
    private static final String NETWORK_STATUS_NONE = "none";
    
    public static final String VENDOR_APPLE   = "Apple";
    public static final String VENDOR_GOOGLE  = "Google";
    public static final String VENDOR_OTHER   = "Other";
    
    
    /**
     * Check if network access is available. 
     * @return true if there is WIFI or 3,4 GM network
     */
    public static boolean isOnline(){
                               
        boolean isNetwork = false;
        isNetwork = DeviceManagerFactory.getDeviceManager().isDeviceOnline();
        return isNetwork;
    }
    
    
    /**
     * verify device runs on IOS
     * @return true/false
     */
    public static boolean isIOS(){
        DeviceManager deviceManager = DeviceManagerFactory.getDeviceManager();        
        String _toUppercaseManufacturerOS = deviceManager.getOs().toUpperCase();        
        return _toUppercaseManufacturerOS.equals("IOS")?true:false;
    }
    
    /**
     * verify device runs Android
     * @return true/false
     */
    public static boolean isAndroid(){
        DeviceManager deviceManager = DeviceManagerFactory.getDeviceManager();        
        String _toUppercaseManufacturerOS = deviceManager.getOs().toUpperCase();        
        return _toUppercaseManufacturerOS.equals("ANDROID")?true:false;
    }
    
    /**
     * Checks if the device supports GEO location retireval, which could be disabled by the device not supporting 
     * GEO locations or the application missing the required permission
     * 
     * @return true if GEO location data can be accessed. False otherwise
     */
    public static boolean isGeoLocationAvailable(){
        return DeviceManagerFactory.getDeviceManager().hasGeolocation();
    }
    
    /**
     * Is it possible to read from the file system?
     * @return true / false
     */
    public static boolean hasFileAccess(){
        return DeviceManagerFactory.getDeviceManager().hasFileAccess();
    }
    
    /**
     *
     * Determibes manufacturer based on OS
     * @return Apple for iOS, Google for Android, Other for the rest
     */
    public static String getOsVendor(){
        
        DeviceManager deviceManager = DeviceManagerFactory.getDeviceManager();
        
        String _toUppercaseManufacturerOS = deviceManager.getOs().toUpperCase();
        
        String manufacturer = _toUppercaseManufacturerOS.equals("IOS") ? VENDOR_APPLE :
                              _toUppercaseManufacturerOS.contains("ANDROID") ? VENDOR_GOOGLE : VENDOR_OTHER;      
        return manufacturer;
        
    }
    
    /**
     * Detect the device operation system
     * @return
     */
    public static String getDeviceOS(){
       
        DeviceManager deviceManager = DeviceManagerFactory.getDeviceManager();
        return deviceManager.getOs();
        
    }
    
    public static String getDeviceOSVersion(){

        DeviceManager deviceManager = DeviceManagerFactory.getDeviceManager();
        return deviceManager.getVersion();        
    }
    
    
}
