package com.oracle.maf.sample.mcs.shared.mbe.constants;


/**
 *
 * Constants used with Mobile Cloud Service (MCS) Mobile Backends (MBE)
 *
 * @author   Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public final class MBEConstants {  
    
    public final static String BASIC_AUTH = "basic";

    public final static String  OAUTH_AUTH = "oauth";
            
    /**
     * Enable analytics events
     */
    public final static boolean ANALYITCS_ENABLED = true;
    
    /**
     * Disable analytic events
     */
    public final static boolean ANALYITCS_DISABLED = false;
    
     /**
     * Enable log writing
     */
    public final static boolean LOGGING_ENABLED = true;
    
    /**
     * Disable log writing
     */
    public final static boolean LOGGING_DISABLED = false;
    
    //constructor
    private MBEConstants() {}
}
