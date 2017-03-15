package com.oracle.maf.sample.mcs.shared.mbe;

import com.oracle.maf.sample.mcs.shared.log.LibraryLogger;

import com.oracle.maf.sample.mcs.shared.mbe.config.base.MBEConfiguration;

import java.util.HashMap;

import oracle.adfmf.framework.exception.IllegalArgumentException;

/**
 * The Mobile Backend Manager (MBM) creates Mobile Backend (MBE) instances. Mobile backend instances are configured
 * from a Java configuration object - MBEConfiguration. The MBEManager maintains a list of MBE instances that have
 * been instantiated before.
 * <p>
 * The MBEManager object is a singleton that can be configured in the MAF ApplicationController project or the View
 * Controller project based on the intended scope. If the MBEManager is configured on the ApplicationController, then
 * MBE instances it holds are available across MAF Features. In this case it is recommended to perform authentication
 * through the BasicAuthentication Service exposed on the MBE instance. If the MBEManager is configured in the View-
 * Controller project then its scope is per single Feature, which usually is for a 1-1 mapping between a MAF Features
 * and an MBE. In this case MAF Feature level declarative authentication can be used.
 * <p>
 * To create an MBE instance, MAF application developers call
 * <pre>
 * MBEManager.getManager().createOrRenewMobileBackend(unique_name, MBEConfiguration);
 * </pre>
 * <p>
 * The unique name in above code is used to reference the MBE instance from the MAF application, if e.g the MBE instance
 * needs to be accessed from different Java objects. The MBEConfiguration object needs to be created first and configured
 * with the minimum information requirered to connect to MCS. A good unique name for an MBE instance could be the MBE Id
 * that the MCS MBE in the cloud has
 * <p>
 * To retrieve an existin MBE instance saved in the MBEManager, MAF developers call
 * <pre>
 * MBE mbe = null;
 * boolean exist = MBEManager.getManager().existMobilBackendWithName(uinque_name);
 * if(exist){
 *    mbe = MBEManager.getManager().getMobileBackend(unique_name);
 * }
 * else{
 *    mbe = ... create ...
 * }
 * </pre>
 * </p>
 * MBE instances can be removed individually from the MBE manager or all at once if no longer needed. Based on the
 * configuration scope (most likely ApplicationController) removing MBE instance helps managing client resources.
 *
  * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public final class MBEManager {

    private String LOG_TAG = "";  
    private LibraryLogger utilLogger = new LibraryLogger();
    private static MBEManager manager = new MBEManager();
    private HashMap<String, MBE> backends = new HashMap<String, MBE>();

    //Singleton
    private MBEManager() {
        super();
    }

    public static MBEManager getManager() {
        return manager;
    }
    
    /**
     * Returns true if an instance of the MBE with the provided name exists in the internal cache. False otherwise
     * @param name the name of the MBE instance that was used when creating it
     * @return true/false
     */
    public boolean existMobilBackendWithName(String name) {
        boolean exists = false;
        
        //return true of name is a valid string value and if it exists in the 
        //map of MBE
        if(name != null && !name.isEmpty() && (backends.get(name)!=null)){
            exists = true;
        }
        
        return exists;
    }

     /**
     *
     * @param name the name of the MBE instance that was used when creating it
     * @return MBE instance or null if the instance could not be found in the internal cache. 
     * @throws IllegalArgumentException if the name of the MBE is passed in as null
     */
    public MBE getMobileBackend(String name) throws IllegalArgumentException {

        if(name == null){
            IllegalArgumentException illegalArgumentException = new  IllegalArgumentException();
            illegalArgumentException.setMessage("\"name\" cannot be null in call to getMobileBackendInstance");
            illegalArgumentException.setException(true);
            illegalArgumentException.setSeverity(IllegalArgumentException.ERROR);
            throw illegalArgumentException;
        }

        utilLogger.logInfo(LOG_TAG + "Obtaining backend with name: " + name, this.getClass().getSimpleName(),"getMobileBackendInstance");
        
        MBE mbe = null;

        //ensure that only a single thread can execute this routine
        synchronized (this) {
            mbe = backends.get(name);
            //if the mobile backend is cached for the session, retrieve existing
            //backend instance
            if (mbe != null) {
                utilLogger.logInfo(LOG_TAG + "Mobile backend found for name: " + name, this.getClass().getSimpleName(),"getMobileBackendInstance");
            }
        }
        return mbe;
    }
    
    /**
     * Creates or renews the settings of a mobile backend instance that represents a remote mobile backend (MBE) in Mobile Cloud Service.
     * 
     * @param name The name of the mobile backend as defined on the server
     * @param mbeConfig The MBEConfiguration object that holds the remote mobile backend access information
     * @return MBE
     * @throws IllegalArgumentException if the MBEConfiguration, name parameter or  restConnectionName is null
     */
    public MBE createOrRenewMobileBackend(String name, MBEConfiguration mbeConfig) throws IllegalArgumentException{
        
        //need to throw exception in case that no MBE Configuration is provided
        if(mbeConfig == null || name == null){
           IllegalArgumentException illegalArgumentException = new  IllegalArgumentException();
           illegalArgumentException.setMessage("MBEConfiguration object and name argument cannot be null in call to createOrRenewMobileBackendInstance");
           illegalArgumentException.setException(true);
           illegalArgumentException.setSeverity(IllegalArgumentException.ERROR);
           throw illegalArgumentException;
        }

        MBE mbe = new MBE(name, mbeConfig);
        backends.put(name, mbe);
        return mbe;
    }
    
    /**
     * Removes a mobile backen instance from the internal map
     * @param mobileBackendObjectName the name of a mobile backend object instance provided when creating it
     */
    public void releaseNamedMobileBackend(String mobileBackendObjectName){      
        this.backends.remove(mobileBackendObjectName);        
    }
    
    /**
     * Clears the internal MAP holding the MBE instances. You would call this from a mobile client to release memory object 
     * if you know that a MAF Feature using these instances is not be used for a longer time
     */
    public void releaseAllMobileBackend(){
        this.backends.clear();        
    }
}
