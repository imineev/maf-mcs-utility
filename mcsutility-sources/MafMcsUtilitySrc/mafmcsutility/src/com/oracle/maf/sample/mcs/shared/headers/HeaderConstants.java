package com.oracle.maf.sample.mcs.shared.headers;


/**
 *
 * Constants for MCS and REST HTTP headerString used in the MAF MCS UTIL library
 *
  * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class HeaderConstants {
    private HeaderConstants() {};
    
    
    /* *** HTTP HEADERS *** */
    
    /**
     * HTTP header to inform the server about the format of the payload sent with the request
     */
    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    
    
    public static final String TRANSFER_ENCODING_TYPE_HEADER = "Transfer-Encoding";
        
    /**
     * HTTP accept header indicating the response format accepted by this client
     */
    public static final String ACCEPT_HEADER = "Accept";
    
    /**
     * Used with Basic and OAuth authentication to hold the authorizationProvider token for the authenticated
     * user session
     */
    public static final String AUTHORIZATON_HEADER = "Authorization";
    
    
    public static final String CONTENT_LENGTH = "Content-Length";
    
    
    /* *** ORACLE MCS SPECIFIC HEADERS *** */
    
    
    /**
     * All MCS requests happen in the context of an Oracle Mobile Backend. For this all requests must have 
     * the ORACLE_MOBILE_BACKEND_ID set to a valid MBE Id
     */
    public static final String ORACLE_MOBILE_BACKEND_ID  = "Oracle-Mobile-Backend-Id";
    
    
    /**
     * The application key defined for Android and iOS applications in the MBE ui
     */
    public static final String APPLICATION_KEY_HEADER = "Oracle-Mobile-Application-Key";
    
}
